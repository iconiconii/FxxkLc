package com.codetop.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comprehensive user learning profile based on FSRS review history.
 * 
 * Provides detailed insights into user's learning patterns, strengths, weaknesses,
 * and preferences across different knowledge domains and difficulty levels.
 * Used by LLM recommendation system for personalized problem suggestions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    
    /**
     * User identifier
     */
    private Long userId;
    
    /**
     * When this profile was generated
     */
    @Builder.Default
    private Instant generatedAt = Instant.now();
    
    /**
     * Time window used for profile calculation (e.g., "recent90d", "lifetime")
     */
    @Builder.Default
    private String window = "recent90d";
    
    /**
     * Domain-specific skill statistics
     * Key: domain name (e.g., "arrays", "dynamic_programming")
     * Value: DomainSkill statistics
     */
    private Map<String, DomainSkill> domainSkills;
    
    /**
     * User's difficulty preference pattern
     */
    private DifficultyPref difficultyPref;
    
    /**
     * Tag affinity scores (0.0 to 1.0)
     * Key: tag name, Value: affinity score based on interaction frequency
     */
    private Map<String, Double> tagAffinity;
    
    /**
     * Overall mastery level (0.0 to 1.0)
     * Weighted average of domain skill scores
     */
    @Builder.Default
    private double overallMastery = 0.5;
    
    /**
     * Total number of problems reviewed across all domains
     */
    @Builder.Default
    private int totalProblemsReviewed = 0;
    
    /**
     * Total number of review attempts across all domains
     */
    @Builder.Default
    private int totalReviewAttempts = 0;
    
    /**
     * Average accuracy across all domains
     */
    @Builder.Default
    private double averageAccuracy = 0.5;
    
    /**
     * Learning pattern classification
     */
    @Builder.Default
    private LearningPattern learningPattern = LearningPattern.STEADY_PROGRESS;
    
    /**
     * Classification of user's learning pattern
     */
    public enum LearningPattern {
        STRUGGLING,        // overallMastery < 0.4
        STEADY_PROGRESS,   // overallMastery 0.4-0.7
        ADVANCED          // overallMastery > 0.7
    }
    
    /**
     * Get weak domains (skill score < 0.45 with sufficient samples)
     */
    public List<String> getWeakDomains() {
        if (domainSkills == null) return List.of();
        
        return domainSkills.entrySet().stream()
                .filter(entry -> entry.getValue().isWeak())
                .map(Map.Entry::getKey)
                .sorted((a, b) -> {
                    // Sort by skill score ascending (weakest first)
                    double scoreA = domainSkills.get(a).getSkillScore();
                    double scoreB = domainSkills.get(b).getSkillScore();
                    return Double.compare(scoreA, scoreB);
                })
                .limit(3) // Top 3 weak domains
                .collect(Collectors.toList());
    }
    
    /**
     * Get strong domains (skill score > 0.75 with sufficient samples)
     */
    public List<String> getStrongDomains() {
        if (domainSkills == null) return List.of();
        
        return domainSkills.entrySet().stream()
                .filter(entry -> entry.getValue().isStrong())
                .map(Map.Entry::getKey)
                .sorted((a, b) -> {
                    // Sort by skill score descending (strongest first)
                    double scoreA = domainSkills.get(a).getSkillScore();
                    double scoreB = domainSkills.get(b).getSkillScore();
                    return Double.compare(scoreB, scoreA);
                })
                .limit(3) // Top 3 strong domains
                .collect(Collectors.toList());
    }
    
    /**
     * Get learning approach based on difficulty preference and pattern
     */
    public String getLearningApproach() {
        if (difficultyPref == null) return "balanced_approach";
        
        return switch (difficultyPref.getPreferredLevel()) {
            case BUILDING_CONFIDENCE -> "building_confidence";
            case SEEKING_CHALLENGE -> "prefers_challenge";
            case BALANCED -> "balanced_approach";
        };
    }
    
    /**
     * Check if user needs more practice (based on overall performance)
     */
    public boolean needsMorePractice() {
        return learningPattern == LearningPattern.STRUGGLING || 
               overallMastery < 0.5 || 
               averageAccuracy < 0.6;
    }
    
    /**
     * Check if user is ready for harder challenges
     */
    public boolean readyForChallenge() {
        return learningPattern == LearningPattern.ADVANCED || 
               overallMastery > 0.7 ||
               (difficultyPref != null && difficultyPref.prefersChallenging());
    }
    
    /**
     * Get recommendation focus based on learning pattern
     */
    public String getRecommendationFocus() {
        List<String> weakDomains = getWeakDomains();
        
        if (!weakDomains.isEmpty()) {
            return "cover_weakness_first";
        } else if (readyForChallenge()) {
            return "progressive_difficulty";
        } else {
            return "balanced_coverage";
        }
    }
    
    /**
     * Generate a concise profile summary for LLM prompts
     */
    public String getProfileSummary() {
        StringBuilder summary = new StringBuilder();
        
        // Learning pattern
        summary.append("Learning: ").append(learningPattern.name().toLowerCase());
        
        // Mastery level
        summary.append(", Mastery: ").append(String.format("%.1f", overallMastery));
        
        // Weak topics
        List<String> weakDomains = getWeakDomains();
        if (!weakDomains.isEmpty()) {
            summary.append(", Weak: ").append(String.join(", ", weakDomains));
        }
        
        // Strong topics  
        List<String> strongDomains = getStrongDomains();
        if (!strongDomains.isEmpty()) {
            summary.append(", Strong: ").append(String.join(", ", strongDomains));
        }
        
        // Difficulty preference
        if (difficultyPref != null) {
            summary.append(", Prefers: ").append(difficultyPref.getDominantDifficulty());
        }
        
        return summary.toString();
    }
    
    /**
     * Check if profile has sufficient data for reliable recommendations
     */
    public boolean hasSufficientData() {
        return totalProblemsReviewed >= 10 && totalReviewAttempts >= 20;
    }
    
    /**
     * Get data quality score (0.0 to 1.0)
     */
    public double getDataQuality() {
        if (domainSkills == null || domainSkills.isEmpty()) {
            return 0.0;
        }
        
        // Count domains with sufficient samples
        long domainsWithSufficientData = domainSkills.values().stream()
                .mapToLong(skill -> skill.hasSufficientSamples() ? 1 : 0)
                .sum();
        
        double coverageScore = Math.min(1.0, domainsWithSufficientData / 5.0); // Target: 5 domains
        double volumeScore = Math.min(1.0, totalReviewAttempts / 100.0); // Target: 100 attempts
        
        return (coverageScore + volumeScore) / 2.0;
    }
}