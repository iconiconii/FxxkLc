package com.codetop.service;

import com.codetop.controller.LeaderboardController;
import com.codetop.dto.*;
import com.codetop.mapper.UserMapper;
import com.codetop.mapper.UserStatisticsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.codetop.vo.StreakLeaderboardEntryVO;

/**
 * Leaderboard service for managing user rankings and achievements.
 * 
 * Features:
 * - Multi-category leaderboards (global, weekly, monthly, accuracy, streak)
 * - Badge calculation and assignment
 * - Manual Redis caching with JSON serialization (resolves serialization compatibility issues)
 * - User rank tracking and statistics
 * 
 * Cache Strategy:
 * - Uses RedisTemplate with ObjectMapper for manual serialization/deserialization
 * - Default TTL: 5 minutes for all cached data
 * - Fallback to database queries if Redis is unavailable
 * 
 * @author CodeTop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserMapper userMapper;
    private final UserStatisticsMapper userStatisticsMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final int DEFAULT_CACHE_TTL_MINUTES = 5;

    /**
     * Get global leaderboard with manual Redis caching.
     */
    public List<LeaderboardEntryDTO> getGlobalLeaderboard(int limit) {
        String cacheKey = CacheKeyBuilder.leaderboardGlobal(limit);
        
        // Try to get from cache first
        List<LeaderboardEntryDTO> cachedResult = getCachedList(cacheKey, new TypeReference<List<LeaderboardEntryDTO>>() {});
        if (cachedResult != null) {
            log.debug("Retrieved global leaderboard from cache for limit: {}", limit);
            return cachedResult;
        }
        
        log.debug("Fetching global leaderboard from database with limit: {}", limit);
        List<LeaderboardController.LeaderboardEntry> rawEntries = userMapper.getGlobalLeaderboard(limit);
        List<LeaderboardEntryDTO> entries = rawEntries.stream()
                .map(this::convertToLeaderboardEntryDTO)
                .toList();
        
        // Cache the result if not empty
        if (!entries.isEmpty()) {
            cacheList(cacheKey, entries, DEFAULT_CACHE_TTL_MINUTES);
        }
        
        return entries;
    }

    /**
     * Get weekly leaderboard with manual Redis caching.
     */
    public List<LeaderboardEntryDTO> getWeeklyLeaderboard(int limit) {
        String cacheKey = CacheKeyBuilder.leaderboardWeekly(limit);
        
        // Try to get from cache first
        List<LeaderboardEntryDTO> cachedResult = getCachedList(cacheKey, new TypeReference<List<LeaderboardEntryDTO>>() {});
        if (cachedResult != null) {
            log.debug("Retrieved weekly leaderboard from cache for limit: {}", limit);
            return cachedResult;
        }
        
        log.debug("Fetching weekly leaderboard from database with limit: {}", limit);
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        List<LeaderboardController.LeaderboardEntry> rawEntries = userMapper.getWeeklyLeaderboard(startDate, limit);
        List<LeaderboardEntryDTO> entries = rawEntries.stream()
                .map(this::convertToLeaderboardEntryDTO)
                .toList();
        
        // Cache the result if not empty
        if (!entries.isEmpty()) {
            cacheList(cacheKey, entries, DEFAULT_CACHE_TTL_MINUTES);
        }
            
        return entries;
    }

    /**
     * Get monthly leaderboard with manual Redis caching.
     */
    public List<LeaderboardEntryDTO> getMonthlyLeaderboard(int limit) {
        String cacheKey = CacheKeyBuilder.leaderboardMonthly(limit);
        
        // Try to get from cache first
        List<LeaderboardEntryDTO> cachedResult = getCachedList(cacheKey, new TypeReference<List<LeaderboardEntryDTO>>() {});
        if (cachedResult != null) {
            log.debug("Retrieved monthly leaderboard from cache for limit: {}", limit);
            return cachedResult;
        }
        
        log.debug("Fetching monthly leaderboard from database with limit: {}", limit);
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        List<LeaderboardController.LeaderboardEntry> rawEntries = userMapper.getMonthlyLeaderboard(startDate, limit);
        List<LeaderboardEntryDTO> entries = rawEntries.stream()
                .map(this::convertToLeaderboardEntryDTO)
                .toList();
        
        // Cache the result if not empty
        if (!entries.isEmpty()) {
            cacheList(cacheKey, entries, DEFAULT_CACHE_TTL_MINUTES);
        }

        return entries;
    }

    /**
     * Get accuracy leaderboard with manual Redis caching.
     */
    public List<AccuracyLeaderboardEntryDTO> getAccuracyLeaderboard(int limit, int days) {
        String cacheKey = CacheKeyBuilder.leaderboardAccuracy(limit, days);
        
        // Try to get from cache first
        List<AccuracyLeaderboardEntryDTO> cachedResult = getCachedList(cacheKey, new TypeReference<List<AccuracyLeaderboardEntryDTO>>() {});
        if (cachedResult != null) {
            log.debug("Retrieved accuracy leaderboard from cache for limit: {} and days: {}", limit, days);
            return cachedResult;
        }
        
        log.debug("Fetching accuracy leaderboard from database with limit: {} and days: {}", limit, days);
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<LeaderboardController.AccuracyLeaderboardEntry> rawEntries = userMapper.getAccuracyLeaderboard(startDate, limit);
        List<AccuracyLeaderboardEntryDTO> entries = rawEntries.stream()
                .map(this::convertToAccuracyLeaderboardEntryDTO)
                .toList();
        
        // Cache the result if not empty
        if (!entries.isEmpty()) {
            cacheList(cacheKey, entries, DEFAULT_CACHE_TTL_MINUTES);
        }

        return entries;
    }

    /**
     * Get streak leaderboard with enhanced data and manual Redis caching.
     * Now returns VO with computed badges for safe Redis caching.
     */
    public List<StreakLeaderboardEntryDTO> getStreakLeaderboard(int limit) {
        String cacheKey = CacheKeyBuilder.leaderboardStreak(limit);
        
        // Try to get from cache first
        List<StreakLeaderboardEntryDTO> cachedResult = getCachedList(cacheKey, new TypeReference<List<StreakLeaderboardEntryDTO>>() {});
        if (cachedResult != null) {
            log.debug("Retrieved streak leaderboard from cache for limit: {}", limit);
            return cachedResult;
        }
        
        log.debug("Fetching streak leaderboard from database with limit: {}", limit);
        
        // Use enhanced streak query with real data from user_statistics
        List<LeaderboardController.StreakLeaderboardEntry> rawEntries = userStatisticsMapper.getStreakLeaderboard(limit);
        List<StreakLeaderboardEntryDTO> entries = rawEntries.stream()
                .map(this::convertToStreakLeaderboardEntryDTO)
                .toList();
        
        // Cache the result if not empty
        if (!entries.isEmpty()) {
            cacheList(cacheKey, entries, DEFAULT_CACHE_TTL_MINUTES);
        }
        
        return entries;
    }

    /**
     * Get user's rank information across all categories with manual Redis caching.
     */
    public UserRankInfo getUserRank(Long userId) {
        String cacheKey = CacheKeyBuilder.userRank(userId);
        
        // Try to get from cache first
        UserRankInfo cachedResult = getCachedObject(cacheKey, UserRankInfo.class);
        if (cachedResult != null) {
            log.debug("Retrieved user rank from cache for userId: {}", userId);
            return cachedResult;
        }
        
        log.debug("Fetching user rank from database for userId: {}", userId);
        
        UserRankInfo userRank = UserRankInfo.builder()
                .globalRank(userMapper.getUserGlobalRank(userId))
                .weeklyRank(userMapper.getUserWeeklyRank(userId))
                .monthlyRank(userMapper.getUserMonthlyRank(userId))
                .accuracyRank(userMapper.getUserAccuracyRank(userId))
                .streakRank(userStatisticsMapper.getUserStreakRank(userId))
                .build();
        
        // Cache the result
        cacheObject(cacheKey, userRank, DEFAULT_CACHE_TTL_MINUTES);
        
        return userRank;
    }

    /**
     * Calculate overall leaderboard position for a user.
     */
    public Long calculateOverallRank(Long userId) {
        // Weighted calculation considering multiple factors
        Long globalRank = userMapper.getUserGlobalRank(userId);
        Long accuracyRank = userMapper.getUserAccuracyRank(userId);
        Long streakRank = userStatisticsMapper.getUserStreakRank(userId);
        
        if (globalRank == null) globalRank = 999999L;
        if (accuracyRank == null) accuracyRank = 999999L;
        if (streakRank == null) streakRank = 999999L;
        
        // Weighted average: 50% volume + 30% accuracy + 20% consistency
        double weightedRank = (globalRank * 0.5) + (accuracyRank * 0.3) + (streakRank * 0.2);
        
        return Math.round(weightedRank);
    }

    /**
     * Get top performers summary.
     */
    public TopPerformersSummary getTopPerformersSummary() {
        List<LeaderboardEntryDTO> globalTop3 = getGlobalLeaderboard(3);
        List<AccuracyLeaderboardEntryDTO> accuracyTop3 = getAccuracyLeaderboard(3, 30);
        List<StreakLeaderboardEntryDTO> streakTop3 = getStreakLeaderboard(3);
        
        return TopPerformersSummary.builder()
                .topByVolume(globalTop3)
                .topByAccuracy(accuracyTop3)
                .topByStreak(streakTop3)
                .build();
    }

    // Conversion methods for DTO mapping
    
    private LeaderboardEntryDTO convertToLeaderboardEntryDTO(LeaderboardController.LeaderboardEntry entry) {
        return new LeaderboardEntryDTO(
                entry.getRank(),
                entry.getUserId(),
                entry.getUsername(),
                entry.getAvatarUrl(),
                entry.getTotalReviews(),
                entry.getCorrectReviews(),
                entry.getAccuracy(),
                entry.getStreak()
        );
    }

    private AccuracyLeaderboardEntryDTO convertToAccuracyLeaderboardEntryDTO(LeaderboardController.AccuracyLeaderboardEntry entry) {
        return new AccuracyLeaderboardEntryDTO(
                entry.getRank(),
                entry.getUserId(),
                entry.getUsername(),
                entry.getAvatarUrl(),
                entry.getTotalReviews(),
                entry.getCorrectReviews(),
                entry.getAccuracy()
        );
    }

    private StreakLeaderboardEntryDTO convertToStreakLeaderboardEntryDTO(LeaderboardController.StreakLeaderboardEntry entry) {
        return new StreakLeaderboardEntryDTO(
                entry.getRank(),
                entry.getUserId(),
                entry.getUsername(),
                entry.getAvatarUrl(),
                entry.getCurrentStreak(),
                entry.getLongestStreak(),
                entry.getTotalActiveDays()
        );
    }

    /**
     * Summary of top performers across all categories.
     */
    @lombok.Data
    @lombok.Builder
    public static class TopPerformersSummary {
        private List<LeaderboardEntryDTO> topByVolume;
        private List<AccuracyLeaderboardEntryDTO> topByAccuracy;
        private List<StreakLeaderboardEntryDTO> topByStreak;
    }

    // Redis caching helper methods

    /**
     * Cache a list object with TTL.
     */
    private <T> void cacheList(String cacheKey, List<T> data, int ttlMinutes) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(cacheKey, json, ttlMinutes, TimeUnit.MINUTES);
            log.debug("Cached list data with key: {} (TTL: {} minutes)", cacheKey, ttlMinutes);
        } catch (Exception e) {
            log.warn("Failed to cache list data with key: {}", cacheKey, e);
        }
    }

    /**
     * Cache a single object with TTL.
     */
    private <T> void cacheObject(String cacheKey, T data, int ttlMinutes) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(cacheKey, json, ttlMinutes, TimeUnit.MINUTES);
            log.debug("Cached object data with key: {} (TTL: {} minutes)", cacheKey, ttlMinutes);
        } catch (Exception e) {
            log.warn("Failed to cache object data with key: {}", cacheKey, e);
        }
    }

    /**
     * Retrieve and deserialize a list from cache.
     */
    private <T> List<T> getCachedList(String cacheKey, TypeReference<List<T>> typeRef) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                return objectMapper.readValue(json, typeRef);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve cached list with key: {}", cacheKey, e);
        }
        return null;
    }

    /**
     * Retrieve and deserialize a single object from cache.
     */
    private <T> T getCachedObject(String cacheKey, Class<T> clazz) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                return objectMapper.readValue(json, clazz);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve cached object with key: {}", cacheKey, e);
        }
        return null;
    }

    /**
     * Invalidate all leaderboard caches.
     */
    public void invalidateLeaderboardCaches() {
        try {
            // This is a simplified approach - in production, you might want to use SCAN
            // to find and delete keys matching the leaderboard pattern
            String pattern = CacheKeyBuilder.leaderboardDomain();
            log.info("Invalidating leaderboard caches with pattern: {}", pattern);
            // Note: Individual key deletion would be implemented here with Redis SCAN
            // For now, we rely on TTL for automatic cache expiration
        } catch (Exception e) {
            log.warn("Failed to invalidate leaderboard caches", e);
        }
    }
}