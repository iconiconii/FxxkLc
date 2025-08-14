package com.codetop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.codetop.algorithm.FSRSAlgorithm;
import com.codetop.dto.FSRSCalculationResult;
import com.codetop.dto.FSRSParametersDTO;
import com.codetop.entity.FSRSCard;
import com.codetop.entity.Problem;
import com.codetop.entity.ReviewLog;
import com.codetop.entity.User;
import com.codetop.enums.FSRSState;
import com.codetop.enums.ReviewType;
import com.codetop.mapper.FSRSCardMapper;
import com.codetop.mapper.ProblemMapper;
import com.codetop.mapper.ReviewLogMapper;
import com.codetop.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * FSRS service for spaced repetition learning system.
 * 
 * Features:
 * - FSRS algorithm implementation
 * - Review queue generation
 * - Learning progress tracking
 * - Parameter optimization
 * - Performance analytics
 * 
 * @author CodeTop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FSRSService {
    
    private final FSRSCardMapper fsrsCardMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final ProblemMapper problemMapper;
    private final UserMapper userMapper;
    private final FSRSAlgorithm fsrsAlgorithm;
    private final UserParametersService userParametersService;
    private final CacheInvalidationManager cacheInvalidationManager;

    /**
     * Get or create FSRS card for user-problem combination.
     */
    @Transactional
    public FSRSCard getOrCreateCard(Long userId, Long problemId) {
        Optional<FSRSCard> existingCard = fsrsCardMapper.findByUserIdAndProblemId(userId, problemId);
        
        if (existingCard.isPresent()) {
            return existingCard.get();
        }

        // Create new card
        FSRSCard card = FSRSCard.builder()
                .userId(userId)
                .problemId(problemId)
                .state(FSRSState.NEW)
                .difficulty(BigDecimal.ZERO)
                .stability(BigDecimal.ZERO)
                .reviewCount(0)
                .lapses(0)
                .intervalDays(0)
                .easeFactor(new BigDecimal("2.5000"))
                .reps(0)
                .build();

        fsrsCardMapper.insert(card);
        log.info("Created new FSRS card for user {} and problem {}", userId, problemId);
        return card;
    }

    /**
     * Process review and update card using FSRS algorithm.
     */
    @Transactional
    public ReviewResult processReview(Long userId, Long problemId, Integer rating, ReviewType reviewType) {
        log.debug("Processing review for user {} problem {} rating {}", userId, problemId, rating);

        FSRSCard card = getOrCreateCard(userId, problemId);
        User user = userMapper.selectById(userId);
        Problem problem = problemMapper.selectById(problemId);

        if (user == null || problem == null) {
            throw new IllegalArgumentException("User or problem not found");
        }

        // Get user-specific FSRS parameters (or default)
        FSRSParametersDTO parameters = getUserFSRSParameters(userId);
        
        // Store old values for logging
        FSRSState oldState = card.getState();
        BigDecimal oldDifficulty = card.getDifficulty();
        BigDecimal oldStability = card.getStability();
        LocalDateTime oldNextReview = card.getNextReview();

        // Calculate new values using FSRS algorithm
        FSRSCalculationResult result = fsrsAlgorithm.calculateNextReview(card, rating, parameters);

        // Update card with new values
        card.setState(result.getNewState());
        card.setDifficulty(result.getNewDifficulty());
        card.setStability(result.getNewStability());
        card.setLastReview(LocalDateTime.now());
        card.setNextReview(result.getNextReviewTime());
        card.setIntervalDays(result.getIntervalDays());
        card.setElapsedDays(result.getElapsedDays());
        card.setReviewCount(card.getReviewCount() + 1);
        card.setGrade(rating);

        // Count lapse if rating was "Again"
        if (rating == 1) {
            card.setLapses(card.getLapses() + 1);
        }

        fsrsCardMapper.updateById(card);

        // Create review log
        ReviewLog reviewLog = ReviewLog.builder()
                .userId(userId)
                .problemId(problemId)
                .cardId(card.getId())
                .rating(rating)
                .reviewType(reviewType)
                .oldState(oldState)
                .newState(result.getNewState())
                .oldDifficulty(oldDifficulty)
                .newDifficulty(result.getNewDifficulty())
                .oldStability(oldStability)
                .newStability(result.getNewStability())
                .elapsedDays(result.getElapsedDays())
                .newIntervalDays(result.getIntervalDays())
                .build();

        reviewLogMapper.insert(reviewLog);

        // Update user parameters training count
        userParametersService.updateTrainingCount(userId, 1);

        // Clear cache for user's review queue
        clearUserReviewQueueCache(userId);

        log.info("Review processed: user={}, problem={}, rating={}, newState={}, nextReview={}", 
                userId, problemId, rating, result.getNewState(), result.getNextReviewTime());

        return ReviewResult.builder()
                .card(card)
                .nextReviewTime(result.getNextReviewTime())
                .intervalDays(result.getIntervalDays())
                .newState(result.getNewState())
                .difficulty(result.getNewDifficulty())
                .stability(result.getNewStability())
                .build();
    }

    /**
     * Generate review queue for user.
     */
    @Cacheable(value = "codetop-fsrs-queue", key = "T(com.codetop.service.CacheKeyBuilder).fsrsReviewQueue(#userId, #limit)")
    public ReviewQueue generateReviewQueue(Long userId, int limit) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Get optimal mix of card types
            int newLimit = Math.min(limit / 3, 10); // Limit new cards
            int learningLimit = Math.min(limit / 2, 20); // Prioritize learning cards
            int reviewLimit = limit; // Fill remaining with review cards

            List<FSRSCardMapper.ReviewQueueCard> cards = fsrsCardMapper.generateOptimalReviewQueue(
                    userId, now, newLimit, learningLimit, reviewLimit);

            // Get user learning statistics (handle null gracefully)
            FSRSCardMapper.UserLearningStats stats;
            try {
                stats = fsrsCardMapper.getUserLearningStats(userId, now);
                if (stats == null) {
                    stats = createEmptyUserLearningStats();
                }
            } catch (Exception e) {
                log.warn("Failed to get user learning stats for user {}, returning empty stats", userId);
                stats = createEmptyUserLearningStats();
            }

            return ReviewQueue.builder()
                    .cards(cards != null ? cards : List.of())
                    .totalCount(cards != null ? cards.size() : 0)
                    .stats(stats)
                    .generatedAt(now)
                    .build();
        } catch (Exception e) {
            log.error("Error generating review queue for user {}: {}", userId, e.getMessage(), e);
            return ReviewQueue.builder()
                    .cards(List.of())
                    .totalCount(0)
                    .stats(createEmptyUserLearningStats())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Get due cards for immediate review.
     */
    public List<FSRSCardMapper.ReviewQueueCard> getDueCards(Long userId, int limit) {
        return fsrsCardMapper.findDueCards(userId, LocalDateTime.now(), limit);
    }

    /**
     * Get new cards for learning.
     */
    public List<FSRSCardMapper.ReviewQueueCard> getNewCards(Long userId, int limit) {
        return fsrsCardMapper.findNewCards(userId, limit);
    }

    /**
     * Get overdue cards.
     */
    public List<FSRSCardMapper.ReviewQueueCard> getOverdueCards(Long userId, int limit) {
        return fsrsCardMapper.findOverdueCards(userId, LocalDateTime.now(), limit);
    }

    /**
     * Get user learning statistics.
     */
    @Cacheable(value = "codetop-fsrs-stats", key = "T(com.codetop.service.CacheKeyBuilder).fsrsUserStats(#userId)")
    public FSRSCardMapper.UserLearningStats getUserLearningStats(Long userId) {
        try {
            FSRSCardMapper.UserLearningStats stats = fsrsCardMapper.getUserLearningStats(userId, LocalDateTime.now());
            return stats != null ? stats : createEmptyUserLearningStats();
        } catch (Exception e) {
            log.warn("Failed to get user learning stats for user {}, returning empty stats: {}", userId, e.getMessage());
            return createEmptyUserLearningStats();
        }
    }

    /**
     * Get all cards for a user.
     */
    public List<FSRSCard> getUserCards(Long userId) {
        try {
            List<FSRSCard> cards = fsrsCardMapper.findByUserId(userId);
            return cards != null ? cards : List.of();
        } catch (Exception e) {
            log.warn("Failed to get user cards for user {}, returning empty list: {}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get cards by state.
     */
    public List<FSRSCard> getUserCardsByState(Long userId, FSRSState state) {
        return fsrsCardMapper.findByUserIdAndState(userId, state.name());
    }

    /**
     * Calculate all possible intervals for a card.
     */
    public int[] calculateAllIntervals(Long userId, Long problemId) {
        FSRSCard card = getOrCreateCard(userId, problemId);
        FSRSParametersDTO parameters = getUserFSRSParameters(userId);
        return fsrsAlgorithm.calculateAllIntervals(card, parameters);
    }

    /**
     * Optimize FSRS parameters for user based on review history.
     */
    @Transactional
    public FSRSParametersDTO optimizeUserParameters(Long userId) {
        if (!userParametersService.isReadyForOptimization(userId)) {
            log.info("User {} is not ready for parameter optimization", userId);
            return getUserFSRSParameters(userId);
        }

        log.info("Starting parameter optimization for user {}", userId);
        
        try {
            userParametersService.optimizeParameters(userId);
            return getUserFSRSParameters(userId);
        } catch (Exception e) {
            log.error("Failed to optimize parameters for user {}: {}", userId, e.getMessage(), e);
            return getUserFSRSParameters(userId);
        }
    }

    /**
     * Get user-specific FSRS parameters.
     */
    private FSRSParametersDTO getUserFSRSParameters(Long userId) {
        return userParametersService.getUserParameters(userId);
    }

    /**
     * Check if user is ready for parameter optimization.
     */
    public boolean isUserReadyForOptimization(Long userId) {
        return userParametersService.isReadyForOptimization(userId);
    }

    /**
     * Clear user's review queue cache.
     */
    public void clearUserReviewQueueCache(Long userId) {
        cacheInvalidationManager.invalidateFSRSCaches(userId);
    }

    /**
     * Get system-wide FSRS performance metrics.
     */
    @Cacheable(value = "codetop-fsrs-metrics", key = "T(com.codetop.service.CacheKeyBuilder).fsrsMetrics(#days)")
    public FSRSCardMapper.SystemFSRSMetrics getSystemMetrics(int days) {
        return fsrsCardMapper.getSystemFSRSMetrics(days);
    }

    /**
     * Create empty user learning statistics for new users.
     */
    private FSRSCardMapper.UserLearningStats createEmptyUserLearningStats() {
        FSRSCardMapper.UserLearningStats stats = new FSRSCardMapper.UserLearningStats();
        stats.setTotalCards(0L);
        stats.setNewCards(0L);
        stats.setLearningCards(0L);
        stats.setReviewCards(0L);
        stats.setRelearningCards(0L);
        stats.setDueCards(0L);
        stats.setAvgReviews(0.0);
        stats.setAvgDifficulty(0.0);
        stats.setAvgStability(0.0);
        stats.setTotalLapses(0L);
        return stats;
    }

    // DTOs

    @lombok.Data
    @lombok.Builder
    public static class ReviewResult {
        private FSRSCard card;
        private LocalDateTime nextReviewTime;
        private Integer intervalDays;
        private FSRSState newState;
        private BigDecimal difficulty;
        private BigDecimal stability;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReviewQueue {
        private List<FSRSCardMapper.ReviewQueueCard> cards;
        private Integer totalCount;
        private FSRSCardMapper.UserLearningStats stats;
        private LocalDateTime generatedAt;
    }
}