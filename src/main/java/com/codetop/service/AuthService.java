package com.codetop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.codetop.dto.UserCacheDTO;
import com.codetop.entity.User;
import com.codetop.enums.AuthProvider;
import com.codetop.logging.TraceContext;
import com.codetop.mapper.UserMapper;
import com.codetop.service.cache.CacheService;
import com.codetop.util.CacheHelper;
import com.codetop.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication service handling user login, registration, and OAuth integration.
 * 
 * Features:
 * - Local authentication with password hashing
 * - OAuth integration (Google, GitHub)
 * - JWT token generation and validation
 * - Account security (lockout, verification)
 * - Manual Redis caching for performance optimization
 * 
 * @author CodeTop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    
    private final UserMapper userMapper;
    private final @Lazy PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    
    // 新增缓存相关依赖
    private final CacheService cacheService;
    private final CacheHelper cacheHelper;
    
    // 缓存相关常量
    private static final String CACHE_PREFIX_USER_PROFILE = "user-profile";
    private static final Duration USER_PROFILE_TTL = Duration.ofHours(1);

    /**
     * Authenticate user with email/username and password.
     */
    @Transactional
    public AuthResult authenticate(String identifier, String password) {
        TraceContext.setOperation("USER_AUTHENTICATE");
        log.info("Authentication attempt for identifier: {} - TraceId: {}", 
            identifier, TraceContext.getTraceId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            Optional<User> userOpt = userMapper.findByEmailOrUsername(identifier);
            if (userOpt.isEmpty()) {
                log.warn("Authentication failed: Account does not exist for identifier: {} - TraceId: {}", 
                    identifier, TraceContext.getTraceId());
                throw new IllegalArgumentException("Account does not exist");
            }
            
            User user = userOpt.get();
            TraceContext.setUserId(user.getId());
            
            log.debug("Found user account: {} (ID: {}) - TraceId: {}", 
                user.getUsername(), user.getId(), TraceContext.getTraceId());
            
            // Account locking functionality disabled - column doesn't exist in current DB schema
            // Check if account is locked
            // if (user.isAccountLocked()) {
            //     log.warn("Authentication failed: Account locked for user: {} - TraceId: {}", 
            //         user.getUsername(), TraceContext.getTraceId());
            //     throw new BadCredentialsException("Account is temporarily locked");
            // }
            
            // Check if account is active
            if (!user.getIsActive()) {
                log.warn("Authentication failed: Account disabled for user: {} - TraceId: {}", 
                    user.getUsername(), TraceContext.getTraceId());
                throw new BadCredentialsException("Account is disabled");
            }
            
            // Validate password
            if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
                log.warn("Authentication failed: Incorrect password for user: {} - TraceId: {}", 
                    user.getUsername(), TraceContext.getTraceId());
                // Login attempts tracking disabled - column doesn't exist in current DB schema
                // user.incrementLoginAttempts();
                // userMapper.updateById(user);
                throw new BadCredentialsException("Incorrect password");
            }
            
            // Successful login - reset attempts and update login time
            log.debug("Password validation successful for user: {} - TraceId: {}", 
                user.getUsername(), TraceContext.getTraceId());
            
            user.updateLastLogin();
            
            // Assign default USER role for all authenticated users
            user.addRole("USER");
            
            userMapper.updateById(user);
            log.debug("Updated last login time for user: {} - TraceId: {}", 
                user.getUsername(), TraceContext.getTraceId());
            
            // 更新用户缓存
            invalidateUserCache(user.getId());
            
            String accessToken = jwtUtil.generateAccessToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("User {} authenticated successfully - Duration: {}ms - TraceId: {}", 
                user.getUsername(), duration, TraceContext.getTraceId());
            
            return AuthResult.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(user)
                    .expiresIn(jwtUtil.getAccessTokenExpiration())
                    .build();
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Authentication failed for identifier: {} - Duration: {}ms - Error: {} - TraceId: {}", 
                identifier, duration, e.getMessage(), TraceContext.getTraceId());
            throw e;
        }
    }

    /**
     * Register new user with local authentication.
     */
    @Transactional
    public AuthResult register(RegisterRequest request) {
        log.debug("Registering new user: {}", request.getEmail());
        
        // Check if email already exists
        if (userMapper.countByEmail(request.getEmail()) > 0) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Check if username already exists
        if (userMapper.countByUsername(request.getUsername()) > 0) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .emailVerified(false)
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .build();
                
        user.addRole("USER");
        userMapper.insert(user);
        
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        
        log.info("User {} registered successfully", user.getUsername());
        return AuthResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(user)
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    /**
     * OAuth authentication (Google, GitHub).
     */
    @Transactional
    public AuthResult authenticateOAuth(OAuthRequest request) {
        log.debug("OAuth authentication for provider: {}", request.getProvider());
        
        // Find existing user by provider ID
        Optional<User> existingUser = userMapper.findByProviderIdAndAuthProvider(
            request.getProviderId(), request.getProvider().name());
        
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.updateLastLogin();
            
            // Update user info from OAuth provider
            if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
                user.setEmail(request.getEmail());
            }
            if (request.getAvatarUrl() != null) {
                user.setAvatarUrl(request.getAvatarUrl());
            }
            
            userMapper.updateById(user);
            
            // 更新用户缓存
            invalidateUserCache(user.getId());
        } else {
            // Create new user from OAuth
            String username = generateUniqueUsername(request.getEmail(), request.getName());
            
            user = User.builder()
                    .username(username)
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .avatarUrl(request.getAvatarUrl())
                    .authProvider(request.getProvider())
                    .providerId(request.getProviderId())
                    .isActive(true)
                    .emailVerified(true) // OAuth emails are considered verified
                    .build();
                    
            user.addRole("USER");
            userMapper.insert(user);
            
            log.info("New OAuth user created: {} from {}", username, request.getProvider());
        }
        
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        
        return AuthResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(user)
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    /**
     * Refresh JWT access token using refresh token.
     */
    public AuthResult refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        
        Long userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);
        User user = userMapper.selectById(userId);
        
        if (user == null || !user.getIsActive()) {
            throw new BadCredentialsException("User not found or inactive");
        }
        
        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        
        return AuthResult.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(user)
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    /**
     * Get user by ID with manual Redis caching using UserCacheDTO.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrCompute() 进行缓存操作
     * - 保持相同的缓存键和TTL策略
     */
    public UserCacheDTO getUserCacheDTOById(Long userId) {
        if (userId == null) {
            log.warn("getUserCacheDTOById called with null userId");
            return null;
        }
        
        String cacheKey = CacheKeyBuilder.userProfile(userId);
        
        return cacheHelper.cacheOrCompute(
            cacheKey,
            UserCacheDTO.class,
            USER_PROFILE_TTL,
            () -> {
                log.info("Cache MISS - Loading user from database for userId: {}", userId);
                
                User user = userMapper.selectById(userId);
                if (user != null) {
                    // Assign default USER role for all users
                    user.addRole("USER");
                    return UserCacheDTO.fromUser(user);
                }
                return null;
            }
        );
    }
    
    /**
     * Get user by ID, using cache when possible.
     */
    public User getUserById(Long userId) {
        log.debug("Getting user by ID: {} - checking cache first", userId);
        
        UserCacheDTO cachedUser = getUserCacheDTOById(userId);
        if (cachedUser != null) {
            log.debug("Cache HIT for userId: {}", userId);
            return cachedUser.toUser();
        } else {
            log.warn("User not found for userId: {}", userId);
            return null;
        }
    }

    /**
     * Logout user (invalidate tokens).
     */
    @Transactional
    public void logout(String accessToken) {
        TraceContext.setOperation("USER_LOGOUT");
        
        long startTime = System.currentTimeMillis();
        String userId = TraceContext.getUserId();
        
        log.info("Logout attempt for user: {} - TraceId: {}", 
            userId != null ? userId : "unknown", TraceContext.getTraceId());
        
        try {
            // Add token to blacklist to prevent further use
            tokenBlacklistService.blacklistToken(accessToken);
            
            // 如果能从token中获取用户ID，清理用户缓存
            try {
                if (jwtUtil.validateAccessToken(accessToken)) {
                    Long userIdFromToken = jwtUtil.getUserIdFromAccessToken(accessToken);
                    if (userIdFromToken != null) {
                        invalidateUserCache(userIdFromToken);
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract user ID from token for cache invalidation: {}", e.getMessage());
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("User {} logged out successfully - Duration: {}ms - TraceId: {}", 
                userId != null ? userId : "unknown", duration, TraceContext.getTraceId());
                
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Logout failed for user: {} - Duration: {}ms - Error: {} - TraceId: {}", 
                userId != null ? userId : "unknown", duration, e.getMessage(), TraceContext.getTraceId());
            throw e;
        }
    }

    /**
     * Change user password.
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("Cannot change password for OAuth users");
        }
        
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        
        // 清理用户缓存
        invalidateUserCache(userId);
        
        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * Generate unique username from email or name.
     */
    private String generateUniqueUsername(String email, String name) {
        String baseUsername = name != null ? name.replaceAll("\\s+", "").toLowerCase() : 
                            email.split("@")[0].toLowerCase();
        
        // Remove non-alphanumeric characters
        baseUsername = baseUsername.replaceAll("[^a-zA-Z0-9]", "");
        
        // Ensure minimum length
        if (baseUsername.length() < 3) {
            baseUsername = "user" + baseUsername;
        }
        
        String username = baseUsername;
        int counter = 1;
        
        while (userMapper.countByUsername(username) > 0) {
            username = baseUsername + counter;
            counter++;
        }
        
        return username;
    }

    /**
     * Find user by ID - alias for compatibility.
     */
    public User findById(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * Update user profile information.
     */
    @Transactional
    public User updateUserProfile(Long userId, com.codetop.controller.UserController.UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        userMapper.updateById(user);
        
        // 清理用户缓存
        invalidateUserCache(userId);
        
        return user;
    }

    /**
     * Update user preferences.
     */
    @Transactional
    public User updateUserPreferences(Long userId, com.codetop.controller.UserController.UpdatePreferencesRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        
        // Convert preferences to JSON and store
        // For now, just store as JSON string - could use ObjectMapper for proper serialization
        String preferencesJson = String.format(
            "{\"theme\":\"%s\",\"language\":\"%s\",\"emailNotifications\":%s,\"pushNotifications\":%s,\"dailyGoal\":%d,\"difficultyPreference\":\"%s\"}",
            request.getTheme() != null ? request.getTheme() : "system",
            request.getLanguage() != null ? request.getLanguage() : "zh-CN",
            request.getEmailNotifications() != null ? request.getEmailNotifications() : true,
            request.getPushNotifications() != null ? request.getPushNotifications() : false,
            request.getDailyGoal() != null ? request.getDailyGoal() : 20,
            request.getDifficultyPreference() != null ? request.getDifficultyPreference() : "mixed"
        );
        
        user.setPreferences(preferencesJson);
        userMapper.updateById(user);
        
        // 清理用户缓存
        invalidateUserCache(userId);
        
        return user;
    }
    
    /**
     * 清理用户相关缓存
     * 
     * @param userId 用户ID
     */
    private void invalidateUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        
        try {
            String cacheKey = CacheKeyBuilder.userProfile(userId);
            boolean deleted = cacheService.delete(cacheKey);
            log.debug("User cache invalidated: userId={}, cacheKey={}, deleted={}", 
                userId, cacheKey, deleted);
        } catch (Exception e) {
            log.error("Failed to invalidate user cache: userId={}, error={}", 
                userId, e.getMessage(), e);
        }
    }

    // DTOs

    @lombok.Data
    @lombok.Builder
    public static class AuthResult {
        private String accessToken;
        private String refreshToken;
        private User user;
        private Long expiresIn;
    }

    @lombok.Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String timezone;
    }

    @lombok.Data
    public static class OAuthRequest {
        private AuthProvider provider;
        private String providerId;
        private String email;
        private String name;
        private String firstName;
        private String lastName;
        private String avatarUrl;
    }
}