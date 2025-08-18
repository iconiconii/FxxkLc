package com.codetop.controller;

import com.codetop.dto.UserRankInfo;
import com.codetop.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import com.codetop.dto.LeaderboardEntryDTO;
import com.codetop.dto.AccuracyLeaderboardEntryDTO;
import com.codetop.dto.StreakLeaderboardEntryDTO;

/**
 * Leaderboard controller for user rankings and achievements.
 * 
 * Features:
 * - Multi-category leaderboards with caching
 * - Badge system and achievement tracking
 * - User rank information across all categories
 * - Performance optimized with service layer
 * 
 * @author CodeTop Team
 */
@RestController
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Leaderboard", description = "User rankings and achievements")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    /**
     * Get global leaderboard.
     */
    @GetMapping
    @Operation(summary = "Get global leaderboard", description = "Get top users by total review count with badge system")
    public ResponseEntity<List<LeaderboardEntryDTO>> getGlobalLeaderboard(
            @Parameter(description = "Maximum number of entries to return (1-100)")
            @RequestParam(defaultValue = "50") 
            @Min(1) @Max(100) int limit) {
        
        log.info("Fetching global leaderboard with limit: {}", limit);
        List<LeaderboardEntryDTO> leaderboard = leaderboardService.getGlobalLeaderboard(limit);
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Get weekly leaderboard.
     */
    @GetMapping("/weekly")
    @Operation(summary = "Get weekly leaderboard", description = "Get top users by review count this week")
    public ResponseEntity<List<LeaderboardEntryDTO>> getWeeklyLeaderboard(
            @Parameter(description = "Maximum number of entries to return (1-100)")
            @RequestParam(defaultValue = "50") 
            @Min(1) @Max(100) int limit) {
        
        log.info("Fetching weekly leaderboard with limit: {}", limit);
        List<LeaderboardEntryDTO> leaderboard = leaderboardService.getWeeklyLeaderboard(limit);
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Get monthly leaderboard.
     */
    @GetMapping("/monthly")
    @Operation(summary = "Get monthly leaderboard", description = "Get top users by review count this month")
    public ResponseEntity<List<LeaderboardEntryDTO>> getMonthlyLeaderboard(
            @Parameter(description = "Maximum number of entries to return (1-100)")
            @RequestParam(defaultValue = "50") 
            @Min(1) @Max(100) int limit) {
        
        log.info("Fetching monthly leaderboard with limit: {}", limit);
        List<LeaderboardEntryDTO> leaderboard = leaderboardService.getMonthlyLeaderboard(limit);
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Get accuracy leaderboard.
     */
    @GetMapping("/accuracy")
    @Operation(summary = "Get accuracy leaderboard", description = "Get top users by accuracy rate (minimum 10 reviews)")
    public ResponseEntity<List<AccuracyLeaderboardEntryDTO>> getAccuracyLeaderboard(
            @Parameter(description = "Maximum number of entries to return (1-100)")
            @RequestParam(defaultValue = "50") 
            @Min(1) @Max(100) int limit,
            @Parameter(description = "Number of days to consider for accuracy calculation (1-365)")
            @RequestParam(defaultValue = "30") 
            @Min(1) @Max(365) int days) {
        
        log.info("Fetching accuracy leaderboard with limit: {} and days: {}", limit, days);
        List<AccuracyLeaderboardEntryDTO> leaderboard = leaderboardService.getAccuracyLeaderboard(limit, days);
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Get streak leaderboard.
     */
    @GetMapping("/streak")
    @Operation(summary = "Get streak leaderboard", description = "Get top users by current learning streak")
    public ResponseEntity<List<StreakLeaderboardEntryDTO>> getStreakLeaderboard(
            @Parameter(description = "Maximum number of entries to return (1-100)")
            @RequestParam(defaultValue = "50") 
            @Min(1) @Max(100) int limit) {
        
        log.info("Fetching streak leaderboard with limit: {}", limit);
        List<StreakLeaderboardEntryDTO> leaderboard = leaderboardService.getStreakLeaderboard(limit);
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Get user's rank in different categories.
     */
    @GetMapping("/user/{userId}/rank")
    @Operation(summary = "Get user rank", description = "Get user's rank in different leaderboard categories")
    public ResponseEntity<UserRankInfo> getUserRank(
            @Parameter(description = "User ID")
            @PathVariable @Min(1) Long userId) {
        
        log.info("Fetching user rank for userId: {}", userId);
        UserRankInfo rankInfo = leaderboardService.getUserRank(userId);
        return ResponseEntity.ok(rankInfo);
    }

    /**
     * Get leaderboard summary statistics.
     */
    @GetMapping("/stats")
    @Operation(summary = "Get leaderboard statistics", description = "Get overall leaderboard statistics and summary")
    public ResponseEntity<LeaderboardService.TopPerformersSummary> getLeaderboardStats() {
        
        log.info("Fetching leaderboard statistics summary");
        LeaderboardService.TopPerformersSummary summary = leaderboardService.getTopPerformersSummary();
        return ResponseEntity.ok(summary);
    }

    // DTOs

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class LeaderboardEntry {
        private Long rank;
        private Long userId;
        private String username;
        private String avatarUrl;
        private Long totalReviews;
        private Long correctReviews;
        private Double accuracy;
        private Integer streak;
        private String badge;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AccuracyLeaderboardEntry {
        private Long rank;
        private Long userId;
        private String username;
        private String avatarUrl;
        private Long totalReviews;
        private Long correctReviews;
        private Double accuracy;
        private String badge;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class StreakLeaderboardEntry {
        private Long rank;
        private Long userId;
        private String username;
        private String avatarUrl;
        private Integer currentStreak;
        private Integer longestStreak;
        private Long totalActiveDays;
        private String badge;
    }

}