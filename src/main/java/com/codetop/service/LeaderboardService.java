package com.codetop.service;

import com.codetop.controller.LeaderboardController;
import com.codetop.dto.UserRankInfo;
import com.codetop.mapper.UserMapper;
import com.codetop.mapper.UserStatisticsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import com.codetop.dto.LeaderboardEntryDTO;
import com.codetop.dto.AccuracyLeaderboardEntryDTO;
import com.codetop.dto.StreakLeaderboardEntryDTO;

/**
 * Leaderboard service for managing user rankings and achievements.
 * 
 * Features:
 * - Multi-category leaderboards (global, weekly, monthly, accuracy, streak)
 * - Badge calculation and assignment
 * - Caching for performance optimization
 * - User rank tracking and statistics
 * 
 * @author CodeTop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserMapper userMapper;
    private final UserStatisticsMapper userStatisticsMapper;

    /**
     * Get global leaderboard with caching.
     */
    @Cacheable(value = "leaderboard:global", key = "#limit", unless = "#result.isEmpty()")
    public List<LeaderboardEntryDTO> getGlobalLeaderboard(int limit) {
        log.debug("Fetching global leaderboard with limit: {}", limit);
        List<LeaderboardController.LeaderboardEntry> rawEntries = userMapper.getGlobalLeaderboard(limit);
        List<LeaderboardEntryDTO> entries = rawEntries.stream()
                .map(this::convertToLeaderboardEntryDTO)
                .toList();
        
        // Add badges to entries
        entries.forEach(this::assignBadge);
        
        return entries;
    }

    /**
     * Get weekly leaderboard with caching.
     */
    @Cacheable(value = "leaderboard:weekly", key = "#limit", unless = "#result.isEmpty()")
    public List<LeaderboardEntryDTO> getWeeklyLeaderboard(int limit) {
        log.debug("Fetching weekly leaderboard with limit: {}", limit);
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        List<LeaderboardController.LeaderboardEntry> rawEntries = userMapper.getWeeklyLeaderboard(startDate, limit);
        List<LeaderboardEntryDTO> entries = rawEntries.stream()
                .map(this::convertToLeaderboardEntryDTO)
                .toList();
        
        // Add badges to entries
        entries.forEach(this::assignBadge);
        
        return entries;
    }

    /**
     * Get monthly leaderboard with caching.
     */
    @Cacheable(value = "leaderboard:monthly", key = "#limit", unless = "#result.isEmpty()")
    public List<LeaderboardEntryDTO> getMonthlyLeaderboard(int limit) {
        log.debug("Fetching monthly leaderboard with limit: {}", limit);
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        List<LeaderboardController.LeaderboardEntry> rawEntries = userMapper.getMonthlyLeaderboard(startDate, limit);
        List<LeaderboardEntryDTO> entries = rawEntries.stream()
                .map(this::convertToLeaderboardEntryDTO)
                .toList();
        
        // Add badges to entries
        entries.forEach(this::assignBadge);
        
        return entries;
    }

    /**
     * Get accuracy leaderboard with caching.
     */
    @Cacheable(value = "leaderboard:accuracy", key = "{#limit, #days}", unless = "#result.isEmpty()")
    public List<AccuracyLeaderboardEntryDTO> getAccuracyLeaderboard(int limit, int days) {
        log.debug("Fetching accuracy leaderboard with limit: {} and days: {}", limit, days);
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<LeaderboardController.AccuracyLeaderboardEntry> rawEntries = userMapper.getAccuracyLeaderboard(startDate, limit);
        List<AccuracyLeaderboardEntryDTO> entries = rawEntries.stream()
                .map(this::convertToAccuracyLeaderboardEntryDTO)
                .toList();
        
        // Add badges to entries
        entries.forEach(this::assignAccuracyBadge);
        
        return entries;
    }

    /**
     * Get streak leaderboard with enhanced data.
     */
    @Cacheable(value = "leaderboard:streak", key = "#limit", unless = "#result.isEmpty()")
    public List<StreakLeaderboardEntryDTO> getStreakLeaderboard(int limit) {
        log.debug("Fetching streak leaderboard with limit: {}", limit);
        
        // Use enhanced streak query with real data from user_statistics
        List<LeaderboardController.StreakLeaderboardEntry> rawEntries = userStatisticsMapper.getStreakLeaderboard(limit);
        List<StreakLeaderboardEntryDTO> entries = rawEntries.stream()
                .map(this::convertToStreakLeaderboardEntryDTO)
                .toList();
        
        // Add badges to entries
        entries.forEach(this::assignStreakBadge);
        
        return entries;
    }

    /**
     * Get user's rank information across all categories.
     */
    @Cacheable(value = "user:rank", key = "#userId")
    public UserRankInfo getUserRank(Long userId) {
        log.debug("Fetching user rank for userId: {}", userId);
        
        return UserRankInfo.builder()
                .globalRank(userMapper.getUserGlobalRank(userId))
                .weeklyRank(userMapper.getUserWeeklyRank(userId))
                .monthlyRank(userMapper.getUserMonthlyRank(userId))
                .accuracyRank(userMapper.getUserAccuracyRank(userId))
                .streakRank(userStatisticsMapper.getUserStreakRank(userId))
                .build();
    }

    /**
     * Assign badge based on rank and total reviews for general leaderboard.
     */
    private void assignBadge(LeaderboardEntryDTO entry) {
        if (entry.getRank() == null) return;
        
        long rank = entry.getRank();
        long totalReviews = entry.getTotalReviews();
        
        if (rank == 1) {
            entry.setBadge("👑 冠军");
        } else if (rank == 2) {
            entry.setBadge("🥈 亚军");
        } else if (rank == 3) {
            entry.setBadge("🥉 季军");
        } else if (rank <= 10) {
            entry.setBadge("🏆 前十");
        } else if (rank <= 50) {
            entry.setBadge("🎖️ 精英");
        } else if (totalReviews >= 1000) {
            entry.setBadge("💪 千题达人");
        } else if (totalReviews >= 500) {
            entry.setBadge("📚 学习达人");
        } else if (totalReviews >= 100) {
            entry.setBadge("🌟 新星");
        }
    }

    /**
     * Assign badge based on accuracy performance.
     */
    private void assignAccuracyBadge(AccuracyLeaderboardEntryDTO entry) {
        if (entry.getRank() == null) return;
        
        long rank = entry.getRank();
        double accuracy = entry.getAccuracy();
        
        if (rank == 1) {
            entry.setBadge("🎯 精准王者");
        } else if (rank <= 3) {
            entry.setBadge("🏹 神射手");
        } else if (rank <= 10) {
            entry.setBadge("🎪 高手");
        } else if (accuracy >= 95.0) {
            entry.setBadge("💎 完美主义");
        } else if (accuracy >= 90.0) {
            entry.setBadge("⭐ 卓越");
        } else if (accuracy >= 85.0) {
            entry.setBadge("✨ 优秀");
        }
    }

    /**
     * Assign badge based on streak performance.
     */
    private void assignStreakBadge(StreakLeaderboardEntryDTO entry) {
        if (entry.getRank() == null) return;
        
        long rank = entry.getRank();
        int currentStreak = entry.getCurrentStreak();
        int longestStreak = entry.getLongestStreak();
        
        if (rank == 1) {
            entry.setBadge("🔥 坚持之王");
        } else if (rank <= 3) {
            entry.setBadge("💪 毅力超群");
        } else if (rank <= 10) {
            entry.setBadge("🎯 持续专注");
        } else if (currentStreak >= 365) {
            entry.setBadge("🏆 年度坚持");
        } else if (currentStreak >= 100) {
            entry.setBadge("💎 百日坚持");
        } else if (currentStreak >= 30) {
            entry.setBadge("🌟 月度坚持");
        } else if (longestStreak >= 50) {
            entry.setBadge("⚡ 曾经辉煌");
        }
    }

    /**
     * Calculate overall leaderboard position for a user.
     */
    public Long calculateOverallRank(Long userId) {
        // Weighted calculation considering multiple factors
        Long globalRank = userMapper.getUserGlobalRank(userId);
        Long accuracyRank = userMapper.getUserAccuracyRank(userId);
        Long streakRank = userStatisticsMapper.getUserStreakRank(userId);
        
        if (globalRank == null) globalRank = 999999L;
        if (accuracyRank == null) accuracyRank = 999999L;
        if (streakRank == null) streakRank = 999999L;
        
        // Weighted average: 50% volume + 30% accuracy + 20% consistency
        double weightedRank = (globalRank * 0.5) + (accuracyRank * 0.3) + (streakRank * 0.2);
        
        return Math.round(weightedRank);
    }

    /**
     * Get top performers summary.
     */
    public TopPerformersSummary getTopPerformersSummary() {
        List<LeaderboardEntryDTO> globalTop3 = getGlobalLeaderboard(3);
        List<AccuracyLeaderboardEntryDTO> accuracyTop3 = getAccuracyLeaderboard(3, 30);
        List<StreakLeaderboardEntryDTO> streakTop3 = getStreakLeaderboard(3);
        
        return TopPerformersSummary.builder()
                .topByVolume(globalTop3)
                .topByAccuracy(accuracyTop3)
                .topByStreak(streakTop3)
                .build();
    }

    // Conversion methods for DTO mapping
    
    private LeaderboardEntryDTO convertToLeaderboardEntryDTO(LeaderboardController.LeaderboardEntry entry) {
        return new LeaderboardEntryDTO(
                entry.getRank(),
                entry.getUserId(),
                entry.getUsername(),
                entry.getAvatarUrl(),
                entry.getTotalReviews(),
                entry.getCorrectReviews(),
                entry.getAccuracy(),
                entry.getStreak(),
                entry.getBadge()
        );
    }

    private AccuracyLeaderboardEntryDTO convertToAccuracyLeaderboardEntryDTO(LeaderboardController.AccuracyLeaderboardEntry entry) {
        return new AccuracyLeaderboardEntryDTO(
                entry.getRank(),
                entry.getUserId(),
                entry.getUsername(),
                entry.getAvatarUrl(),
                entry.getTotalReviews(),
                entry.getCorrectReviews(),
                entry.getAccuracy(),
                entry.getBadge()
        );
    }

    private StreakLeaderboardEntryDTO convertToStreakLeaderboardEntryDTO(LeaderboardController.StreakLeaderboardEntry entry) {
        return new StreakLeaderboardEntryDTO(
                entry.getRank(),
                entry.getUserId(),
                entry.getUsername(),
                entry.getAvatarUrl(),
                entry.getCurrentStreak(),
                entry.getLongestStreak(),
                entry.getTotalActiveDays(),
                entry.getBadge()
        );
    }

    /**
     * Summary of top performers across all categories.
     */
    @lombok.Data
    @lombok.Builder
    public static class TopPerformersSummary {
        private List<LeaderboardEntryDTO> topByVolume;
        private List<AccuracyLeaderboardEntryDTO> topByAccuracy;
        private List<StreakLeaderboardEntryDTO> topByStreak;
    }
}