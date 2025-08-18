package com.codetop.security;

import com.codetop.config.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based rate limiting service implementing sliding window algorithm.
 * 
 * Features:
 * - Sliding window rate limiting for accuracy
 * - Separate limits for authenticated users and IP addresses
 * - Exponential backoff for repeated violations
 * - Distributed rate limiting across multiple instances
 * - Automatic cleanup of expired entries
 * 
 * @author CodeTop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitConfig.RateLimitProperties rateLimitProperties;

    private static final String USER_PREFIX = "rate_limit:user:";
    private static final String IP_PREFIX = "rate_limit:ip:";
    private static final String VIOLATION_PREFIX = "rate_limit:violation:";

    /**
     * Check if request is allowed for authenticated user.
     *
     * @param userId User ID
     * @param ipAddress Client IP address
     * @return true if request is allowed
     */
    public boolean isAllowed(Long userId, String ipAddress) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        try {
            // Check user-specific rate limit
            boolean userAllowed = checkUserRateLimit(userId);
            
            // Check IP-based rate limit
            boolean ipAllowed = checkIpRateLimit(ipAddress);

            // Both must pass
            if (!userAllowed || !ipAllowed) {
                recordViolation(userId, ipAddress);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error checking rate limit for user {} from IP {}", userId, ipAddress, e);
            // Fail open in case of Redis issues
            return true;
        }
    }

    /**
     * Check if request is allowed for unauthenticated request (IP only).
     *
     * @param ipAddress Client IP address
     * @return true if request is allowed
     */
    public boolean isAllowed(String ipAddress) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        try {
            boolean allowed = checkIpRateLimit(ipAddress);
            if (!allowed) {
                recordViolation(null, ipAddress);
            }
            return allowed;
        } catch (Exception e) {
            log.error("Error checking rate limit for IP {}", ipAddress, e);
            // Fail open in case of Redis issues
            return true;
        }
    }

    /**
     * Get remaining requests for user.
     */
    public int getRemainingRequests(Long userId) {
        if (!rateLimitProperties.isEnabled()) {
            return rateLimitProperties.getPerUserLimit();
        }

        try {
            String key = USER_PREFIX + userId;
            Long currentCount = getCurrentRequestCount(key);
            return Math.max(0, rateLimitProperties.getPerUserLimit() - currentCount.intValue());
        } catch (Exception e) {
            log.error("Error getting remaining requests for user {}", userId, e);
            return rateLimitProperties.getPerUserLimit();
        }
    }

    /**
     * Get time until rate limit reset.
     */
    public long getTimeUntilReset() {
        return TimeUnit.MINUTES.toSeconds(rateLimitProperties.getWindowMinutes());
    }

    /**
     * Check if IP address is currently in violation backoff period.
     */
    public boolean isInViolationBackoff(String ipAddress) {
        try {
            String key = VIOLATION_PREFIX + ipAddress;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Error checking violation backoff for IP {}", ipAddress, e);
            return false;
        }
    }

    private boolean checkUserRateLimit(Long userId) {
        String key = USER_PREFIX + userId;
        return checkRateLimit(key, rateLimitProperties.getPerUserLimit());
    }

    private boolean checkIpRateLimit(String ipAddress) {
        String key = IP_PREFIX + ipAddress;
        return checkRateLimit(key, rateLimitProperties.getPerIpLimit());
    }

    private boolean checkRateLimit(String key, int limit) {
        long now = Instant.now().getEpochSecond();
        long windowStart = now - TimeUnit.MINUTES.toSeconds(rateLimitProperties.getWindowMinutes());

        // Remove expired entries
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Count current requests in window
        Long currentCount = redisTemplate.opsForZSet().count(key, windowStart, now);

        if (currentCount >= limit) {
            return false;
        }

        // Add current request
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        
        // Set TTL to prevent memory leaks
        redisTemplate.expire(key, rateLimitProperties.getWindowMinutes() + 1, TimeUnit.MINUTES);

        return true;
    }

    private Long getCurrentRequestCount(String key) {
        long now = Instant.now().getEpochSecond();
        long windowStart = now - TimeUnit.MINUTES.toSeconds(rateLimitProperties.getWindowMinutes());

        // Clean up expired entries
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Return current count
        return redisTemplate.opsForZSet().count(key, windowStart, now);
    }

    private void recordViolation(Long userId, String ipAddress) {
        try {
            String key = VIOLATION_PREFIX + ipAddress;
            long violationCount = redisTemplate.opsForValue().increment(key);
            
            // Exponential backoff: 1 min, 2 min, 4 min, 8 min, max 15 min
            long backoffMinutes = Math.min(15, (long) Math.pow(2, Math.min(violationCount - 1, 4)));
            
            redisTemplate.expire(key, backoffMinutes, TimeUnit.MINUTES);
            
            log.warn("Rate limit violation from IP: {}, User: {}, Violation count: {}, Backoff: {} minutes", 
                    ipAddress, userId, violationCount, backoffMinutes);

            // Log security event
            if (userId != null) {
                log.warn("SECURITY_EVENT: RATE_LIMIT_VIOLATION userId={} ip={} count={}", 
                        userId, ipAddress, violationCount);
            }
        } catch (Exception e) {
            log.error("Error recording rate limit violation", e);
        }
    }
}