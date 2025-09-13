package com.codetop.recommendation.service.impl;

import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Hybrid recommendation strategy implementation.
 * 
 * Combines AI-powered recommendations with FSRS fallback for optimal
 * user experience. Uses AI when available, gracefully degrades to FSRS.
 */
@Service("hybridRecommendationStrategy")
public class HybridRecommendationStrategy implements RecommendationStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(HybridRecommendationStrategy.class);
    
    private final AiRecommendationStrategy aiStrategy;
    private final FsrsRecommendationStrategy fsrsStrategy;
    
    @Autowired
    public HybridRecommendationStrategy(
            AiRecommendationStrategy aiStrategy,
            FsrsRecommendationStrategy fsrsStrategy) {
        this.aiStrategy = aiStrategy;
        this.fsrsStrategy = fsrsStrategy;
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
        log.debug("Generating hybrid recommendations for userId={}, limit={}", userId, limit);
        
        // Try AI first if available
        if (aiStrategy.isAvailable()) {
            try {
                AIRecommendationResponse aiResponse = aiStrategy.getRecommendations(
                        userId, limit, objective, targetDomains, desiredDifficulty, timeboxMinutes);
                
                // Check if AI response is successful (not busy, has items)
                if (aiResponse.getMeta() != null && 
                    !aiResponse.getMeta().isBusy() && 
                    aiResponse.getItems() != null && 
                    !aiResponse.getItems().isEmpty()) {
                    
                    // Mark as hybrid strategy in metadata
                    aiResponse.getMeta().setStrategy("hybrid_ai");
                    aiResponse.getMeta().setRecommendationType(getType().getValue());
                    List<String> hops = aiResponse.getMeta().getChainHops();
                    if (hops == null) hops = new ArrayList<>();
                    hops.add(0, "hybrid"); // Prepend hybrid indicator
                    aiResponse.getMeta().setChainHops(hops);
                    
                    log.debug("Hybrid strategy using AI: {} items for userId={}", 
                              aiResponse.getItems().size(), userId);
                    return aiResponse;
                }
            } catch (Exception e) {
                log.warn("AI strategy failed in hybrid mode for userId={}, falling back to FSRS: {}", 
                         userId, e.getMessage());
            }
        }
        
        // Fallback to FSRS
        log.debug("Hybrid strategy falling back to FSRS for userId={}", userId);
        AIRecommendationResponse fsrsResponse = fsrsStrategy.getRecommendations(
                userId, limit, objective, targetDomains, desiredDifficulty, timeboxMinutes);
        
        // Mark as hybrid fallback in metadata
        if (fsrsResponse.getMeta() != null) {
            fsrsResponse.getMeta().setStrategy("hybrid_fsrs");
            fsrsResponse.getMeta().setFallbackReason("ai_unavailable_or_failed");
            fsrsResponse.getMeta().setRecommendationType(getType().getValue());
            List<String> hops = fsrsResponse.getMeta().getChainHops();
            if (hops == null) hops = new ArrayList<>();
            hops.add(0, "hybrid"); // Prepend hybrid indicator
            fsrsResponse.getMeta().setChainHops(hops);
        }
        
        return fsrsResponse;
    }
    
    @Override
    public RecommendationType getType() {
        return RecommendationType.HYBRID;
    }
    
    @Override
    public boolean isAvailable() {
        // Hybrid is available if either AI or FSRS is available
        // FSRS should always be available as the core fallback
        return aiStrategy.isAvailable() || fsrsStrategy.isAvailable();
    }
    
    @Override
    public int getPriority() {
        return 80; // High priority, balanced approach
    }
    
    @Override
    public long getEstimatedResponseTime() {
        if (aiStrategy.isAvailable()) {
            return aiStrategy.getEstimatedResponseTime();
        } else {
            return fsrsStrategy.getEstimatedResponseTime();
        }
    }
    
    @Override
    public boolean supportsObjective(LearningObjective objective) {
        // Support objective if either underlying strategy supports it
        return aiStrategy.supportsObjective(objective) || fsrsStrategy.supportsObjective(objective);
    }
}