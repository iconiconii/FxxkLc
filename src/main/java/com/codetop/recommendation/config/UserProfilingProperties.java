package com.codetop.recommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for user profiling system.
 * Allows runtime configuration of thresholds, windows, and domain mappings.
 */
@Component
@ConfigurationProperties(prefix = "user-profiling")
public class UserProfilingProperties {
    
    private Windows windows = new Windows();
    private Thresholds thresholds = new Thresholds();
    private Cache cache = new Cache();
    private Map<String, String> tagDomainMapping = new HashMap<>();
    
    @PostConstruct
    public void init() {
        // Initialize default tag-to-domain mapping if empty
        if (tagDomainMapping.isEmpty()) {
            initDefaultTagDomainMapping();
        }
    }
    
    private void initDefaultTagDomainMapping() {
        tagDomainMapping.put("array", "arrays");
        tagDomainMapping.put("linked-list", "linked_lists");
        tagDomainMapping.put("hash-table", "hash_tables");
        tagDomainMapping.put("string", "strings");
        tagDomainMapping.put("two-pointers", "two_pointers");
        tagDomainMapping.put("sliding-window", "sliding_window");
        tagDomainMapping.put("binary-search", "binary_search");
        tagDomainMapping.put("sorting", "sorting");
        tagDomainMapping.put("backtracking", "backtracking");
        tagDomainMapping.put("divide-and-conquer", "divide_conquer");
        tagDomainMapping.put("greedy", "greedy");
        tagDomainMapping.put("dynamic-programming", "dynamic_programming");
        tagDomainMapping.put("graph", "graph");
        tagDomainMapping.put("tree", "trees");
        tagDomainMapping.put("binary-tree", "binary_trees");
        tagDomainMapping.put("heap", "heaps");
        tagDomainMapping.put("priority-queue", "heaps");
        tagDomainMapping.put("stack", "stacks_queues");
        tagDomainMapping.put("queue", "stacks_queues");
        tagDomainMapping.put("bit-manipulation", "bit_manipulation");
        tagDomainMapping.put("math", "math");
        tagDomainMapping.put("prefix-sum", "prefix_sum");
        // New mappings from GPT-5 suggestions
        tagDomainMapping.put("union-find", "union_find");
        tagDomainMapping.put("monotonic-stack", "monotonic_stack");
        tagDomainMapping.put("trie", "tries");
        tagDomainMapping.put("geometry", "geometry");
        tagDomainMapping.put("matrix", "matrices");
        tagDomainMapping.put("design", "system_design");
        tagDomainMapping.put("simulation", "simulation");
    }
    
    // Getters and setters
    public Windows getWindows() { return windows; }
    public void setWindows(Windows windows) { this.windows = windows; }
    public Thresholds getThresholds() { return thresholds; }
    public void setThresholds(Thresholds thresholds) { this.thresholds = thresholds; }
    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }
    public Map<String, String> getTagDomainMapping() { return tagDomainMapping; }
    public void setTagDomainMapping(Map<String, String> tagDomainMapping) { this.tagDomainMapping = tagDomainMapping; }
    
    public static class Windows {
        private int recentDays = 90;
        private double halfLifeDays = 30.0;
        private int defaultReviewLimit = 2000;
        private int trendComparisonDays = 30; // For difficulty trend computation
        
        public int getRecentDays() { return recentDays; }
        public void setRecentDays(int recentDays) { this.recentDays = recentDays; }
        public double getHalfLifeDays() { return halfLifeDays; }
        public void setHalfLifeDays(double halfLifeDays) { this.halfLifeDays = halfLifeDays; }
        public int getDefaultReviewLimit() { return defaultReviewLimit; }
        public void setDefaultReviewLimit(int defaultReviewLimit) { this.defaultReviewLimit = defaultReviewLimit; }
        public int getTrendComparisonDays() { return trendComparisonDays; }
        public void setTrendComparisonDays(int trendComparisonDays) { this.trendComparisonDays = trendComparisonDays; }
    }
    
    public static class Thresholds {
        private int minSamplesForReliability = 10;
        private double weakSkillThreshold = 0.45;
        private double strongSkillThreshold = 0.75;
        private double betaSmoothingAlpha = 1.0;
        private double betaSmoothingBeta = 1.0;
        private double difficultyTrendThreshold = 0.15; // 15% change threshold for trends
        
        public int getMinSamplesForReliability() { return minSamplesForReliability; }
        public void setMinSamplesForReliability(int minSamplesForReliability) { this.minSamplesForReliability = minSamplesForReliability; }
        public double getWeakSkillThreshold() { return weakSkillThreshold; }
        public void setWeakSkillThreshold(double weakSkillThreshold) { this.weakSkillThreshold = weakSkillThreshold; }
        public double getStrongSkillThreshold() { return strongSkillThreshold; }
        public void setStrongSkillThreshold(double strongSkillThreshold) { this.strongSkillThreshold = strongSkillThreshold; }
        public double getBetaSmoothingAlpha() { return betaSmoothingAlpha; }
        public void setBetaSmoothingAlpha(double betaSmoothingAlpha) { this.betaSmoothingAlpha = betaSmoothingAlpha; }
        public double getBetaSmoothingBeta() { return betaSmoothingBeta; }
        public void setBetaSmoothingBeta(double betaSmoothingBeta) { this.betaSmoothingBeta = betaSmoothingBeta; }
        public double getDifficultyTrendThreshold() { return difficultyTrendThreshold; }
        public void setDifficultyTrendThreshold(double difficultyTrendThreshold) { this.difficultyTrendThreshold = difficultyTrendThreshold; }
    }
    
    public static class Cache {
        private Duration ttl = Duration.ofHours(1);
        private boolean enabled = true;
        
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}