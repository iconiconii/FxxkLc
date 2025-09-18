package com.codetop.recommendation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for multi-dimensional recommendation mixing strategy.
 * Maps to rec.strategy.mix configuration block in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "rec.strategy.mix")
@Data
public class MixingStrategyProperties {
    
    /**
     * Whether multi-dimensional mixing is enabled.
     */
    private boolean enabled = true;
    
    /**
     * Strategy quotas for different learning objectives.
     */
    private StrategyQuotas quotas = new StrategyQuotas();
    
    /**
     * Adaptive strategy configuration.
     */
    private Adaptive adaptive = new Adaptive();
    
    /**
     * Performance tuning for mixing operations.
     */
    private Performance performance = new Performance();
    
    @Data
    public static class StrategyQuotas {
        /**
         * Default quota ratios for WEAKNESS_FOCUS objective.
         */
        private WeaknessFocus weaknessFocus = new WeaknessFocus();
        
        /**
         * Default quota ratios for PROGRESSIVE_DIFFICULTY objective.
         */
        private ProgressiveDifficulty progressiveDifficulty = new ProgressiveDifficulty();
        
        /**
         * Default quota ratios for TOPIC_COVERAGE objective.
         */
        private TopicCoverage topicCoverage = new TopicCoverage();
        
        /**
         * Default quota ratios for EXAM_PREP objective.
         */
        private ExamPrep examPrep = new ExamPrep();
        
        /**
         * Default quota ratios for REFRESH_MASTERED objective.
         */
        private RefreshMastered refreshMastered = new RefreshMastered();
        
        @Data
        public static class WeaknessFocus {
            private double weakness = 0.6;
            private double progressive = 0.2;
            private double coverage = 0.2;
            private double exam = 0.0;
            private double refresh = 0.0;
        }
        
        @Data
        public static class ProgressiveDifficulty {
            private double weakness = 0.3;
            private double progressive = 0.5;
            private double coverage = 0.2;
            private double exam = 0.0;
            private double refresh = 0.0;
        }
        
        @Data
        public static class TopicCoverage {
            private double weakness = 0.2;
            private double progressive = 0.3;
            private double coverage = 0.5;
            private double exam = 0.0;
            private double refresh = 0.0;
        }
        
        @Data
        public static class ExamPrep {
            private double weakness = 0.25;
            private double progressive = 0.0;
            private double coverage = 0.0;
            private double exam = 0.6;
            private double refresh = 0.15;
        }
        
        @Data
        public static class RefreshMastered {
            private double weakness = 0.0;
            private double progressive = 0.15;
            private double coverage = 0.25;
            private double exam = 0.0;
            private double refresh = 0.6;
        }
    }
    
    @Data
    public static class Adaptive {
        /**
         * Enable adaptive quota adjustment based on user profile.
         */
        private boolean enabled = true;
        
        /**
         * Minimum mastery level to enable adaptive adjustments.
         */
        private double minMasteryLevel = 0.3;
        
        /**
         * Maximum quota adjustment factor (e.g., 0.2 = 20% adjustment).
         */
        private double maxAdjustmentFactor = 0.2;
        
        /**
         * Number of recent sessions to consider for adaptiveness.
         */
        private int recentSessionsWindow = 10;
    }
    
    @Data
    public static class Performance {
        /**
         * Maximum candidates to process in each strategy category.
         */
        private int maxCandidatesPerStrategy = 100;
        
        /**
         * Timeout for mixing operations in milliseconds.
         */
        private int timeoutMs = 300;
        
        /**
         * Enable caching of categorized candidates.
         */
        private boolean enableCategoryCaching = true;
        
        /**
         * Cache TTL for categorized candidates in seconds.
         */
        private int categoryCacheTtlSeconds = 300;
    }
    
    /**
     * Validate the entire configuration.
     */
    public boolean isValidConfiguration() {
        return quotas != null && adaptive != null && performance != null &&
               performance.maxCandidatesPerStrategy > 0 && performance.timeoutMs > 0 &&
               adaptive.minMasteryLevel >= 0.0 && adaptive.minMasteryLevel <= 1.0 &&
               adaptive.maxAdjustmentFactor >= 0.0 && adaptive.maxAdjustmentFactor <= 1.0;
    }
    
    /**
     * Validate quota ratios for a specific objective sum to approximately 1.0.
     */
    public boolean areQuotasValid() {
        return isQuotaSumValid(quotas.weaknessFocus.weakness, quotas.weaknessFocus.progressive, 
                               quotas.weaknessFocus.coverage, quotas.weaknessFocus.exam, quotas.weaknessFocus.refresh) &&
               isQuotaSumValid(quotas.progressiveDifficulty.weakness, quotas.progressiveDifficulty.progressive,
                               quotas.progressiveDifficulty.coverage, quotas.progressiveDifficulty.exam, quotas.progressiveDifficulty.refresh) &&
               isQuotaSumValid(quotas.topicCoverage.weakness, quotas.topicCoverage.progressive,
                               quotas.topicCoverage.coverage, quotas.topicCoverage.exam, quotas.topicCoverage.refresh) &&
               isQuotaSumValid(quotas.examPrep.weakness, quotas.examPrep.progressive,
                               quotas.examPrep.coverage, quotas.examPrep.exam, quotas.examPrep.refresh) &&
               isQuotaSumValid(quotas.refreshMastered.weakness, quotas.refreshMastered.progressive,
                               quotas.refreshMastered.coverage, quotas.refreshMastered.exam, quotas.refreshMastered.refresh);
    }
    
    private boolean isQuotaSumValid(double... quotas) {
        double sum = 0.0;
        for (double quota : quotas) {
            sum += quota;
        }
        return Math.abs(sum - 1.0) < 0.001; // Allow small floating point differences
    }
    
    /**
     * Get configuration summary for logging/debugging.
     */
    public String getConfigurationSummary() {
        return String.format("MixingStrategy[enabled=%s, adaptive=%s, maxCandidates=%d, timeout=%dms]",
            enabled, adaptive.enabled, performance.maxCandidatesPerStrategy, performance.timeoutMs);
    }
}