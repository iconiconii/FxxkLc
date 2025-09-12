package com.codetop.recommendation.controller;

import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.service.UserProfilingService;
import com.codetop.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for user profile analytics.
 * Provides read-only access to user learning profiles for:
 * - Frontend dashboard display
 * - Operational troubleshooting
 * - Analytics and reporting
 */
@RestController
@RequestMapping("/analytics/user-profile")
public class UserProfileAnalyticsController {
    
    private final UserProfilingService userProfilingService;
    
    @Autowired
    public UserProfileAnalyticsController(UserProfilingService userProfilingService) {
        this.userProfilingService = userProfilingService;
    }
    
    /**
     * Get user profile for the current authenticated user.
     * Uses authentication context to determine user ID.
     */
    @GetMapping
    public ResponseEntity<UserProfile> getCurrentUserProfile() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        UserProfile profile = userProfilingService.getUserProfile(currentUserId, true);
        return ResponseEntity.ok(profile);
    }
    
    /**
     * Get user profile for a specific user ID.
     * Intended for admin/operations use.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> getUserProfile(
            @PathVariable Long userId,
            @RequestParam(value = "useCache", defaultValue = "true") boolean useCache) {
        
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        UserProfile profile = userProfilingService.getUserProfile(userId, useCache);
        return ResponseEntity.ok(profile);
    }
    
    /**
     * Get user profile summary with key metrics only.
     * Lightweight version for dashboard widgets.
     */
    @GetMapping("/{userId}/summary")
    public ResponseEntity<Map<String, Object>> getUserProfileSummary(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        UserProfile profile = userProfilingService.getUserProfile(userId, true);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", profile.getUserId());
        summary.put("generatedAt", profile.getGeneratedAt());
        summary.put("window", profile.getWindow());
        summary.put("overallMastery", Math.round(profile.getOverallMastery() * 1000.0) / 10.0); // Round to 1 decimal
        summary.put("averageAccuracy", Math.round(profile.getAverageAccuracy() * 1000.0) / 10.0);
        summary.put("learningPattern", profile.getLearningPattern());
        summary.put("totalProblemsReviewed", profile.getTotalProblemsReviewed());
        summary.put("totalReviewAttempts", profile.getTotalReviewAttempts());
        
        // Add top 3 strongest and weakest domains
        summary.put("strongDomains", profile.getStrongDomains().stream().limit(3).toList());
        summary.put("weakDomains", profile.getWeakDomains().stream().limit(3).toList());
        
        // Add difficulty preferences
        Map<String, Object> difficultyPref = new HashMap<>();
        if (profile.getDifficultyPref() != null) {
            difficultyPref.put("easy", Math.round(profile.getDifficultyPref().getEasy() * 1000.0) / 10.0);
            difficultyPref.put("medium", Math.round(profile.getDifficultyPref().getMedium() * 1000.0) / 10.0);
            difficultyPref.put("hard", Math.round(profile.getDifficultyPref().getHard() * 1000.0) / 10.0);
            difficultyPref.put("trend", profile.getDifficultyPref().getTrend());
            difficultyPref.put("preferredLevel", profile.getDifficultyPref().getPreferredLevel());
        }
        summary.put("difficultyPreference", difficultyPref);
        
        // Add data quality indicator
        summary.put("dataQuality", Math.round(profile.getDataQuality() * 1000.0) / 10.0);
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get domain skills breakdown for a specific user.
     * Detailed view for analytics and skill assessment.
     */
    @GetMapping("/{userId}/domains")
    public ResponseEntity<Map<String, Object>> getDomainSkillsBreakdown(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        UserProfile profile = userProfilingService.getUserProfile(userId, true);
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("domainSkills", profile.getDomainSkills());
        response.put("domainCount", profile.getDomainSkills().size());
        response.put("strongDomains", profile.getStrongDomains());
        response.put("weakDomains", profile.getWeakDomains());
        response.put("overallMastery", profile.getOverallMastery());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Invalidate user profile cache.
     * For operational use when profile data needs to be refreshed.
     */
    @DeleteMapping("/{userId}/cache")
    public ResponseEntity<Map<String, String>> invalidateProfileCache(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        userProfilingService.invalidateUserProfileCache(userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Profile cache invalidated for user " + userId);
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get tag affinity data for a specific user.
     * Shows which problem tags the user has most experience with.
     */
    @GetMapping("/{userId}/tag-affinity")
    public ResponseEntity<Map<String, Object>> getTagAffinity(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        UserProfile profile = userProfilingService.getUserProfile(userId, true);
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("tagAffinity", profile.getTagAffinity());
        response.put("totalTags", profile.getTagAffinity().size());
        
        // Find top 10 most familiar tags
        var topTags = profile.getTagAffinity().entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                ));
        response.put("topTags", topTags);
        
        return ResponseEntity.ok(response);
    }
}