package com.codetop.recommendation.service;

import com.codetop.service.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service for handling cache invalidation for AI recommendations.
 * 
 * This service provides methods to invalidate recommendation caches
 * when user data changes that would affect recommendation quality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationCacheInvalidationService {
    
    private final CacheService cacheService;
    
    /**
     * Invalidate all recommendation caches for a specific user.
     * This is the most comprehensive invalidation - use when unsure.
     * 
     * @param userId The user ID whose caches should be invalidated
     */
    public void invalidateAllUserCaches(Long userId) {
        if (userId == null) {
            log.warn("Cannot invalidate caches for null userId");
            return;
        }
        
        try {
            // Use pattern-based deletion for all recommendation caches for this user
            String userPattern = "rec-*:" + userId + ":*";
            cacheService.deleteByPattern(userPattern);
            
            // Also invalidate user profile cache
            String profilePattern = "userProfile:" + userId;
            cacheService.delete(profilePattern);
            
            log.info("Invalidated all recommendation caches for userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to invalidate caches for userId={}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Invalidate recommendation result caches for a user.
     * Called after review completion to ensure fresh recommendations.
     * 
     * @param userId The user ID whose result caches should be invalidated
     */
    public void invalidateResultCaches(Long userId) {
        if (userId == null) {
            log.warn("Cannot invalidate result caches for null userId");
            return;
        }
        
        try {
            // Invalidate AI recommendation result caches
            String resultPattern = "rec-ai:" + userId + ":*";
            cacheService.deleteByPattern(resultPattern);
            
            log.debug("Invalidated result caches for userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to invalidate result caches for userId={}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Invalidate intermediate caches for a user (candidates, user profile).
     * Called when user preferences or FSRS parameters change.
     * 
     * @param userId The user ID whose intermediate caches should be invalidated
     */
    public void invalidateIntermediateCaches(Long userId) {
        if (userId == null) {
            log.warn("Cannot invalidate intermediate caches for null userId");
            return;
        }
        
        try {
            // Invalidate candidate caches
            String candidatesPattern = "rec-candidates:" + userId + ":*";
            cacheService.deleteByPattern(candidatesPattern);
            
            // Invalidate user profile cache
            String profileKey = "userProfile:" + userId;
            cacheService.delete(profileKey);
            
            // Invalidate prompt variables cache
            String promptPattern = "prompt-variables:" + userId + ":*";
            cacheService.deleteByPattern(promptPattern);
            
            log.debug("Invalidated intermediate caches for userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to invalidate intermediate caches for userId={}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Invalidate caches after review completion.
     * This is the most common invalidation trigger.
     * 
     * @param userId The user who completed a review
     */
    public void onReviewCompleted(Long userId) {
        log.debug("Review completed for userId={}, invalidating caches", userId);
        
        // Invalidate both result and intermediate caches since review affects:
        // 1. FSRS card states (affects candidates)
        // 2. User profiling data (affects user profile cache)
        // 3. Recommendation relevance (affects result cache)
        invalidateResultCaches(userId);
        invalidateIntermediateCaches(userId);
    }
    
    /**
     * Invalidate caches after user preferences change.
     * 
     * @param userId The user whose preferences changed
     */
    public void onPreferencesChanged(Long userId) {
        log.debug("Preferences changed for userId={}, invalidating caches", userId);
        
        // Preference changes affect recommendation generation but not necessarily
        // the underlying candidate pool or user profiling data
        invalidateResultCaches(userId);
    }
    
    /**
     * Invalidate caches after FSRS parameters change.
     * 
     * @param userId The user whose FSRS parameters changed
     */
    public void onFsrsParametersChanged(Long userId) {
        log.debug("FSRS parameters changed for userId={}, invalidating caches", userId);
        
        // FSRS parameter changes affect both candidate scoring and recommendations
        invalidateAllUserCaches(userId);
    }
    
    /**
     * Invalidate caches after problem metadata changes.
     * This affects all users since problem tags/difficulty affect matching.
     * 
     * @param problemIds The problem IDs that were modified
     */
    public void onProblemsModified(Set<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return;
        }
        
        log.debug("Problems modified: {}, invalidating relevant caches", problemIds);
        
        try {
            // For now, we do a broad invalidation since we don't track which users
            // have which problems in their candidate sets. In a high-traffic system,
            // we could implement more granular tracking.
            
            // Invalidate all recommendation result caches (they depend on problem metadata)
            cacheService.deleteByPattern("rec-ai:*");
            
            // Invalidate all candidate caches (they include problem data)
            cacheService.deleteByPattern("rec-candidates:*");
            
            log.info("Invalidated recommendation caches due to problem modifications: {}", problemIds);
        } catch (Exception e) {
            log.error("Failed to invalidate caches for modified problems {}: {}", problemIds, e.getMessage(), e);
        }
    }
    
    /**
     * Get cache statistics for monitoring.
     * 
     * @return Cache statistics or null if not available
     */
    public CacheService.CacheStats getCacheStats() {
        try {
            return cacheService.getStats();
        } catch (Exception e) {
            log.warn("Failed to get cache stats: {}", e.getMessage());
            return null;
        }
    }
}