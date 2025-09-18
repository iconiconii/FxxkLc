package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationRequest;

import java.util.List;

/**
 * Strategy interface for different recommendation approaches.
 * 
 * This interface abstracts the recommendation logic to support multiple
 * recommendation strategies (AI, FSRS, Hybrid) with consistent input/output.
 */
public interface RecommendationStrategy {
    
    /**
     * Generate recommendations for a user with specified parameters.
     * 
     * @param userId The user ID to generate recommendations for
     * @param limit Maximum number of recommendations to return
     * @param objective Learning objective (optional)
     * @param targetDomains Target knowledge domains (optional)
     * @param desiredDifficulty Preferred difficulty level (optional)
     * @param timeboxMinutes Time budget in minutes (optional)
     * @return AIRecommendationResponse with recommendations and metadata
     */
    AIRecommendationResponse getRecommendations(
            Long userId,
            int limit,
            LearningObjective objective,
            List<String> targetDomains,
            DifficultyPreference desiredDifficulty,
            Integer timeboxMinutes
    );
    
    /**
     * Generate recommendations using a RecommendationRequest DTO.
     * This is a convenience method that delegates to the parameterized version.
     * 
     * @param request The recommendation request DTO
     * @return AIRecommendationResponse with recommendations and metadata
     */
    default AIRecommendationResponse getRecommendations(RecommendationRequest request) {
        return getRecommendations(
            request.getUserId(),
            request.getLimit() != null ? request.getLimit() : 10,
            request.getObjective(),
            request.getDomains(),
            request.getDifficultyPreference(),
            request.getTimebox()
        );
    }
    
    /**
     * Get the recommendation type supported by this strategy.
     * 
     * @return RecommendationType enum value
     */
    RecommendationType getType();
    
    /**
     * Check if this strategy is available/enabled.
     * 
     * @return true if strategy can be used, false if disabled or unavailable
     */
    boolean isAvailable();
    
    /**
     * Get strategy priority for AUTO mode selection.
     * Higher values indicate higher priority.
     * 
     * @return Priority value (0-100)
     */
    default int getPriority() {
        return 50; // Default medium priority
    }
    
    /**
     * Check if this strategy supports the given learning objective.
     * 
     * @param objective Learning objective to check
     * @return true if objective is supported, false otherwise
     */
    default boolean supportsObjective(LearningObjective objective) {
        return true; // Default: support all objectives
    }
    
    /**
     * Get estimated response time in milliseconds for this strategy.
     * Used for strategy selection in AUTO mode.
     * 
     * @return Estimated response time in ms
     */
    default long getEstimatedResponseTime() {
        return 1000L; // Default 1 second
    }
}