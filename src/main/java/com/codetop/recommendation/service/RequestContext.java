package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.UserProfile;
import java.util.List;

public class RequestContext {
    private Long userId;
    private String tier; // FREE | BRONZE | SILVER | GOLD | PLATINUM (fixed for now)
    private String abGroup; // e.g., A/B
    private String route; // api route name
    private String traceId;
    private UserProfile userProfile; // User learning profile for personalized recommendations
    private String userProfileSummary; // Diagnostic summary for headers
    
    // New fields for intelligent recommendation engine
    private LearningObjective objective; // Learning objective (weakness_focus, progressive_difficulty, etc.)
    private List<String> targetDomains; // Target knowledge domains (arrays, graphs, dynamic programming, etc.)
    private DifficultyPreference desiredDifficulty; // Desired difficulty level (EASY, MEDIUM, HARD)
    private Integer timeboxMinutes; // Optional time constraint for practice session
    private String promptVersion; // Prompt template version for A/B testing
    
    // Cache control
    private boolean forceRefresh; // Skip cache and force fresh generation
    
    // Chain execution metadata for observability
    private String chainId; // Selected chain ID
    private List<String> chainHops; // List of provider hops in execution
    private String fallbackReason; // Reason for fallback if applicable

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
    public String getAbGroup() { return abGroup; }
    public void setAbGroup(String abGroup) { this.abGroup = abGroup; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public UserProfile getUserProfile() { return userProfile; }
    public void setUserProfile(UserProfile userProfile) { this.userProfile = userProfile; }
    public String getUserProfileSummary() { return userProfileSummary; }
    public void setUserProfileSummary(String userProfileSummary) { this.userProfileSummary = userProfileSummary; }
    
    // New getters and setters for intelligent recommendation engine
    public LearningObjective getObjective() { return objective; }
    public void setObjective(LearningObjective objective) { this.objective = objective; }
    public List<String> getTargetDomains() { return targetDomains; }
    public void setTargetDomains(List<String> targetDomains) { this.targetDomains = targetDomains; }
    public DifficultyPreference getDesiredDifficulty() { return desiredDifficulty; }
    public void setDesiredDifficulty(DifficultyPreference desiredDifficulty) { this.desiredDifficulty = desiredDifficulty; }
    public Integer getTimeboxMinutes() { return timeboxMinutes; }
    public void setTimeboxMinutes(Integer timeboxMinutes) { this.timeboxMinutes = timeboxMinutes; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    
    // Chain metadata getters and setters
    public String getChainId() { return chainId; }
    public void setChainId(String chainId) { this.chainId = chainId; }
    public List<String> getChainHops() { return chainHops; }
    public void setChainHops(List<String> chainHops) { this.chainHops = chainHops; }
    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }
    
    // Cache control getters and setters
    public boolean isForceRefresh() { return forceRefresh; }
    public void setForceRefresh(boolean forceRefresh) { this.forceRefresh = forceRefresh; }
}

