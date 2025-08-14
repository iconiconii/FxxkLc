package com.codetop.event;

import com.codetop.event.Events.*;
import com.codetop.service.CacheInvalidationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event-driven cache invalidation system
 * 
 * Listens to domain events and automatically invalidates related caches
 * to maintain data consistency across the application.
 * 
 * @author CodeTop Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CacheInvalidationEventListener {
    
    private final CacheInvalidationManager cacheInvalidationManager;
    
    /**
     * Handle problem-related events
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleProblemEvent(ProblemEvent event) {
        log.info("Handling problem event: type={}, problemId={}", event.getType(), event.getProblemId());
        
        switch (event.getType()) {
            case CREATED:
            case UPDATED:
            case DELETED:
                cacheInvalidationManager.invalidateProblemCaches();
                break;
            case STATUS_CHANGED:
                // More targeted invalidation for status changes
                cacheInvalidationManager.invalidateCodetopFilterCaches();
                break;
        }
    }
    
    /**
     * Handle user-related events
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleUserEvent(UserEvent event) {
        log.info("Handling user event: type={}, userId={}", event.getType(), event.getUserId());
        
        switch (event.getType()) {
            case PROFILE_UPDATED:
                cacheInvalidationManager.invalidateUserCaches(event.getUserId());
                break;
            case PROGRESS_UPDATED:
                // Clear user progress and mastery caches
                cacheInvalidationManager.invalidateUserCaches(event.getUserId());
                break;
            case PARAMETERS_OPTIMIZED:
                // Clear FSRS-related caches for the user
                cacheInvalidationManager.invalidateFSRSCaches(event.getUserId());
                break;
        }
    }
    
    /**
     * Handle FSRS review events
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleReviewEvent(ReviewEvent event) {
        log.info("Handling review event: type={}, userId={}, problemId={}", 
                event.getType(), event.getUserId(), event.getProblemId());
        
        switch (event.getType()) {
            case REVIEW_COMPLETED:
                // Clear user-specific FSRS caches
                cacheInvalidationManager.invalidateFSRSCaches(event.getUserId());
                
                // Update user mastery cache for this specific problem
                cacheInvalidationManager.evictCacheEntry("codetop-user-mastery", 
                    String.format("codetop:user:mastery:userId_%d:problemId_%d", 
                        event.getUserId(), event.getProblemId()));
                break;
            case BULK_REVIEWS:
                // For bulk operations, clear broader caches
                cacheInvalidationManager.invalidateUserCaches(event.getUserId());
                break;
        }
    }
    
    /**
     * Handle company/organization events
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleCompanyEvent(CompanyEvent event) {
        log.info("Handling company event: type={}, companyId={}", event.getType(), event.getCompanyId());
        
        switch (event.getType()) {
            case COMPANY_UPDATED:
            case DEPARTMENT_UPDATED:
            case POSITION_UPDATED:
                // Company hierarchy changes affect CodeTop filter results
                cacheInvalidationManager.invalidateCompanyCaches();
                break;
            case FREQUENCY_UPDATED:
                // Frequency updates affect rankings and statistics
                cacheInvalidationManager.invalidateCodetopFilterCaches();
                cacheInvalidationManager.invalidateProblemCaches(); // Statistics may change
                break;
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
     * Handle scheduled cache warm-up events
     */
    @EventListener
    @Async
    public void handleCacheWarmupEvent(CacheWarmupEvent event) {
        log.info("Handling cache warm-up event: priority={}", event.getPriority());
        
        // Warm up critical caches based on priority
        cacheInvalidationManager.warmUpCriticalCaches();
    }
}