package com.codetop.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User difficulty preference profile based on review history.
 * 
 * Tracks user's interaction patterns across different difficulty levels
 * and their recent learning trajectory.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DifficultyPref {
    
    /**
     * Proportion of Easy problems attempted (0.0 to 1.0)
     */
    @Builder.Default
    private double easy = 0.0;
    
    /**
     * Proportion of Medium problems attempted (0.0 to 1.0)
     */
    @Builder.Default
    private double medium = 0.0;
    
    /**
     * Proportion of Hard problems attempted (0.0 to 1.0)
     */
    @Builder.Default
    private double hard = 0.0;
    
    /**
     * Recent trend in difficulty preference
     */
    @Builder.Default
    private DifficultyTrend trend = DifficultyTrend.STABLE;
    
    /**
     * Preferred difficulty level based on recent performance
     */
    @Builder.Default
    private PreferredLevel preferredLevel = PreferredLevel.BALANCED;
    
    /**
     * User's recent difficulty trend
     */
    public enum DifficultyTrend {
        INCREASING,     // User is attempting harder problems
        DECREASING,     // User is attempting easier problems
        STABLE          // No significant trend
    }
    
    /**
     * User's preferred difficulty level
     */
    public enum PreferredLevel {
        BUILDING_CONFIDENCE,  // Prefers easier problems to build confidence
        BALANCED,            // Balanced approach across difficulties
        SEEKING_CHALLENGE    // Prefers harder problems for challenge
    }
    
    /**
     * Get the dominant difficulty preference
     */
    public String getDominantDifficulty() {
        if (hard >= easy && hard >= medium) {
            return "HARD";
        } else if (medium >= easy) {
            return "MEDIUM";
        } else {
            return "EASY";
        }
    }
    
    /**
     * Check if user prefers challenging problems
     */
    public boolean prefersChallenging() {
        return preferredLevel == PreferredLevel.SEEKING_CHALLENGE || hard > 0.4;
    }
    
    /**
     * Check if user is building confidence with easier problems
     */
    public boolean isBuildingConfidence() {
        return preferredLevel == PreferredLevel.BUILDING_CONFIDENCE || easy > 0.5;
    }
    
    /**
     * Get difficulty distribution as a readable string
     */
    public String getDistributionSummary() {
        return String.format("E:%.1f%% M:%.1f%% H:%.1f%%", 
                easy * 100, medium * 100, hard * 100);
    }
    
    /**
     * Validate that proportions sum to approximately 1.0
     */
    public boolean isValidDistribution() {
        double sum = easy + medium + hard;
        return sum >= 0.95 && sum <= 1.05; // Allow small floating point errors
    }
    
    /**
     * Normalize proportions to ensure they sum to 1.0
     */
    public void normalize() {
        double sum = easy + medium + hard;
        if (sum > 0) {
            this.easy = easy / sum;
            this.medium = medium / sum;
            this.hard = hard / sum;
        } else {
            // Default balanced distribution if no data
            this.easy = 0.3;
            this.medium = 0.5;
            this.hard = 0.2;
        }
    }
}