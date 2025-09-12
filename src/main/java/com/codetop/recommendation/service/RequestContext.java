package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.UserProfile;

public class RequestContext {
    private Long userId;
    private String tier; // FREE | BRONZE | SILVER | GOLD | PLATINUM (fixed for now)
    private String abGroup; // e.g., A/B
    private String route; // api route name
    private String traceId;
    private UserProfile userProfile; // User learning profile for personalized recommendations
    private String userProfileSummary; // Diagnostic summary for headers

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
}

