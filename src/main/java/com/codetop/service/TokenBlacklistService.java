package com.codetop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for managing JWT token blacklist.
 * When a user logs out, their token is added to the blacklist
 * to prevent further use until it expires naturally.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    
    /**
     * Add token to blacklist with expiration time matching the JWT expiration
     */
    public void blacklistToken(String token) {
        try {
            // Extract the token without "Bearer " prefix if present
            String actualToken = token.replace("Bearer ", "");
            
            // Create blacklist key
            String key = BLACKLIST_PREFIX + actualToken;
            
            // Get remaining TTL from the token
            long ttl = getTokenRemainingTime(actualToken);
            
            if (ttl > 0) {
                // Add to blacklist with TTL matching token expiration
                redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
                log.info("Token blacklisted successfully, TTL: {}ms", ttl);
            } else {
                // Token is already expired, no need to blacklist
                log.info("Token already expired, no need to blacklist");
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token: {}", e.getMessage());
            // Don't throw exception to avoid disrupting logout flow
        }
    }
    
    /**
     * Check if token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String actualToken = token.replace("Bearer ", "");
            String key = BLACKLIST_PREFIX + actualToken;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check blacklist status: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove expired tokens from blacklist (cleanup)
     */
    public void cleanupExpiredTokens() {
        try {
            // Redis automatically removes expired keys, so no explicit cleanup needed
            log.debug("Token blacklist cleanup not required - Redis handles TTL automatically");
        } catch (Exception e) {
            log.error("Failed to cleanup token blacklist: {}", e.getMessage());
        }
    }
    
    /**
     * Get remaining time from JWT token
     */
    private long getTokenRemainingTime(String token) {
        try {
            // Extract expiration from JWT token (without full validation)
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return 0;
            }
            
            // Decode payload
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            java.util.Map<String, Object> payloadMap = new com.fasterxml.jackson.databind.ObjectMapper().readValue(payload, java.util.Map.class);
            
            // Get expiration timestamp
            Number exp = (Number) payloadMap.get("exp");
            if (exp == null) {
                return 0;
            }
            
            long expirationTime = exp.longValue() * 1000; // Convert to milliseconds
            long currentTime = System.currentTimeMillis();
            
            return Math.max(0, expirationTime - currentTime);
        } catch (Exception e) {
            log.error("Failed to extract token expiration: {}", e.getMessage());
            // Default to JWT expiration time from config
            return jwtExpiration;
        }
    }
}