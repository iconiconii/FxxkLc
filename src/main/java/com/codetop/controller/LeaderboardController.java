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
import java.util.stream.Collectors;
import com.codetop.dto.LeaderboardEntryDTO;
import com.codetop.dto.StreakLeaderboardEntryDTO;
import com.codetop.dto.LeaderboardEntryVO;
import com.codetop.dto.StreakLeaderboardEntryVO;

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
    public ResponseEntity<List<LeaderboardEntryVO>> getGlobalLeaderboard(
            @Parameter(description = "Maximum number of entries to return (1-100)")
            @RequestParam(defaultValue = "50") 
            @Min(1) @Max(100) int limit) {
        
        log.info("Fetching global leaderboard with limit: {}", limit);
        List<LeaderboardEntryDTO> leaderboard = leaderboardService.getGlobalLeaderboard(limit);
        List<LeaderboardEntryVO> response = leaderboard.stream()
                .map(this::convertLeaderboardEntryToVO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get weekly leaderboard.
     */
    @GetMapping("/weekly")
    @Operation(summary = "Get weekly leaderboard", description = "Get top users by review count this week")
    public ResponseEntity<List<LeaderboardEntryVO>> getWeeklyLeaderboard(
            @Parameter(description = "Maximum number of entries to return (1-100)")
            @RequestParam(defaultValue = "50") 
            @Min(1) @Max(100) int limit) {
        
        log.info("Fetching weekly leaderboard with limit: {}", limit);
        List<LeaderboardEntryDTO> leaderboard = leaderboardService.getWeeklyLeaderboard(limit);
        // 转换为V0,根据vo的rank设置badge
        List<LeaderboardEntryVO> response = leaderboard.stream()
                .map(this::convertLeaderboardEntryToVO)
                .collect(Collectors.toList());
        // 根据rank设置badge
        response.forEach(LeaderboardEntryVO::assignBadge);
        return ResponseEntity.ok(response);
    }

    /**
     * Get monthly leaderboard.
     */
    @GetMapping("/monthly")
    @Operation(summary = "Get monthly leaderboard", description = "Get top users by review count this month")
    public ResponseEntity<List<LeaderboardEntryVO>> getMonthlyLeaderboard(
            @Parameter(description = "Maximum number of entries to return (1-100)")
            @RequestParam(defaultValue = "50") 
            @Min(1) @Max(100) int limit) {
        
        log.info("Fetching monthly leaderboard with limit: {}", limit);
        List<LeaderboardEntryDTO> leaderboard = leaderboardService.getMonthlyLeaderboard(limit);
        List<LeaderboardEntryVO> response = leaderboard.stream()
                .map(this::convertLeaderboardEntryToVO)
                .collect(Collectors.toList());
        response.forEach(LeaderboardEntryVO::assignBadge);
        return ResponseEntity.ok(response);
    }


    /**
     * Get streak leaderboard.
     */
    @GetMapping("/streak")
    @Operation(summary = "Get streak leaderboard", description = "Get top users by current learning streak")
    public ResponseEntity<List<StreakLeaderboardEntryVO>> getStreakLeaderboard(
            @Parameter(description = "Maximum number of entries to return (1-100)")
            @RequestParam(defaultValue = "50") 
            @Min(1) @Max(100) int limit) {
        
        log.info("Fetching streak leaderboard with limit: {}", limit);
        List<StreakLeaderboardEntryDTO> leaderboard = leaderboardService.getStreakLeaderboard(limit);
        List<StreakLeaderboardEntryVO> response = leaderboard.stream()
                .map(this::convertStreakLeaderboardEntryToVO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
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

    // Converter methods
    
    /**
     * Convert LeaderboardEntryDTO to VO.
     */
    private LeaderboardEntryVO convertLeaderboardEntryToVO(LeaderboardEntryDTO dto) {
        return LeaderboardEntryVO.builder()
                .rank(dto.getRank())
                .userId(dto.getUserId())
                .username(dto.getUsername())
                .avatarUrl(dto.getAvatarUrl())
                .totalReviews(dto.getTotalReviews())
                .streak(dto.getStreak())
                .build();
    }

    
    /**
     * Convert StreakLeaderboardEntryDTO to VO.
     */
    private StreakLeaderboardEntryVO convertStreakLeaderboardEntryToVO(StreakLeaderboardEntryDTO dto) {
        return StreakLeaderboardEntryVO.builder()
                .rank(dto.getRank())
                .userId(dto.getUserId())
                .username(dto.getUsername())
                .avatarUrl(dto.getAvatarUrl())
                .currentStreak(dto.getCurrentStreak())
                .longestStreak(dto.getLongestStreak())
                .totalActiveDays(dto.getTotalActiveDays())
                .build();
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
        private Integer streak;
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