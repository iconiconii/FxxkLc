package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.provider.LlmProvider.ProblemCandidate;
import com.codetop.recommendation.provider.LlmProvider.PromptOptions;
import com.codetop.recommendation.service.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating structured prompt templates for LLM recommendation requests.
 * Implements versioned prompt engineering templates with user profiling and contextual guidance.
 */
@Service
public class PromptTemplateService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Build system message for the LLM conversation.
     * Establishes role, constraints, and output format requirements.
     */
    public String buildSystemMessage(String promptVersion) {
        if ("v2".equals(promptVersion)) {
            return """
                You are an intelligent algorithm problem recommendation engine. Your role is to analyze user learning patterns 
                and recommend the most suitable problems for optimal learning progression.
                
                Core Principles:
                - Prioritize weak areas to strengthen knowledge gaps
                - Balance difficulty progression (not too easy/hard)  
                - Ensure topic diversity to avoid burnout
                - Consider recent performance and attempt patterns
                
                Output Requirements:
                - Return ONLY valid JSON with exact schema: {"items": [{"problemId": number, "reason": string, "confidence": number, "strategy": string, "score": number}]}
                - No markdown formatting, explanations, or additional text
                - problemId must match provided candidates
                - confidence: 0.0-1.0 (how sure you are this is good for the user)
                - score: 0.0-1.0 (overall recommendation strength)
                - strategy: one of "weakness_focus", "progressive_difficulty", "topic_coverage", "review_reinforcement"
                
                Constraints:
                - Never invent problemId not in the candidates list
                - Limit output to requested number of items
                - Ensure each recommendation has clear, actionable reasoning
                """;
        }
        
        // v1 (default) - simpler system message
        return """
            You are a recommendation re-ranking engine for algorithm problems. 
            Return STRICT JSON only with schema: {"items": [{"problemId": number, "reason": string, 
            "confidence": number, "strategy": string, "score": number}]} . 
            No markdown, no explanations, no extra fields.
            """;
    }
    
    /**
     * Build user message with comprehensive context including user profile and problem candidates.
     */
    public String buildUserMessage(RequestContext ctx, List<ProblemCandidate> candidates, PromptOptions options, String promptVersion) {
        if ("v2".equals(promptVersion)) {
            return buildAdvancedUserMessage(ctx, candidates, options);
        }
        return buildBasicUserMessage(ctx, candidates, options);
    }
    
    private String buildBasicUserMessage(RequestContext ctx, List<ProblemCandidate> candidates, PromptOptions options) {
        int limit = options != null ? Math.max(1, Math.min(50, options.limit)) : 10;
        StringBuilder sb = new StringBuilder();
        sb.append("Task: Rank the candidates and return top ").append(limit).append(" items.\\n");
        sb.append("User: ").append(ctx != null && ctx.getUserId() != null ? ctx.getUserId() : "unknown").append("\\n");
        sb.append("Route: ").append(ctx != null && ctx.getRoute() != null ? ctx.getRoute() : "ai-recommendations").append("\\n");
        sb.append("Constraints: Output strictly as JSON object with field 'items'.\\n");
        sb.append("Candidates: ");
        
        try {
            List<Map<String, Object>> candidateArray = buildCandidateArray(candidates);
            sb.append(objectMapper.writeValueAsString(candidateArray));
        } catch (Exception e) {
            sb.append("[]");
        }
        
        sb.append("\\nReturn only JSON. Example: {\"items\":[{\"problemId\":1,\"reason\":\"...\",\"confidence\":0.8,\"strategy\":\"progressive\",\"score\":0.8}]}");
        return sb.toString();
    }
    
    private String buildAdvancedUserMessage(RequestContext ctx, List<ProblemCandidate> candidates, PromptOptions options) {
        int limit = options != null ? Math.max(1, Math.min(50, options.limit)) : 10;
        
        StringBuilder sb = new StringBuilder();
        sb.append("## Task\n");
        sb.append("Select and rank the top ").append(limit).append(" algorithm problems for this user based on learning optimization principles.\n\n");
        
        // User Context
        sb.append("## User Profile\n");
        sb.append("- User ID: ").append(ctx != null && ctx.getUserId() != null ? ctx.getUserId() : "unknown").append("\n");
        sb.append("- Tier: ").append(ctx != null && ctx.getTier() != null ? ctx.getTier() : "BRONZE").append("\n");
        sb.append("- AB Group: ").append(ctx != null && ctx.getAbGroup() != null ? ctx.getAbGroup() : "default").append("\n");
        
        // Use UserProfile from RequestContext if available, otherwise fallback to candidate-derived insights
        UserProfile profile = ctx != null ? ctx.getUserProfile() : null;
        if (profile != null) {
            // Use comprehensive user profile data
            sb.append("- Learning Pattern: ").append(profile.getLearningPattern()).append("\n");
            sb.append("- Overall Mastery: ").append(String.format("%.1f%%", profile.getOverallMastery() * 100)).append("\n");
            sb.append("- Average Accuracy: ").append(String.format("%.1f%%", profile.getAverageAccuracy() * 100)).append("\n");
            sb.append("- Weak Domains: ").append(String.join(", ", profile.getWeakDomains())).append("\n");
            java.util.List<String> strongDomainsSorted = new java.util.ArrayList<>(profile.getStrongDomains());
            java.util.Collections.sort(strongDomainsSorted);
            sb.append("- Strong Domains: ").append(String.join(", ", strongDomainsSorted)).append("\n");
            sb.append("- Difficulty Distribution: ").append(profile.getDifficultyPref() != null ? 
                profile.getDifficultyPref().getDistributionSummary() : "balanced").append("\n");
            sb.append("- Learning Approach: ").append(formatLearningApproach(profile)).append("\n");
            sb.append("- Data Quality: ").append(String.format("%.1f%%", profile.getDataQuality() * 100)).append("\n\n");
        } else {
            // Fallback to candidate-derived insights when profile is not available
            UserInsights insights = analyzeUserProfile(candidates);
            sb.append("- Learning Pattern: ").append(insights.learningPattern).append("\n");
            sb.append("- Weak Topics: ").append(String.join(", ", insights.weakTopics)).append("\n");  
            sb.append("- Strong Topics: ").append(String.join(", ", insights.strongTopics)).append("\n");
            sb.append("- Average Accuracy: ").append(String.format("%.1f%%", insights.averageAccuracy * 100)).append("\n");
            sb.append("- Difficulty Preference: ").append(insights.difficultyTrend).append("\n\n");
        }
        
        // Problem Candidates
        sb.append("## Available Problems\n");
        try {
            List<Map<String, Object>> candidateArray = buildEnhancedCandidateArray(candidates);
            sb.append(objectMapper.writeValueAsString(candidateArray));
        } catch (Exception e) {
            sb.append("[]");
        }
        sb.append("\n\n");
        
        // Recommendation Strategy
        sb.append("## Recommendation Strategy\n");
        if (profile != null) {
            // Use profile-driven strategy
            double dataQuality = profile.getDataQuality();
            sb.append("Focus on: ").append(formatLearningApproach(profile)).append("\n");
            sb.append("Learning Pattern: ").append(profile.getLearningPattern()).append("\n");
            
            if (dataQuality < 0.5) {
                sb.append("Note: Limited data available (").append(String.format("%.1f%%", dataQuality * 100))
                  .append(" quality) - use broader exploration strategy\n");
            }
            
            sb.append("Prioritize problems that:\n");
            sb.append("1. Address weak domains: ").append(String.join(", ", profile.getWeakDomains())).append("\n");
            java.util.List<String> strongDomainsSorted2 = new java.util.ArrayList<>(profile.getStrongDomains());
            java.util.Collections.sort(strongDomainsSorted2);
            sb.append("2. Build on strong domains: ").append(String.join(", ", strongDomainsSorted2)).append("\n");
            sb.append("3. Match difficulty preference: ").append(profile.getDifficultyPref() != null ? 
                profile.getDifficultyPref().getPreferredLevel() : "BALANCED").append("\n");
            sb.append("4. Provide appropriate challenge based on overall mastery (").append(String.format("%.1f%%", profile.getOverallMastery() * 100)).append(")\n\n");
        } else {
            // Fallback to candidate-derived strategy
            UserInsights insights = analyzeUserProfile(candidates);
            sb.append("Focus on: ").append(insights.recommendationFocus).append("\n");
            sb.append("Prioritize problems that:\n");
            sb.append("1. Address identified weak areas\n");
            sb.append("2. Maintain appropriate difficulty progression\n"); 
            sb.append("3. Provide topic diversity\n");
            sb.append("4. Build on recent learning momentum\n\n");
        }
        
        sb.append("Return exactly ").append(limit).append(" items as JSON only.");
        
        return sb.toString();
    }

    private String formatLearningApproach(UserProfile profile) {
        if (profile == null) return "BALANCED";
        if (profile.getDifficultyPref() != null && profile.getDifficultyPref().getPreferredLevel() != null) {
            return switch (profile.getDifficultyPref().getPreferredLevel()) {
                case SEEKING_CHALLENGE -> "CHALLENGE_FOCUSED";
                case BUILDING_CONFIDENCE -> "CONFIDENCE_BUILDING";
                case BALANCED -> "BALANCED_GROWTH";
            };
        }
        String fallback = profile.getLearningApproach();
        // Map legacy lowercase styles to expected tokens
        return switch (fallback) {
            case "prefers_challenge" -> "CHALLENGE_FOCUSED";
            case "building_confidence" -> "CONFIDENCE_BUILDING";
            default -> "BALANCED_GROWTH";
        };
    }
    
    private List<Map<String, Object>> buildCandidateArray(List<ProblemCandidate> candidates) {
        List<Map<String, Object>> array = new ArrayList<>();
        if (candidates != null) {
            for (ProblemCandidate c : candidates) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", c.id);
                if (c.topic != null) map.put("topic", c.topic);
                if (c.difficulty != null) map.put("difficulty", c.difficulty);
                if (c.tags != null) map.put("tags", c.tags);
                if (c.recentAccuracy != null) map.put("recentAccuracy", c.recentAccuracy);
                if (c.attempts != null) map.put("attempts", c.attempts);
                array.add(map);
            }
        }
        return array;
    }
    
    private List<Map<String, Object>> buildEnhancedCandidateArray(List<ProblemCandidate> candidates) {
        List<Map<String, Object>> array = new ArrayList<>();
        if (candidates != null) {
            for (ProblemCandidate c : candidates) {
                Map<String, Object> map = new HashMap<>();
                map.put("problemId", c.id);
                map.put("topic", c.topic != null ? c.topic : "未分类");
                map.put("difficulty", c.difficulty != null ? c.difficulty : "MEDIUM");
                map.put("tags", c.tags != null ? c.tags : List.of());
                map.put("accuracy", c.recentAccuracy != null ? Math.round(c.recentAccuracy * 100) / 100.0 : 0.0);
                map.put("attempts", c.attempts != null ? c.attempts : 0);
                
                // Add learning indicators
                if (c.recentAccuracy != null) {
                    if (c.recentAccuracy < 0.4) {
                        map.put("learningStatus", "needs_attention");
                    } else if (c.recentAccuracy > 0.8) {
                        map.put("learningStatus", "mastered");
                    } else {
                        map.put("learningStatus", "progressing");
                    }
                }
                
                array.add(map);
            }
        }
        return array;
    }
    
    private UserInsights analyzeUserProfile(List<ProblemCandidate> candidates) {
        UserInsights insights = new UserInsights();
        
        if (candidates == null || candidates.isEmpty()) {
            insights.learningPattern = "insufficient_data";
            insights.recommendationFocus = "exploration";
            return insights;
        }
        
        // Analyze performance patterns
        Map<String, List<Double>> topicAccuracy = new HashMap<>();
        Map<String, Integer> difficultyCount = new HashMap<>();
        double totalAccuracy = 0;
        int validAccuracyCount = 0;
        
        for (ProblemCandidate c : candidates) {
            if (c.topic != null) {
                topicAccuracy.computeIfAbsent(c.topic, k -> new ArrayList<>());
                if (c.recentAccuracy != null) {
                    topicAccuracy.get(c.topic).add(c.recentAccuracy);
                    totalAccuracy += c.recentAccuracy;
                    validAccuracyCount++;
                }
            }
            
            if (c.difficulty != null) {
                difficultyCount.merge(c.difficulty, 1, Integer::sum);
            }
        }
        
        // Calculate average accuracy
        insights.averageAccuracy = validAccuracyCount > 0 ? totalAccuracy / validAccuracyCount : 0.5;
        
        // Identify weak and strong topics
        for (Map.Entry<String, List<Double>> entry : topicAccuracy.entrySet()) {
            List<Double> accuracies = entry.getValue();
            double avgAccuracy = accuracies.stream().mapToDouble(Double::doubleValue).average().orElse(0.5);
            
            if (avgAccuracy < 0.5) {
                insights.weakTopics.add(entry.getKey());
            } else if (avgAccuracy > 0.75) {
                insights.strongTopics.add(entry.getKey());
            }
        }
        
        // Determine learning pattern
        if (insights.averageAccuracy < 0.4) {
            insights.learningPattern = "struggling";
            insights.recommendationFocus = "weakness_reinforcement";
        } else if (insights.averageAccuracy > 0.8) {
            insights.learningPattern = "advanced";
            insights.recommendationFocus = "challenge_progression";
        } else {
            insights.learningPattern = "steady_progress";
            insights.recommendationFocus = "balanced_growth";
        }
        
        // Analyze difficulty trend
        int easyCount = difficultyCount.getOrDefault("EASY", 0);
        int mediumCount = difficultyCount.getOrDefault("MEDIUM", 0);
        int hardCount = difficultyCount.getOrDefault("HARD", 0);
        
        if (hardCount > mediumCount && hardCount > easyCount) {
            insights.difficultyTrend = "prefers_challenge";
        } else if (easyCount > mediumCount && easyCount > hardCount) {
            insights.difficultyTrend = "building_confidence";
        } else {
            insights.difficultyTrend = "balanced_approach";
        }
        
        return insights;
    }
    
    private static class UserInsights {
        String learningPattern = "unknown";
        List<String> weakTopics = new ArrayList<>();
        List<String> strongTopics = new ArrayList<>();
        double averageAccuracy = 0.5;
        String difficultyTrend = "balanced";
        String recommendationFocus = "general";
    }
    
    /**
     * Get current prompt version (can be made configurable via properties)
     */
    public String getCurrentPromptVersion() {
        return "v2"; // Default to advanced prompting
    }
}
