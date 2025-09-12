package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.dto.DomainSkill;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.config.UserProfilingProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhances candidate selection with domain-based intelligence.
 * Uses UserProfile to filter, weight, and optimize candidate sets before LLM processing.
 * Reduces LLM workload while improving recommendation relevance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateEnhancer {
    
    private final UserProfilingProperties config;
    private final ObjectMapper objectMapper;
    
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
        
        log.debug("Enhancing {} candidates with user profile for userId={}", 
                 candidates.size(), userProfile.getUserId());
        
        // Step 1: Add domain and tag information to candidates
        List<EnhancedCandidate> enhanced = enhanceCandidatesWithDomains(candidates, userProfile);
        
        // Step 2: Apply domain-based filtering strategy
        List<EnhancedCandidate> filtered = applyDomainBasedFiltering(enhanced, userProfile);
        
        // Step 3: Apply intelligent weighting based on learning strategy
        List<EnhancedCandidate> weighted = applyLearningStrategyWeighting(filtered, userProfile);
        
        // Step 4: Select optimal mix of candidates
        List<EnhancedCandidate> selected = selectOptimalMix(weighted, userProfile, limit);
        
        // Convert back to standard candidates
        List<LlmProvider.ProblemCandidate> result = selected.stream()
                .map(EnhancedCandidate::toCandidate)
                .collect(Collectors.toList());
        
        log.debug("Enhanced candidate selection: {} -> {} candidates for userId={}", 
                 candidates.size(), result.size(), userProfile.getUserId());
        
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
                    if (hasStrongDomain && Math.random() < strongDomainThreshold) return true;
                    
                    // Include problems in unfamiliar domains for exploration
                    boolean hasUnfamiliarDomain = candidate.domains.isEmpty() || 
                        candidate.domains.stream().noneMatch(domain -> 
                            userProfile.getDomainSkills().containsKey(domain));
                    if (hasUnfamiliarDomain && Math.random() < weakDomainThreshold) return true;
                    
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
     * Select optimal mix of candidates considering diversity and learning goals
     */
    private List<EnhancedCandidate> selectOptimalMix(
            List<EnhancedCandidate> candidates, UserProfile userProfile, int limit) {
        
        // Sort by enhancement weight (descending)
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
        
        log.debug("Selected {} candidates with {} domains, {} weak domains, {} strong domains",
                 selected.size(), selectedDomains.size(), weakDomainCount, strongDomainCount);
        
        return selected;
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
     * Enhanced candidate wrapper with domain intelligence
     */
    private static class EnhancedCandidate {
        final LlmProvider.ProblemCandidate candidate;
        Set<String> domains = Collections.emptySet();
        double domainAffinityScore = 0.0;
        double tagFamiliarityScore = 0.0;
        double enhancementWeight = 1.0;
        
        EnhancedCandidate(LlmProvider.ProblemCandidate candidate) {
            this.candidate = candidate;
        }
        
        LlmProvider.ProblemCandidate toCandidate() {
            return candidate;
        }
    }
}