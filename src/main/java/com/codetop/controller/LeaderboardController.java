package com.codetop.controller;

import com.codetop.mapper.ReviewLogMapper;
import com.codetop.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Leaderboard controller for user rankings and achievements.
 * 
 * @author CodeTop Team
 */
@RestController
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Leaderboard", description = "User rankings and achievements")
public class LeaderboardController {

    private final UserMapper userMapper;
    private final ReviewLogMapper reviewLogMapper;

    /**
     * Get global leaderboard.
     */
    @GetMapping
    @Operation(summary = "Get global leaderboard", description = "Get top users by total review count")
    public ResponseEntity<List<LeaderboardEntry>> getGlobalLeaderboard(
            @RequestParam(defaultValue = "50") int limit) {
        
        List<LeaderboardEntry> leaderboard = userMapper.getGlobalLeaderboard(Math.min(limit, 100));
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Get weekly leaderboard.
     */
    @GetMapping("/weekly")
    @Operation(summary = "Get weekly leaderboard", description = "Get top users by review count this week")
    public ResponseEntity<List<LeaderboardEntry>> getWeeklyLeaderboard(
            @RequestParam(defaultValue = "50") int limit) {
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        List<LeaderboardEntry> leaderboard = userMapper.getWeeklyLeaderboard(startDate, Math.min(limit, 100));
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Get monthly leaderboard.
     */
    @GetMapping("/monthly")
    @Operation(summary = "Get monthly leaderboard", description = "Get top users by review count this month")
    public ResponseEntity<List<LeaderboardEntry>> getMonthlyLeaderboard(
            @RequestParam(defaultValue = "50") int limit) {
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        List<LeaderboardEntry> leaderboard = userMapper.getMonthlyLeaderboard(startDate, Math.min(limit, 100));
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Get accuracy leaderboard.
     */
    @GetMapping("/accuracy")
    @Operation(summary = "Get accuracy leaderboard", description = "Get top users by accuracy rate")
    public ResponseEntity<List<AccuracyLeaderboardEntry>> getAccuracyLeaderboard(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "30") int days) {
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<AccuracyLeaderboardEntry> leaderboard = userMapper.getAccuracyLeaderboard(
                startDate, Math.min(limit, 100));
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Get streak leaderboard.
     */
    @GetMapping("/streak")
    @Operation(summary = "Get streak leaderboard", description = "Get top users by current learning streak")
    public ResponseEntity<List<StreakLeaderboardEntry>> getStreakLeaderboard(
            @RequestParam(defaultValue = "50") int limit) {
        
        List<StreakLeaderboardEntry> leaderboard = userMapper.getStreakLeaderboard(Math.min(limit, 100));
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Get user's rank in different categories.
     */
    @GetMapping("/user/{userId}/rank")
    @Operation(summary = "Get user rank", description = "Get user's rank in different leaderboard categories")
    public ResponseEntity<UserRankInfo> getUserRank(@PathVariable Long userId) {
        
        UserRankInfo rankInfo = UserRankInfo.builder()
                .globalRank(userMapper.getUserGlobalRank(userId))
                .weeklyRank(userMapper.getUserWeeklyRank(userId))
                .monthlyRank(userMapper.getUserMonthlyRank(userId))
                .accuracyRank(userMapper.getUserAccuracyRank(userId))
                .streakRank(userMapper.getUserStreakRank(userId))
                .build();
        
        return ResponseEntity.ok(rankInfo);
    }

    // DTOs

    @lombok.Data
    @lombok.Builder
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
    @lombok.Builder
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
    @lombok.Builder
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

    @lombok.Data
    @lombok.Builder
    public static class UserRankInfo {
        private Long globalRank;
        private Long weeklyRank;
        private Long monthlyRank;
        private Long accuracyRank;
        private Long streakRank;
    }
}