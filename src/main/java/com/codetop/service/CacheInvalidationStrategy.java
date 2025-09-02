package com.codetop.service;

import com.codetop.service.CacheKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Removed CacheManager dependency after cache migration
// import org.springframework.cache.Cache;
// import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Cache-Aside Pattern Implementation with Cache-First Invalidation Strategy
 * 
 * Strategy: Delete cache BEFORE database update to ensure consistency
 * - Step 1: Delete cache entries synchronously
 * - Step 2: Update database (in calling transaction)
 * - Step 3: Subsequent requests will rebuild cache from database
 * 
 * Benefits:
 * - Eliminates cache-database inconsistency window
 * - Ensures database is always the source of truth
 * - Prevents serving stale data to users
 * 
 * @author CodeTop Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CacheInvalidationStrategy {
    
    // Removed CacheManager dependency - using RedisTemplate directly after cache migration
    // private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Invalidate problem-related caches BEFORE database update
     * Use in transaction with REQUIRES_NEW to ensure immediate execution
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidateProblemCachesSync() {
        log.info("Synchronously invalidating problem-related caches before database update");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Clear specific cache patterns using safe SCAN operations
            clearCacheSafely("codetop:problem:*");
            clearCacheSafely("codetop:tag:*");
            clearCacheSafely("codetop:codetop:*"); // CodeTop filter results
            
            // Clear named caches
            clearCacheByName("codetop-problem-single");
            clearCacheByName("codetop-problem-list");
            clearCacheByName("codetop-problem-search");
            clearCacheByName("problem-statistics");
            clearCacheByName("tag-statistics");
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Problem cache invalidation completed synchronously in {}ms", duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to invalidate problem caches synchronously in {}ms", duration, e);
            throw new RuntimeException("Cache invalidation failed", e);
        }
    }
    
    /**
     * Invalidate user-specific caches BEFORE database update
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidateUserCachesSync(Long userId) {
        log.info("Synchronously invalidating caches for user: {}", userId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Use precise cache key patterns to avoid over-invalidation
            String userPattern = CacheKeyBuilder.userDomain(userId);
            clearCacheSafely(userPattern);
            
            // Also clear FSRS caches for this user
            String fsrsPattern = CacheKeyBuilder.fsrsDomain(userId);
            clearCacheSafely(fsrsPattern);
            
            // Clear named caches with user-specific entries
            clearCacheByName("codetop-user-profile");
            clearCacheByName("codetop-user-progress");
            clearCacheByName("codetop-user-mastery");
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("User cache invalidation completed synchronously for user {} in {}ms", userId, duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to invalidate user caches for user {} in {}ms", userId, duration, e);
            throw new RuntimeException("Cache invalidation failed", e);
        }
    }
    
    /**
     * Invalidate FSRS-related caches BEFORE database update
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidateFSRSCachesSync(Long userId) {
        log.info("Synchronously invalidating FSRS caches for user: {}", userId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Clear user-specific FSRS caches
            String fsrsPattern = CacheKeyBuilder.fsrsDomain(userId);
            clearCacheSafely(fsrsPattern);
            
            // Clear named FSRS caches
            clearCacheByName("codetop-fsrs-queue");
            clearCacheByName("codetop-fsrs-stats");
            clearCacheByName("fsrs-metrics");
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("FSRS cache invalidation completed synchronously for user {} in {}ms", userId, duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to invalidate FSRS caches for user {} in {}ms", userId, duration, e);
            throw new RuntimeException("Cache invalidation failed", e);
        }
    }
    
    /**
     * Invalidate CodeTop filter caches BEFORE database update
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidateCodetopFilterCachesSync() {
        log.info("Synchronously invalidating CodeTop filter caches");
        
        long startTime = System.currentTimeMillis();
        
        try {
            String filterPattern = CacheKeyBuilder.codetopFilterDomain();
            clearCacheSafely(filterPattern);
            
            // Clear named filter caches
            clearCacheByName("codetop-filter-results");
            clearCacheByName("codetop-global-problems");
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("CodeTop filter cache invalidation completed synchronously in {}ms", duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to invalidate CodeTop filter caches in {}ms", duration, e);
            throw new RuntimeException("Cache invalidation failed", e);
        }
    }
    
    /**
     * Safe cache clearing using existing pattern - delegates to existing implementation
     * This avoids Redis API version compatibility issues
     */
    private void clearCacheSafely(String pattern) {
        try {
            java.util.Set<String> keys = new java.util.HashSet<>();
            org.springframework.data.redis.connection.RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            org.springframework.data.redis.core.ScanOptions options = org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match(pattern)
                    .count(1000)
                    .build();
            try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Safely deleted {} cache keys matching pattern (SCAN): {}", keys.size(), pattern);
            } else {
                log.debug("No cache keys found matching pattern (SCAN): {}", pattern);
            }
        } catch (Exception e) {
            log.error("Error safely clearing cache with pattern (SCAN): {}", pattern, e);
            // Don't throw exception to avoid breaking the main transaction
        }
    }
    
    /**
     * Clear cache by pattern (using Redis pattern matching)
     * This method now handles all cache clearing since we migrated from Spring Cache
     */
    private void clearCacheByName(String pattern) {
        try {
            // Convert cache name to pattern if needed
            String cachePattern = pattern.contains(":") ? pattern + ":*" : "codetop:" + pattern + ":*";
            clearCacheSafely(cachePattern);
            log.debug("Cleared cache pattern: {}", cachePattern);
        } catch (Exception e) {
            log.error("Error clearing cache pattern: {}", pattern, e);
            // Don't throw exception to avoid breaking the main transaction
        }
    }
    
    /**
     * Invalidate specific cache entry BEFORE database update
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evictCacheEntrySync(String cacheName, String key) {
        try {
            // Build the actual Redis key
            String redisKey = cacheName.contains(":") ? cacheName + ":" + key : "codetop:" + cacheName + ":" + key;
            Boolean deleted = redisTemplate.delete(redisKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Evicted cache entry synchronously - cache: {}, key: {}", cacheName, key);
            }
        } catch (Exception e) {
            log.error("Error evicting cache entry - cache: {}, key: {}", cacheName, key, e);
            throw new RuntimeException("Cache eviction failed", e);
        }
    }
}
