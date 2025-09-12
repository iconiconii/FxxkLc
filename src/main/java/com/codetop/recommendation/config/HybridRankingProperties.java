package com.codetop.recommendation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for hybrid ranking system.
 * Maps to rec.hybrid configuration block in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "rec.hybrid")
@Data
public class HybridRankingProperties {
    
    /**
     * Whether hybrid ranking is enabled.
     */
    private boolean enabled = true;
    
    /**
     * Weights for different scoring components in hybrid ranking.
     */
    private Weights weights = new Weights();
    
    /**
     * Fallback configuration when hybrid ranking fails.
     */
    private Fallback fallback = new Fallback();
    
    /**
     * Performance configuration for hybrid ranking.
     */
    private Performance performance = new Performance();
    
    @Data
    public static class Weights {
        /**
         * Weight for LLM score in final ranking (0.0-1.0).
         */
        private double llm = 0.45;
        
        /**
         * Weight for FSRS urgency score (0.0-1.0).
         */
        private double fsrs = 0.30;
        
        /**
         * Weight for similarity-based score (0.0-1.0).
         */
        private double similarity = 0.15;
        
        /**
         * Weight for personalization score (0.0-1.0).
         */
        private double personalization = 0.10;
        
        /**
         * Validate that weights sum to approximately 1.0.
         */
        public boolean isValid() {
            double sum = llm + fsrs + similarity + personalization;
            return Math.abs(sum - 1.0) < 0.001; // Allow small floating point differences
        }
        
        /**
         * Get the sum of all weights for validation purposes.
         */
        public double getSum() {
            return llm + fsrs + similarity + personalization;
        }
    }
    
    @Data
    public static class Fallback {
        /**
         * Fallback strategy when hybrid ranking fails.
         * Options: llm-only, fsrs-only, disabled
         */
        private String onError = "llm-only";
        
        /**
         * Check if fallback strategy is valid.
         */
        public boolean isValidStrategy() {
            return "llm-only".equals(onError) || "fsrs-only".equals(onError) || "disabled".equals(onError);
        }
    }
    
    @Data
    public static class Performance {
        /**
         * Maximum candidates to process for hybrid ranking.
         */
        private int maxCandidates = 50;
        
        /**
         * Timeout for hybrid ranking operations in milliseconds.
         */
        private int timeoutMs = 500;
    }
    
    /**
     * Validate the entire configuration.
     */
    public boolean isValidConfiguration() {
        return weights.isValid() && fallback.isValidStrategy() && 
               performance.maxCandidates > 0 && performance.timeoutMs > 0;
    }
    
    /**
     * Get a summary string for logging/debugging.
     */
    public String getConfigurationSummary() {
        return String.format("HybridRanking[enabled=%s, weights=(llm=%.2f,fsrs=%.2f,sim=%.2f,pers=%.2f), fallback=%s]",
            enabled, weights.llm, weights.fsrs, weights.similarity, weights.personalization, fallback.onError);
    }
}