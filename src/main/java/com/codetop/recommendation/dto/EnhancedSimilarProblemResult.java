package com.codetop.recommendation.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * Enhanced similar problem result with category metadata integration (P1).
 * Extends the basic similar problem result with rich category information.
 */
@Data
@Builder
public class EnhancedSimilarProblemResult {
    
    /**
     * Basic problem information
     */
    private Long problemId;
    private String title;
    private String difficulty;
    private Double similarityScore;
    
    /**
     * Enhanced category information with metadata
     */
    private List<EnhancedCategoryInfo> categories;
    
    /**
     * Problem tags
     */
    private List<String> tags;
    
    /**
     * Similarity breakdown
     */
    private SimilarityBreakdown similarityBreakdown;
    
    /**
     * Enhanced category information including metadata
     */
    @Data
    @Builder
    public static class EnhancedCategoryInfo {
        private Long categoryId;
        private String name;
        private Double relevanceScore;
        
        // Metadata from CategoryMetadata
        private Integer defaultComplexity;
        private String complexityDescription;
        private List<String> representativeTechniques;
        private String typicalTimeComplexity;
        private String learningDifficulty;
        private List<String> prerequisites;
    }
    
    /**
     * Detailed similarity score breakdown
     */
    @Data
    @Builder
    public static class SimilarityBreakdown {
        private Double tagsScore;
        private Double categoriesScore;
        private Double difficultyScore;
        private String explanation;
        
        // Detailed analysis
        private List<String> commonTags;
        private List<String> commonCategories;
        private String difficultyMatch;
    }
    
    /**
     * Learning insights based on category metadata
     */
    @Data
    @Builder
    public static class LearningInsights {
        private String recommendedApproach;
        private List<String> keyTechniques;
        private String complexityAnalysis;
        private List<String> practiceRecommendations;
    }
    
    private LearningInsights learningInsights;
}