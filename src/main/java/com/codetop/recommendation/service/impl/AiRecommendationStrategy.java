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
                timeboxMinutes
        );
    }
    
    @Override
    public RecommendationType getType() {
        return RecommendationType.AI;
    }
    
    @Override
    public boolean isAvailable() {
        if (llmToggleService == null || llmProperties == null) {
            return false;
        }
        
        try {
            // Create minimal request context to check availability
            RequestContext ctx = new RequestContext();
            ctx.setUserId(1L); // Use dummy user ID for availability check
            ctx.setTier("BRONZE"); // Use default tier
            ctx.setAbGroup("default");
            ctx.setRoute("ai-recommendations");
            
            return llmToggleService.isEnabled(ctx, llmProperties);
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