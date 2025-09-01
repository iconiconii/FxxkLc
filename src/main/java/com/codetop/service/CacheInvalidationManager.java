package com.codetop.service;

import com.codetop.service.CacheKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Removed CacheManager dependency after cache migration
// import org.springframework.cache.Cache;
// import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Enterprise Cache Invalidation Manager
 * 
 * Handles sophisticated cache invalidation strategies:
 * - Single key eviction
 * - Pattern-based batch eviction  
 * - Cross-domain invalidation
 * - Event-driven cache clearing
 * 
 * @author CodeTop Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CacheInvalidationManager {
    
    // Removed CacheManager dependency - using RedisTemplate directly after cache migration
    // private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Invalidate all problem-related caches
     * Triggered by: problem creation, update, deletion
     */
    public void invalidateProblemCaches() {
        log.info("Invalidating all problem-related caches");
        
        // Clear specific caches
        clearCache("codetop:problem:*");
        clearCache("codetop:tag:*");
        clearCache("codetop:codetop:*"); // CodeTop filter results
        
        // Clear statistical caches that depend on problems
        clearCacheByName("problem-statistics");
        clearCacheByName("tag-statistics");
        
        log.info("Problem cache invalidation completed");
    }
    
    /**
     * Invalidate user-specific caches
     * Triggered by: user profile update, progress changes
     */
    public void invalidateUserCaches(Long userId) {
        log.info("Invalidating caches for user: {}", userId);
        
        String userPattern = CacheKeyBuilder.userDomain(userId);
        clearCache(userPattern);
        
        // Also clear FSRS caches for this user
        String fsrsPattern = CacheKeyBuilder.fsrsDomain(userId);
        clearCache(fsrsPattern);
        
        log.info("User cache invalidation completed for user: {}", userId);
    }
    
    /**
     * Invalidate FSRS-related caches
     * Triggered by: review completion, parameter optimization
     */
    public void invalidateFSRSCaches(Long userId) {
        log.info("Invalidating FSRS caches for user: {}", userId);
        
        // Clear user-specific FSRS caches
        String fsrsPattern = CacheKeyBuilder.fsrsDomain(userId);
        clearCache(fsrsPattern);
        
        // Clear global FSRS metrics if significant changes
        clearCacheByName("fsrs-metrics");
        
        log.info("FSRS cache invalidation completed for user: {}", userId);
    }
    
    /**
     * Invalidate CodeTop filter caches
     * Triggered by: company/department/position changes, frequency updates
     */
    public void invalidateCodetopFilterCaches() {
        log.info("Invalidating CodeTop filter caches");
        
        String filterPattern = CacheKeyBuilder.codetopFilterDomain();
        clearCache(filterPattern);
        
        log.info("CodeTop filter cache invalidation completed");
    }
    
    /**
     * Invalidate caches when company data changes
     * Triggered by: company activation/deactivation, hierarchy changes
     */
    public void invalidateCompanyCaches() {
        log.info("Invalidating company-related caches");
        
        // CodeTop filters depend heavily on company data
        invalidateCodetopFilterCaches();
        
        // Problem statistics may change based on company associations
        clearCacheByName("problem-statistics");
        
        log.info("Company cache invalidation completed");
    }
    
    /**
     * Comprehensive cache warm-up after major data changes
     */
    public void warmUpCriticalCaches() {
        log.info("Starting critical cache warm-up");
        
        // This would typically be called by background jobs
        // to pre-populate frequently accessed caches
        
        log.info("Critical cache warm-up completed");
    }
    
    /**
     * Emergency cache flush - use with caution
     */
    public void flushAllCaches() {
        log.warn("Emergency cache flush initiated");
        
        // Clear all Redis keys with our cache prefixes
        try {
            Set<String> keys = redisTemplate.keys("codetop:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.warn("Deleted {} cache keys during emergency flush", keys.size());
            }
        } catch (Exception e) {
            log.error("Error during emergency cache flush", e);
        }
        
        // Alternative: flush entire Redis database (more aggressive)
        // redisTemplate.getConnectionFactory().getConnection().flushDb();
        
        log.warn("Emergency cache flush completed");
    }
    
    /**
     * Clear cache by pattern (Redis keys with wildcards)
     */
    private void clearCache(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Deleted {} cache keys matching pattern: {}", keys.size(), pattern);
            } else {
                log.info("No cache keys found matching pattern: {}", pattern);
            }
        } catch (Exception e) {
            log.error("Error clearing cache with pattern: {}", pattern, e);
        }
    }
    
    /**
     * Clear cache by pattern (Redis keys with wildcards)
     * This method now handles all cache clearing since we migrated from Spring Cache
     */
    private void clearCacheByName(String pattern) {
        // Convert cache name to pattern if needed
        String cachePattern = pattern.contains(":") ? pattern : "codetop:" + pattern + ":*";
        clearCache(cachePattern);
    }
    
    /**
     * Clear specific cache entry
     */
    public void evictCacheEntry(String cacheName, String key) {
        try {
            // Build the actual Redis key
            String redisKey = cacheName.contains(":") ? cacheName + ":" + key : "codetop:" + cacheName + ":" + key;
            Boolean deleted = redisTemplate.delete(redisKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Evicted cache entry - cache: {}, key: {}", cacheName, key);
            }
        } catch (Exception e) {
            log.error("Error evicting cache entry - cache: {}, key: {}", cacheName, key, e);
        }
    }
}