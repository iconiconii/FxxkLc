package com.codetop.controller;

import com.codetop.mapper.FSRSCardMapper;
import com.codetop.mapper.ReviewLogMapper;
import com.codetop.security.UserPrincipal;
import com.codetop.service.FSRSService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Analytics controller for learning progress and performance metrics.
 * 
 * @author CodeTop Team
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Learning analytics and progress tracking")
@SecurityRequirement(name = "Bearer Authentication")
public class AnalyticsController {

    private final FSRSService fsrsService;
    private final ReviewLogMapper reviewLogMapper;

    /**
     * Get user learning overview.
     */
    @GetMapping("/overview")
    @Operation(summary = "Get learning overview", description = "Get user's learning statistics and overview")
    public ResponseEntity<AnalyticsOverview> getOverview(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        FSRSCardMapper.UserLearningStats stats = fsrsService.getUserLearningStats(userPrincipal.getId());
        
        AnalyticsOverview overview = AnalyticsOverview.builder()
                .totalCards(stats.getTotalCards())
                .newCards(stats.getNewCards())
                .learningCards(stats.getLearningCards())
                .reviewCards(stats.getReviewCards())
                .relearningCards(stats.getRelearningCards())
                .dueCards(stats.getDueCards())
                .avgReviews(stats.getAvgReviews())
                .avgDifficulty(stats.getAvgDifficulty())
                .avgStability(stats.getAvgStability())
                .totalLapses(stats.getTotalLapses())
                .build();
        
        return ResponseEntity.ok(overview);
    }

    /**
     * Get daily review activity.
     */
    @GetMapping("/daily")
    @Operation(summary = "Get daily activity", description = "Get daily review activity for the past 30 days")
    public ResponseEntity<List<ReviewLogMapper.DailyReviewActivity>> getDailyActivity(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "30") int days) {
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<ReviewLogMapper.DailyReviewActivity> activity = reviewLogMapper.getDailyReviewActivity(
                userPrincipal.getId(), startDate);
        
        return ResponseEntity.ok(activity);
    }

    /**
     * Get weekly review statistics.
     */
    @GetMapping("/weekly")
    @Operation(summary = "Get weekly statistics", description = "Get weekly review statistics")
    public ResponseEntity<ReviewLogMapper.UserReviewStats> getWeeklyStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        ReviewLogMapper.UserReviewStats stats = reviewLogMapper.getUserReviewStats(
                userPrincipal.getId(), startDate, endDate);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get monthly progress.
     */
    @GetMapping("/monthly")
    @Operation(summary = "Get monthly progress", description = "Get monthly learning progress")
    public ResponseEntity<List<ReviewLogMapper.MonthlyProgress>> getMonthlyProgress(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        List<ReviewLogMapper.MonthlyProgress> progress = reviewLogMapper.getMonthlyProgress(userPrincipal.getId());
        return ResponseEntity.ok(progress);
    }

    /**
     * Get difficulty performance breakdown.
     */
    @GetMapping("/difficulty-performance")
    @Operation(summary = "Get difficulty performance", description = "Get performance breakdown by problem difficulty")
    public ResponseEntity<List<ReviewLogMapper.DifficultyPerformance>> getDifficultyPerformance(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "30") int days) {
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<ReviewLogMapper.DifficultyPerformance> performance = reviewLogMapper.getDifficultyPerformance(
                userPrincipal.getId(), startDate);
        
        return ResponseEntity.ok(performance);
    }

    /**
     * Get system-wide metrics (for admin users).
     */
    @GetMapping("/system")
    @Operation(summary = "Get system metrics", description = "Get system-wide FSRS performance metrics")
    public ResponseEntity<FSRSCardMapper.SystemFSRSMetrics> getSystemMetrics(
            @RequestParam(defaultValue = "30") int days) {
        
        FSRSCardMapper.SystemFSRSMetrics metrics = fsrsService.getSystemMetrics(days);
        return ResponseEntity.ok(metrics);
    }

    // DTOs

    @lombok.Data
    @lombok.Builder
    public static class AnalyticsOverview {
        private Long totalCards;
        private Long newCards;
        private Long learningCards;
        private Long reviewCards;
        private Long relearningCards;
        private Long dueCards;
        private Double avgReviews;
        private Double avgDifficulty;
        private Double avgStability;
        private Long totalLapses;
    }
}