package com.codetop.recommendation.service.impl;

import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.service.*;
import com.codetop.recommendation.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI-powered recommendation strategy implementation.
 * 
 * Uses the existing AIRecommendationService to generate recommendations
 * powered by large language models with user profiling and intelligent ranking.
 */
@Service("aiRecommendationStrategy")
public class AiRecommendationStrategy implements RecommendationStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(AiRecommendationStrategy.class);
    
    private final AIRecommendationService aiRecommendationService;
    private final LlmToggleService llmToggleService;
    private final LlmProperties llmProperties;
    
    @Autowired
    public AiRecommendationStrategy(
            AIRecommendationService aiRecommendationService,
            LlmToggleService llmToggleService,
            LlmProperties llmProperties) {
        this.aiRecommendationService = aiRecommendationService;
        this.llmToggleService = llmToggleService;
        this.llmProperties = llmProperties;
    }
    
    @Override
    public AIRecommendationResponse getRecommendations(
            Long userId,
            int limit,
            LearningObjective objective,
            List<String> targetDomains,
            DifficultyPreference desiredDifficulty,
            Integer timeboxMinutes
    ) {
        log.debug("Generating AI recommendations for userId={}, limit={}, objective={}", 
                  userId, limit, objective);
        
        // Delegate to the existing AIRecommendationService with full parameter support
        return aiRecommendationService.getRecommendations(
                userId, 
                limit, 
                objective, 
                targetDomains, 
                desiredDifficulty, 
                timeboxMinutes, 
                false, // forceRefresh - handled by strategy pattern for now
                null   // abGroup - use default in legacy method
        );
    }
    
    @Override
    public AIRecommendationResponse getRecommendations(
            com.codetop.recommendation.dto.RecommendationRequest request
    ) {
        log.debug("Generating AI recommendations with request DTO - userId={}, limit={}, forceRefresh={}", 
                  request.getUserId(), request.getLimit(), request.getForceRefresh());
        
        // Extract parameters from request and pass forceRefresh and abGroup parameters
        return aiRecommendationService.getRecommendations(
                request.getUserId(),
                request.getLimit() != null ? request.getLimit() : 10,
                request.getObjective(),
                request.getDomains(),
                request.getDifficultyPreference(),
                request.getTimebox(),
                request.getForceRefresh() != null ? request.getForceRefresh() : false,
                request.getAbGroup()
        );
    }
    
    @Override
    public RecommendationType getType() {
        return RecommendationType.AI;
    }
    
    @Override
    public boolean isAvailable() {
        if (llmProperties == null) {
            return false;
        }
        
        try {
            // Check global LLM availability (avoid user-specific context for availability checks)
            return llmProperties.isEnabled();
        } catch (Exception e) {
            log.warn("Failed to check AI recommendation availability: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public int getPriority() {
        return 90; // Highest priority when available
    }
    
    @Override
    public long getEstimatedResponseTime() {
        return 1800L; // ~1.8 seconds for LLM calls
    }
    
    @Override
    public boolean supportsObjective(LearningObjective objective) {
        // AI strategy supports all learning objectives through intelligent prompting
        return true;
    }
}