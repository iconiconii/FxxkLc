package com.codetop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.entity.Problem;
import com.codetop.enums.Difficulty;
import com.codetop.mapper.ProblemMapper;
import com.codetop.entity.FSRSCard;
import com.codetop.enums.ReviewType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Problem service for managing algorithm problems.
 * 
 * Features:
 * - Simplified problem management for FSRS algorithm
 * - Problem search and filtering by title and tags
 * - Company-based problem queries
 * - Basic statistics
 * - Tag management
 * - Caching for performance
 * 
 * @author CodeTop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProblemService {
    
    private final ProblemMapper problemMapper;
    private final FSRSService fsrsService;
    private final CacheInvalidationManager cacheInvalidationManager;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Get problem by ID with caching.
     */
    @Cacheable(value = "codetop-problem-single", key = "T(com.codetop.service.CacheKeyBuilder).problemSingle(#problemId)")
    public Optional<Problem> findById(Long problemId) {
        return Optional.ofNullable(problemMapper.selectById(problemId));
    }

    /**
     * Find all problems with optional filters.
     */
    @Cacheable(value = "codetop-problem-list", key = "T(com.codetop.service.CacheKeyBuilder).problemList(#difficulty, #page.current, #page.size, #search)")
    public Page<Problem> findAllProblems(Page<Problem> page, String difficulty, String search) {
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            if (search != null && !search.trim().isEmpty()) {
                return problemMapper.findByDifficultyAndSearch(page, difficulty, search.trim());
            }
            return problemMapper.findByDifficultyWithPagination(page, difficulty);
        } else if (search != null && !search.trim().isEmpty()) {
            return problemMapper.searchProblems(page, search.trim());
        }
        return problemMapper.selectPage(page, null);
    }

    /**
     * Search problems with pagination.
     */
    @Cacheable(value = "codetop-problem-search", key = "T(com.codetop.service.CacheKeyBuilder).problemSearch(#keyword, #page.current, #page.size)")
    public Page<Problem> searchProblems(String keyword, Page<Problem> page) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return problemMapper.selectPage(page, null);
        }
        return problemMapper.searchProblems(page, keyword.trim());
    }

    /**
     * Advanced search with multiple filters.
     */
    public Page<Problem> advancedSearch(AdvancedSearchRequest request, Page<Problem> page) {
        return problemMapper.advancedSearch(
            page,
            request.getKeyword(),
            request.getDifficulty() != null ? request.getDifficulty().name() : null,
            request.getTag(),
            request.getIsPremium()
        );
    }

    /**
     * Get problems by difficulty.
     */
    @Cacheable(value = "codetop-problem-difficulty", key = "T(com.codetop.service.CacheKeyBuilder).problemsByDifficulty(#difficulty.name(), #page.current)")
    public Page<Problem> findByDifficulty(Difficulty difficulty, Page<Problem> page) {
        return problemMapper.findByDifficultyWithPagination(page, difficulty.name());
    }

    /**
     * Get problems by tag.
     */
    public Page<Problem> findByTag(String tag, Page<Problem> page) {
        return problemMapper.findByTag(page, tag);
    }

    /**
     * Get problems by company.
     */
    public Page<Problem> findByCompany(Long companyId, Page<Problem> page) {
        return problemMapper.findByCompanyId(page, companyId);
    }

    /**
     * Get problems by company name.
     */
    public Page<Problem> findByCompanyName(String companyName, Page<Problem> page) {
        return problemMapper.findByCompanyName(page, companyName);
    }

    /**
     * Get hot problems (frequently asked).
     */
    @Cacheable(value = "codetop-problem-hot", key = "T(com.codetop.service.CacheKeyBuilder).problemsHot(#minCompanies, #limit)")
    public List<ProblemMapper.HotProblem> getHotProblems(int minCompanies, int limit) {
        return problemMapper.findHotProblems(minCompanies, limit);
    }

    /**
     * Get recently added problems.
     */
    @Cacheable(value = "codetop-problem-recent", key = "T(com.codetop.service.CacheKeyBuilder).problemsRecent(#limit)")
    public List<Problem> getRecentProblems(int limit) {
        return problemMapper.findRecentProblems(limit);
    }

    /**
     * Get all tags with usage statistics.
     */
    @Cacheable(value = "codetop-tag-stats", key = "T(com.codetop.service.CacheKeyBuilder).tagStatistics()")
    public List<ProblemMapper.TagUsage> getTagStatistics() {
        return problemMapper.getTagUsageStatistics();
    }

    /**
     * Get problem statistics.
     */
    @Cacheable(value = "codetop-problem-stats", key = "T(com.codetop.service.CacheKeyBuilder).problemStatistics()")
    public ProblemStatistics getStatistics() {
        Long totalProblems = problemMapper.countActiveProblems();
        Long easyCount = problemMapper.countByDifficulty(Difficulty.EASY.name());
        Long mediumCount = problemMapper.countByDifficulty(Difficulty.MEDIUM.name());
        Long hardCount = problemMapper.countByDifficulty(Difficulty.HARD.name());
        Long premiumCount = problemMapper.countPremiumProblems();

        return ProblemStatistics.builder()
                .totalProblems(totalProblems)
                .easyCount(easyCount)
                .mediumCount(mediumCount)
                .hardCount(hardCount)
                .premiumCount(premiumCount)
                .build();
    }

    /**
     * Create new problem.
     */
    @Transactional
    public Problem createProblem(CreateProblemRequest request) {
        Problem problem = Problem.builder()
                .title(request.getTitle())
                .difficulty(request.getDifficulty())
                .tags(request.getTagsJson())
                .problemUrl(request.getProblemUrl())
                .leetcodeId(request.getLeetcodeId())
                .isPremium(request.getIsPremium())
                .build();

        problemMapper.insert(problem);
        log.info("Created new problem: {}", problem.getTitle());
        
        // Publish event for cache invalidation
        eventPublisher.publishEvent(new com.codetop.event.Events.ProblemEvent(
            com.codetop.event.Events.ProblemEvent.ProblemEventType.CREATED,
            problem.getId(),
            problem.getTitle()
        ));
        
        return problem;
    }

    /**
     * Update problem.
     */
    @Transactional
    public Problem updateProblem(Long problemId, UpdateProblemRequest request) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new IllegalArgumentException("Problem not found");
        }

        if (request.getTitle() != null) problem.setTitle(request.getTitle());
        if (request.getDifficulty() != null) problem.setDifficulty(request.getDifficulty());
        if (request.getTagsJson() != null) problem.setTags(request.getTagsJson());
        if (request.getProblemUrl() != null) problem.setProblemUrl(request.getProblemUrl());

        problemMapper.updateById(problem);
        log.info("Updated problem: {}", problem.getTitle());
        
        // Publish event for cache invalidation
        eventPublisher.publishEvent(new com.codetop.event.Events.ProblemEvent(
            com.codetop.event.Events.ProblemEvent.ProblemEventType.UPDATED,
            problem.getId(),
            problem.getTitle()
        ));
        
        return problem;
    }

    /**
     * Delete problem (soft delete).
     */
    @Transactional
    public void deleteProblem(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new IllegalArgumentException("Problem not found");
        }

        String problemTitle = problem.getTitle();
        problem.softDelete();
        problemMapper.updateById(problem);
        log.info("Soft deleted problem: {}", problemTitle);
        
        // Publish event for cache invalidation
        eventPublisher.publishEvent(new com.codetop.event.Events.ProblemEvent(
            com.codetop.event.Events.ProblemEvent.ProblemEventType.DELETED,
            problemId,
            problemTitle
        ));
    }

    // DTOs

    @lombok.Data
    @lombok.Builder
    public static class ProblemStatistics {
        private Long totalProblems;
        private Long easyCount;
        private Long mediumCount;
        private Long hardCount;
        private Long premiumCount;
    }

    @lombok.Data
    public static class AdvancedSearchRequest {
        private String keyword;
        private Difficulty difficulty;
        private String tag;
        private Boolean isPremium;
    }

    @lombok.Data
    public static class CreateProblemRequest {
        private String title;
        private Difficulty difficulty;
        private String tagsJson;
        private String problemUrl;
        private String leetcodeId;
        private Boolean isPremium = false;
    }

    @lombok.Data
    public static class UpdateProblemRequest {
        private String title;
        private Difficulty difficulty;
        private String tagsJson;
        private String problemUrl;
    }

    // User problem status methods

    /**
     * Get user's problem progress for all problems.
     */
    @Cacheable(value = "codetop-user-progress", key = "T(com.codetop.service.CacheKeyBuilder).userProblemProgress(#userId)")
    public List<com.codetop.controller.ProblemController.UserProblemStatus> getUserProblemProgress(Long userId) {
        // This is a simplified implementation - would typically query user's FSRS cards and review logs
        List<Problem> problems = problemMapper.selectList(null);
        
        return problems.stream()
                .limit(10) // Limit for demo purposes
                .map(problem -> com.codetop.controller.ProblemController.UserProblemStatus.builder()
                        .problemId(problem.getId())
                        .title(problem.getTitle())
                        .difficulty(problem.getDifficulty())
                        .status("not_done")
                        .mastery(0)
                        .lastAttemptDate(null)
                        .lastConsideredDate(null)
                        .attemptCount(0)
                        .accuracy(0.0)
                        .notes("")
                        .build())
                .toList();
    }

    /**
     * Update problem status for user.
     */
    @Transactional
    public com.codetop.controller.ProblemController.UserProblemStatus updateProblemStatus(
            Long userId, Long problemId, com.codetop.controller.ProblemController.UpdateProblemStatusRequest request) {
        
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new IllegalArgumentException("Problem not found");
        }
        
        try {
            // Convert mastery level to FSRS rating (1-4 scale)
            Integer fsrsRating = convertMasteryToFSRSRating(request.getMastery(), request.getStatus());
            
            // Process FSRS review to update card state and scheduling
            FSRSService.ReviewResult reviewResult = fsrsService.processReview(
                    userId, problemId, fsrsRating, 
                    determineReviewType(request.getStatus())
            );
            
            FSRSCard card = reviewResult.getCard();
            
            // Calculate accuracy based on FSRS stability and difficulty
            Double accuracy = calculateAccuracyFromFSRS(card);
            
            log.info("Updated problem {} status to {} for user {} with FSRS rating {} - next review: {}", 
                    problemId, request.getStatus(), userId, fsrsRating, reviewResult.getNextReviewTime());
            
            return com.codetop.controller.ProblemController.UserProblemStatus.builder()
                    .problemId(problemId)
                    .title(problem.getTitle())
                    .difficulty(problem.getDifficulty())
                    .status(request.getStatus())
                    .mastery(request.getMastery())
                    .lastAttemptDate(LocalDateTime.now().toString())
                    .lastConsideredDate(reviewResult.getNextReviewTime() != null ? 
                            reviewResult.getNextReviewTime().toString() : LocalDateTime.now().toString())
                    .attemptCount(card.getReviewCount())
                    .accuracy(accuracy)
                    .notes(request.getNotes())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to update FSRS status for user {} problem {}: {}", userId, problemId, e.getMessage());
            
            // Fallback to basic status update without FSRS integration
            return com.codetop.controller.ProblemController.UserProblemStatus.builder()
                    .problemId(problemId)
                    .title(problem.getTitle())
                    .difficulty(problem.getDifficulty())
                    .status(request.getStatus())
                    .mastery(request.getMastery())
                    .lastAttemptDate(LocalDateTime.now().toString())
                    .lastConsideredDate(LocalDateTime.now().toString())
                    .attemptCount(1)
                    .accuracy(0.0)
                    .notes(request.getNotes())
                    .build();
        }
    }

    /**
     * Get problem mastery level for user with FSRS integration.
     */
    @Cacheable(value = "codetop-user-mastery", key = "T(com.codetop.service.CacheKeyBuilder).userProblemMastery(#userId, #problemId)")
    public com.codetop.controller.ProblemController.ProblemMastery getProblemMastery(Long userId, Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new IllegalArgumentException("Problem not found");
        }
        
        try {
            // Get or create FSRS card for this user-problem combination
            FSRSCard card = fsrsService.getOrCreateCard(userId, problemId);
            
            // Calculate mastery level based on FSRS state and stability
            Integer masteryLevel = calculateMasteryLevel(card);
            
            // Calculate accuracy from FSRS metrics
            Double accuracy = calculateAccuracyFromFSRS(card);
            
            // Format dates for response
            String lastAttemptDate = card.getLastReview() != null ? 
                    card.getLastReview().toString() : null;
            String nextReviewDate = card.getNextReview() != null ? 
                    card.getNextReview().toString() : null;
            
            return com.codetop.controller.ProblemController.ProblemMastery.builder()
                    .problemId(problemId)
                    .masteryLevel(masteryLevel)
                    .attemptCount(card.getReviewCount() != null ? card.getReviewCount() : 0)
                    .accuracy(accuracy)
                    .lastAttemptDate(lastAttemptDate)
                    .nextReviewDate(nextReviewDate)
                    .difficulty(problem.getDifficulty().name())
                    .notes(String.format("Stability: %.2f, Difficulty: %.2f, State: %s, Lapses: %d", 
                            card.getStabilityAsDouble(), 
                            card.getDifficultyAsDouble(),
                            card.getState().name(),
                            card.getLapses() != null ? card.getLapses() : 0))
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to get FSRS mastery for user {} problem {}: {}", userId, problemId, e.getMessage());
            
            // Fallback to basic response
            return com.codetop.controller.ProblemController.ProblemMastery.builder()
                    .problemId(problemId)
                    .masteryLevel(0)
                    .attemptCount(0)
                    .accuracy(0.0)
                    .lastAttemptDate(null)
                    .nextReviewDate(null)
                    .difficulty(problem.getDifficulty().name())
                    .notes("FSRS data unavailable")
                    .build();
        }
    }

    /**
     * Get user's learning statistics across all problems.
     */
    public UserLearningStatistics getUserLearningStatistics(Long userId) {
        try {
            // Get FSRS learning stats
            var fsrsStats = fsrsService.getUserLearningStats(userId);
            
            // Get basic problem counts
            List<Problem> userProblems = findProblemsForUser(userId, null, null);
            
            // Calculate derived statistics
            int totalProblems = userProblems.size();
            int solvedProblems = (int) userProblems.stream()
                    .filter(p -> hasUserSolvedProblem(userId, p.getId()))
                    .count();
            
            Map<String, Integer> difficultyBreakdown = calculateDifficultyBreakdown(userProblems);
            
            return UserLearningStatistics.builder()
                    .totalProblems(totalProblems)
                    .solvedProblems(solvedProblems)
                    .newCards(fsrsStats.getNewCards().intValue())
                    .learningCards(fsrsStats.getLearningCards().intValue())
                    .reviewCards(fsrsStats.getReviewCards().intValue())
                    .relearningCards(fsrsStats.getRelearningCards().intValue())
                    .dueCards(fsrsStats.getDueCards().intValue())
                    .averageAccuracy(fsrsStats.getAvgStability() != null ? fsrsStats.getAvgStability() * 20 : 0.0) // Convert stability to percentage
                    .averageDifficulty(fsrsStats.getAvgDifficulty() != null ? fsrsStats.getAvgDifficulty() : 0.0)
                    .totalReviews(fsrsStats.getTotalCards().intValue())
                    .totalLapses(fsrsStats.getTotalLapses().intValue())
                    .difficultyBreakdown(difficultyBreakdown)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to get user learning statistics for user {}: {}", userId, e.getMessage());
            return UserLearningStatistics.builder()
                    .totalProblems(0)
                    .solvedProblems(0)
                    .newCards(0)
                    .learningCards(0)
                    .reviewCards(0)
                    .relearningCards(0)
                    .dueCards(0)
                    .averageAccuracy(0.0)
                    .averageDifficulty(0.0)
                    .totalReviews(0)
                    .totalLapses(0)
                    .difficultyBreakdown(Map.of())
                    .build();
        }
    }

    /**
     * Mark problem as mastered (highest proficiency).
     */
    @Transactional
    public void markProblemAsMastered(Long userId, Long problemId) {
        try {
            // Process with highest FSRS rating (Easy = 4)
            fsrsService.processReview(userId, problemId, 4, ReviewType.MANUAL);
            
            // Additional logic for mastery tracking could be added here
            log.info("Marked problem {} as mastered for user {}", problemId, userId);
            
        } catch (Exception e) {
            log.error("Failed to mark problem {} as mastered for user {}: {}", problemId, userId, e.getMessage());
            throw new RuntimeException("Failed to mark problem as mastered", e);
        }
    }

    /**
     * Reset problem learning progress.
     */
    @Transactional
    public void resetProblemProgress(Long userId, Long problemId) {
        try {
            // Get existing card
            FSRSCard card = fsrsService.getOrCreateCard(userId, problemId);
            
            // Reset to initial state by processing with "Again" rating
            fsrsService.processReview(userId, problemId, 1, ReviewType.MANUAL);
            
            log.info("Reset progress for problem {} for user {}", problemId, userId);
            
        } catch (Exception e) {
            log.error("Failed to reset problem {} progress for user {}: {}", problemId, userId, e.getMessage());
            throw new RuntimeException("Failed to reset problem progress", e);
        }
    }

    /**
     * Get recommended problems for user based on FSRS scheduling.
     */
    public List<Problem> getRecommendedProblems(Long userId, int limit) {
        try {
            // Get FSRS review queue
            var reviewQueue = fsrsService.generateReviewQueue(userId, limit);
            
            if (reviewQueue.getCards().isEmpty()) {
                // If no FSRS cards available, return new problems
                return findProblemsForUser(userId, null, null)
                        .stream()
                        .filter(p -> !hasUserSolvedProblem(userId, p.getId()))
                        .limit(limit)
                        .toList();
            }
            
            // Convert FSRS cards to problems
            List<Long> problemIds = reviewQueue.getCards().stream()
                    .map(card -> card.getProblemId())
                    .toList();
            
            return problemIds.stream()
                    .map(problemMapper::selectById)
                    .filter(Objects::nonNull)
                    .toList();
                    
        } catch (Exception e) {
            log.error("Failed to get recommended problems for user {}: {}", userId, e.getMessage());
            // Fallback to random problems
            return findProblemsForUser(userId, null, null)
                    .stream()
                    .limit(limit)
                    .toList();
        }
    }

    /**
     * Get due problems for review.
     */
    public List<Problem> getDueProblems(Long userId, int limit) {
        try {
            var dueCards = fsrsService.getDueCards(userId, limit);
            
            List<Long> problemIds = dueCards.stream()
                    .map(card -> card.getProblemId())
                    .toList();
            
            return problemIds.stream()
                    .map(problemMapper::selectById)
                    .filter(Objects::nonNull)
                    .toList();
                    
        } catch (Exception e) {
            log.error("Failed to get due problems for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Convert mastery level and status to FSRS rating (1-4 scale).
     * 
     * FSRS Ratings:
     * 1 - Again (failed to remember)
     * 2 - Hard (remembered with difficulty)
     * 3 - Good (remembered easily)
     * 4 - Easy (remembered very easily)
     */
    private Integer convertMasteryToFSRSRating(Integer mastery, String status) {
        // Handle null mastery
        if (mastery == null) {
            mastery = 0;
        }
        
        // Consider status for rating adjustment
        boolean isSolved = "SOLVED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status);
        boolean isAttempted = "ATTEMPTED".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status);
        
        return switch (mastery) {
            case 0 -> isSolved ? 2 : 1;  // No mastery but solved = Hard, otherwise Again
            case 1 -> isSolved ? 3 : 2;  // Low mastery + solved = Good, otherwise Hard  
            case 2 -> isSolved ? 4 : 3;  // Medium mastery + solved = Easy, otherwise Good
            case 3 -> 4;                 // High mastery = Easy
            default -> isAttempted ? 2 : 1;  // Default: Hard if attempted, Again otherwise
        };
    }

    /**
     * Determine review type based on status.
     */
    private ReviewType determineReviewType(String status) {
        return switch (status != null ? status.toUpperCase() : "") {
            case "SOLVED", "COMPLETED" -> ReviewType.SCHEDULED;  // Regular review
            case "IN_PROGRESS", "ATTEMPTED" -> ReviewType.MANUAL;  // User-initiated
            case "REVIEWING", "PRACTICING" -> ReviewType.EXTRA;   // Extra practice
            default -> ReviewType.MANUAL;  // Default to manual review
        };
    }

    /**
     * Calculate accuracy percentage based on FSRS stability and difficulty.
     * Higher stability and lower difficulty indicate better accuracy.
     */
    private Double calculateAccuracyFromFSRS(FSRSCard card) {
        if (card == null) {
            return 0.0;
        }

        double stability = card.getStabilityAsDouble();
        double difficulty = card.getDifficultyAsDouble();
        int reviewCount = card.getReviewCount() != null ? card.getReviewCount() : 0;
        int lapses = card.getLapses() != null ? card.getLapses() : 0;

        // Base accuracy from stability (higher stability = higher accuracy)
        double stabilityScore = Math.min(stability / 30.0, 1.0); // Normalize to 0-1, 30 days = 100%
        
        // Difficulty penalty (higher difficulty = lower accuracy)
        double difficultyPenalty = Math.min(difficulty / 10.0, 0.5); // Max 50% penalty
        
        // Review experience bonus (more reviews = slight accuracy boost)
        double experienceBonus = Math.min(reviewCount * 0.02, 0.2); // Max 20% bonus
        
        // Lapse penalty (more lapses = lower accuracy)  
        double lapsePenalty = Math.min(lapses * 0.1, 0.4); // Max 40% penalty

        // Calculate final accuracy (0.3-1.0 range)
        double accuracy = 0.3 + (stabilityScore * 0.7) - difficultyPenalty + experienceBonus - lapsePenalty;
        
        // Ensure bounds [0.0, 1.0] and convert to percentage
        return Math.max(0.0, Math.min(1.0, accuracy)) * 100.0;
    }

    /**
     * Calculate mastery level (0-3) based on FSRS card state and stability.
     */
    private Integer calculateMasteryLevel(FSRSCard card) {
        if (card == null) {
            return 0;
        }

        double stability = card.getStabilityAsDouble();
        int reviewCount = card.getReviewCount() != null ? card.getReviewCount() : 0;
        int lapses = card.getLapses() != null ? card.getLapses() : 0;
        
        // Base level from stability
        int baseLevel = 0;
        if (stability >= 30.0) baseLevel = 3;      // Excellent (1 month+)
        else if (stability >= 14.0) baseLevel = 2; // Good (2 weeks+)
        else if (stability >= 7.0) baseLevel = 1;  // Fair (1 week+)
        else baseLevel = 0;                        // Poor (<1 week)
        
        // Review experience adjustment
        if (reviewCount >= 10) baseLevel = Math.min(3, baseLevel + 1);
        else if (reviewCount >= 5) baseLevel = Math.min(3, baseLevel);
        
        // Lapse penalty
        if (lapses > 3) baseLevel = Math.max(0, baseLevel - 1);
        else if (lapses > 1) baseLevel = Math.max(0, baseLevel);
        
        return baseLevel;
    }

    /**
     * Find problems for user (stub implementation - to be replaced with actual logic).
     */
    private List<Problem> findProblemsForUser(Long userId, String difficulty, String search) {
        // This is a placeholder - in a real implementation, this would:
        // 1. Get problems user has attempted or interacted with
        // 2. Apply difficulty and search filters
        // 3. Return filtered results
        
        // For now, return all problems
        return problemMapper.selectList(null);
    }

    /**
     * Check if user has solved a problem (stub implementation).
     */
    private boolean hasUserSolvedProblem(Long userId, Long problemId) {
        try {
            FSRSCard card = fsrsService.getOrCreateCard(userId, problemId);
            // Consider solved if has reviews and reasonable stability
            return card.getReviewCount() != null && 
                   card.getReviewCount() > 0 && 
                   card.getStabilityAsDouble() > 1.0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculate difficulty breakdown for problems.
     */
    private Map<String, Integer> calculateDifficultyBreakdown(List<Problem> problems) {
        Map<String, Integer> breakdown = new java.util.HashMap<>();
        
        for (Problem problem : problems) {
            String difficulty = problem.getDifficulty().name();
            breakdown.merge(difficulty, 1, Integer::sum);
        }
        
        return breakdown;
    }

    // DTOs

    /**
     * User learning statistics DTO.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserLearningStatistics {
        private Integer totalProblems;
        private Integer solvedProblems;
        private Integer newCards;
        private Integer learningCards;
        private Integer reviewCards;
        private Integer relearningCards;
        private Integer dueCards;
        private Double averageAccuracy;
        private Double averageDifficulty;
        private Integer totalReviews;
        private Integer totalLapses;
        private Map<String, Integer> difficultyBreakdown;
        
        // Derived metrics
        public Double getSolveRate() {
            if (totalProblems == null || totalProblems == 0) return 0.0;
            return (solvedProblems != null ? solvedProblems : 0) * 100.0 / totalProblems;
        }
        
        public Double getRetentionRate() {
            if (totalReviews == null || totalReviews == 0) return 100.0;
            int lapses = totalLapses != null ? totalLapses : 0;
            return Math.max(0.0, (totalReviews - lapses) * 100.0 / totalReviews);
        }
        
        public Integer getActiveCards() {
            int learning = learningCards != null ? learningCards : 0;
            int review = reviewCards != null ? reviewCards : 0;
            int relearning = relearningCards != null ? relearningCards : 0;
            return learning + review + relearning;
        }
    }
}