package com.codetop.recommendation.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * Enhanced category metadata DTO for problem feature enhancement (P1).
 * Provides default complexity and representative techniques for each category.
 */
@Data
@Builder
public class CategoryMetadata {
    
    /**
     * Category ID
     */
    private Long categoryId;
    
    /**
     * Category name
     */
    private String name;
    
    /**
     * Default complexity level (1=Easy, 2=Medium, 3=Hard)
     */
    private Integer defaultComplexity;
    
    /**
     * Complexity description
     */
    private String complexityDescription;
    
    /**
     * Representative techniques for this category
     */
    private List<String> representativeTechniques;
    
    /**
     * Common patterns used in this category
     */
    private List<String> commonPatterns;
    
    /**
     * Typical time complexity for problems in this category
     */
    private String typicalTimeComplexity;
    
    /**
     * Typical space complexity for problems in this category
     */
    private String typicalSpaceComplexity;
    
    /**
     * Learning difficulty (beginner, intermediate, advanced)
     */
    private String learningDifficulty;
    
    /**
     * Prerequisites - other categories that should be learned first
     */
    private List<String> prerequisites;
    
    /**
     * Related categories that often appear together
     */
    private List<String> relatedCategories;
    
    /**
     * Additional metadata as key-value pairs
     */
    private Map<String, Object> additionalMetadata;
    
    /**
     * Category usage statistics
     */
    @Data
    @Builder
    public static class CategoryStats {
        private Integer problemCount;
        private Double averageDifficulty;
        private Integer popularityRank;
        private Double successRate;
    }
    
    private CategoryStats stats;
}