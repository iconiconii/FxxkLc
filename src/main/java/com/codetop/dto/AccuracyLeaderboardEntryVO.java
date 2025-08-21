package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Accuracy leaderboard entry VO for API responses.
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
public class AccuracyLeaderboardEntryVO {
    private Long rank;
    private Long userId;
    private String username;
    private String avatarUrl;
    private Long totalReviews;
    private Long correctReviews;
    private Double accuracy;
    private String badge;
    
    /**
     * Assign badge based on accuracy performance.
     */
    public void assignAccuracyBadge() {
        if (this.getRank() == null)
            return;

        long rank = this.getRank();
        double accuracy = this.getAccuracy();

        if (rank == 1) {
            this.setBadge("🎯 精准王者");
        } else if (rank <= 3) {
            this.setBadge("🏹 神射手");
        } else if (rank <= 10) {
            this.setBadge("🎪 高手");
        } else if (accuracy >= 95.0) {
            this.setBadge("💎 完美主义");
        } else if (accuracy >= 90.0) {
            this.setBadge("⭐ 卓越");
        } else if (accuracy >= 85.0) {
            this.setBadge("✨ 优秀");
        }
    }
}