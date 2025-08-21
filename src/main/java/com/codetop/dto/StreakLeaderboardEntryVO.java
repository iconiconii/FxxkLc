package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Streak leaderboard entry VO for API responses.
 * 
 * This VO is specifically designed for API responses to avoid
 * exposing internal type information from cached DTOs.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StreakLeaderboardEntryVO {
    private Long rank;
    private Long userId;
    private String username;
    private String avatarUrl;
    private Integer currentStreak;
    private Integer longestStreak;
    private Long totalActiveDays;
    private String badge;
    
    private void assignStreakBadge() {
        if (this.getRank() == null)
            return;

        long rank = this.getRank();
        int currentStreak = this.getCurrentStreak();
        int longestStreak = this.getLongestStreak();

        if (rank == 1) {
            this.setBadge("🔥 坚持之王");
        } else if (rank <= 3) {
            this.setBadge("💪 毅力超群");
        } else if (rank <= 10) {
            this.setBadge("🎯 持续专注");
        } else if (currentStreak >= 365) {
            this.setBadge("🏆 年度坚持");
        } else if (currentStreak >= 100) {
            this.setBadge("💎 百日坚持");
        } else if (currentStreak >= 30) {
            this.setBadge("🌟 月度坚持");
        } else if (longestStreak >= 50) {
            this.setBadge("⚡ 曾经辉煌");
        }
    }
}