package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.config.ConfidenceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Confidence calibrator that assesses recommendation quality using multiple data signals.
 * 
 * Evaluates confidence based on:
 * - LLM response quality and consistency
 * - FSRS data completeness and reliability
 * - User profile data richness
 * - Historical recommendation accuracy
 * - Cross-signal consensus and alignment
 * 
 * Produces calibrated confidence scores that help users assess recommendation reliability.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfidenceCalibrator {
    
    private final ConfidenceProperties confidenceConfig;
    
    /**
     * Calibrate confidence scores for a list of recommendations.
     * 
     * @param recommendations List of recommendations to calibrate
     * @param candidateMap Problem candidate features and FSRS data
     * @param userProfile User learning profile for context assessment
     * @param llmMetadata LLM response metadata for quality assessment
     * @return Recommendations with calibrated confidence scores
     */
    public List<RecommendationItemDTO> calibrateConfidence(
            List<RecommendationItemDTO> recommendations,
            Map<Long, LlmProvider.ProblemCandidate> candidateMap,
            UserProfile userProfile,
            LlmResponseMetadata llmMetadata) {
        
        if (recommendations == null || recommendations.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (!confidenceConfig.isEnabled()) {
            log.debug("Confidence calibration disabled, returning original recommendations");
            return recommendations;
        }
        
        log.debug("Calibrating confidence for {} recommendations", recommendations.size());
        
        // Calculate overall context quality for baseline confidence
        double contextQuality = assessContextQuality(userProfile, llmMetadata);
        
        List<RecommendationItemDTO> calibratedRecommendations = new ArrayList<>();
        int filteredCount = 0;
        
        for (RecommendationItemDTO recommendation : recommendations) {
            LlmProvider.ProblemCandidate candidate = candidateMap.get(recommendation.getProblemId());
            
            // Calculate individual confidence components
            ConfidenceComponents components = calculateConfidenceComponents(
                    recommendation, candidate, userProfile, llmMetadata, contextQuality);
            
            // Combine components into final confidence score
            double finalConfidence = combineConfidenceComponents(components);
            
            // Apply confidence threshold filtering
            if (!confidenceConfig.shouldShowRecommendation(finalConfidence)) {
                filteredCount++;
                log.debug("Filtered low confidence recommendation: problem={}, confidence={:.3f}, threshold={:.3f}",
                        recommendation.getProblemId(), finalConfidence, confidenceConfig.getThresholds().getMinimumShow());
                continue;
            }
            
            // Create calibrated recommendation with confidence metadata
            RecommendationItemDTO calibrated = createCalibratedRecommendation(
                    recommendation, finalConfidence, components);
            
            calibratedRecommendations.add(calibrated);
            
            log.debug("Confidence calibration for problem {}: LLM={:.3f}, FSRS={:.3f}, Profile={:.3f}, Historical={:.3f}, Final={:.3f}",
                    recommendation.getProblemId(), components.llmQuality, components.fsrsDataQuality,
                    components.profileRelevance, components.historicalAccuracy, finalConfidence);
        }
        
        if (filteredCount > 0) {
            log.debug("Filtered {} low-confidence recommendations below threshold {:.3f}", 
                    filteredCount, confidenceConfig.getThresholds().getMinimumShow());
        }
        
        log.debug("Confidence calibration completed for {} recommendations", calibratedRecommendations.size());
        
        return calibratedRecommendations;
    }
    
    /**
     * Assess overall context quality for baseline confidence calculation.
     */
    private double assessContextQuality(UserProfile userProfile, LlmResponseMetadata llmMetadata) {
        double contextScore = 0.0;
        
        // User profile completeness (0-0.3)
        double profileCompleteness = assessUserProfileCompleteness(userProfile);
        contextScore += profileCompleteness * 0.3;
        
        // LLM response quality (0-0.4)
        double llmQuality = assessLlmResponseQuality(llmMetadata);
        contextScore += llmQuality * 0.4;
        
        // System availability and health (0-0.3)
        double systemHealth = assessSystemHealth();
        contextScore += systemHealth * 0.3;
        
        return Math.max(0.0, Math.min(1.0, contextScore));
    }
    
    /**
     * Calculate confidence components for an individual recommendation.
     */
    private ConfidenceComponents calculateConfidenceComponents(
            RecommendationItemDTO recommendation,
            LlmProvider.ProblemCandidate candidate,
            UserProfile userProfile,
            LlmResponseMetadata llmMetadata,
            double contextQuality) {
        
        ConfidenceComponents components = new ConfidenceComponents();
        
        // LLM quality assessment (0-1)
        components.llmQuality = assessLlmResponseQuality(llmMetadata);
        
        // FSRS data quality assessment (0-1)
        components.fsrsDataQuality = assessFsrsDataQuality(candidate);
        
        // Profile relevance assessment (0-1)
        components.profileRelevance = assessProfileRelevance(recommendation, candidate, userProfile);
        
        // Historical accuracy assessment (0-1)
        components.historicalAccuracy = assessHistoricalAccuracy(recommendation, candidate, userProfile);
        
        // Cross-signal consensus assessment (0-1)
        components.crossSignalConsensus = assessCrossSignalConsensus(recommendation, candidate);
        
        // Context quality (baseline)
        components.contextQuality = contextQuality;
        
        return components;
    }
    
    /**
     * Combine confidence components into final confidence score.
     */
    private double combineConfidenceComponents(ConfidenceComponents components) {
        ConfidenceProperties.Weights weights = confidenceConfig.getWeights();
        
        double confidence = 
                weights.getLlmQuality() * components.llmQuality +
                weights.getFsrsData() * components.fsrsDataQuality +
                weights.getProfileRelevance() * components.profileRelevance +
                weights.getHistoricalAccuracy() * components.historicalAccuracy +
                weights.getCrossSignalConsensus() * components.crossSignalConsensus +
                weights.getContextQuality() * components.contextQuality;
        
        return Math.max(0.0, Math.min(1.0, confidence));
    }
    
    /**
     * Assess user profile completeness and quality.
     */
    private double assessUserProfileCompleteness(UserProfile userProfile) {
        if (userProfile == null) {
            return 0.1; // Minimal confidence without profile
        }
        
        double completeness = 0.0;
        
        // Check for weak/strong domains data
        if (userProfile.getWeakDomains() != null && !userProfile.getWeakDomains().isEmpty()) {
            completeness += 0.25;
        }
        if (userProfile.getStrongDomains() != null && !userProfile.getStrongDomains().isEmpty()) {
            completeness += 0.25;
        }
        
        // Check for difficulty preferences
        if (userProfile.getDifficultyPref() != null) {
            completeness += 0.2;
        }
        
        // Check for overall mastery level
        if (userProfile.getOverallMastery() > 0) {
            completeness += 0.3;
        }
        
        return completeness;
    }
    
    /**
     * Assess LLM response quality indicators.
     */
    private double assessLlmResponseQuality(LlmResponseMetadata llmMetadata) {
        if (llmMetadata == null) {
            return 0.5; // Neutral when metadata unavailable
        }
        
        double quality = 0.0;
        
        // Response time indicator (faster = more confident)
        if (llmMetadata.responseTimeMs != null) {
            double timeScore = Math.max(0.0, 1.0 - (llmMetadata.responseTimeMs / 10000.0)); // 10s max
            quality += timeScore * 0.2;
        }
        
        // Token usage efficiency
        if (llmMetadata.inputTokens != null && llmMetadata.outputTokens != null) {
            double tokenRatio = Math.min(1.0, (double) llmMetadata.outputTokens / Math.max(1, llmMetadata.inputTokens));
            quality += tokenRatio * 0.3;
        }
        
        // Provider reliability
        if (llmMetadata.provider != null) {
            double providerScore = getProviderReliabilityScore(llmMetadata.provider);
            quality += providerScore * 0.3;
        }
        
        // Response completeness
        if (llmMetadata.isComplete != null && llmMetadata.isComplete) {
            quality += 0.2;
        }
        
        return Math.max(0.0, Math.min(1.0, quality));
    }
    
    /**
     * Assess FSRS data quality and completeness.
     */
    private double assessFsrsDataQuality(LlmProvider.ProblemCandidate candidate) {
        if (candidate == null) {
            return 0.1; // Low confidence without candidate data
        }
        
        double quality = 0.0;
        
        // FSRS signals availability
        if (candidate.urgencyScore != null && candidate.urgencyScore > 0) {
            quality += 0.3;
        }
        
        if (candidate.retentionProbability != null) {
            quality += 0.2;
        }
        
        if (candidate.daysOverdue != null) {
            quality += 0.1;
        }
        
        // Review history depth
        if (candidate.attempts != null) {
            double historyScore = Math.min(1.0, candidate.attempts / 10.0); // 10 attempts = full score
            quality += historyScore * 0.2;
        }
        
        // Accuracy data reliability
        if (candidate.recentAccuracy != null) {
            quality += 0.2;
        }
        
        return Math.max(0.0, Math.min(1.0, quality));
    }
    
    /**
     * Assess how relevant the recommendation is to the user profile.
     */
    private double assessProfileRelevance(RecommendationItemDTO recommendation, 
                                         LlmProvider.ProblemCandidate candidate, 
                                         UserProfile userProfile) {
        if (userProfile == null || candidate == null) {
            return 0.5; // Neutral without profile data
        }
        
        double relevance = 0.0;
        
        // Tag alignment with weak/strong domains
        if (candidate.tags != null && !candidate.tags.isEmpty()) {
            Set<String> candidateTags = new HashSet<>(candidate.tags);
            
            // Relevance to weak domains (higher relevance)
            if (userProfile.getWeakDomains() != null) {
                Set<String> weakDomains = new HashSet<>(userProfile.getWeakDomains());
                candidateTags.retainAll(weakDomains);
                if (!candidateTags.isEmpty()) {
                    relevance += 0.6; // High relevance for weakness addressing
                }
            }
            
            // Relevance to strong domains (moderate relevance)
            if (userProfile.getStrongDomains() != null) {
                Set<String> strongDomains = new HashSet<>(userProfile.getStrongDomains());
                candidateTags.retainAll(strongDomains);
                if (!candidateTags.isEmpty()) {
                    relevance += 0.4; // Moderate relevance for reinforcement
                }
            }
        }
        
        // Difficulty appropriateness
        if (candidate.difficulty != null && userProfile.getDifficultyPref() != null) {
            String preferredDifficulty = userProfile.getDifficultyPref().getPreferredLevel().name();
            if (candidate.difficulty.equals(preferredDifficulty)) {
                relevance += 0.3;
            }
        }
        
        return Math.max(0.0, Math.min(1.0, relevance));
    }
    
    /**
     * Assess historical accuracy of similar recommendations.
     */
    private double assessHistoricalAccuracy(RecommendationItemDTO recommendation, 
                                           LlmProvider.ProblemCandidate candidate, 
                                           UserProfile userProfile) {
        // Placeholder implementation - would integrate with feedback service
        // For now, use candidate accuracy as proxy
        if (candidate != null && candidate.recentAccuracy != null) {
            return candidate.recentAccuracy;
        }
        
        return 0.5; // Neutral without historical data
    }
    
    /**
     * Assess consensus between different recommendation signals.
     */
    private double assessCrossSignalConsensus(RecommendationItemDTO recommendation, 
                                             LlmProvider.ProblemCandidate candidate) {
        double consensus = 0.5; // Start with neutral consensus
        
        // Check if FSRS and LLM signals agree
        if (candidate != null && candidate.urgencyScore != null && recommendation.getScore() != null) {
            double scoreDifference = Math.abs(candidate.urgencyScore - recommendation.getScore());
            consensus = Math.max(0.0, 1.0 - (scoreDifference * 2.0)); // Penalize large differences
        }
        
        return consensus;
    }
    
    /**
     * Assess overall system health indicators.
     */
    private double assessSystemHealth() {
        // Placeholder implementation - would check cache hit rates, response times, etc.
        return 0.9; // Assume good system health for now
    }
    
    /**
     * Get provider reliability score based on historical performance.
     */
    private double getProviderReliabilityScore(String provider) {
        // Placeholder implementation - would use historical provider performance data
        switch (provider.toLowerCase()) {
            case "openai": return 0.9;
            case "anthropic": return 0.9;
            case "deepseek": return 0.8;
            default: return 0.7;
        }
    }
    
    /**
     * Create calibrated recommendation with confidence metadata.
     */
    private RecommendationItemDTO createCalibratedRecommendation(
            RecommendationItemDTO original, 
            double confidence, 
            ConfidenceComponents components) {
        
        // Clone original recommendation preserving all metadata
        RecommendationItemDTO calibrated = new RecommendationItemDTO();
        
        // Core fields
        calibrated.setProblemId(original.getProblemId());
        calibrated.setScore(original.getScore());
        calibrated.setReason(original.getReason());
        calibrated.setSource(original.getSource());
        
        // Preserve metadata fields
        calibrated.setStrategy(original.getStrategy());
        calibrated.setExplanations(original.getExplanations());
        calibrated.setModel(original.getModel());
        calibrated.setPromptVersion(original.getPromptVersion());
        calibrated.setLatencyMs(original.getLatencyMs());
        
        // Set the calibrated confidence score properly
        calibrated.setConfidence(confidence);
        
        // Add confidence information to the reason if configured
        if (confidenceConfig.getDisplay().isIncludeInReason()) {
            String enhancedReason = enhanceReasonWithConfidence(original.getReason(), confidence, components);
            calibrated.setReason(enhancedReason);
        }
        
        return calibrated;
    }
    
    /**
     * Enhance recommendation reason with confidence insights.
     */
    private String enhanceReasonWithConfidence(String originalReason, double confidence, ConfidenceComponents components) {
        StringBuilder enhanced = new StringBuilder(originalReason != null ? originalReason : "");
        
        // Add confidence level indicator
        if (confidence >= 0.8) {
            enhanced.append(" [High Confidence]");
        } else if (confidence >= 0.6) {
            enhanced.append(" [Medium Confidence]");
        } else if (confidence >= 0.4) {
            enhanced.append(" [Low Confidence]");
        } else {
            enhanced.append(" [Very Low Confidence]");
        }
        
        // Add specific confidence factors if enabled
        if (confidenceConfig.getDisplay().isIncludeFactors()) {
            if (components.fsrsDataQuality > 0.8) {
                enhanced.append(" • Rich FSRS data");
            }
            if (components.profileRelevance > 0.8) {
                enhanced.append(" • High profile match");
            }
            if (components.llmQuality > 0.8) {
                enhanced.append(" • Strong LLM response");
            }
        }
        
        return enhanced.toString();
    }
    
    /**
     * Data class for LLM response metadata.
     */
    public static class LlmResponseMetadata {
        public String provider;
        public Long responseTimeMs;
        public Integer inputTokens;
        public Integer outputTokens;
        public Boolean isComplete;
        public String model;
        public Double temperature;
        
        public LlmResponseMetadata(String provider, Long responseTimeMs, Integer inputTokens, Integer outputTokens) {
            this.provider = provider;
            this.responseTimeMs = responseTimeMs;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.isComplete = true;
        }
    }
    
    /**
     * Internal class for confidence component scores.
     */
    private static class ConfidenceComponents {
        double llmQuality = 0.0;
        double fsrsDataQuality = 0.0;
        double profileRelevance = 0.0;
        double historicalAccuracy = 0.0;
        double crossSignalConsensus = 0.0;
        double contextQuality = 0.0;
    }
}