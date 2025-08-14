package com.codetop.controller;

import com.codetop.service.AuthService;
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

    /**
     * User login with email/username and password.
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with credentials")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.authenticate(request.getIdentifier(), request.getPassword());
        
        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(result.getAccessToken())
                .refreshToken(result.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(result.getExpiresIn())
                .user(UserInfo.from(result.getUser()))
                .build());
    }

    /**
     * User registration.
     */
    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register new user account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.RegisterRequest serviceRequest = new AuthService.RegisterRequest();
        serviceRequest.setUsername(request.getUsername());
        serviceRequest.setEmail(request.getEmail());
        serviceRequest.setPassword(request.getPassword());
        serviceRequest.setFirstName(request.getFirstName());
        serviceRequest.setLastName(request.getLastName());
        serviceRequest.setTimezone(request.getTimezone());

        AuthService.AuthResult result = authService.register(serviceRequest);
        
        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(result.getAccessToken())
                .refreshToken(result.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(result.getExpiresIn())
                .user(UserInfo.from(result.getUser()))
                .build());
    }

    /**
     * Refresh access token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthService.AuthResult result = authService.refreshToken(request.getRefreshToken());
        
        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(result.getAccessToken())
                .refreshToken(result.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(result.getExpiresIn())
                .user(UserInfo.from(result.getUser()))
                .build());
    }

    /**
     * User logout.
     */
    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout user and invalidate token")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.ok().build();
    }

    // DTOs

    @lombok.Data
    public static class LoginRequest {
        @NotBlank(message = "Email or username is required")
        private String identifier;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @lombok.Data
    public static class RegisterRequest {
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
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @lombok.Data
    @lombok.Builder
    public static class AuthResponse {
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
}