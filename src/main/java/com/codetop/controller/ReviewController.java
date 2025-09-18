package com.codetop.controller;

import com.codetop.annotation.SimpleIdempotent;
import com.codetop.entity.FSRSCard;
import com.codetop.enums.ReviewType;
import com.codetop.mapper.FSRSCardMapper;
import com.codetop.security.UserPrincipal;
import com.codetop.service.FSRSService;
import com.codetop.dto.FSRSReviewQueueVO;
import com.codetop.dto.FSRSReviewResultVO;
import com.codetop.dto.ReviewQueueCardVO;
import com.codetop.dto.LearningStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Review controller for FSRS spaced repetition system.
 * 
 * @author CodeTop Team
 */
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Review", description = "Spaced repetition review system")
@SecurityRequirement(name = "Bearer Authentication")
public class ReviewController {

    private final FSRSService fsrsService;

    /**
     * Get review queue for user with pagination support.
     */
    @GetMapping("/queue")
    @Operation(summary = "Get review queue", description = "Get personalized review queue using FSRS algorithm with pagination")
    public ResponseEntity<FSRSReviewQueueVO> getReviewQueue(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "false") boolean showAll) {
        
        if (showAll) {
            // Show all due problems without mixed card type optimization
            int cappedLimit = Math.min(limit, 200);
            List<FSRSCardMapper.ReviewQueueCard> dueCards = fsrsService.getAllDueProblems(
                    userPrincipal.getId(), cappedLimit);

            // If not enough due/overdue cards to fill the limit, include upcoming (future scheduled) cards
            java.util.List<FSRSCardMapper.ReviewQueueCard> cards = new java.util.ArrayList<>(dueCards);
            int remaining = cappedLimit - cards.size();
            if (remaining > 0) {
                List<FSRSCardMapper.ReviewQueueCard> upcoming = fsrsService.getUpcomingCards(userPrincipal.getId(), remaining);
                cards.addAll(upcoming);
            }

            // Defensive de-duplication by problemId to avoid any overlap from data anomalies
            java.util.LinkedHashMap<Long, FSRSCardMapper.ReviewQueueCard> dedup = new java.util.LinkedHashMap<>();
            for (FSRSCardMapper.ReviewQueueCard c : cards) {
                if (!dedup.containsKey(c.getProblemId())) {
                    dedup.put(c.getProblemId(), c);
                }
            }
            cards = new java.util.ArrayList<>(dedup.values());
            
            // Convert to VO format
            List<ReviewQueueCardVO> cardVOs = cards.stream()
                    .map(this::convertToCardVO)
                    .collect(Collectors.toList());
            
            // Get user learning statistics
            FSRSCardMapper.UserLearningStats stats = fsrsService.getUserLearningStats(userPrincipal.getId());
            LearningStatsVO statsVO = convertToStatsVO(stats);
            
            FSRSReviewQueueVO response = FSRSReviewQueueVO.builder()
                    .cards(cardVOs)
                    .totalCount(cardVOs.size())
                    .stats(statsVO)
                    .generatedAt(LocalDateTime.now())
                    .currentPage(null)
                    .pageSize(null)
                    .totalPages(null)
                    .build();
            
            // Apply pagination if needed
            if (page > 1 || pageSize < cardVOs.size()) {
                response = applyClientSidePagination(response, page, pageSize);
            }
            
            return ResponseEntity.ok(response);
        } else {
            // Use existing optimized mixed card type logic
            // 对于分页请求，使用更高的limit确保有足够数据
            int actualLimit = page == 1 ? Math.min(limit, 100) : Math.min(limit, 200);
            
            com.codetop.dto.FSRSReviewQueueDTO queue = fsrsService.generateReviewQueue(
                    userPrincipal.getId(), actualLimit);
            
            // Update the queue with filtered cards
            com.codetop.dto.FSRSReviewQueueDTO filteredQueue = com.codetop.dto.FSRSReviewQueueDTO.builder()
                    .cards(queue.getCards())
                    .totalCount(queue.getCards().size())
                    .stats(queue.getStats())
                    .generatedAt(queue.getGeneratedAt())
                    .build();
            
            FSRSReviewQueueVO response = convertReviewQueueToVO(filteredQueue);
            
            // 如果请求指定了分页参数且不是第一页或者pageSize不等于limit，则进行客户端分页处理
            if (page > 1 || pageSize < actualLimit) {
                response = applyClientSidePagination(response, page, pageSize);
            }
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Submit review for a problem.
     */
    @PostMapping("/submit")
    @Operation(summary = "Submit review", description = "Submit review result and update FSRS scheduling")
    @SimpleIdempotent(
        operation = "SUBMIT_REVIEW",
        returnCachedResult = false
    )
    public ResponseEntity<FSRSReviewResultVO> submitReview(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SubmitReviewRequest request) {
        
        ReviewType reviewType = ReviewType.valueOf(request.getReviewType().toUpperCase());
        
        com.codetop.dto.FSRSReviewResultDTO result = fsrsService.processReview(
                userPrincipal.getId(), 
                request.getProblemId(),
                request.getRating(),
                reviewType);
        
        FSRSReviewResultVO response = convertReviewResultToVO(result);
        return ResponseEntity.ok(response);
    }

    /**
     * Get due cards for immediate review.
     */
    @GetMapping("/due")
    @Operation(summary = "Get due cards", description = "Get cards that are due for review")
    public ResponseEntity<List<FSRSCardMapper.ReviewQueueCard>> getDueCards(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<FSRSCardMapper.ReviewQueueCard> cards = fsrsService.getDueCards(
                userPrincipal.getId(), Math.min(limit, 50));
        return ResponseEntity.ok(cards);
    }

    /**
     * Get new cards for learning.
     */
    @GetMapping("/new")
    @Operation(summary = "Get new cards", description = "Get new cards for initial learning")
    public ResponseEntity<List<FSRSCardMapper.ReviewQueueCard>> getNewCards(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<FSRSCardMapper.ReviewQueueCard> cards = fsrsService.getNewCards(
                userPrincipal.getId(), Math.min(limit, 20));
        return ResponseEntity.ok(cards);
    }

    /**
     * Get overdue cards.
     */
    @GetMapping("/overdue")
    @Operation(summary = "Get overdue cards", description = "Get cards that are overdue for review")
    public ResponseEntity<List<FSRSCardMapper.ReviewQueueCard>> getOverdueCards(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<FSRSCardMapper.ReviewQueueCard> cards = fsrsService.getOverdueCards(
                userPrincipal.getId(), Math.min(limit, 50));
        return ResponseEntity.ok(cards);
    }

    /**
     * Get user's learning statistics.
     */
    @GetMapping("/stats")
    @Operation(summary = "Get learning statistics", description = "Get user's learning progress and statistics")
    public ResponseEntity<FSRSCardMapper.UserLearningStats> getLearningStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        FSRSCardMapper.UserLearningStats stats = fsrsService.getUserLearningStats(userPrincipal.getId());
        return ResponseEntity.ok(stats);
    }

    /**
     * Get all user cards.
     */
    @GetMapping("/cards")
    @Operation(summary = "Get user cards", description = "Get all FSRS cards for user")
    public ResponseEntity<List<FSRSCard>> getUserCards(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        List<FSRSCard> cards = fsrsService.getUserCards(userPrincipal.getId());
        return ResponseEntity.ok(cards);
    }

    /**
     * Get card intervals for a problem.
     */
    @GetMapping("/intervals/{problemId}")
    @Operation(summary = "Get card intervals", description = "Get all possible intervals for a problem")
    public ResponseEntity<int[]> getCardIntervals(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long problemId) {
        
        int[] intervals = fsrsService.calculateAllIntervals(userPrincipal.getId(), problemId);
        return ResponseEntity.ok(intervals);
    }

    /**
     * Optimize FSRS parameters for user.
     */
    @PostMapping("/optimize-parameters")
    @Operation(summary = "Optimize FSRS parameters", description = "Optimize FSRS parameters based on review history")
    @SimpleIdempotent(
        operation = "OPTIMIZE_FSRS_PARAMETERS",
        returnCachedResult = false
    )
    public ResponseEntity<OptimizationResult> optimizeParameters(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OptimizationRequest request) {
        
        var optimizedParameters = fsrsService.optimizeUserParameters(userPrincipal.getId());
        
        OptimizationResult result = OptimizationResult.builder()
                .success(true)
                .message("Parameters optimized successfully")
                .parameters(optimizedParameters)
                .build();
        
        return ResponseEntity.ok(result);
    }

    // Converter methods
    
    /**
     * Apply client-side pagination to review queue response.
     */
    private FSRSReviewQueueVO applyClientSidePagination(FSRSReviewQueueVO response, int page, int pageSize) {
        List<ReviewQueueCardVO> allCards = response.getCards();
        int totalCards = allCards.size();
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalCards);
        
        if (startIndex >= totalCards) {
            // 请求页超出范围，返回空结果
            return FSRSReviewQueueVO.builder()
                    .cards(List.of())
                    .totalCount(totalCards)
                    .stats(response.getStats())
                    .generatedAt(response.getGeneratedAt())
                    .currentPage(page)
                    .pageSize(pageSize)
                    .totalPages((int) Math.ceil((double) totalCards / pageSize))
                    .build();
        }
        
        List<ReviewQueueCardVO> pageCards = allCards.subList(startIndex, endIndex);
        
        return FSRSReviewQueueVO.builder()
                .cards(pageCards)
                .totalCount(totalCards)
                .stats(response.getStats())
                .generatedAt(response.getGeneratedAt())
                .currentPage(page)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) totalCards / pageSize))
                .build();
    }
    
    /**
     * Convert FSRSReviewQueueDTO to VO.
     */
    private FSRSReviewQueueVO convertReviewQueueToVO(com.codetop.dto.FSRSReviewQueueDTO dto) {
        // Convert cards to simplified VO
        List<ReviewQueueCardVO> cardVOs = dto.getCards().stream()
                .map(this::convertToCardVO)
                .collect(Collectors.toList());
        
        // Convert stats to simplified VO
        LearningStatsVO statsVO = convertToStatsVO(dto.getStats());
        
        return FSRSReviewQueueVO.builder()
                .cards(cardVOs)
                .totalCount(dto.getTotalCount())
                .stats(statsVO)
                .generatedAt(dto.getGeneratedAt())
                .currentPage(null) // Will be set later if pagination is applied
                .pageSize(null)
                .totalPages(null)
                .build();
    }
    
    /**
     * Convert ReviewQueueCard to simplified VO.
     */
    private ReviewQueueCardVO convertToCardVO(FSRSCardMapper.ReviewQueueCard card) {
        // Ensure dueDate is populated from nextReview if present
        LocalDate dueDate = null;
        if (card.getNextReview() != null) {
            dueDate = card.getNextReview().toLocalDate();
        } else if (card.getDueDate() != null) {
            dueDate = card.getDueDate();
        }

        return ReviewQueueCardVO.builder()
                .id(card.getId())
                .problemId(card.getProblemId())
                .problemTitle(card.getProblemTitle())
                .problemDifficulty(card.getProblemDifficulty())
                .state(card.getState().name())
                .dueDate(dueDate)
                .intervalDays(card.getIntervalDays())
                .priority(card.getPriority())
                .difficulty(card.getDifficulty() != null ? card.getDifficulty().doubleValue() : 0.0)
                .stability(card.getStability() != null ? card.getStability().doubleValue() : 0.0)
                .reviewCount(card.getReviewCount())
                .lapses(card.getLapses())
                .build();
    }
    
    /**
     * Convert UserLearningStats to simplified VO.
     */
    private LearningStatsVO convertToStatsVO(FSRSCardMapper.UserLearningStats stats) {
        return LearningStatsVO.builder()
                .totalCards(stats.getTotalCards())
                .newCards(stats.getNewCards())
                .learningCards(stats.getLearningCards())
                .reviewCards(stats.getReviewCards())
                .relearningCards(stats.getRelearningCards())
                .dueCards(stats.getDueCards())
                .avgDifficulty(stats.getAvgDifficulty())
                .avgStability(stats.getAvgStability())
                .build();
    }
    
    /**
     * Convert FSRSReviewResultDTO to VO.
     */
    private FSRSReviewResultVO convertReviewResultToVO(com.codetop.dto.FSRSReviewResultDTO dto) {
        return FSRSReviewResultVO.builder()
                .card(dto.getCard())
                .nextReviewTime(dto.getNextReviewTime())
                .intervalDays(dto.getIntervalDays())
                .newState(dto.getNewState())
                .difficulty(dto.getDifficulty())
                .stability(dto.getStability())
                .build();
    }

    // DTOs

    @lombok.Data
    public static class SubmitReviewRequest {
        @NotNull(message = "Problem ID is required")
        private Long problemId;

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 4")
        @Max(value = 4, message = "Rating must be between 1 and 4")
        private Integer rating;

        @NotNull(message = "Review type is required")
        private String reviewType; // LEARNING, REVIEW, CRAM
        
        /**
         * 幂等性请求ID，用于防止重复提交
         */
        private String requestId;
    }

    @lombok.Data
    public static class OptimizationRequest {
        /**
         * 幂等性请求ID，用于防止重复优化
         */
        private String requestId;
    }

    @lombok.Data
    @lombok.Builder
    public static class OptimizationResult {
        private Boolean success;
        private String message;
        private Object parameters;
    }

}
