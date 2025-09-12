package com.codetop.recommendation.service;

import com.codetop.recommendation.config.PromptTemplateProperties;
import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.provider.LlmProvider.ProblemCandidate;
import com.codetop.recommendation.provider.LlmProvider.PromptOptions;
import com.codetop.recommendation.service.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * External template-based prompt service with A/B testing support.
 * Loads prompt templates from resource files and supports variable substitution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalPromptTemplateService {
    
    private final PromptTemplateProperties templateProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Simple Mustache-style template pattern: {{variableName}}
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    /**
     * Build system message using external template files.
     */
    public String buildSystemMessage(String version, RequestContext context) {
        // Select appropriate version based on user context
        String selectedVersion = selectPromptVersion(version, context);
        
        try {
            String template = loadTemplate(selectedVersion, "system");
            
            // System messages typically don't need variable substitution
            // but we can add context-specific variations if needed
            Map<String, Object> variables = new HashMap<>();
            variables.put("version", selectedVersion);
            
            return substituteVariables(template, variables);
            
        } catch (Exception e) {
            log.error("Failed to load system template for version: {}, falling back to hardcoded", selectedVersion, e);
            return getFallbackSystemMessage(selectedVersion);
        }
    }
    
    /**
     * Build user message using external template files with full variable substitution.
     */
    public String buildUserMessage(RequestContext ctx, List<ProblemCandidate> candidates, PromptOptions options) {
        String version = ctx != null && ctx.getPromptVersion() != null 
            ? ctx.getPromptVersion() 
            : templateProperties.getDefaultVersion();
            
        String selectedVersion = selectPromptVersion(version, ctx);
        
        try {
            // Determine template type based on version sophistication
            String templateType = isAdvancedVersion(selectedVersion) ? "user-advanced" : "user-basic";
            String template = loadTemplate(selectedVersion, templateType);
            
            // Build template variables
            Map<String, Object> variables = buildTemplateVariables(ctx, candidates, options, selectedVersion);
            
            return substituteVariables(template, variables);
            
        } catch (Exception e) {
            log.error("Failed to load user template for version: {}, falling back to hardcoded", selectedVersion, e);
            return getFallbackUserMessage(ctx, candidates, options, selectedVersion);
        }
    }
    
    /**
     * Get current prompt version with A/B testing support.
     */
    public String getCurrentPromptVersion(RequestContext context) {
        return selectPromptVersion(templateProperties.getDefaultVersion(), context);
    }
    
    @Cacheable(value = "promptTemplates", key = "#version + '-' + #type", condition = "#root.target.templateProperties.cacheEnabled")
    private String loadTemplate(String version, String type) throws IOException {
        String templateFile = templateProperties.getTemplateFile(version, type);
        ClassPathResource resource = new ClassPathResource(templateFile);
        
        if (!resource.exists()) {
            throw new IOException("Template file not found: " + templateFile);
        }
        
        try {
            byte[] bytes = resource.getInputStream().readAllBytes();
            String template = new String(bytes, StandardCharsets.UTF_8);
            
            log.debug("Loaded template from {}: {} characters", templateFile, template.length());
            return template;
            
        } catch (IOException e) {
            throw new IOException("Failed to read template file: " + templateFile, e);
        }
    }
    
    private String selectPromptVersion(String requestedVersion, RequestContext context) {
        if (context == null) {
            return requestedVersion != null ? requestedVersion : templateProperties.getDefaultVersion();
        }
        
        // Use A/B testing logic from properties
        String selectedVersion = templateProperties.selectVersion(
            context.getTier(),
            context.getAbGroup(), 
            context.getRoute()
        );
        
        // If no A/B test matches, use requested version or default
        if (selectedVersion.equals(templateProperties.getDefaultVersion()) && requestedVersion != null) {
            selectedVersion = requestedVersion;
        }
        
        log.debug("Selected prompt version {} for user {} (tier={}, ab={}, route={})", 
                 selectedVersion, context.getUserId(), context.getTier(), context.getAbGroup(), context.getRoute());
        
        return selectedVersion;
    }
    
    private boolean isAdvancedVersion(String version) {
        return "v2".equals(version) || "v3".equals(version) || version.startsWith("v3.");
    }
    
    private Map<String, Object> buildTemplateVariables(RequestContext ctx, List<ProblemCandidate> candidates, 
                                                      PromptOptions options, String version) {
        Map<String, Object> variables = new HashMap<>();
        
        // Basic variables
        int limit = options != null ? Math.max(1, Math.min(50, options.limit)) : 10;
        variables.put("limit", limit);
        variables.put("userId", ctx != null && ctx.getUserId() != null ? ctx.getUserId() : "unknown");
        variables.put("tier", ctx != null && ctx.getTier() != null ? ctx.getTier() : "BRONZE");
        variables.put("abGroup", ctx != null && ctx.getAbGroup() != null ? ctx.getAbGroup() : "default");
        variables.put("route", ctx != null && ctx.getRoute() != null ? ctx.getRoute() : "ai-recommendations");
        
        // Candidates JSON
        try {
            List<Map<String, Object>> candidateArray = isAdvancedVersion(version) 
                ? buildEnhancedCandidateArray(candidates)
                : buildBasicCandidateArray(candidates);
            variables.put("candidatesJson", objectMapper.writeValueAsString(candidateArray));
        } catch (Exception e) {
            log.warn("Failed to serialize candidates, using empty array", e);
            variables.put("candidatesJson", "[]");
        }
        
        // Advanced template variables
        if (isAdvancedVersion(version)) {
            buildAdvancedTemplateVariables(variables, ctx);
        }
        
        return variables;
    }
    
    private void buildAdvancedTemplateVariables(Map<String, Object> variables, RequestContext ctx) {
        if (ctx == null) return;
        
        UserProfile profile = ctx.getUserProfile();
        if (profile != null) {
            // User profile variables
            variables.put("userProfile", true);
            variables.put("learningPattern", profile.getLearningPattern());
            variables.put("overallMasteryPercent", String.format("%.1f", profile.getOverallMastery() * 100));
            variables.put("averageAccuracyPercent", String.format("%.1f", profile.getAverageAccuracy() * 100));
            variables.put("weakDomains", String.join(", ", profile.getWeakDomains()));
            
            List<String> strongDomainsSorted = new ArrayList<>(profile.getStrongDomains());
            Collections.sort(strongDomainsSorted);
            variables.put("strongDomains", String.join(", ", strongDomainsSorted));
            
            variables.put("difficultyDistribution", profile.getDifficultyPref() != null ? 
                profile.getDifficultyPref().getDistributionSummary() : "balanced");
            variables.put("learningApproach", formatLearningApproach(profile));
            variables.put("dataQualityPercent", String.format("%.1f", profile.getDataQuality() * 100));
            
            // Data quality flags
            variables.put("lowDataQuality", profile.getDataQuality() < 0.5);
        } else {
            // Fallback user insights (would need to implement analyzeUserProfile method)
            variables.put("userInsights", true);
            variables.put("learningPattern", "unknown");
            variables.put("weakTopics", "");
            variables.put("strongTopics", "");
            variables.put("averageAccuracyPercent", "50.0");
            variables.put("difficultyTrend", "balanced");
        }
        
        // Goals and objectives
        if (ctx.getObjective() != null) {
            variables.put("objective", ctx.getObjective().getValue());
            variables.put("objectiveStrategy", getObjectiveStrategy(ctx.getObjective()));
        }
        
        if (ctx.getTargetDomains() != null && !ctx.getTargetDomains().isEmpty()) {
            variables.put("targetDomains", String.join(", ", ctx.getTargetDomains()));
            variables.put("targetDomainsSpecified", true);
        }
        
        if (ctx.getDesiredDifficulty() != null) {
            variables.put("desiredDifficulty", ctx.getDesiredDifficulty().getValue());
            variables.put("desiredDifficultySpecified", true);
        }
        
        if (ctx.getTimeboxMinutes() != null) {
            variables.put("timeboxMinutes", ctx.getTimeboxMinutes());
            variables.put("timeboxSpecified", true);
        }
        
        // Default goal flag
        boolean hasSpecificGoals = ctx.getObjective() != null || 
                                  (ctx.getTargetDomains() != null && !ctx.getTargetDomains().isEmpty()) ||
                                  ctx.getDesiredDifficulty() != null;
        variables.put("defaultGoal", !hasSpecificGoals);
        
        // Strategy flags
        variables.put("profileStrategy", profile != null);
        variables.put("insightStrategy", profile == null);
        
        if (profile != null) {
            variables.put("difficultyTarget", profile.getDifficultyPref() != null ? 
                profile.getDifficultyPref().getPreferredLevel().name() : "BALANCED");
        }
    }
    
    private String getObjectiveStrategy(LearningObjective objective) {
        return switch (objective) {
            case WEAKNESS_FOCUS -> "Primary Focus: Address weak knowledge domains and struggling topics";
            case PROGRESSIVE_DIFFICULTY -> "Primary Focus: Gradually increase difficulty to build confidence and skills";
            case TOPIC_COVERAGE -> "Primary Focus: Explore diverse domains for comprehensive knowledge building";
            case EXAM_PREP -> "Primary Focus: High-frequency and important problems for exam preparation";
            case REFRESH_MASTERED -> "Primary Focus: Review previously learned concepts to maintain retention";
        };
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
        return "BALANCED_GROWTH";
    }
    
    private String substituteVariables(String template, Map<String, Object> variables) {
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            Object value = variables.get(variableName);
            
            String replacement = "";
            if (value != null) {
                if (value instanceof Boolean && (Boolean) value) {
                    // For boolean flags, don't replace with "true", just indicate presence
                    replacement = "";
                } else if (!(value instanceof Boolean && !(Boolean) value)) {
                    // Replace with actual value for non-false booleans
                    replacement = value.toString();
                }
            }
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        // Clean up conditional sections (basic implementation)
        // This would ideally use a proper template engine like Mustache
        return cleanupConditionalSections(result.toString());
    }
    
    private String cleanupConditionalSections(String text) {
        // Remove empty conditional blocks and extra whitespace
        return text
            .replaceAll("(?m)^\\s*-\\s*$", "")  // Remove empty bullet points
            .replaceAll("(?m)^\\s*\\n", "")     // Remove empty lines
            .replaceAll("\\n{3,}", "\n\n")     // Collapse multiple newlines
            .trim();
    }
    
    private List<Map<String, Object>> buildBasicCandidateArray(List<ProblemCandidate> candidates) {
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
                
                // Add FSRS urgency signals
                if (c.retentionProbability != null) {
                    map.put("retentionProbability", Math.round(c.retentionProbability * 100) / 100.0);
                }
                if (c.daysOverdue != null) {
                    map.put("daysOverdue", c.daysOverdue);
                }
                if (c.urgencyScore != null) {
                    map.put("urgencyScore", Math.round(c.urgencyScore * 100) / 100.0);
                    if (c.urgencyScore > 0.7) {
                        map.put("urgencyLevel", "high");
                    } else if (c.urgencyScore > 0.4) {
                        map.put("urgencyLevel", "medium");
                    } else {
                        map.put("urgencyLevel", "low");
                    }
                }
                
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
    
    // Fallback methods for when template loading fails
    private String getFallbackSystemMessage(String version) {
        if ("v3".equals(version)) {
            return "You are an advanced AI-powered algorithm problem recommendation system. Return ONLY valid JSON with schema: {\"items\": [{\"problemId\": number, \"reason\": string, \"confidence\": number, \"strategy\": string, \"score\": number}]}";
        } else if ("v2".equals(version)) {
            return "You are an intelligent algorithm problem recommendation engine. Return ONLY valid JSON with schema: {\"items\": [{\"problemId\": number, \"reason\": string, \"confidence\": number, \"strategy\": string, \"score\": number}]}";
        } else {
            return "You are a recommendation re-ranking engine. Return STRICT JSON only with schema: {\"items\": [{\"problemId\": number, \"reason\": string, \"confidence\": number, \"strategy\": string, \"score\": number}]}";
        }
    }
    
    private String getFallbackUserMessage(RequestContext ctx, List<ProblemCandidate> candidates, 
                                        PromptOptions options, String version) {
        int limit = options != null ? Math.max(1, Math.min(50, options.limit)) : 10;
        String userId = ctx != null && ctx.getUserId() != null ? ctx.getUserId().toString() : "unknown";
        
        try {
            List<Map<String, Object>> candidateArray = buildBasicCandidateArray(candidates);
            String candidatesJson = objectMapper.writeValueAsString(candidateArray);
            
            return String.format("Task: Rank the candidates and return top %d items.\nUser: %s\nCandidates: %s\nReturn only JSON.", 
                               limit, userId, candidatesJson);
        } catch (Exception e) {
            return String.format("Task: Rank and return top %d items. User: %s. Return only JSON.", limit, userId);
        }
    }
}