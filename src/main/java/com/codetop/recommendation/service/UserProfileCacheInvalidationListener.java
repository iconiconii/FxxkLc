package com.codetop.recommendation.service;

import com.codetop.event.Events;
import com.codetop.service.cache.CacheService;
import com.codetop.service.CacheKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced cache invalidation listener for user profile and recommendation caches.
 * 
 * Supports granular invalidation strategies:
 * - Immediate invalidation: Critical changes that require immediate cache clearing
 * - Selective invalidation: Invalidate only specific recommendation types or limits
 * - Smart invalidation: Invalidate based on the significance of the change
 * - Batch invalidation: Efficient handling of multiple cache operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileCacheInvalidationListener {
    
    private final UserProfilingService userProfilingService;
    private final CacheService cacheService;
    
    // Configuration for invalidation behavior
    @Value("${recommendation.cache-invalidation.smart-enabled:true}")
    private boolean smartInvalidationEnabled;
    
    @Value("${recommendation.cache-invalidation.batch-size:20}")
    private int batchInvalidationSize;
    
    /**
     * Handle review completion events with smart cache invalidation.
     */
    @EventListener
    @Async
    public void handleReviewEvent(Events.ReviewEvent event) {
        try {
            if (event.getType() == Events.ReviewEvent.ReviewEventType.REVIEW_COMPLETED) {
                Long userId = event.getUserId();
                Integer rating = event.getRating();
                
                log.debug("Handling review completion event for cache invalidation: " +
                         "userId={}, problemId={}, rating={}", 
                         userId, event.getProblemId(), rating);
                
                if (smartInvalidationEnabled) {
                    // Smart invalidation based on review significance
                    handleSmartInvalidation(userId, rating, event.getProblemId());
                } else {
                    // Traditional full invalidation
                    handleFullInvalidation(userId);
                }
                
                log.debug("Cache invalidation completed successfully after review: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to handle review event for cache invalidation: " +
                     "userId={}, problemId={}, error={}", 
                     event.getUserId(), event.getProblemId(), e.getMessage(), e);
        }
    }
    
    /**
     * Smart invalidation based on the significance of the review.
     */
    private void handleSmartInvalidation(Long userId, Integer rating, Long problemId) {
        // Always invalidate user profile for learning progress tracking
        userProfilingService.invalidateUserProfileCache(userId);
        
        if (rating != null) {
            if (rating == 1) {
                // Major failure - invalidate all AI recommendations as difficulty preferences may change
                invalidateRecommendationsByType(userId, Set.of(
                    RecommendationType.AI, 
                    RecommendationType.HYBRID, 
                    RecommendationType.AUTO
                ));
            } else if (rating <= 2) {
                // Difficulty - invalidate AI and hybrid recommendations
                invalidateRecommendationsByType(userId, Set.of(
                    RecommendationType.AI, 
                    RecommendationType.HYBRID
                ));
            } else if (rating >= 4) {
                // Success - may affect FSRS scheduling and AI confidence
                invalidateRecommendationsByType(userId, Set.of(
                    RecommendationType.FSRS, 
                    RecommendationType.HYBRID, 
                    RecommendationType.AUTO
                ));
            } else {
                // Moderate success - light invalidation for hybrid only
                invalidateRecommendationsByType(userId, Set.of(RecommendationType.HYBRID));
            }
        } else {
            // Unknown rating - conservative full invalidation
            handleFullInvalidation(userId);
        }
    }
    
    /**
     * Traditional full invalidation of all recommendation caches.
     */
    private void handleFullInvalidation(Long userId) {
        // Invalidate user profile
        userProfilingService.invalidateUserProfileCache(userId);
        
        // Invalidate all recommendation caches
        if (cacheService != null) {
            try {
                String recommendationPattern = CacheKeyBuilder.buildUserKey("rec-ai", userId, "*");
                long deletedCount = cacheService.deleteByPattern(recommendationPattern);
                
                log.debug("All recommendation caches invalidated for userId={}, deleted {} keys", 
                         userId, deletedCount);
            } catch (Exception cacheException) {
                log.warn("Failed to invalidate recommendation cache for userId={}: {}", 
                         userId, cacheException.getMessage());
            }
        }
    }
    
    /**
     * Invalidate recommendation caches for specific recommendation types.
     */
    private void invalidateRecommendationsByType(Long userId, Set<RecommendationType> types) {
        if (cacheService == null) return;
        
        int totalDeleted = 0;
        
        for (RecommendationType type : types) {
            try {
                String pattern = buildTypeSpecificPattern(userId, type);
                long deleted = cacheService.deleteByPattern(pattern);
                totalDeleted += deleted;
                
                log.debug("Invalidated {} cache for userId={}, type={}, deleted {} keys", 
                         "recommendation", userId, type.getValue(), deleted);
                
            } catch (Exception e) {
                log.warn("Failed to invalidate {} recommendations for userId={}: {}", 
                         type.getValue(), userId, e.getMessage());
            }
        }
        
        log.debug("Smart invalidation completed for userId={}, types={}, total deleted: {}", 
                 userId, types, totalDeleted);
    }
    
    /**
     * Build type-specific cache key patterns for granular invalidation.
     */
    private String buildTypeSpecificPattern(Long userId, RecommendationType type) {
        switch (type) {
            case AI:
                return CacheKeyBuilder.buildUserKey("rec-ai", userId, "*_pv_*");
            case FSRS:
                return CacheKeyBuilder.buildUserKey("rec-fsrs", userId, "*");
            case HYBRID:
                return CacheKeyBuilder.buildUserKey("rec-ai", userId, "*hybrid*");
            case AUTO:
                return CacheKeyBuilder.buildUserKey("rec-ai", userId, "*auto*");
            default:
                return CacheKeyBuilder.buildUserKey("rec-ai", userId, "*");
        }
    }
    
    /**
     * Batch invalidation for multiple users (for bulk operations).
     */
    public CompletableFuture<Void> invalidateUsersCache(List<Long> userIds) {
        return CompletableFuture.runAsync(() -> {
            log.info("Starting batch cache invalidation for {} users", userIds.size());
            
            for (int i = 0; i < userIds.size(); i += batchInvalidationSize) {
                int end = Math.min(i + batchInvalidationSize, userIds.size());
                List<Long> batch = userIds.subList(i, end);
                
                for (Long userId : batch) {
                    try {
                        handleFullInvalidation(userId);
                    } catch (Exception e) {
                        log.warn("Failed to invalidate cache for userId={} in batch: {}", 
                                userId, e.getMessage());
                    }
                }
                
                log.debug("Completed batch {}/{} cache invalidations", 
                         end, userIds.size());
            }
            
            log.info("Batch cache invalidation completed for {} users", userIds.size());
        });
    }
    
    /**
     * Selective invalidation for specific cache keys (for partial updates).
     */
    public void invalidateSpecificKeys(Long userId, List<String> cacheKeys) {
        if (cacheService == null || cacheKeys.isEmpty()) return;
        
        try {
            Set<String> keysToDelete = Set.copyOf(cacheKeys);
            cacheService.delete(keysToDelete);
            
            log.debug("Selective cache invalidation completed for userId={}, keys: {}", 
                     userId, keysToDelete.size());
                     
        } catch (Exception e) {
            log.warn("Failed selective cache invalidation for userId={}: {}", 
                     userId, e.getMessage());
        }
    }
    
    /**
     * Get invalidation statistics for monitoring.
     */
    public InvalidationStats getInvalidationStats() {
        // This is a simple implementation - in production you might want to 
        // track these metrics using Micrometer or similar
        return new InvalidationStats(
            smartInvalidationEnabled,
            batchInvalidationSize,
            cacheService != null
        );
    }
    
    /**
     * Statistics for cache invalidation monitoring.
     */
    public static class InvalidationStats {
        private final boolean smartInvalidationEnabled;
        private final int batchSize;
        private final boolean cacheServiceAvailable;
        
        public InvalidationStats(boolean smartInvalidationEnabled, int batchSize, boolean cacheServiceAvailable) {
            this.smartInvalidationEnabled = smartInvalidationEnabled;
            this.batchSize = batchSize;
            this.cacheServiceAvailable = cacheServiceAvailable;
        }
        
        public boolean isSmartInvalidationEnabled() { return smartInvalidationEnabled; }
        public int getBatchSize() { return batchSize; }
        public boolean isCacheServiceAvailable() { return cacheServiceAvailable; }
    }
}