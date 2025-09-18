package com.codetop.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain-specific skill statistics for user profiling.
 * 
 * Represents aggregated learning performance in a specific knowledge domain
 * (e.g., arrays, dynamic programming, graphs) based on FSRS review history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainSkill {
    
    /**
     * Knowledge domain name (e.g., "arrays", "dynamic_programming", "graphs")
     */
    private String domain;
    
    /**
     * Number of problems reviewed in this domain
     */
    private int samples;
    
    /**
     * Success rate in this domain (0.0 to 1.0)
     * Based on reviews with rating >= 3
     */
    private double accuracy;
    
    /**
     * Average retention probability based on FSRS stability
     */
    private double retention;
    
    /**
     * Lapse rate - ratio of forgotten cards (0.0 to 1.0)
     */
    private double lapseRate;
    
    /**
     * Average response time in milliseconds
     */
    private long avgRtMs;
    
    /**
     * Total number of review attempts in this domain
     */
    private int attempts;
    
    /**
     * Composite skill score (0.0 to 1.0)
     * Weighted combination of accuracy, retention, and response time
     */
    private double skillScore;
    
    /**
     * Strength classification based on skill score and sample size
     */
    @Builder.Default
    private StrengthLevel strength = StrengthLevel.NORMAL;

    /**
     * Configurable sample size threshold for reliability checks.
     * Default 10, can be overridden by service using configuration.
     */
    @Builder.Default
    private int minSamplesForReliability = 10;
    
    /**
     * When this domain statistics was last updated
     */
    @Builder.Default
    private Instant lastUpdated = Instant.now();
    
    /**
     * Classification of user strength in this domain
     */
    public enum StrengthLevel {
        WEAK,     // skillScore < 0.45 with samples >= 10
        NORMAL,   // skillScore between 0.45 and 0.75
        STRONG    // skillScore > 0.75 with samples >= 10
    }
    
    /**
     * Check if this domain has sufficient sample size for reliable statistics
     */
    public boolean hasSufficientSamples() {
        return samples >= minSamplesForReliability;
    }
    
    /**
     * Check if user is weak in this domain
     */
    public boolean isWeak() {
        return strength == StrengthLevel.WEAK && hasSufficientSamples();
    }
    
    /**
     * Check if user is strong in this domain
     */
    public boolean isStrong() {
        return strength == StrengthLevel.STRONG && hasSufficientSamples();
    }
    
    /**
     * Get normalized response time score (0.0 to 1.0, where 1.0 is fastest)
     */
    public double getResponseTimeScore() {
        if (avgRtMs <= 0) return 0.5; // Default neutral score
        
        // Normalize based on typical response times
        // Under 30 seconds = excellent (1.0)
        // 30s - 2min = good (0.8-1.0)  
        // 2-5min = average (0.4-0.8)
        // Over 5min = slow (0.0-0.4)
        double minutes = avgRtMs / 60000.0;
        
        if (minutes <= 0.5) return 1.0;
        if (minutes <= 2.0) return Math.max(0.8, 1.0 - (minutes - 0.5) * 0.13);
        if (minutes <= 5.0) return Math.max(0.4, 0.8 - (minutes - 2.0) * 0.13);
        return Math.max(0.0, 0.4 - (minutes - 5.0) * 0.05);
    }
}
