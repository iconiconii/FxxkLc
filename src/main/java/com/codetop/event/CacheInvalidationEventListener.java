package com.codetop.event;

import com.codetop.event.Events.*;
import com.codetop.service.CacheInvalidationManager;
import com.codetop.service.CacheInvalidationStrategy;
import com.codetop.service.CodeTopFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Synchronous event-driven cache invalidation system with Cache-First Strategy
 * 
 * Strategy Change:
 * - Removed @Async to ensure synchronous cache invalidation
 * - Cache invalidation now happens BEFORE database updates
 * - Uses BEFORE_COMMIT phase for immediate consistency
 * - Ensures database is always the source of truth
 * 
 * @author CodeTop Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CacheInvalidationEventListener {
    
    private final CacheInvalidationManager cacheInvalidationManager;
    private final CacheInvalidationStrategy cacheInvalidationStrategy;
    private final CodeTopFilterService codeTopFilterService;
    
    /**
     * Handle problem-related events - Synchronous execution with cache-first strategy
     * Cache invalidation happens BEFORE database commit to ensure consistency
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleProblemEvent(ProblemEvent event) {
        log.info("Synchronously handling problem event: type={}, problemId={}", event.getType(), event.getProblemId());
        
        try {
            switch (event.getType()) {
                case CREATED:
                case UPDATED:
                case DELETED:
                    // Use synchronous cache invalidation strategy
                    cacheInvalidationStrategy.invalidateProblemCachesSync();
                    log.info("Problem caches invalidated synchronously for event: {}", event.getType());
                    break;
                case STATUS_CHANGED:
                    // More targeted invalidation for status changes
                    cacheInvalidationStrategy.invalidateCodetopFilterCachesSync();
                    log.info("CodeTop filter caches invalidated synchronously for status change");
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to handle problem event synchronously: type={}, problemId={}", 
                     event.getType(), event.getProblemId(), e);
            // Don't re-throw to avoid breaking the main transaction
        }
    }
    
    /**
     * Handle user-related events - Synchronous execution with cache-first strategy
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleUserEvent(UserEvent event) {
        log.info("Synchronously handling user event: type={}, userId={}", event.getType(), event.getUserId());
        
        try {
            switch (event.getType()) {
                case PROFILE_UPDATED:
                    cacheInvalidationStrategy.invalidateUserCachesSync(event.getUserId());
                    log.info("User caches invalidated synchronously for profile update: userId={}", event.getUserId());
                    break;
                case PROGRESS_UPDATED:
                    // Clear user progress and mastery caches
                    cacheInvalidationStrategy.invalidateUserCachesSync(event.getUserId());
                    // Also clear unified API cache for this user
                    codeTopFilterService.clearUserStatusCache(event.getUserId());
                    log.info("User progress caches invalidated synchronously: userId={}", event.getUserId());
                    break;
                case PARAMETERS_OPTIMIZED:
                    // Clear FSRS-related caches for the user
                    cacheInvalidationStrategy.invalidateFSRSCachesSync(event.getUserId());
                    log.info("FSRS caches invalidated synchronously for parameter optimization: userId={}", event.getUserId());
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to handle user event synchronously: type={}, userId={}", 
                     event.getType(), event.getUserId(), e);
            // Don't re-throw to avoid breaking the main transaction
        }
    }
    
    /**
     * Handle FSRS review events - Synchronous execution with cache-first strategy
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReviewEvent(ReviewEvent event) {
        log.info("Synchronously handling review event: type={}, userId={}, problemId={}", 
                event.getType(), event.getUserId(), event.getProblemId());
        
        try {
            switch (event.getType()) {
                case REVIEW_COMPLETED:
                    // Clear user-specific FSRS caches
                    cacheInvalidationStrategy.invalidateFSRSCachesSync(event.getUserId());
                    
                    // Update user mastery cache for this specific problem
                    cacheInvalidationStrategy.evictCacheEntrySync("codetop-user-mastery", 
                        String.format("codetop:user:mastery:userId_%d:problemId_%d", 
                            event.getUserId(), event.getProblemId()));
                    
                    log.info("Review completion caches invalidated synchronously: userId={}, problemId={}", 
                             event.getUserId(), event.getProblemId());
                    break;
                case BULK_REVIEWS:
                    // For bulk operations, clear broader caches
                    cacheInvalidationStrategy.invalidateUserCachesSync(event.getUserId());
                    log.info("Bulk review caches invalidated synchronously: userId={}", event.getUserId());
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to handle review event synchronously: type={}, userId={}, problemId={}", 
                     event.getType(), event.getUserId(), event.getProblemId(), e);
            // Don't re-throw to avoid breaking the main transaction
        }
    }
    
    /**
     * Handle company/organization events - Synchronous execution with cache-first strategy
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleCompanyEvent(CompanyEvent event) {
        log.info("Synchronously handling company event: type={}, companyId={}", event.getType(), event.getCompanyId());
        
        try {
            switch (event.getType()) {
                case COMPANY_UPDATED:
                case DEPARTMENT_UPDATED:
                case POSITION_UPDATED:
                    // Company hierarchy changes affect CodeTop filter results
                    cacheInvalidationStrategy.invalidateCodetopFilterCachesSync();
                    cacheInvalidationStrategy.invalidateProblemCachesSync();
                    log.info("Company hierarchy caches invalidated synchronously: companyId={}", event.getCompanyId());
                    break;
                case FREQUENCY_UPDATED:
                    // Frequency updates affect rankings and statistics
                    cacheInvalidationStrategy.invalidateCodetopFilterCachesSync();
                    cacheInvalidationStrategy.invalidateProblemCachesSync(); // Statistics may change
                    log.info("Frequency update caches invalidated synchronously: companyId={}", event.getCompanyId());
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to handle company event synchronously: type={}, companyId={}", 
                     event.getType(), event.getCompanyId(), e);
            // Don't re-throw to avoid breaking the main transaction
        }
    }
    
    /**
     * Handle system-wide cache flush events
     */
    @EventListener
    public void handleCacheFlushEvent(CacheFlushEvent event) {
        log.warn("Handling cache flush event: scope={}, reason={}", event.getScope(), event.getReason());
        
        switch (event.getScope()) {
            case ALL:
                cacheInvalidationManager.flushAllCaches();
                break;
            case PROBLEMS:
                cacheInvalidationManager.invalidateProblemCaches();
                break;
            case USERS:
                // This would require iterating through all users - use carefully
                log.warn("Bulk user cache flush requested - this is expensive");
                break;
            case FSRS:
                // Clear all FSRS-related caches
                cacheInvalidationManager.flushAllCaches(); // Simplified for now
                break;
        }
    }
    
    /**
     * Handle scheduled cache warm-up events - Synchronous execution for immediate completion
     */
    @EventListener
    public void handleCacheWarmupEvent(CacheWarmupEvent event) {
        log.info("Synchronously handling cache warm-up event: priority={}", event.getPriority());
        
        try {
            // Warm up critical caches based on priority
            cacheInvalidationManager.warmUpCriticalCaches();
            log.info("Cache warm-up completed synchronously");
        } catch (Exception e) {
            log.error("Failed to handle cache warm-up event synchronously", e);
            // Don't re-throw to avoid breaking event processing
        }
    }
}