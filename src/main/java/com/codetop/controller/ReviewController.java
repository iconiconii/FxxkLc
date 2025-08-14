package com.codetop.controller;

import com.codetop.entity.FSRSCard;
import com.codetop.enums.ReviewType;
import com.codetop.mapper.FSRSCardMapper;
import com.codetop.security.UserPrincipal;
import com.codetop.service.FSRSService;
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

import java.util.List;

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
     * Get review queue for user.
     */
    @GetMapping("/queue")
    @Operation(summary = "Get review queue", description = "Get personalized review queue using FSRS algorithm")
    public ResponseEntity<FSRSService.ReviewQueue> getReviewQueue(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "20") int limit) {
        
        FSRSService.ReviewQueue queue = fsrsService.generateReviewQueue(
                userPrincipal.getId(), Math.min(limit, 50));
        return ResponseEntity.ok(queue);
    }

    /**
     * Submit review for a problem.
     */
    @PostMapping("/submit")
    @Operation(summary = "Submit review", description = "Submit review result and update FSRS scheduling")
    public ResponseEntity<FSRSService.ReviewResult> submitReview(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SubmitReviewRequest request) {
        
        ReviewType reviewType = ReviewType.valueOf(request.getReviewType().toUpperCase());
        
        FSRSService.ReviewResult result = fsrsService.processReview(
                userPrincipal.getId(), 
                request.getProblemId(),
                request.getRating(),
                reviewType);
        
        return ResponseEntity.ok(result);
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
    public ResponseEntity<OptimizationResult> optimizeParameters(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        var optimizedParameters = fsrsService.optimizeUserParameters(userPrincipal.getId());
        
        OptimizationResult result = OptimizationResult.builder()
                .success(true)
                .message("Parameters optimized successfully")
                .parameters(optimizedParameters)
                .build();
        
        return ResponseEntity.ok(result);
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
    }

    @lombok.Data
    @lombok.Builder
    public static class OptimizationResult {
        private Boolean success;
        private String message;
        private Object parameters;
    }

}