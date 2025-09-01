package com.codetop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.codetop.algorithm.FSRSAlgorithm;
import com.codetop.dto.FSRSCalculationResult;
import com.codetop.dto.FSRSParametersDTO;
import com.codetop.dto.FSRSReviewResultDTO;
import com.codetop.dto.FSRSReviewQueueDTO;
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
import com.codetop.service.cache.CacheService;
import com.codetop.util.CacheHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codetop.logging.TraceContext;

import java.math.BigDecimal;
import java.time.Duration;
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
    
    // 新增缓存相关依赖
    private final CacheService cacheService;
    private final CacheHelper cacheHelper;
    
    // 缓存相关常量
    private static final String CACHE_PREFIX_FSRS_QUEUE = "fsrs-queue";
    private static final String CACHE_PREFIX_FSRS_STATS = "fsrs-stats"; 
    private static final String CACHE_PREFIX_FSRS_METRICS = "fsrs-metrics";
    
    private static final Duration FSRS_QUEUE_TTL = Duration.ofMinutes(5);
    private static final Duration FSRS_STATS_TTL = Duration.ofMinutes(10);
    private static final Duration FSRS_METRICS_TTL = Duration.ofHours(1);

    /**
     * Get or create FSRS card for user-problem combination.
     * 依靠数据库唯一约束防止竞态条件
     */
    @Transactional
    public FSRSCard getOrCreateCard(Long userId, Long problemId) {
        TraceContext.setOperation("FSRS_GET_OR_CREATE_CARD");
        TraceContext.setUserId(userId);
        
        long startTime = System.currentTimeMillis();
        log.debug("Getting or creating FSRS card: userId={}, problemId={}", userId, problemId);
        
        try {
            // 先检查是否已存在
            Optional<FSRSCard> existingCard = fsrsCardMapper.findByUserIdAndProblemId(userId, problemId);
            if (existingCard.isPresent()) {
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Found existing FSRS card: cardId={}, duration={}ms", existingCard.get().getId(), duration);
                return existingCard.get();
            }

            // 创建新卡片 - 依靠数据库唯一约束防止重复
            try {
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
                long duration = System.currentTimeMillis() - startTime;
                
                log.info("FSRS card created successfully: userId={}, problemId={}, cardId={}, duration={}ms", 
                        userId, problemId, card.getId(), duration);
                return card;
                
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 数据库唯一约束冲突，重新查询已存在的记录
                log.info("Database constraint violation, querying existing card: userId={}, problemId={}", 
                        userId, problemId);
                existingCard = fsrsCardMapper.findByUserIdAndProblemId(userId, problemId);
                if (existingCard.isPresent()) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.debug("Retrieved existing card after constraint violation: cardId={}, duration={}ms", 
                            existingCard.get().getId(), duration);
                    return existingCard.get();
                } else {
                    log.error("Card should exist after constraint violation but not found: userId={}, problemId={}", 
                            userId, problemId);
                    throw e;
                }
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to get or create FSRS card: userId={}, problemId={}, duration={}ms, error={}", 
                    userId, problemId, duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Process review and update card using FSRS algorithm.
     */
    @Transactional
    public FSRSReviewResultDTO processReview(Long userId, Long problemId, Integer rating, ReviewType reviewType) {
        TraceContext.setOperation("FSRS_PROCESS_REVIEW");
        TraceContext.setUserId(userId);
        
        long startTime = System.currentTimeMillis();
        log.info("Processing FSRS review: userId={}, problemId={}, rating={}, reviewType={}", 
                userId, problemId, rating, reviewType);

        try {
            // Validate input parameters
            if (rating < 1 || rating > 4) {
                log.warn("Invalid review rating: userId={}, problemId={}, rating={}", userId, problemId, rating);
                throw new IllegalArgumentException("Rating must be between 1-4");
            }

            FSRSCard card = getOrCreateCard(userId, problemId);
            User user = userMapper.selectById(userId);
            Problem problem = problemMapper.selectById(problemId);

            if (user == null || problem == null) {
                log.error("User or problem not found: userId={}, problemId={}, userExists={}, problemExists={}", 
                        userId, problemId, user != null, problem != null);
                throw new IllegalArgumentException("User or problem not found");
            }

            // Get user-specific FSRS parameters (or default)
            FSRSParametersDTO parameters = getUserFSRSParameters(userId);
            log.debug("Using FSRS parameters for user {}: isDefault={}", userId, 
                    parameters.isDefault());
            
            // Store old values for logging and audit
            FSRSState oldState = card.getState();
            BigDecimal oldDifficulty = card.getDifficulty();
            BigDecimal oldStability = card.getStability();
            LocalDateTime oldNextReview = card.getNextReview();
            int oldReviewCount = card.getReviewCount();

            long algorithmStartTime = System.currentTimeMillis();
            // Calculate new values using FSRS algorithm
            FSRSCalculationResult result = fsrsAlgorithm.calculateNextReview(card, rating, parameters);
            long algorithmDuration = System.currentTimeMillis() - algorithmStartTime;
            
            log.debug("FSRS algorithm calculation completed: userId={}, duration={}ms, " +
                    "oldState={}, newState={}, oldDifficulty={}, newDifficulty={}, " +
                    "oldStability={}, newStability={}, intervalDays={}", 
                    userId, algorithmDuration, oldState, result.getNewState(), 
                    oldDifficulty, result.getNewDifficulty(), oldStability, result.getNewStability(), 
                    result.getIntervalDays());

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
            boolean isLapse = rating == 1;
            if (isLapse) {
                card.setLapses(card.getLapses() + 1);
                log.info("Lapse recorded: userId={}, problemId={}, totalLapses={}", 
                        userId, problemId, card.getLapses());
            }

            // Update card in database
            long dbStartTime = System.currentTimeMillis();
            fsrsCardMapper.updateById(card);
            long dbUpdateDuration = System.currentTimeMillis() - dbStartTime;

            // Create review log for audit trail
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
            log.info("Review log inserted successfully: userId={}, problemId={}, cardId={}", userId, problemId, card.getId());

            // Update user parameters review count
            log.info("Starting to update review count: userId={}", userId);
            try {
                userParametersService.updateReviewCount(userId, 1);
                log.info("Review count updated successfully: userId={}", userId);
            } catch (Exception e) {
                log.error("Failed to update review count: userId={}, error={}", userId, e.getMessage(), e);
                throw e;
            }

            // Clear cache for user's review queue
            log.info("Starting to clear review queue cache: userId={}", userId);
            try {
                clearUserReviewQueueCache(userId);
                log.info("Review queue cache cleared successfully: userId={}", userId);
            } catch (Exception e) {
                log.error("Failed to clear review queue cache: userId={}, error={}", userId, e.getMessage(), e);
                throw e;
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            
            // Business metrics logging
            log.info("FSRS review completed successfully: userId={}, problemId={}, cardId={}, " +
                    "rating={}, reviewType={}, oldState={}, newState={}, isLapse={}, " +
                    "nextReview={}, intervalDays={}, totalDuration={}ms, algorithmDuration={}ms, dbDuration={}ms", 
                    userId, problemId, card.getId(), rating, reviewType, oldState, 
                    result.getNewState(), isLapse, result.getNextReviewTime(), result.getIntervalDays(),
                    totalDuration, algorithmDuration, dbUpdateDuration);

            return FSRSReviewResultDTO.builder()
                    .card(card)
                    .nextReviewTime(result.getNextReviewTime())
                    .intervalDays(result.getIntervalDays())
                    .newState(result.getNewState())
                    .difficulty(result.getNewDifficulty())
                    .stability(result.getNewStability())
                    .build();
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to process FSRS review: userId={}, problemId={}, rating={}, " +
                    "reviewType={}, duration={}ms, error={}", 
                    userId, problemId, rating, reviewType, duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Generate review queue for user with manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrCompute() 进行缓存操作
     * - 保持相同的缓存键和5分钟TTL
     * - 在用户提交复习后通过 clearUserReviewQueueCache 方法失效
     */
    public FSRSReviewQueueDTO generateReviewQueue(Long userId, int limit) {
        if (userId == null) {
            log.warn("generateReviewQueue called with null userId");
            return FSRSReviewQueueDTO.builder()
                    .cards(List.of())
                    .totalCount(0)
                    .stats(createEmptyUserLearningStats())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
        
        String cacheKey = CacheKeyBuilder.fsrsReviewQueue(userId, limit);
        
        return cacheHelper.cacheOrCompute(
            cacheKey,
            FSRSReviewQueueDTO.class,
            FSRS_QUEUE_TTL,
            () -> {
                TraceContext.setOperation("FSRS_GENERATE_REVIEW_QUEUE");
                TraceContext.setUserId(userId);
                
                long startTime = System.currentTimeMillis();
                log.info("Cache MISS - Generating FSRS review queue: userId={}, requestedLimit={}", userId, limit);
                
                try {
                    LocalDateTime now = LocalDateTime.now();
                    
                    // Get optimal mix of card types
                    int newLimit = Math.min(limit / 3, 10); // Limit new cards
                    int learningLimit = Math.min(limit / 2, 20); // Prioritize learning cards
                    int reviewLimit = limit; // Fill remaining with review cards
                    
                    log.debug("Queue generation limits: userId={}, newLimit={}, learningLimit={}, reviewLimit={}", 
                            userId, newLimit, learningLimit, reviewLimit);

                    long dbStartTime = System.currentTimeMillis();
                    List<FSRSCardMapper.ReviewQueueCard> cards = fsrsCardMapper.generateOptimalReviewQueue(
                            userId, now, newLimit, learningLimit, reviewLimit);
                    long dbDuration = System.currentTimeMillis() - dbStartTime;

                    // Get user learning statistics (handle null gracefully)
                    FSRSCardMapper.UserLearningStats stats;
                    try {
                        long statsStartTime = System.currentTimeMillis();
                        stats = fsrsCardMapper.getUserLearningStats(userId, now);
                        long statsDuration = System.currentTimeMillis() - statsStartTime;
                        
                        if (stats == null) {
                            log.debug("No learning stats found for user {}, using empty stats", userId);
                            stats = createEmptyUserLearningStats();
                        } else {
                            log.debug("Retrieved learning stats: userId={}, totalCards={}, dueCards={}, duration={}ms", 
                                    userId, stats.getTotalCards(), stats.getDueCards(), statsDuration);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get user learning stats for user {}, returning empty stats: {}", 
                                userId, e.getMessage());
                        stats = createEmptyUserLearningStats();
                    }

                    int actualCardCount = cards != null ? cards.size() : 0;
                    long totalDuration = System.currentTimeMillis() - startTime;
                    
                    // Business metrics logging
                    log.info("Review queue generated successfully: userId={}, requestedLimit={}, actualCards={}, " +
                            "totalDuration={}ms, dbDuration={}ms, cacheHit={}", 
                            userId, limit, actualCardCount, totalDuration, dbDuration, false);
                    
                    if (actualCardCount < limit) {
                        log.debug("Generated fewer cards than requested: userId={}, requested={}, actual={}, " +
                                "possibleReasons=[insufficient_due_cards, learning_cards_prioritized]", 
                                userId, limit, actualCardCount);
                    }

                    return FSRSReviewQueueDTO.builder()
                            .cards(cards != null ? cards : List.of())
                            .totalCount(actualCardCount)
                            .stats(stats)
                            .generatedAt(now)
                            .build();
                            
                } catch (Exception e) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Failed to generate review queue: userId={}, limit={}, duration={}ms, error={}", 
                            userId, limit, duration, e.getMessage(), e);
                    return FSRSReviewQueueDTO.builder()
                            .cards(List.of())
                            .totalCount(0)
                            .stats(createEmptyUserLearningStats())
                            .generatedAt(LocalDateTime.now())
                            .build();
                }
            }
        );
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
     * Get all problems that need review, excluding those reviewed today.
     * This includes new cards, learning/relearning cards, and due review cards.
     * Ordered by next review time ascending to prioritize urgent reviews.
     */
    public List<FSRSCardMapper.ReviewQueueCard> getAllDueProblems(Long userId, int limit) {
        TraceContext.setOperation("FSRS_GET_ALL_DUE_PROBLEMS");
        TraceContext.setUserId(userId);
        
        long startTime = System.currentTimeMillis();
        log.info("Getting all due problems: userId={}, limit={}", userId, limit);
        
        try {
            LocalDateTime now = LocalDateTime.now();
            List<FSRSCardMapper.ReviewQueueCard> cards = fsrsCardMapper.getAllDueProblems(userId, now, limit);
            
            long duration = System.currentTimeMillis() - startTime;
            int actualCount = cards != null ? cards.size() : 0;
            
            log.info("Retrieved all due problems: userId={}, requestedLimit={}, actualCount={}, duration={}ms", 
                    userId, limit, actualCount, duration);
            
            return cards != null ? cards : List.of();
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to get all due problems: userId={}, limit={}, duration={}ms, error={}", 
                    userId, limit, duration, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get user learning statistics with manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrCompute() 进行缓存操作
     * - 保持相同的缓存键和10分钟TTL
     * - 优雅处理空统计数据
     */
    public FSRSCardMapper.UserLearningStats getUserLearningStats(Long userId) {
        if (userId == null) {
            log.warn("getUserLearningStats called with null userId");
            return createEmptyUserLearningStats();
        }
        
        String cacheKey = CacheKeyBuilder.fsrsUserStats(userId);
        
        return cacheHelper.cacheOrCompute(
            cacheKey,
            FSRSCardMapper.UserLearningStats.class,
            FSRS_STATS_TTL,
            () -> {
                log.debug("Cache MISS - Loading user learning stats from database: userId={}", userId);
                
                try {
                    FSRSCardMapper.UserLearningStats stats = fsrsCardMapper.getUserLearningStats(userId, LocalDateTime.now());
                    
                    if (stats != null) {
                        log.debug("Retrieved learning stats: userId={}, totalCards={}, dueCards={}", 
                                userId, stats.getTotalCards(), stats.getDueCards());
                        return stats;
                    } else {
                        log.debug("No learning stats found for user {}, returning empty stats", userId);
                        return createEmptyUserLearningStats();
                    }
                } catch (Exception e) {
                    log.warn("Failed to get user learning stats for user {}, returning empty stats: {}", userId, e.getMessage());
                    return createEmptyUserLearningStats();
                }
            }
        );
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
     * Clear user's FSRS related caches after review submission.
     * 
     * 更新为手动缓存管理：
     * - 清理用户复习队列缓存（所有limit的组合）
     * - 清理用户学习统计缓存
     * - 保留系统指标缓存（不受单个用户影响）
     * - 保持向后兼容性，同时调用旧的缓存失效管理器
     */
    public void clearUserReviewQueueCache(Long userId) {
        if (userId == null) {
            log.warn("clearUserReviewQueueCache called with null userId");
            return;
        }
        
        try {
            // 清理FSRS复习队列缓存（使用通配符模式）
            String queuePattern = CacheKeyBuilder.buildUserDomainPattern("fsrs", userId);
            long deletedQueueCount = cacheService.deleteByPattern(queuePattern);
            
            // 清理用户学习统计缓存
            String statsKey = CacheKeyBuilder.fsrsUserStats(userId);
            boolean statsDeleted = cacheService.delete(statsKey);
            
            log.debug("FSRS cache invalidation completed: userId={}, queueCacheDeleted={}, statsDeleted={}", 
                    userId, deletedQueueCount, statsDeleted);
            
            // 保持向后兼容性 - 同时调用旧的缓存失效管理器
            cacheInvalidationManager.invalidateFSRSCaches(userId);
            
        } catch (Exception e) {
            log.error("Failed to clear FSRS caches for user {}: {}", userId, e.getMessage(), e);
            
            // 如果手动缓存清理失败，仍然尝试旧的方式
            try {
                cacheInvalidationManager.invalidateFSRSCaches(userId);
            } catch (Exception fallbackError) {
                log.error("Fallback cache invalidation also failed for user {}: {}", userId, fallbackError.getMessage(), fallbackError);
            }
        }
    }

    /**
     * Get system-wide FSRS performance metrics with manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrCompute() 进行缓存操作
     * - 保持相同的缓存键和1小时TTL
     * - 系统级指标适合较长的缓存时间
     */
    public FSRSCardMapper.SystemFSRSMetrics getSystemMetrics(int days) {
        String cacheKey = CacheKeyBuilder.fsrsMetrics(days);
        
        return cacheHelper.cacheOrCompute(
            cacheKey,
            FSRSCardMapper.SystemFSRSMetrics.class,
            FSRS_METRICS_TTL,
            () -> {
                log.debug("Cache MISS - Loading system FSRS metrics from database: days={}", days);
                
                try {
                    FSRSCardMapper.SystemFSRSMetrics metrics = fsrsCardMapper.getSystemFSRSMetrics(days);
                    
                    if (metrics != null) {
                        log.debug("Retrieved system metrics: days={}, totalUsers={}, totalReviews={}", 
                                days, metrics.getTotalUsers(), metrics.getTotalReviews());
                    } else {
                        log.warn("No system metrics found for days={}", days);
                    }
                    
                    return metrics;
                } catch (Exception e) {
                    log.error("Failed to get system FSRS metrics for days={}, error={}", days, e.getMessage(), e);
                    throw e; // 重新抛出异常，让调用方处理
                }
            }
        );
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