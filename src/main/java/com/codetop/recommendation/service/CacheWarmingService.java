package com.codetop.recommendation.service;

import com.codetop.entity.User;
import com.codetop.mapper.UserMapper;
import com.codetop.mapper.ReviewLogMapper;
import com.codetop.service.cache.CacheService;
import com.codetop.service.CacheKeyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service for intelligent cache warming to improve performance for active users.
 * 
 * This service identifies active users and pre-computes their recommendations
 * during low-traffic periods to improve response times during peak usage.
 */
@Service
public class CacheWarmingService {
    
    private static final Logger log = LoggerFactory.getLogger(CacheWarmingService.class);
    
    private final UserMapper userMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final CacheService cacheService;
    private final UserProfilingService userProfilingService;
    private final AIRecommendationService aiRecommendationService;
    private final RecommendationStrategyResolver strategyResolver;
    
    // Configuration
    @Value("${recommendation.cache-warming.enabled:true}")
    private boolean warmingEnabled;
    
    @Value("${recommendation.cache-warming.active-user-days:7}")
    private int activeUserDays;
    
    @Value("${recommendation.cache-warming.min-reviews:5}")
    private int minReviewsForActive;
    
    @Value("${recommendation.cache-warming.batch-size:50}")
    private int batchSize;
    
    private final int maxConcurrentWarmups;
    
    // Thread pool for async warming operations
    private final Executor warmingExecutor;
    
    @Autowired
    public CacheWarmingService(
            UserMapper userMapper,
            ReviewLogMapper reviewLogMapper,
            CacheService cacheService,
            UserProfilingService userProfilingService,
            AIRecommendationService aiRecommendationService,
            RecommendationStrategyResolver strategyResolver,
            @Value("${recommendation.cache-warming.max-concurrent:5}") int maxConcurrentWarmups) {
        
        this.userMapper = userMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.cacheService = cacheService;
        this.userProfilingService = userProfilingService;
        this.aiRecommendationService = aiRecommendationService;
        this.strategyResolver = strategyResolver;
        this.maxConcurrentWarmups = maxConcurrentWarmups;
        this.warmingExecutor = Executors.newFixedThreadPool(Math.max(1, maxConcurrentWarmups));
    }

    // Backward-compatible constructor for existing tests
    public CacheWarmingService(
            UserMapper userMapper,
            ReviewLogMapper reviewLogMapper,
            CacheService cacheService,
            UserProfilingService userProfilingService,
            AIRecommendationService aiRecommendationService,
            RecommendationStrategyResolver strategyResolver) {
        this(userMapper, reviewLogMapper, cacheService, userProfilingService, aiRecommendationService, strategyResolver, 5);
    }
    
    /**
     * Scheduled cache warming during low-traffic periods.
     * Runs every 4 hours to keep caches fresh.
     */
    @Scheduled(fixedRate = 14400000) // 4 hours in milliseconds
    public void scheduledCacheWarming() {
        if (!warmingEnabled) {
            log.debug("Cache warming is disabled, skipping scheduled warming");
            return;
        }
        
        log.info("Starting scheduled cache warming for active users");
        long startTime = System.currentTimeMillis();
        
        try {
            List<Long> activeUserIds = getActiveUserIds();
            int totalUsers = activeUserIds.size();
            
            if (totalUsers == 0) {
                log.info("No active users found for cache warming");
                return;
            }
            
            log.info("Found {} active users for cache warming", totalUsers);
            
            // Process users in batches
            int batches = (int) Math.ceil((double) totalUsers / batchSize);
            int warmedCount = 0;
            
            for (int i = 0; i < batches; i++) {
                int start = i * batchSize;
                int end = Math.min(start + batchSize, totalUsers);
                List<Long> batch = activeUserIds.subList(start, end);
                
                List<CompletableFuture<Integer>> futures = batch.stream()
                        .map(this::warmUserCacheAsync)
                        .toList();
                
                // Wait for batch completion
                CompletableFuture<Void> batchCompletion = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]));
                
                try {
                    batchCompletion.get(); // Wait for batch
                    
                    // Count successful warming operations
                    int batchWarmed = futures.stream()
                            .mapToInt(future -> {
                                try {
                                    return future.get();
                                } catch (Exception e) {
                                    return 0;
                                }
                            })
                            .sum();
                    
                    warmedCount += batchWarmed;
                    log.debug("Completed batch {}/{}, warmed {} users in this batch", 
                              i + 1, batches, batchWarmed);
                    
                } catch (Exception e) {
                    log.error("Failed to complete warming batch {}: {}", i + 1, e.getMessage());
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Cache warming completed: {}/{} users warmed in {}ms", 
                     warmedCount, totalUsers, duration);
            
        } catch (Exception e) {
            log.error("Failed to complete scheduled cache warming: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Manually trigger cache warming for specific users.
     */
    public CompletableFuture<Integer> warmUsersCache(List<Long> userIds) {
        log.info("Manual cache warming requested for {} users", userIds.size());
        
        return CompletableFuture.supplyAsync(() -> {
            int warmedCount = 0;
            for (Long userId : userIds) {
                try {
                    int result = warmUserCache(userId);
                    warmedCount += result;
                } catch (Exception e) {
                    log.warn("Failed to warm cache for userId={}: {}", userId, e.getMessage());
                }
            }
            return warmedCount;
        }, warmingExecutor);
    }
    
    /**
     * Warm cache for a single user asynchronously.
     */
    private CompletableFuture<Integer> warmUserCacheAsync(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return warmUserCache(userId);
            } catch (Exception e) {
                log.warn("Failed to warm cache for userId={}: {}", userId, e.getMessage());
                return 0;
            }
        }, warmingExecutor);
    }
    
    /**
     * Warm cache for a single user synchronously.
     * Returns 1 if successful, 0 if failed.
     */
    private int warmUserCache(Long userId) {
        log.debug("Warming cache for userId={}", userId);
        
        try {
            // 1. Warm user profile cache
            userProfilingService.getUserProfile(userId, true);
            log.debug("Warmed user profile cache for userId={}", userId);
            
            // 2. Warm recommendation caches for different strategies
            warmRecommendationCaches(userId);
            
            return 1; // Success
            
        } catch (Exception e) {
            log.warn("Failed to warm cache for userId={}: {}", userId, e.getMessage());
            return 0; // Failure
        }
    }
    
    /**
     * Warm recommendation caches for different strategies and limits.
     */
    private void warmRecommendationCaches(Long userId) {
        // Common recommendation limits to pre-compute
        int[] limits = {5, 10, 15, 20};
        
        // Warm different recommendation types
        RecommendationType[] types = {
            RecommendationType.HYBRID,  // Most common
            RecommendationType.AI,      // When AI is available
            RecommendationType.FSRS     // Fallback
        };
        
        for (RecommendationType type : types) {
            for (int limit : limits) {
                try {
                    // Check if strategy is available before warming
                    RecommendationStrategy strategy = strategyResolver.resolveStrategy(type, userId, null);
                    
                    if (strategy != null && strategy.isAvailable()) {
                        // Pre-compute recommendations to warm cache
                        strategy.getRecommendations(userId, limit, null, null, null, null);
                        log.debug("Warmed {} recommendations cache for userId={}, limit={}", 
                                  type, userId, limit);
                    } else {
                        log.debug("Strategy {} not available for userId={}, skipping", type, userId);
                    }
                    
                } catch (Exception e) {
                    log.debug("Failed to warm {} cache for userId={}, limit={}: {}", 
                              type, userId, limit, e.getMessage());
                }
            }
        }
    }
    
    /**
     * Identify active users based on recent review activity.
     */
    private List<Long> getActiveUserIds() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(activeUserDays);
        
        // Query for users with recent review activity
        return reviewLogMapper.findActiveUserIds(cutoffTime, minReviewsForActive, batchSize * 10);
    }
    
    /**
     * Get cache warming statistics for monitoring.
     */
    public CacheWarmingStats getStats() {
        try {
            List<Long> activeUsers = getActiveUserIds();
            int profileCacheHits = 0;
            int recommendationCacheHits = 0;
            
            // Sample cache hit rates
            for (Long userId : activeUsers.subList(0, Math.min(20, activeUsers.size()))) {
                // Check profile cache
                String profileKey = CacheKeyBuilder.userProfile(userId);
                if (cacheService.exists(profileKey)) {
                    profileCacheHits++;
                }
                
                // Check recommendation cache (sample one type)
                String recKey = CacheKeyBuilder.buildUserKey("rec-ai", userId, "limit_10_pv_v1_default");
                if (cacheService.exists(recKey)) {
                    recommendationCacheHits++;
                }
            }
            
            return new CacheWarmingStats(
                activeUsers.size(),
                profileCacheHits,
                recommendationCacheHits,
                warmingEnabled
            );
            
        } catch (Exception e) {
            log.warn("Failed to get cache warming stats: {}", e.getMessage());
            return new CacheWarmingStats(0, 0, 0, warmingEnabled);
        }
    }
    
    /**
     * Cache warming statistics for monitoring.
     */
    public static class CacheWarmingStats {
        private final int activeUsers;
        private final int profileCacheHits;
        private final int recommendationCacheHits;
        private final boolean enabled;
        
        public CacheWarmingStats(int activeUsers, int profileCacheHits, int recommendationCacheHits, boolean enabled) {
            this.activeUsers = activeUsers;
            this.profileCacheHits = profileCacheHits;
            this.recommendationCacheHits = recommendationCacheHits;
            this.enabled = enabled;
        }
        
        public int getActiveUsers() { return activeUsers; }
        public int getProfileCacheHits() { return profileCacheHits; }
        public int getRecommendationCacheHits() { return recommendationCacheHits; }
        public boolean isEnabled() { return enabled; }
    }
}
