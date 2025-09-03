package com.codetop.controller;

import com.codetop.annotation.CurrentUserId;
import com.codetop.annotation.SimpleIdempotent;
import com.codetop.service.AuthService;
import com.codetop.config.CookieAuthProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.codetop.logging.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import com.codetop.dto.LoginRequestDTO;
import com.codetop.dto.RegisterRequestDTO;
import com.codetop.dto.AuthResponseDTO;
import com.codetop.dto.UserInfoDTO;
import com.codetop.dto.AuthStatusResponseDTO;

/**
 * Authentication controller handling user login, registration, and OAuth.
 * 
 * @author CodeTop Team
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and registration")
public class AuthController {

    private final AuthService authService;
    private final com.codetop.security.TokenCookieService tokenCookieService;
    private final CookieAuthProperties cookieProps;

    /**
     * User login with email/username and password.
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with credentials")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request, 
                                           HttpServletRequest httpRequest,
                                           jakarta.servlet.http.HttpServletResponse httpResponse) {
        TraceContext.setOperation("AUTH_LOGIN");
        
        long startTime = System.currentTimeMillis();
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        log.info("Login attempt: identifier='{}', clientIp={}, userAgent='{}'", 
                maskIdentifier(request.getIdentifier()), clientIp, userAgent);
        
        try {
            AuthService.AuthResult result = authService.authenticate(request.getIdentifier(), request.getPassword());
            
            long duration = System.currentTimeMillis() - startTime;
            TraceContext.setUserId(result.getUser().getId());
            
            log.info("Login successful: userId={}, identifier='{}', clientIp={}, duration={}ms", 
                    result.getUser().getId(), maskIdentifier(request.getIdentifier()), clientIp, duration);
            
            // Set auth cookies (HttpOnly) for browser-based clients
            tokenCookieService.writeAuthCookies(httpResponse, result.getAccessToken(), result.getRefreshToken());
            return ResponseEntity.ok(AuthResponseDTO.builder()
                    .accessToken(result.getAccessToken())
                    .refreshToken(result.getRefreshToken())
                    .tokenType("Bearer")
                    .expiresIn(result.getExpiresIn())
                    .user(UserInfo.from(result.getUser()))
                    .build());
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("Login failed: identifier='{}', clientIp={}, duration={}ms, error={}", 
                    maskIdentifier(request.getIdentifier()), clientIp, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * User registration - TEMPORARILY DISABLED
     */
    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register new user account")
    @SimpleIdempotent(
        operation = "USER_REGISTER",
        returnCachedResult = false
    )
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        log.warn("Registration attempt blocked - registration currently disabled");
        throw new RuntimeException("注册功能暂时关闭，请稍后再试");
    }

    /**
     * Refresh access token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponseDTO> refresh(@RequestBody(required = false) java.util.Map<String, Object> request,
                                                   jakarta.servlet.http.HttpServletRequest httpRequest,
                                                   jakarta.servlet.http.HttpServletResponse httpResponse) {
        String refreshToken = null;
        String tokenSource = "none";
        // 1) Try reading from request body
        if (request != null) {
            Object rt = request.get("refreshToken");
            if (rt instanceof String && !((String) rt).isBlank()) {
                refreshToken = (String) rt;
                tokenSource = "body";
            }
        }
        // 2) Fallback to cookie if body missing or blank
        if (refreshToken == null || refreshToken.isBlank()) {
            try {
                var cookies = httpRequest.getCookies();
                if (cookies != null) {
                    final String refreshName = cookieProps.getRefreshName();
                    for (jakarta.servlet.http.Cookie c : cookies) {
                        if (refreshName.equals(c.getName())) {
                            refreshToken = c.getValue();
                            tokenSource = "cookie";
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        log.info("AuthController.refresh: source={}, tokenLen={}", tokenSource, refreshToken == null ? 0 : refreshToken.length());
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Refresh token is required"
            );
        }

        AuthService.AuthResult result = authService.refreshToken(refreshToken);
        // Refresh cookies
        tokenCookieService.writeAuthCookies(httpResponse, result.getAccessToken(), result.getRefreshToken());
        return ResponseEntity.ok(AuthResponseDTO.builder()
                .accessToken(result.getAccessToken())
                .refreshToken(result.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(result.getExpiresIn())
                .user(UserInfo.from(result.getUser()))
                .build());
    }

    // Note: /auth/refresh-cookie has been removed in favor of unified /auth/refresh

    /**
     * User logout.
     */
    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout user and invalidate token")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @Valid @RequestBody LogoutRequest request,
                                      jakarta.servlet.http.HttpServletResponse httpResponse) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            authService.logout(token);
        }
        // Clear cookies regardless of header
        tokenCookieService.clearAuthCookies(httpResponse);
        return ResponseEntity.ok().build();
    }

    /**
     * Get current authenticated user information.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get information about the currently authenticated user")
    public ResponseEntity<UserInfo> getCurrentUser(@CurrentUserId Long userId) {
        log.info("Getting current user info for userId: {}", userId);
        var user = authService.getUserById(userId);
        return ResponseEntity.ok(UserInfo.from(user));
    }

    /**
     * Check authentication status.
     */
    @GetMapping("/status")
    @Operation(summary = "Auth status", description = "Check if current authentication is valid")
    public ResponseEntity<AuthStatusResponseDTO> status() {
        // This endpoint is accessible without authentication to check auth status
        // The JWT filter will validate the token if present
        return ResponseEntity.ok(AuthStatusResponseDTO.builder()
                .authenticated(false)
                .message("No authentication token provided")
                .build());
    }

    // DTOs

    @lombok.Data
    public static class LoginRequestDTO {
        @NotBlank(message = "Email or username is required")
        private String identifier;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @lombok.Data
    public static class RegisterRequestDTO {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        private String password;

        private String firstName;
        private String lastName;
        private String timezone;
    }

    @lombok.Data
    public static class RefreshTokenRequestDTO {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @lombok.Data
    public static class LogoutRequest {
        /**
         * 幂等性请求ID，用于防止重复登出
         */
        private String requestId;
    }

    @lombok.Data
    @lombok.Builder
    public static class AuthResponseDTO {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
        private UserInfo user;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String avatarUrl;
        private String timezone;
        private String authProvider;
        private Boolean emailVerified;

        public static UserInfo from(com.codetop.entity.User user) {
            return UserInfo.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .avatarUrl(user.getAvatarUrl())
                    .timezone(user.getTimezone())
                    .authProvider(user.getAuthProvider().name())
                    .emailVerified(user.getEmailVerified())
                    .build();
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class AuthStatusResponseDTO {
        private Boolean authenticated;
        private String message;
        private UserInfo user;
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
     * Mask sensitive identifier for logging (email/username).
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() <= 3) {
            return "***";
        }
        
        if (identifier.contains("@")) {
            // Email masking: show first 2 chars + *** + domain
            String[] parts = identifier.split("@");
            if (parts.length == 2 && parts[0].length() > 2) {
                return parts[0].substring(0, 2) + "***@" + parts[1];
            }
        } else {
            // Username masking: show first 2 and last 1 char
            if (identifier.length() > 3) {
                return identifier.substring(0, 2) + "***" + identifier.substring(identifier.length() - 1);
            }
        }
        
        return "***";
    }
}
