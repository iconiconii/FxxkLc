package com.codetop.recommendation.service;

import com.codetop.recommendation.service.impl.AiRecommendationStrategy;
import com.codetop.recommendation.service.impl.FsrsRecommendationStrategy;
import com.codetop.recommendation.service.impl.HybridRecommendationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Service to resolve and select the appropriate recommendation strategy
 * based on the requested type, availability, and user context.
 */
@Service
public class RecommendationStrategyResolver {
    
    private static final Logger log = LoggerFactory.getLogger(RecommendationStrategyResolver.class);
    
    private final Map<RecommendationType, RecommendationStrategy> strategies;
    private final RecommendationStrategy defaultStrategy;
    
    @Autowired
    public RecommendationStrategyResolver(
            AiRecommendationStrategy aiStrategy,
            FsrsRecommendationStrategy fsrsStrategy,
            HybridRecommendationStrategy hybridStrategy) {
        
        this.strategies = new HashMap<>();
        strategies.put(RecommendationType.AI, aiStrategy);
        strategies.put(RecommendationType.FSRS, fsrsStrategy);
        strategies.put(RecommendationType.HYBRID, hybridStrategy);
        
        // Default to hybrid strategy for best user experience
        this.defaultStrategy = hybridStrategy;
    }
    
    /**
     * Resolve the appropriate recommendation strategy based on type and context.
     * 
     * @param requestedType The requested recommendation type
     * @param userId User ID for context-aware selection
     * @param objective Learning objective for strategy compatibility
     * @return The selected recommendation strategy
     */
    public RecommendationStrategy resolveStrategy(
            RecommendationType requestedType,
            Long userId,
            LearningObjective objective) {
        
        log.debug("Resolving strategy for type={}, userId={}, objective={}", 
                  requestedType, userId, objective);
        
        if (requestedType == RecommendationType.AUTO) {
            return selectBestAvailableStrategy(userId, objective);
        }
        
        RecommendationStrategy strategy = strategies.get(requestedType);
        if (strategy == null) {
            log.warn("Unknown recommendation type: {}, falling back to default", requestedType);
            return defaultStrategy;
        }
        
        if (!strategy.isAvailable()) {
            log.warn("Requested strategy {} is not available, falling back to default", requestedType);
            return selectFallbackStrategy(requestedType, objective);
        }
        
        if (!strategy.supportsObjective(objective)) {
            log.warn("Requested strategy {} does not support objective {}, falling back", 
                     requestedType, objective);
            return selectObjectiveCompatibleStrategy(objective);
        }
        
        return strategy;
    }
    
    /**
     * Select the best available strategy for AUTO mode.
     */
    private RecommendationStrategy selectBestAvailableStrategy(Long userId, LearningObjective objective) {
        log.debug("Auto-selecting best strategy for userId={}, objective={}", userId, objective);
        
        // Score and rank available strategies
        RecommendationStrategy bestStrategy = null;
        int bestScore = -1;
        
        for (RecommendationStrategy strategy : strategies.values()) {
            if (!strategy.isAvailable() || !strategy.supportsObjective(objective)) {
                continue;
            }
            
            int score = calculateStrategyScore(strategy, userId, objective);
            log.debug("Strategy {} scored {} for auto-selection", strategy.getType(), score);
            
            if (score > bestScore) {
                bestScore = score;
                bestStrategy = strategy;
            }
        }
        
        if (bestStrategy != null) {
            log.debug("Auto-selected strategy: {}", bestStrategy.getType());
            return bestStrategy;
        }
        
        log.warn("No suitable strategy found for auto-selection, using default");
        return defaultStrategy;
    }
    
    /**
     * Calculate a score for strategy selection in AUTO mode.
     */
    private int calculateStrategyScore(RecommendationStrategy strategy, Long userId, LearningObjective objective) {
        int score = strategy.getPriority(); // Base score from strategy priority
        
        // Bonus for fast response times (prefer responsive strategies)
        long responseTime = strategy.getEstimatedResponseTime();
        if (responseTime < 500) {
            score += 20; // Very fast
        } else if (responseTime < 1500) {
            score += 10; // Fast enough
        }
        
        // Bonus for objective compatibility
        if (strategy.supportsObjective(objective)) {
            score += 15;
        }
        
        // Strategy-specific bonuses
        if (strategy.getType() == RecommendationType.HYBRID) {
            score += 5; // Slight preference for hybrid approach
        }
        
        return score;
    }
    
    /**
     * Select a fallback strategy when the requested one is unavailable.
     */
    private RecommendationStrategy selectFallbackStrategy(RecommendationType requestedType, LearningObjective objective) {
        log.debug("Selecting fallback for unavailable strategy: {}", requestedType);
        
        // Fallback logic based on requested type
        switch (requestedType) {
            case AI:
                // AI requested but unavailable -> try hybrid, then FSRS
                if (strategies.get(RecommendationType.HYBRID).isAvailable()) {
                    return strategies.get(RecommendationType.HYBRID);
                }
                return strategies.get(RecommendationType.FSRS);
                
            case FSRS:
                // FSRS should always be available, but just in case...
                return strategies.get(RecommendationType.HYBRID);
                
            case HYBRID:
            default:
                // For hybrid or unknown, try to find any available strategy
                return selectBestAvailableStrategy(null, objective);
        }
    }
    
    /**
     * Select a strategy that supports the given learning objective.
     */
    private RecommendationStrategy selectObjectiveCompatibleStrategy(LearningObjective objective) {
        log.debug("Selecting objective-compatible strategy for: {}", objective);
        
        for (RecommendationStrategy strategy : strategies.values()) {
            if (strategy.isAvailable() && strategy.supportsObjective(objective)) {
                log.debug("Found compatible strategy: {}", strategy.getType());
                return strategy;
            }
        }
        
        // If no specific strategy supports the objective, return default
        return defaultStrategy;
    }
    
    /**
     * Get all available strategies for diagnostics.
     */
    public Map<RecommendationType, Boolean> getStrategyAvailability() {
        Map<RecommendationType, Boolean> availability = new HashMap<>();
        strategies.forEach((type, strategy) -> 
            availability.put(type, strategy.isAvailable()));
        return availability;
    }
}