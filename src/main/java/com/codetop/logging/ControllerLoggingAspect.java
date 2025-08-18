package com.codetop.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

/**
 * AOP aspect for logging all controller method calls.
 * Provides comprehensive request/response logging with performance metrics.
 * 
 * @author CodeTop Team
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ControllerLoggingAspect {

    private final ObjectMapper objectMapper;
    
    // Sensitive fields that should be masked in logs
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password", "token", "secret", "key", "credential", "authorization"
    );

    /**
     * Log all controller method calls with request/response details.
     */
    @Around("execution(* com.codetop.controller..*(..))")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // Get HTTP request information
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        
        String methodName = joinPoint.getSignature().toShortString();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Log request start
        if (request != null) {
            String httpMethod = request.getMethod();
            String requestUri = request.getRequestURI();
            String queryString = request.getQueryString();
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            log.info("Controller request started: class={}, method={}, httpMethod={}, uri={}, " +
                    "queryString='{}', clientIp={}, userAgent='{}'", 
                    className, methodName, httpMethod, requestUri, queryString, clientIp, userAgent);
            
            // Log request parameters (masked)
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                try {
                    String maskedArgs = maskSensitiveData(Arrays.toString(args));
                    log.debug("Controller request parameters: class={}, method={}, parameters={}", 
                            className, methodName, maskedArgs);
                } catch (Exception e) {
                    log.debug("Controller request parameters: class={}, method={}, parameterCount={}", 
                            className, methodName, args.length);
                }
            }
        } else {
            log.info("Controller method started: class={}, method={}", className, methodName);
        }

        Object result;
        boolean success = false;
        String errorMessage = null;
        
        try {
            // Execute the actual controller method
            result = joinPoint.proceed();
            success = true;
            
            // Log successful response
            long duration = System.currentTimeMillis() - startTime;
            
            if (result instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
                log.info("Controller request completed: class={}, method={}, httpStatus={}, " +
                        "duration={}ms, success={}", 
                        className, methodName, responseEntity.getStatusCode(), duration, success);
                
                // Log response body for debugging (masked)
                if (log.isDebugEnabled() && responseEntity.getBody() != null) {
                    try {
                        String responseBody = objectMapper.writeValueAsString(responseEntity.getBody());
                        String maskedResponse = maskSensitiveData(responseBody);
                        log.debug("Controller response body: class={}, method={}, response={}", 
                                className, methodName, 
                                maskedResponse.length() > 500 ? maskedResponse.substring(0, 500) + "..." : maskedResponse);
                    } catch (Exception e) {
                        log.debug("Controller response body: class={}, method={}, responseType={}", 
                                className, methodName, responseEntity.getBody().getClass().getSimpleName());
                    }
                }
            } else {
                log.info("Controller request completed: class={}, method={}, duration={}ms, success={}", 
                        className, methodName, duration, success);
            }
            
            return result;
            
        } catch (Exception e) {
            // Log error response
            long duration = System.currentTimeMillis() - startTime;
            errorMessage = e.getMessage();
            
            log.error("Controller request failed: class={}, method={}, duration={}ms, success={}, error={}", 
                    className, methodName, duration, success, errorMessage, e);
            
            throw e;
        }
    }

    /**
     * Extract client IP address from request, handling proxies and load balancers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String xRealIp = request.getHeader("X-Real-IP");
        String xClientIp = request.getHeader("X-Client-IP");
        
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // Return first IP if there are multiple
            return xForwardedFor.split(",")[0].trim();
        } else if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        } else if (xClientIp != null && !xClientIp.isEmpty() && !"unknown".equalsIgnoreCase(xClientIp)) {
            return xClientIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Mask sensitive data in logs.
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        String result = data;
        for (String sensitiveField : SENSITIVE_FIELDS) {
            // Simple regex to mask sensitive fields
            result = result.replaceAll("(?i)" + sensitiveField + "=[^,\\]\\}\\s]+", 
                    sensitiveField + "=***");
            result = result.replaceAll("(?i)\"" + sensitiveField + "\"\\s*:\\s*\"[^\"]+\"", 
                    "\"" + sensitiveField + "\":\"***\"");
        }
        
        return result;
    }
}