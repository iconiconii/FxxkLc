package com.codetop.security;

import com.codetop.util.UserContext;
import com.codetop.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Rate limiting filter that intercepts requests and applies rate limits.
 * 
 * Features:
 * - Per-user rate limiting for authenticated requests
 * - Per-IP rate limiting for all requests
 * - Proper HTTP headers for rate limit status
 * - Graceful handling of rate limit exceptions
 * - Skips rate limiting for health checks and static resources
 * 
 * @author CodeTop Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Skip rate limiting for excluded paths
        if (shouldSkipRateLimit(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ipAddress = getClientIpAddress(request);
        Long userId = extractUserIdFromToken(request);

        boolean allowed;
        if (userId != null) {
            allowed = rateLimitService.isAllowed(userId, ipAddress);
        } else {
            allowed = rateLimitService.isAllowed(ipAddress);
        }

        if (!allowed) {
            handleRateLimitExceeded(request, response, userId, ipAddress);
            return;
        }

        // Add rate limit headers
        addRateLimitHeaders(response, userId);

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipRateLimit(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip for health checks and monitoring
        if (path.startsWith("/actuator/")) {
            return true;
        }
        
        // Skip for static resources
        if (path.startsWith("/static/") || path.startsWith("/css/") || 
            path.startsWith("/js/") || path.startsWith("/images/")) {
            return true;
        }
        
        // Skip for API documentation
        if (path.startsWith("/swagger-ui/") || path.startsWith("/api-docs/")) {
            return true;
        }

        return false;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        // Check various headers for the real IP address
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "X-Originating-IP",
            "CF-Connecting-IP",
            "True-Client-IP"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (StringUtils.hasText(ip) && !isUnknown(ip)) {
                // Take the first IP if there are multiple
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    private boolean isUnknown(String ip) {
        return ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip) || 
               "0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip);
    }

    private Long extractUserIdFromToken(HttpServletRequest request) {
        // Try to get user ID from UserContext (set by JwtAuthenticationFilter)
        Long userId = UserContext.getCurrentUserId();
        
        if (userId != null) {
            log.debug("Found user ID from UserContext: {}", userId);
            return userId;
        }
        
        // Fallback: try to get from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
            && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            log.debug("Found user ID from SecurityContext: {}", userPrincipal.getId());
            return userPrincipal.getId();
        }
        
        // No authenticated user found
        return null;
    }

    private void addRateLimitHeaders(HttpServletResponse response, Long userId) {
        if (userId != null) {
            int remaining = rateLimitService.getRemainingRequests(userId);
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        }
        
        long resetTime = Instant.now().getEpochSecond() + rateLimitService.getTimeUntilReset();
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
        response.setHeader("X-RateLimit-Limit", "100"); // Per-user limit
    }

    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, 
                                         Long userId, String ipAddress) throws IOException {
        
        log.warn("Rate limit exceeded for request: {} {} from IP: {}, User: {}", 
                request.getMethod(), request.getRequestURI(), ipAddress, userId);

        // Check if in violation backoff
        boolean inBackoff = rateLimitService.isInViolationBackoff(ipAddress);
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // Add standard rate limit headers
        response.setHeader("X-RateLimit-Remaining", "0");
        long resetTime = Instant.now().getEpochSecond() + rateLimitService.getTimeUntilReset();
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
        response.setHeader("Retry-After", String.valueOf(rateLimitService.getTimeUntilReset()));

        // Create error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "rate_limit_exceeded");
        errorResponse.put("message", "Too many requests. Please try again later.");
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        
        if (inBackoff) {
            errorResponse.put("backoff", true);
            errorResponse.put("message", "Rate limit violated multiple times. Extended backoff period active.");
        }

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}