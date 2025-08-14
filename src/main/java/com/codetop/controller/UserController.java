package com.codetop.controller;

import com.codetop.entity.User;
import com.codetop.mapper.UserMapper;
import com.codetop.security.UserPrincipal;
import com.codetop.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * User controller for user profile and settings management.
 * 
 * @author CodeTop Team
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "User profile and settings")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final AuthService authService;
    private final UserMapper userMapper;

    /**
     * Get current user profile.
     */
    @GetMapping("/profile")
    @Operation(summary = "Get user profile", description = "Get current user's profile information")
    public ResponseEntity<UserProfile> getCurrentUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User user = authService.findById(userPrincipal.getId());
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        UserProfile profile = UserProfile.from(user);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update user profile.
     */
    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update current user's profile information")
    public ResponseEntity<UserProfile> updateUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        User updatedUser = authService.updateUserProfile(userPrincipal.getId(), request);
        UserProfile profile = UserProfile.from(updatedUser);
        return ResponseEntity.ok(profile);
    }

    /**
     * Get user preferences.
     */
    @GetMapping("/preferences")
    @Operation(summary = "Get user preferences", description = "Get current user's preferences and settings")
    public ResponseEntity<UserPreferences> getUserPreferences(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User user = authService.findById(userPrincipal.getId());
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        UserPreferences preferences = UserPreferences.from(user);
        return ResponseEntity.ok(preferences);
    }

    /**
     * Update user preferences.
     */
    @PutMapping("/preferences")
    @Operation(summary = "Update user preferences", description = "Update current user's preferences and settings")
    public ResponseEntity<UserPreferences> updateUserPreferences(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        
        User updatedUser = authService.updateUserPreferences(userPrincipal.getId(), request);
        UserPreferences preferences = UserPreferences.from(updatedUser);
        return ResponseEntity.ok(preferences);
    }

    /**
     * Get user statistics.
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get user statistics", description = "Get comprehensive user statistics")
    public ResponseEntity<UserStatistics> getUserStatistics(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            UserStatistics statistics = userMapper.getUserStatistics(userPrincipal.getId());
            if (statistics == null) {
                // Return empty statistics for new users
                statistics = UserStatistics.builder()
                        .totalProblemsAttempted(0L)
                        .totalProblemsSolved(0L)
                        .easyProblemsSolved(0L)
                        .mediumProblemsSolved(0L)
                        .hardProblemsSolved(0L)
                        .overallAccuracy(0.0)
                        .currentStreak(0)
                        .longestStreak(0)
                        .totalReviewTime(0L)
                        .totalReviews(0L)
                        .averageRating(0.0)
                        .build();
            }
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.warn("Failed to get user statistics for user {}, returning empty statistics: {}", 
                    userPrincipal.getId(), e.getMessage());
            UserStatistics emptyStats = UserStatistics.builder()
                    .totalProblemsAttempted(0L)
                    .totalProblemsSolved(0L)
                    .easyProblemsSolved(0L)
                    .mediumProblemsSolved(0L)
                    .hardProblemsSolved(0L)
                    .overallAccuracy(0.0)
                    .currentStreak(0)
                    .longestStreak(0)
                    .totalReviewTime(0L)
                    .totalReviews(0L)
                    .averageRating(0.0)
                    .build();
            return ResponseEntity.ok(emptyStats);
        }
    }

    /**
     * Check if user is authenticated.
     */
    @GetMapping("/status")
    @Operation(summary = "Get authentication status", description = "Check if user is authenticated")
    public ResponseEntity<AuthStatus> getAuthStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        boolean isAuthenticated = userPrincipal != null;
        
        AuthStatus status = AuthStatus.builder()
                .isAuthenticated(isAuthenticated)
                .userId(isAuthenticated ? userPrincipal.getId() : null)
                .username(isAuthenticated ? userPrincipal.getUsername() : null)
                .build();
        
        return ResponseEntity.ok(status);
    }

    // DTOs

    @lombok.Data
    @lombok.Builder
    public static class UserProfile {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String avatarUrl;
        private String timezone;
        private String authProvider;
        private Boolean emailVerified;
        private String createdAt;
        private String lastLoginAt;

        public static UserProfile from(User user) {
            return UserProfile.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .avatarUrl(user.getAvatarUrl())
                    .timezone(user.getTimezone())
                    .authProvider(user.getAuthProvider().name())
                    .emailVerified(user.getEmailVerified())
                    .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                    .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
                    .build();
        }
    }

    @lombok.Data
    public static class UpdateProfileRequest {
        private String firstName;
        private String lastName;
        private String timezone;
        private String avatarUrl;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserPreferences {
        private String theme;
        private String language;
        private Boolean emailNotifications;
        private Boolean pushNotifications;
        private Integer dailyGoal;
        private String difficultyPreference;

        public static UserPreferences from(User user) {
            // Extract preferences from user preferences JSON or use defaults
            return UserPreferences.builder()
                    .theme("system")
                    .language("zh-CN")
                    .emailNotifications(true)
                    .pushNotifications(false)
                    .dailyGoal(20)
                    .difficultyPreference("mixed")
                    .build();
        }
    }

    @lombok.Data
    public static class UpdatePreferencesRequest {
        private String theme;
        private String language;
        private Boolean emailNotifications;
        private Boolean pushNotifications;
        private Integer dailyGoal;
        private String difficultyPreference;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserStatistics {
        private Long totalProblemsAttempted;
        private Long totalProblemsSolved;
        private Long easyProblemsSolved;
        private Long mediumProblemsSolved;
        private Long hardProblemsSolved;
        private Double overallAccuracy;
        private Integer currentStreak;
        private Integer longestStreak;
        private Long totalReviewTime;
        private Long totalReviews;
        private Double averageRating;
    }

    @lombok.Data
    @lombok.Builder
    public static class AuthStatus {
        private Boolean isAuthenticated;
        private Long userId;
        private String username;
    }
}