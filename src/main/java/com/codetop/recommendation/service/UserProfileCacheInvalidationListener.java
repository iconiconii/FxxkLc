package com.codetop.recommendation.service;

import com.codetop.event.Events;
import com.codetop.service.cache.CacheService;
import com.codetop.service.CacheKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for invalidating user profile and recommendation caches when user learning data changes.
 * 
 * Listens to FSRS review events and other user data changes to ensure 
 * both user profiles and recommendations remain consistent with latest learning activity.
 * 
 * Cache invalidation strategy:
 * - User profile cache: Immediate invalidation to reflect learning progress
 * - Recommendation cache: Wildcard pattern invalidation to prevent stale recommendations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileCacheInvalidationListener {
    
    private final UserProfilingService userProfilingService;
    private final CacheService cacheService;
    
    /**
     * Handle review completion events to invalidate user profile and recommendation caches.
     * 
     * After FSRS review events, both user profiles and recommendations become stale:
     * - User profile changes due to new learning data
     * - Recommendations change due to updated FSRS urgency signals and learning patterns
     */
    @EventListener
    @Async
    public void handleReviewEvent(Events.ReviewEvent event) {
        try {
            if (event.getType() == Events.ReviewEvent.ReviewEventType.REVIEW_COMPLETED) {
                Long userId = event.getUserId();
                
                log.debug("Handling review completion event for cache invalidation: " +
                         "userId={}, problemId={}, rating={}", 
                         userId, event.getProblemId(), event.getRating());
                
                // 1. Invalidate user profile cache after review completion
                // This ensures the next profile request will reflect updated learning data
                userProfilingService.invalidateUserProfileCache(userId);
                
                // 2. Invalidate recommendation caches for this user
                // Pattern: rec-ai:{userId}:* to clear all recommendation variants for this user
                if (cacheService != null) {
                    try {
                        String recommendationPattern = CacheKeyBuilder.buildUserKey("rec-ai", userId, "*");
                        long deletedCount = cacheService.deleteByPattern(recommendationPattern);
                        
                        log.debug("Recommendation caches invalidated for pattern: {}, deleted {} keys", 
                               recommendationPattern, deletedCount);
                    } catch (Exception cacheException) {
                        log.warn("Failed to invalidate recommendation cache for userId={}: {}", 
                               userId, cacheException.getMessage());
                        // Continue - profile cache invalidation is more critical
                    }
                }
                
                log.debug("Cache invalidation completed successfully after review: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to handle review event for cache invalidation: " +
                     "userId={}, problemId={}, error={}", 
                     event.getUserId(), event.getProblemId(), e.getMessage(), e);
        }
    }
}