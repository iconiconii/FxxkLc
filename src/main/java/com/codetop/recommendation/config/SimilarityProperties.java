package com.codetop.recommendation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Configuration properties for similarity scoring and recommendation features.
 * Allows runtime configuration of weights, thresholds, and parameters for A/B testing.
 */
@Data
@Component
@ConfigurationProperties(prefix = "rec.similarity")
public class SimilarityProperties {
    
    /**
     * Weights for similarity calculation (should sum to approximately 1.0)
     */
    private Weights weights = new Weights();
    
    /**
     * Thresholds for filtering and processing
     */
    private Thresholds thresholds = new Thresholds();
    
    /**
     * Caching configuration
     */
    private Cache cache = new Cache();
    
    /**
     * Batch processing configuration
     */
    private Batch batch = new Batch();
    
    /**
     * Integration settings for main recommendation pipeline
     */
    private Integration integration = new Integration();
    
    @Data
    public static class Weights {
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double tagsWeight = 0.4;
        
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double categoriesWeight = 0.3;
        
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double difficultyWeight = 0.3;
        
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double diversityWeight = 0.2;
    }
    
    @Data
    public static class Thresholds {
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double minSimilarityScore = 0.3;
        
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double emptyFeatureSimilarity = 0.0; // Changed from 1.0 as per OPT-2
        
        @Min(1)
        @Max(50)
        private int maxSimilarProblems = 5;
        
        @Min(1)
        @Max(10)
        private int minSharedCategories = 1;
    }
    
    @Data
    public static class Cache {
        private boolean enabled = true;
        
        @Min(1)
        @Max(3600) // 1 hour max
        private int ttlMinutes = 15;
        
        @Min(10)
        @Max(10000)
        private int maxEntries = 1000;
    }
    
    @Data
    public static class Batch {
        @Min(50)
        @Max(1000)
        private int insertBatchSize = 200;
        
        @Min(1000)
        @Max(30000) // 30 seconds max
        private int batchTimeoutMs = 5000;
    }
    
    @Data
    public static class Integration {
        private boolean preFilterEnabled = true;
        private boolean hybridScoringEnabled = false;
        
        @Min(5)
        @Max(100)
        private int preFilterLimit = 30;
        
        @Min(10)
        @Max(200)
        private int maxCandidatesForSimilarity = 50; // Performance guardrail for similarity enhancement
        
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double llmWeight = 0.5;
        
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double fsrsWeight = 0.3;
        
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double similarityWeight = 0.2;
    }
}