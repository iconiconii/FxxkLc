package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.dto.DomainSkill;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.config.UserProfilingProperties;
import com.codetop.recommendation.alg.SimilarityScorer;
import com.codetop.recommendation.config.SimilarityProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhances candidate selection with domain-based intelligence and similarity scoring.
 * Uses UserProfile to filter, weight, and optimize candidate sets before LLM processing.
 * Integrates P0-4 similarity-based enhancements for better recommendation relevance.
 * GPT5-Fix-5: Consolidated with similarity functionality from alg.CandidateEnhancer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateEnhancer {
    
    private final UserProfilingProperties config;
    private final ObjectMapper objectMapper;
    private final SimilarityScorer similarityScorer;
    private final SimilarProblemService similarProblemService;
    private final SimilarityProperties similarityConfig;
    
    /**
     * Enhanced candidate selection with domain-based filtering and weighting.
     * 
     * @param candidates Raw candidates from CandidateBuilder
     * @param userProfile User learning profile for personalization
     * @param limit Target number of candidates for LLM
     * @return Enhanced and filtered candidate list
     */
    public List<LlmProvider.ProblemCandidate> enhanceCandidates(
            List<LlmProvider.ProblemCandidate> candidates,
            UserProfile userProfile,
            int limit) {
        
        if (candidates == null || candidates.isEmpty()) {
            log.debug("No candidates to enhance");
            return candidates;
        }
        
        if (userProfile == null) {
            log.debug("No user profile available, using basic candidate selection");
            return candidates.stream().limit(limit).collect(Collectors.toList());
        }
        
        long startTime = System.nanoTime();
        log.debug("Enhancing {} candidates with user profile for userId={}", 
                 candidates.size(), userProfile.getUserId());
        
        // Step 1: Add domain and tag information to candidates
        List<EnhancedCandidate> enhanced = enhanceCandidatesWithDomains(candidates, userProfile);
        
        // Step 2: Add similarity-based enhancement (P0-4 integration)
        enhanced = enhanceCandidatesWithSimilarity(enhanced, userProfile);
        
        // Step 3: Apply similarity-based pre-filtering if enabled
        if (similarityConfig.getIntegration().isPreFilterEnabled()) {
            enhanced = applySimilarityPreFiltering(enhanced, userProfile);
            log.debug("Applied similarity pre-filtering for userId={}, remaining candidates={}", 
                     userProfile.getUserId(), enhanced.size());
        }
        
        // Step 4: Apply domain-based filtering strategy
        List<EnhancedCandidate> filtered = applyDomainBasedFiltering(enhanced, userProfile);
        
        // Step 5: Apply intelligent weighting based on learning strategy
        List<EnhancedCandidate> weighted = applyLearningStrategyWeighting(filtered, userProfile);
        
        // Step 6: Apply hybrid scoring if enabled
        if (similarityConfig.getIntegration().isHybridScoringEnabled()) {
            weighted = applyHybridScoring(weighted, userProfile);
            log.debug("Applied hybrid scoring for userId={}", userProfile.getUserId());
        }
        
        // Step 7: Select optimal mix of candidates
        List<EnhancedCandidate> selected = selectOptimalMix(weighted, userProfile, limit);
        
        // Convert back to standard candidates
        List<LlmProvider.ProblemCandidate> result = selected.stream()
                .map(EnhancedCandidate::toCandidate)
                .collect(Collectors.toList());
        
        long totalTime = System.nanoTime() - startTime;
        
        // Calculate enhancement statistics
        double avgDomainAffinity = selected.stream().mapToDouble(c -> c.domainAffinityScore).average().orElse(0.0);
        double avgTagFamiliarity = selected.stream().mapToDouble(c -> c.tagFamiliarityScore).average().orElse(0.0);
        double avgSimilarityScore = selected.stream().mapToDouble(c -> c.averageSimilarityScore).average().orElse(0.0);
        
        log.info("Candidate enhancement completed - userId={}, input_candidates={}, final_candidates={}, " +
                "processing_time_ms={}, avg_domain_affinity={}, avg_tag_familiarity={}, " +
                "avg_similarity_score={}", 
                userProfile.getUserId(), candidates.size(), result.size(), 
                String.format("%.2f", totalTime / 1_000_000.0), 
                String.format("%.3f", avgDomainAffinity), 
                String.format("%.3f", avgTagFamiliarity), 
                String.format("%.3f", avgSimilarityScore));
        
        if (log.isDebugEnabled()) {
            // Log learning pattern and domain focus for analysis
            log.debug("Enhancement details - userId={}, learning_pattern={}, weak_domains={}, strong_domains={}", 
                     userProfile.getUserId(), userProfile.getLearningPattern(),
                     userProfile.getWeakDomains().size(), userProfile.getStrongDomains().size());
        }
        
        return result;
    }
    
    /**
     * Enhance candidates with domain and tag analysis
     */
    private List<EnhancedCandidate> enhanceCandidatesWithDomains(
            List<LlmProvider.ProblemCandidate> candidates, UserProfile userProfile) {
        
        return candidates.stream()
                .map(candidate -> {
                    EnhancedCandidate enhanced = new EnhancedCandidate(candidate);
                    
                    // Extract domains from tags
                    if (candidate.tags != null && !candidate.tags.isEmpty()) {
                        enhanced.domains = candidate.tags.stream()
                                .map(tag -> config.getTagDomainMapping().getOrDefault(tag.toLowerCase(), "other"))
                                .filter(domain -> !"other".equals(domain))
                                .collect(Collectors.toSet());
                    } else {
                        enhanced.domains = Collections.emptySet();
                    }
                    
                    // Calculate domain affinity score
                    enhanced.domainAffinityScore = calculateDomainAffinityScore(enhanced.domains, userProfile);
                    
                    // Calculate tag familiarity score  
                    enhanced.tagFamiliarityScore = calculateTagFamiliarityScore(candidate.tags, userProfile);
                    
                    return enhanced;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Enhance candidates with similarity-based scoring (P0-4 integration).
     * GPT5-Fix-5: Integrated from alg.CandidateEnhancer.
     * 
     * Performance guardrails:
     * - Skip similarity enhancement if candidate count is too high
     * - Gate similarity calls behind data quality checks
     * - Limit concurrent similarity operations
     */
    private List<EnhancedCandidate> enhanceCandidatesWithSimilarity(
            List<EnhancedCandidate> candidates, UserProfile userProfile) {
        
        if (candidates.isEmpty()) return candidates;
        
        // Performance guardrail: Skip similarity enhancement for large candidate sets
        int maxCandidatesForSimilarity = similarityConfig.getIntegration().getMaxCandidatesForSimilarity();
        if (candidates.size() > maxCandidatesForSimilarity) {
            log.debug("Skipping similarity enhancement due to candidate count: {} > {}", 
                     candidates.size(), maxCandidatesForSimilarity);
            return candidates;
        }
        
        // Performance guardrail: Check user profile data quality
        if (!hasMinimumDataQuality(userProfile)) {
            log.debug("Skipping similarity enhancement due to insufficient user profile data quality");
            return candidates;
        }
        
        try {
            // Performance guardrail: Limit processing to conservative subset
            int processLimit = Math.min(candidates.size(), 
                similarityConfig.getIntegration().getPreFilterLimit());
            
            // For each candidate (up to limit), find similar problems and calculate similarity metrics
            for (int i = 0; i < processLimit; i++) {
                EnhancedCandidate enhanced = candidates.get(i);
                List<SimilarProblemService.EnhancedSimilarProblem> similarProblems = 
                        similarProblemService.findEnhancedSimilarProblems(
                                enhanced.candidate.id, 3, 1, 0.3); // Max 3 similar, min similarity 0.3
                
                if (!similarProblems.isEmpty()) {
                    // Calculate aggregate similarity metrics
                    List<Double> scores = similarProblems.stream()
                            .map(SimilarProblemService.EnhancedSimilarProblem::getSimilarityScore)
                            .collect(Collectors.toList());
                    
                    enhanced.maxSimilarityScore = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                    enhanced.averageSimilarityScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    
                    // Calculate diversity score
                    enhanced.similarityDiversityScore = calculateSimilarityDiversityScore(
                            enhanced.candidate.tags, similarProblems);
                } else {
                    enhanced.maxSimilarityScore = 0.0;
                    enhanced.averageSimilarityScore = 0.0;
                    enhanced.similarityDiversityScore = 1.0; // High diversity if no similar problems found
                }
            }
        } catch (Exception e) {
            log.warn("Error enhancing candidates with similarity: {}", e.getMessage());
            // Continue without similarity enhancement - graceful degradation
        }
        
        return candidates;
    }
    
    /**
     * Calculate similarity diversity score for better recommendation variety.
     */
    private double calculateSimilarityDiversityScore(List<String> candidateTags, 
                                                   List<SimilarProblemService.EnhancedSimilarProblem> similarProblems) {
        if (candidateTags == null || candidateTags.isEmpty() || similarProblems.isEmpty()) {
            return 1.0; // High diversity
        }
        
        Set<String> allTags = new HashSet<>(candidateTags);
        for (SimilarProblemService.EnhancedSimilarProblem sp : similarProblems) {
            if (sp.getTags() != null) {
                allTags.addAll(sp.getTags());
            }
        }
        
        int totalTagOccurrences = candidateTags.size() + 
                similarProblems.stream().mapToInt(sp -> sp.getTags() != null ? sp.getTags().size() : 0).sum();
        
        return totalTagOccurrences == 0 ? 1.0 : Math.min(1.0, (double) allTags.size() / totalTagOccurrences);
    }
    
    /**
     * Apply domain-based filtering based on user's learning needs
     */
    private List<EnhancedCandidate> applyDomainBasedFiltering(
            List<EnhancedCandidate> candidates, UserProfile userProfile) {
        
        // Get weak domains for targeted improvement
        Set<String> weakDomains = new HashSet<>(userProfile.getWeakDomains());
        Set<String> strongDomains = new HashSet<>(userProfile.getStrongDomains());
        
        // Filter strategy based on learning pattern
        double weakDomainThreshold = 0.3;
        double strongDomainThreshold = 0.7;
        
        return candidates.stream()
                .filter(candidate -> {
                    // Always include problems in weak domains (for improvement)
                    boolean hasWeakDomain = candidate.domains.stream().anyMatch(weakDomains::contains);
                    if (hasWeakDomain) return true;
                    
                    // Include problems in strong domains with some probability (for maintenance)
                    boolean hasStrongDomain = candidate.domains.stream().anyMatch(strongDomains::contains);
                    if (hasStrongDomain && deterministicProbability(candidate.candidate.id, userProfile.getUserId(), "strong") < strongDomainThreshold) return true;
                    
                    // Include problems in unfamiliar domains for exploration
                    boolean hasUnfamiliarDomain = candidate.domains.isEmpty() || 
                        candidate.domains.stream().noneMatch(domain -> 
                            userProfile.getDomainSkills().containsKey(domain));
                    if (hasUnfamiliarDomain && deterministicProbability(candidate.candidate.id, userProfile.getUserId(), "unfamiliar") < weakDomainThreshold) return true;
                    
                    return false;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Apply learning strategy-based weighting
     */
    private List<EnhancedCandidate> applyLearningStrategyWeighting(
            List<EnhancedCandidate> candidates, UserProfile userProfile) {
        
        UserProfile.LearningPattern pattern = userProfile.getLearningPattern();
        
        candidates.forEach(candidate -> {
            double weight = 1.0;
            
            // Base weight from domain affinity and familiarity
            weight = 0.4 * candidate.domainAffinityScore + 0.3 * candidate.tagFamiliarityScore + 0.3;
            
            // Adjust based on learning pattern
            switch (pattern) {
                case STRUGGLING -> {
                    // Focus on easier problems in weak domains
                    if ("EASY".equals(candidate.candidate.difficulty)) weight *= 1.5;
                    if (candidate.domains.stream().anyMatch(userProfile.getWeakDomains()::contains)) weight *= 1.3;
                }
                case ADVANCED -> {
                    // Focus on challenging problems and new domains
                    if ("HARD".equals(candidate.candidate.difficulty)) weight *= 1.4;
                    if ("MEDIUM".equals(candidate.candidate.difficulty)) weight *= 1.2;
                    if (candidate.domains.isEmpty()) weight *= 1.3; // Unfamiliar domains
                }
                case STEADY_PROGRESS -> {
                    // Balanced approach with slight preference for medium difficulty
                    if ("MEDIUM".equals(candidate.candidate.difficulty)) weight *= 1.2;
                    weight *= (1.0 + candidate.domainAffinityScore * 0.2);
                }
            }
            
            // Boost problems in weak domains for targeted improvement
            if (candidate.domains.stream().anyMatch(userProfile.getWeakDomains()::contains)) {
                weight *= 1.4;
            }
            
            // Slight penalty for overly familiar tags to encourage diversity
            if (candidate.tagFamiliarityScore > 0.8) {
                weight *= 0.9;
            }
            
            candidate.enhancementWeight = Math.max(0.1, Math.min(2.0, weight));
        });
        
        return candidates;
    }

    /**
     * Deterministic probability in [0,1) based on candidate/user IDs and a salt to avoid true randomness
     * for reproducibility and stable A/B analysis.
     */
    private double deterministicProbability(Long problemId, Long userId, String salt) {
        long pid = problemId != null ? problemId : 0L;
        long uid = userId != null ? userId : 0L;
        long h = 1469598103934665603L; // FNV-1a offset basis
        h ^= pid; h *= 1099511628211L;
        h ^= uid; h *= 1099511628211L;
        if (salt != null) {
            for (int i = 0; i < salt.length(); i++) {
                h ^= salt.charAt(i);
                h *= 1099511628211L;
            }
        }
        // Normalize to [0,1)
        double v = (double) (h >>> 1) / (double) Long.MAX_VALUE;
        return v < 0 ? 0.0 : (v >= 1.0 ? 0.999999999 : v);
    }
    
    /**
     * Select optimal mix of candidates considering diversity and learning goals
     */
    private List<EnhancedCandidate> selectOptimalMix(
            List<EnhancedCandidate> candidates, UserProfile userProfile, int limit) {
        
        // Apply diversity weight to final scoring
        double diversityWeight = similarityConfig.getWeights().getDiversityWeight();
        
        for (EnhancedCandidate candidate : candidates) {
            // Calculate final score combining enhancement weight and diversity
            double diversityScore = candidate.similarityDiversityScore;
            double finalScore = (1.0 - diversityWeight) * candidate.enhancementWeight + 
                              diversityWeight * diversityScore;
            candidate.enhancementWeight = finalScore;
        }
        
        // Sort by final enhanced weight (descending)
        candidates.sort((a, b) -> Double.compare(b.enhancementWeight, a.enhancementWeight));
        
        List<EnhancedCandidate> selected = new ArrayList<>();
        Set<String> selectedDomains = new HashSet<>();
        Set<String> selectedTags = new HashSet<>();
        
        int weakDomainCount = 0;
        int strongDomainCount = 0;
        
        for (EnhancedCandidate candidate : candidates) {
            if (selected.size() >= limit) break;
            
            // Ensure diversity in domains
            boolean addsDomainDiversity = candidate.domains.stream()
                    .anyMatch(domain -> !selectedDomains.contains(domain));
            
            // Ensure diversity in tags
            boolean addsTagDiversity = candidate.candidate.tags != null &&
                    candidate.candidate.tags.stream().anyMatch(tag -> !selectedTags.contains(tag));
            
            // Count weak/strong domain coverage
            boolean hasWeakDomain = candidate.domains.stream()
                    .anyMatch(userProfile.getWeakDomains()::contains);
            boolean hasStrongDomain = candidate.domains.stream()
                    .anyMatch(userProfile.getStrongDomains()::contains);
            
            // Selection criteria
            boolean shouldSelect = selected.size() < limit * 0.6 || // First 60% by weight
                    addsDomainDiversity ||
                    addsTagDiversity ||
                    (hasWeakDomain && weakDomainCount < limit * 0.4) ||
                    (hasStrongDomain && strongDomainCount < limit * 0.3);
            
            if (shouldSelect) {
                selected.add(candidate);
                selectedDomains.addAll(candidate.domains);
                if (candidate.candidate.tags != null) {
                    selectedTags.addAll(candidate.candidate.tags);
                }
                
                if (hasWeakDomain) weakDomainCount++;
                if (hasStrongDomain) strongDomainCount++;
            }
        }
        
        log.debug("Selected {} candidates with {} domains, {} weak domains, {} strong domains, diversity_weight={}",
                 selected.size(), selectedDomains.size(), weakDomainCount, strongDomainCount, 
                 String.format("%.2f", diversityWeight));
        
        return selected;
    }
    
    /**
     * Check if user profile has minimum data quality for expensive similarity operations.
     * 
     * @param userProfile User learning profile
     * @return true if profile has sufficient data for similarity enhancement
     */
    private boolean hasMinimumDataQuality(UserProfile userProfile) {
        if (userProfile == null) {
            return false;
        }
        
        // Check for minimum profile completeness
        boolean hasWeakDomains = userProfile.getWeakDomains() != null && !userProfile.getWeakDomains().isEmpty();
        boolean hasStrongDomains = userProfile.getStrongDomains() != null && !userProfile.getStrongDomains().isEmpty();
        boolean hasDifficultyPref = userProfile.getDifficultyPref() != null;
        boolean hasReasonableMastery = userProfile.getOverallMastery() > 0.1; // At least 10% mastery
        
        // Require at least 2 out of 4 quality indicators
        int qualityIndicators = 0;
        if (hasWeakDomains) qualityIndicators++;
        if (hasStrongDomains) qualityIndicators++;
        if (hasDifficultyPref) qualityIndicators++;
        if (hasReasonableMastery) qualityIndicators++;
        
        return qualityIndicators >= 2;
    }
    
    /**
     * Calculate domain affinity score based on user's domain skills
     */
    private double calculateDomainAffinityScore(Set<String> domains, UserProfile userProfile) {
        if (domains.isEmpty()) return 0.0;
        
        double totalScore = 0.0;
        int validDomains = 0;
        
        for (String domain : domains) {
            DomainSkill skill = userProfile.getDomainSkills().get(domain);
            if (skill != null) {
                totalScore += skill.getSkillScore();
                validDomains++;
            }
        }
        
        return validDomains > 0 ? totalScore / validDomains : 0.5; // Neutral for unknown domains
    }
    
    /**
     * Calculate tag familiarity score based on user's tag affinity
     */
    private double calculateTagFamiliarityScore(List<String> tags, UserProfile userProfile) {
        if (tags == null || tags.isEmpty()) return 0.0;
        
        Map<String, Double> tagAffinity = userProfile.getTagAffinity();
        if (tagAffinity.isEmpty()) return 0.0;
        
        double totalScore = 0.0;
        int validTags = 0;
        
        for (String tag : tags) {
            Double affinity = tagAffinity.get(tag.toLowerCase());
            if (affinity != null) {
                totalScore += affinity;
                validTags++;
            }
        }
        
        return validTags > 0 ? totalScore / validTags : 0.0;
    }
    
    /**
     * Apply similarity-based pre-filtering to reduce candidate set.
     * Uses configurable pre-filter limit and diversity scoring.
     */
    private List<EnhancedCandidate> applySimilarityPreFiltering(
            List<EnhancedCandidate> candidates, UserProfile userProfile) {
        
        int preFilterLimit = similarityConfig.getIntegration().getPreFilterLimit();
        if (candidates.size() <= preFilterLimit) {
            return candidates; // No filtering needed
        }
        
        // Sort by combined similarity and diversity score
        return candidates.stream()
                .sorted((a, b) -> {
                    double scoreA = (a.maxSimilarityScore * 0.6) + (a.similarityDiversityScore * 0.4);
                    double scoreB = (b.maxSimilarityScore * 0.6) + (b.similarityDiversityScore * 0.4);
                    return Double.compare(scoreB, scoreA); // Descending order
                })
                .limit(preFilterLimit)
                .collect(Collectors.toList());
    }
    
    /**
     * Apply hybrid scoring that combines LLM weight, FSRS weight, and similarity weight.
     * Note: LLM and FSRS scores would come from external systems, here we simulate the concept.
     */
    private List<EnhancedCandidate> applyHybridScoring(
            List<EnhancedCandidate> candidates, UserProfile userProfile) {
        
        SimilarityProperties.Integration integration = similarityConfig.getIntegration();
        
        for (EnhancedCandidate candidate : candidates) {
            // Use deterministic simulation based on problem features (replaces Math.random() for reproducibility)
            // LLM score simulation: based on problem ID and features
            long problemIdSeed = candidate.candidate.id != null ? candidate.candidate.id : 1L;
            double llmScore = 0.2 + (0.8 * ((problemIdSeed % 1000) / 1000.0)); // Deterministic score between 0.2-1.0
            
            // FSRS score simulation: based on attempts and accuracy if available
            int attempts = candidate.candidate.attempts != null ? candidate.candidate.attempts : 0;
            double baseAccuracy = candidate.candidate.recentAccuracy != null ? candidate.candidate.recentAccuracy : 0.5;
            double fsrsScore = 0.3 + (0.7 * Math.min(1.0, (baseAccuracy + (attempts * 0.05)))); // Deterministic score between 0.3-1.0
            
            // Use our calculated similarity score
            double similarityScore = (candidate.maxSimilarityScore + candidate.averageSimilarityScore) / 2.0;
            
            // Calculate hybrid score using configured weights
            double hybridScore = integration.getLlmWeight() * llmScore +
                               integration.getFsrsWeight() * fsrsScore +
                               integration.getSimilarityWeight() * similarityScore;
            
            // Update enhancement weight with hybrid score
            candidate.enhancementWeight = Math.max(candidate.enhancementWeight, hybridScore);
            
            log.debug("Hybrid scoring - problemId={}, llm_score={}, fsrs_score={}, similarity_score={}, hybrid_score={}", 
                     candidate.candidate.id, String.format("%.3f", llmScore), String.format("%.3f", fsrsScore), 
                     String.format("%.3f", similarityScore), String.format("%.3f", hybridScore));
        }
        
        return candidates;
    }
    
    /**
     * Enhanced candidate wrapper with domain intelligence and similarity scoring.
     * GPT5-Fix-5: Enhanced with similarity fields from alg.CandidateEnhancer.
     */
    private static class EnhancedCandidate {
        final LlmProvider.ProblemCandidate candidate;
        Set<String> domains = Collections.emptySet();
        double domainAffinityScore = 0.0;
        double tagFamiliarityScore = 0.0;
        double enhancementWeight = 1.0;
        
        // Similarity-based enhancement fields (P0-4 integration)
        double maxSimilarityScore = 0.0;
        double averageSimilarityScore = 0.0;
        double similarityDiversityScore = 1.0;
        
        EnhancedCandidate(LlmProvider.ProblemCandidate candidate) {
            this.candidate = candidate;
        }
        
        LlmProvider.ProblemCandidate toCandidate() {
            return candidate;
        }
    }
}
