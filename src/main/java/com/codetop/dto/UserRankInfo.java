package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User rank information across all leaderboard categories.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRankInfo {
    
    /**
     * User's global rank based on total reviews.
     */
    private Long globalRank;
    
    /**
     * User's weekly rank based on reviews in the last 7 days.
     */
    private Long weeklyRank;
    
    /**
     * User's monthly rank based on reviews in the last 30 days.
     */
    private Long monthlyRank;
    
    /**
     * User's accuracy rank based on correct review percentage.
     */
    private Long accuracyRank;
    
    /**
     * User's streak rank based on current learning streak.
     */
    private Long streakRank;
    
    /**
     * Overall weighted rank considering all factors.
     */
    private Long overallRank;
    
    /**
     * Percentile ranking (0-100).
     */
    private Double percentile;
    
    /**
     * User's highest achievement badge.
     */
    private String badge;
    
    /**
     * Get the best rank across all categories.
     */
    public Long getBestRank() {
        return java.util.stream.Stream.of(globalRank, weeklyRank, monthlyRank, accuracyRank, streakRank)
                .filter(java.util.Objects::nonNull)
                .filter(rank -> rank > 0)
                .min(Long::compareTo)
                .orElse(null);
    }
    
    /**
     * Get the category where user ranks best.
     */
    public String getBestCategory() {
        Long bestRank = getBestRank();
        if (bestRank == null) return null;
        
        if (bestRank.equals(globalRank)) return "全球总榜";
        if (bestRank.equals(weeklyRank)) return "周榜";
        if (bestRank.equals(monthlyRank)) return "月榜";
        if (bestRank.equals(accuracyRank)) return "准确率榜";
        if (bestRank.equals(streakRank)) return "连续学习榜";
        
        return null;
    }
    
    /**
     * Check if user has any top rankings (top 100).
     */
    public boolean hasTopRanking() {
        return java.util.stream.Stream.of(globalRank, weeklyRank, monthlyRank, accuracyRank, streakRank)
                .filter(java.util.Objects::nonNull)
                .anyMatch(rank -> rank <= 100);
    }
    
    /**
     * Check if user has elite rankings (top 10).
     */
    public boolean hasEliteRanking() {
        return java.util.stream.Stream.of(globalRank, weeklyRank, monthlyRank, accuracyRank, streakRank)
                .filter(java.util.Objects::nonNull)
                .anyMatch(rank -> rank <= 10);
    }
}