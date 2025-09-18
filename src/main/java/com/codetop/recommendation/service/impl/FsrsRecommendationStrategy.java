package com.codetop.recommendation.service.impl;

import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.service.*;
import com.codetop.service.ProblemService;
import com.codetop.entity.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FSRS-based recommendation strategy implementation.
 * 
 * Uses traditional spaced repetition scheduling (FSRS) algorithm
 * to generate problem recommendations based on review history and due dates.
 */
@Service("fsrsRecommendationStrategy")
public class FsrsRecommendationStrategy implements RecommendationStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(FsrsRecommendationStrategy.class);
    
    private final ProblemService problemService;
    
    @Autowired
    public FsrsRecommendationStrategy(ProblemService problemService) {
        this.problemService = problemService;
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
        log.debug("Generating FSRS recommendations for userId={}, limit={}", userId, limit);
        
        try {
            // Get FSRS recommendations explicitly to avoid AUTO routing
            List<Problem> fsrsProblems = problemService.getRecommendedProblems(userId, limit, RecommendationType.FSRS);
            
            // Convert to recommendation items
            List<RecommendationItemDTO> items = new ArrayList<>();
            for (int i = 0; i < fsrsProblems.size(); i++) {
                Problem problem = fsrsProblems.get(i);
                RecommendationItemDTO item = new RecommendationItemDTO();
                item.setProblemId(problem.getId());
                item.setReason("FSRS spaced repetition: Due for review based on learning history");
                item.setConfidence(0.85); // FSRS has high confidence in scheduling
                item.setScore(1.0 - (i * 0.1)); // Decreasing score by ranking
                item.setStrategy("fsrs");
                item.setSource("FSRS");
                item.setModel("fsrs-v4");
                item.setPromptVersion("fsrs");
                items.add(item);
            }
            
            // Build response
            AIRecommendationResponse response = new AIRecommendationResponse();
            response.setItems(items);
            
            AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
            meta.setTraceId(UUID.randomUUID().toString());
            meta.setGeneratedAt(Instant.now());
            meta.setCached(false);
            meta.setBusy(false);
            meta.setStrategy("fsrs");
            meta.setChainHops(List.of("fsrs"));
            meta.setChainId("fsrs-direct");
            meta.setRecommendationType(getType().getValue());
            response.setMeta(meta);
            
            log.debug("Generated {} FSRS recommendations for userId={}", items.size(), userId);
            return response;
            
        } catch (Exception e) {
            log.error("Failed to generate FSRS recommendations for userId={}: {}", userId, e.getMessage(), e);
            return createErrorResponse(e.getMessage());
        }
    }
    
    @Override
    public RecommendationType getType() {
        return RecommendationType.FSRS;
    }
    
    @Override
    public boolean isAvailable() {
        // FSRS is always available as it's the core scheduling algorithm
        return problemService != null;
    }
    
    @Override
    public int getPriority() {
        return 70; // High priority as fallback strategy
    }
    
    @Override
    public long getEstimatedResponseTime() {
        return 200L; // Very fast, no external API calls
    }
    
    @Override
    public boolean supportsObjective(LearningObjective objective) {
        // FSRS supports basic learning objectives through its scheduling algorithm
        return objective == null || 
               objective == LearningObjective.WEAKNESS_FOCUS ||
               objective == LearningObjective.REFRESH_MASTERED;
    }
    
    /**
     * Create error response when FSRS recommendation fails.
     */
    private AIRecommendationResponse createErrorResponse(String errorMessage) {
        AIRecommendationResponse response = new AIRecommendationResponse();
        response.setItems(new ArrayList<>());
        
        AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
        meta.setTraceId(UUID.randomUUID().toString());
        meta.setGeneratedAt(Instant.now());
        meta.setCached(false);
        meta.setBusy(true);
        meta.setStrategy("fsrs_error");
        meta.setFallbackReason("FSRS error: " + errorMessage);
        meta.setChainHops(List.of("fsrs-error"));
        meta.setRecommendationType(getType().getValue());
        response.setMeta(meta);
        
        return response;
    }
}