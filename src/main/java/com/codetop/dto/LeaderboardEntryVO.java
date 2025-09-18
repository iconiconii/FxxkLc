package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Leaderboard entry VO for API responses.
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
public class LeaderboardEntryVO {
    private Long rank;
    private Long userId;
    private String username;
    private String avatarUrl;
    private Long totalReviews;
    private Integer streak;
    private String badge;

    // 根据rank设置badge
    /**
     * Assign badge based on rank and total reviews for general leaderboard.
     */
    public void assignBadge() {
        if (this.getRank() == null) return;

        long rank = this.getRank();
        long totalReviews = this.getTotalReviews();

        if (rank == 1) {
            this.setBadge("👑 冠军");
        } else if (rank == 2) {
            this.setBadge("🥈 亚军");
        } else if (rank == 3) {
            this.setBadge("🥉 季军");
        } else if (rank <= 10) {
            this.setBadge("🏆 前十");
        } else if (rank <= 50) {
            this.setBadge("🎖️ 精英");
        } else if (totalReviews >= 1000) {
            this.setBadge("💪 千题达人");
        } else if (totalReviews >= 500) {
            this.setBadge("📚 学习达人");
        } else if (totalReviews >= 100) {
            this.setBadge("🌟 新星");
        }
    }
}