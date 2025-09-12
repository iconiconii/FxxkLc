package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.alg.SimilarityScorer;
import com.codetop.recommendation.config.SimilarityProperties;
import com.codetop.recommendation.config.HybridRankingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid ranking service that combines LLM scores with FSRS, similarity, and personalization signals.
 * Implements the post-LLM hybrid ranking strategy for intelligent recommendations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridRankingService {
    
    private final SimilarityScorer similarityScorer;
    private final SimilarityProperties similarityConfig;
    private final HybridRankingProperties hybridConfig;
    
    /**
     * Re-ranks LLM recommendations using hybrid scoring that combines multiple signals.
     * 
     * @param llmItems Original LLM-ranked recommendations
     * @param candidateMap Map of candidate ID to candidate features
     * @param userProfile User learning profile for personalization
     * @return Re-ranked list with hybrid scores and updated metadata
     */
    public List<RecommendationItemDTO> rankWithHybridScores(
            List<RecommendationItemDTO> llmItems,
            Map<Long, LlmProvider.ProblemCandidate> candidateMap,
            UserProfile userProfile) {
        
        if (llmItems == null || llmItems.isEmpty()) {
            return new ArrayList<>();
        }
        
        log.debug("Re-ranking {} LLM items with hybrid scoring", llmItems.size());
        
        List<HybridScoredItem> scoredItems = new ArrayList<>();
        
        for (RecommendationItemDTO item : llmItems) {
            LlmProvider.ProblemCandidate candidate = candidateMap.get(item.getProblemId());
            if (candidate == null) {
                // If candidate not found, keep original LLM score but log warning
                log.warn("Candidate not found for problemId {}, keeping LLM-only score", item.getProblemId());
                HybridScoredItem hybridItem = new HybridScoredItem(item);
                hybridItem.finalScore = item.getScore() != null ? item.getScore() : 0.5;
                scoredItems.add(hybridItem);
                continue;
            }
            
            // Calculate component scores
            double llmScore = normalizeLlmScore(item.getScore());
            double fsrsScore = calculateFsrsScore(candidate);
            double similarityScore = calculateSimilarityScore(candidate, userProfile);
            double personalizationScore = calculatePersonalizationScore(candidate, userProfile);
            
            // Weighted combination using configuration
            HybridRankingProperties.Weights weights = hybridConfig.getWeights();
            double finalScore = 
                weights.getLlm() * llmScore +
                weights.getFsrs() * fsrsScore +
                weights.getSimilarity() * similarityScore +
                weights.getPersonalization() * personalizationScore;
            
            // Create hybrid item with component scores for transparency
            HybridScoredItem hybridItem = new HybridScoredItem(item);
            hybridItem.finalScore = Math.max(0.0, Math.min(1.0, finalScore)); // Clamp to [0,1]
            hybridItem.llmScore = llmScore;
            hybridItem.fsrsScore = fsrsScore;
            hybridItem.similarityScore = similarityScore;
            hybridItem.personalizationScore = personalizationScore;
            
            scoredItems.add(hybridItem);
            
            log.debug("Hybrid scoring for problem {}: LLM={:.3f}, FSRS={:.3f}, Sim={:.3f}, Pers={:.3f}, Final={:.3f}",
                item.getProblemId(), llmScore, fsrsScore, similarityScore, personalizationScore, finalScore);
        }
        
        // Sort by final score (descending)
        scoredItems.sort(Comparator.comparingDouble((HybridScoredItem h) -> h.finalScore).reversed());
        
        // Convert back to RecommendationItemDTO with updated scores and source
        List<RecommendationItemDTO> result = new ArrayList<>();
        for (HybridScoredItem hybrid : scoredItems) {
            RecommendationItemDTO item = hybrid.originalItem;
            
            // Update with hybrid score and mark as hybrid source
            item.setScore(hybrid.finalScore);
            item.setSource("HYBRID");
            
            // Optionally enhance the reason with hybrid insights
            if (item.getReason() != null && !item.getReason().isEmpty()) {
                String hybridReason = enhanceReasonWithHybridInsights(item.getReason(), hybrid, userProfile);
                item.setReason(hybridReason);
            }
            
            result.add(item);
        }
        
        log.debug("Hybrid ranking completed: {} items reordered", result.size());
        return result;
    }
    
    /**
     * Normalize LLM score to [0,1] range, handling various input formats.
     */
    private double normalizeLlmScore(Double score) {
        if (score == null) return 0.5; // Default neutral score
        
        // Assume LLM scores are already in [0,1] range
        // If they're in different ranges (e.g., [0,5] or [0,10]), add conversion logic here
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Calculate FSRS urgency score from candidate signals.
     * Higher score for more urgent review needs.
     */
    private double calculateFsrsScore(LlmProvider.ProblemCandidate candidate) {
        double score = 0.0;
        
        // Use urgency score if available (already normalized to [0,1])
        if (candidate.urgencyScore != null) {
            score += candidate.urgencyScore * 0.6; // 60% weight to urgency
        }
        
        // Add retention-based component
        if (candidate.retentionProbability != null) {
            double retentionUrgency = 1.0 - candidate.retentionProbability;
            score += retentionUrgency * 0.3; // 30% weight to retention
        }
        
        // Add overdue component
        if (candidate.daysOverdue != null && candidate.daysOverdue > 0) {
            double overdueNormalized = Math.min(1.0, candidate.daysOverdue / 7.0); // Normalize by week
            score += overdueNormalized * 0.1; // 10% weight to overdue days
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Calculate similarity score based on user learning patterns and problem features.
     */
    private double calculateSimilarityScore(LlmProvider.ProblemCandidate candidate, UserProfile userProfile) {
        if (userProfile == null || similarityScorer == null) {
            return 0.5; // Default neutral score
        }
        
        double score = 0.0;
        
        // Tag-based similarity to user's learning patterns
        if (candidate.tags != null && !candidate.tags.isEmpty()) {
            // Calculate similarity to user's strong/weak domains
            Set<String> candidateTags = new HashSet<>(candidate.tags);
            Set<String> userWeakDomains = new HashSet<>(userProfile.getWeakDomains());
            Set<String> userStrongDomains = new HashSet<>(userProfile.getStrongDomains());
            
            // Jaccard similarity with weak domains (higher is better for learning)
            double weakOverlap = calculateJaccardSimilarity(candidateTags, userWeakDomains);
            score += weakOverlap * 0.6;
            
            // Jaccard similarity with strong domains (for reinforcement)
            double strongOverlap = calculateJaccardSimilarity(candidateTags, userStrongDomains);
            score += strongOverlap * 0.4;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Calculate personalization score based on user preferences and difficulty matching.
     */
    private double calculatePersonalizationScore(LlmProvider.ProblemCandidate candidate, UserProfile userProfile) {
        if (userProfile == null) {
            return 0.5; // Default neutral score
        }
        
        double score = 0.0;
        
        // Difficulty matching
        if (candidate.difficulty != null && userProfile.getDifficultyPref() != null) {
            String candidateDifficulty = candidate.difficulty.toUpperCase();
            String preferredDifficulty = userProfile.getDifficultyPref().getPreferredLevel().name();
            
            if (candidateDifficulty.equals(preferredDifficulty)) {
                score += 0.5; // Perfect match
            } else {
                // Partial match based on progression direction
                score += calculateDifficultyMatchScore(candidateDifficulty, preferredDifficulty) * 0.5;
            }
        }
        
        // Domain affinity score
        if (candidate.tags != null && !candidate.tags.isEmpty()) {
            double domainAffinity = calculateDomainAffinityScore(new HashSet<>(candidate.tags), userProfile);
            score += domainAffinity * 0.3;
        }
        
        // Accuracy-based learning match
        if (candidate.recentAccuracy != null) {
            double accuracyScore = calculateAccuracyLearningMatch(candidate.recentAccuracy, userProfile);
            score += accuracyScore * 0.2;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Calculate Jaccard similarity between two sets.
     */
    private double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() && set2.isEmpty()) return 0.0;
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Calculate difficulty match score for progressive learning.
     */
    private double calculateDifficultyMatchScore(String candidateDifficulty, String preferredDifficulty) {
        // Simple difficulty progression scoring
        Map<String, Integer> difficultyLevels = Map.of(
            "EASY", 1,
            "MEDIUM", 2,
            "HARD", 3
        );
        
        Integer candidateLevel = difficultyLevels.get(candidateDifficulty);
        Integer preferredLevel = difficultyLevels.get(preferredDifficulty);
        
        if (candidateLevel == null || preferredLevel == null) return 0.0;
        
        int diff = Math.abs(candidateLevel - preferredLevel);
        return Math.max(0.0, 1.0 - (diff * 0.3)); // Penalize difficulty mismatch
    }
    
    /**
     * Calculate domain affinity based on user learning patterns.
     */
    private double calculateDomainAffinityScore(Set<String> candidateDomains, UserProfile userProfile) {
        // Implementation would use CandidateEnhancer's calculateDomainAffinityScore
        // For now, return a placeholder
        return 0.5;
    }
    
    /**
     * Calculate learning match based on accuracy patterns.
     */
    private double calculateAccuracyLearningMatch(double recentAccuracy, UserProfile userProfile) {
        double overallMastery = userProfile.getOverallMastery();
        
        // Prefer problems slightly below current mastery level for optimal learning
        double targetAccuracy = Math.max(0.3, Math.min(0.8, overallMastery - 0.1));
        double diff = Math.abs(recentAccuracy - targetAccuracy);
        
        return Math.max(0.0, 1.0 - (diff * 2.0)); // Penalize large accuracy differences
    }
    
    /**
     * Enhance the LLM reason with hybrid scoring insights.
     */
    private String enhanceReasonWithHybridInsights(String originalReason, HybridScoredItem hybrid, UserProfile userProfile) {
        // Keep original reason and optionally add hybrid insights
        StringBuilder enhanced = new StringBuilder(originalReason);
        
        // Add the most significant contributing factor
        if (hybrid.fsrsScore > 0.7) {
            enhanced.append(" [High FSRS urgency]");
        } else if (hybrid.personalizationScore > 0.7) {
            enhanced.append(" [Strong personal match]");
        } else if (hybrid.similarityScore > 0.7) {
            enhanced.append(" [Similar to your learning pattern]");
        }
        
        return enhanced.toString();
    }
    
    /**
     * Internal class to hold hybrid scoring components during processing.
     */
    private static class HybridScoredItem {
        final RecommendationItemDTO originalItem;
        double finalScore;
        double llmScore;
        double fsrsScore;
        double similarityScore;
        double personalizationScore;
        
        HybridScoredItem(RecommendationItemDTO originalItem) {
            this.originalItem = originalItem;
        }
    }
    
}