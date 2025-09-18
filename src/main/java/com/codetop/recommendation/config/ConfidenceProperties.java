package com.codetop.recommendation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for confidence calibration system.
 * Maps to rec.confidence configuration block in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "rec.confidence")
@Data
public class ConfidenceProperties {
    
    /**
     * Whether confidence calibration is enabled.
     */
    private boolean enabled = true;
    
    /**
     * Weights for different confidence components.
     */
    private Weights weights = new Weights();
    
    /**
     * Thresholds for confidence classification.
     */
    private Thresholds thresholds = new Thresholds();
    
    /**
     * Display configuration for confidence information.
     */
    private Display display = new Display();
    
    /**
     * Performance configuration for confidence calibration.
     */
    private Performance performance = new Performance();
    
    @Data
    public static class Weights {
        /**
         * Weight for LLM response quality in confidence calculation (0.0-1.0).
         */
        private double llmQuality = 0.25;
        
        /**
         * Weight for FSRS data quality (0.0-1.0).
         */
        private double fsrsData = 0.20;
        
        /**
         * Weight for profile relevance (0.0-1.0).
         */
        private double profileRelevance = 0.20;
        
        /**
         * Weight for historical accuracy (0.0-1.0).
         */
        private double historicalAccuracy = 0.15;
        
        /**
         * Weight for cross-signal consensus (0.0-1.0).
         */
        private double crossSignalConsensus = 0.10;
        
        /**
         * Weight for overall context quality (0.0-1.0).
         */
        private double contextQuality = 0.10;
        
        /**
         * Validate that weights sum to approximately 1.0.
         */
        public boolean isValid() {
            double sum = llmQuality + fsrsData + profileRelevance + historicalAccuracy + crossSignalConsensus + contextQuality;
            return Math.abs(sum - 1.0) < 0.001;
        }
        
        /**
         * Get the sum of all weights for validation purposes.
         */
        public double getSum() {
            return llmQuality + fsrsData + profileRelevance + historicalAccuracy + crossSignalConsensus + contextQuality;
        }
    }
    
    @Data
    public static class Thresholds {
        /**
         * Threshold for high confidence classification.
         */
        private double highConfidence = 0.8;
        
        /**
         * Threshold for medium confidence classification.
         */
        private double mediumConfidence = 0.6;
        
        /**
         * Threshold for low confidence classification.
         */
        private double lowConfidence = 0.4;
        
        /**
         * Minimum confidence required to show a recommendation.
         */
        private double minimumShow = 0.2;
        
        /**
         * Minimum LLM quality score to trust LLM recommendations.
         */
        private double minLlmQuality = 0.3;
        
        /**
         * Minimum FSRS data quality to use FSRS signals.
         */
        private double minFsrsQuality = 0.4;
        
        /**
         * Minimum profile completeness to use personalization.
         */
        private double minProfileCompleteness = 0.3;
        
        /**
         * Validate that thresholds are in proper order.
         */
        public boolean isValid() {
            return highConfidence > mediumConfidence && 
                   mediumConfidence > lowConfidence && 
                   lowConfidence > minimumShow &&
                   minimumShow >= 0.0 && highConfidence <= 1.0;
        }
    }
    
    @Data
    public static class Display {
        /**
         * Include confidence level in recommendation reason.
         */
        private boolean includeInReason = true;
        
        /**
         * Include specific confidence factors in reason.
         */
        private boolean includeFactors = false;
        
        /**
         * Show confidence warnings for low-quality recommendations.
         */
        private boolean showWarnings = true;
        
        /**
         * Format for confidence display in reasons.
         */
        private String confidenceFormat = "[%s Confidence]";
        
        /**
         * Show confidence score numerically.
         */
        private boolean showNumericScore = false;
        
        /**
         * Number of decimal places for numeric confidence scores.
         */
        private int numericPrecision = 2;
    }
    
    @Data
    public static class Performance {
        /**
         * Cache confidence calculations.
         */
        private boolean enableCaching = true;
        
        /**
         * TTL for confidence cache in seconds.
         */
        private int cacheTtlSeconds = 600; // 10 minutes
        
        /**
         * Maximum items to cache.
         */
        private int maxCacheSize = 1000;
        
        /**
         * Timeout for confidence calculation in milliseconds.
         */
        private int calculationTimeoutMs = 200;
        
        /**
         * Enable async confidence calculation.
         */
        private boolean enableAsyncCalculation = false;
        
        /**
         * Batch size for bulk confidence calculations.
         */
        private int batchSize = 50;
    }
    
    /**
     * Historical accuracy tracking configuration.
     */
    @Data
    public static class Historical {
        /**
         * Enable historical accuracy tracking.
         */
        private boolean enabled = true;
        
        /**
         * Window size in days for historical accuracy calculation.
         */
        private int windowDays = 30;
        
        /**
         * Minimum feedback samples needed for reliable historical accuracy.
         */
        private int minSamples = 5;
        
        /**
         * Weight decay for older feedback samples.
         */
        private double decayFactor = 0.95;
        
        /**
         * Update frequency for historical accuracy calculations.
         */
        private String updateFrequency = "daily"; // daily, hourly, realtime
    }
    
    private Historical historical = new Historical();
    
    /**
     * Validate the entire configuration.
     */
    public boolean isValidConfiguration() {
        return weights.isValid() && thresholds.isValid() &&
               performance.cacheTtlSeconds > 0 && performance.maxCacheSize > 0 &&
               performance.calculationTimeoutMs > 0 && performance.batchSize > 0 &&
               historical.windowDays > 0 && historical.minSamples >= 0 &&
               historical.decayFactor > 0.0 && historical.decayFactor <= 1.0;
    }
    
    /**
     * Get confidence level label based on score.
     */
    public String getConfidenceLevel(double confidenceScore) {
        if (confidenceScore >= thresholds.highConfidence) {
            return "High";
        } else if (confidenceScore >= thresholds.mediumConfidence) {
            return "Medium";
        } else if (confidenceScore >= thresholds.lowConfidence) {
            return "Low";
        } else {
            return "Very Low";
        }
    }
    
    /**
     * Check if a recommendation should be shown based on confidence.
     */
    public boolean shouldShowRecommendation(double confidenceScore) {
        return confidenceScore >= thresholds.minimumShow;
    }
    
    /**
     * Get configuration summary for logging/debugging.
     */
    public String getConfigurationSummary() {
        return String.format("Confidence[enabled=%s, weights=%.2f/%.2f/%.2f/%.2f/%.2f/%.2f, thresholds=%.2f/%.2f/%.2f/%.2f]",
            enabled, weights.llmQuality, weights.fsrsData, weights.profileRelevance, 
            weights.historicalAccuracy, weights.crossSignalConsensus, weights.contextQuality,
            thresholds.highConfidence, thresholds.mediumConfidence, thresholds.lowConfidence, thresholds.minimumShow);
    }
}