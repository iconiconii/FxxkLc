package com.codetop.recommendation.controller;

import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.service.AIRecommendationService;
import com.codetop.recommendation.service.LearningObjective;
import com.codetop.recommendation.service.DifficultyPreference;
import com.codetop.util.UserContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

import com.codetop.recommendation.dto.RecommendationFeedbackRequest;
import com.codetop.recommendation.service.RecommendationFeedbackService;
import com.codetop.recommendation.config.UserProfilingProperties;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/problems")
public class ProblemRecommendationController {

    private final AIRecommendationService aiRecommendationService;
    private final RecommendationFeedbackService feedbackService;
    private final UserProfilingProperties userProfilingProperties;

    public ProblemRecommendationController(AIRecommendationService aiRecommendationService,
                                           RecommendationFeedbackService feedbackService,
                                           UserProfilingProperties userProfilingProperties) {
        this.aiRecommendationService = aiRecommendationService;
        this.feedbackService = feedbackService;
        this.userProfilingProperties = userProfilingProperties;
    }

    @GetMapping("/ai-recommendations")
    public ResponseEntity<AIRecommendationResponse> getAiRecommendations(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(value = "objective", required = false) String objective,
            @RequestParam(value = "domains", required = false) String[] domains,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "timebox", required = false) Integer timebox
    ) {
        // Input validation and sanitization
        Long uid = userId != null ? userId : (UserContext.getCurrentUserId() != null ? UserContext.getCurrentUserId() : 1L);
        
        // Clamp limit to safe range [1, 50]
        int safeLimit = Math.max(1, Math.min(50, limit != null ? limit : 10));
        
        // Clamp timebox to safe range [5, 240] minutes (5 min - 4 hours)
        Integer safeTimebox = null;
        if (timebox != null) {
            safeTimebox = Math.max(5, Math.min(240, timebox));
        }

        // Parse and validate learning objective
        LearningObjective learningObjective = null;
        if (objective != null && !objective.trim().isEmpty()) {
            try {
                learningObjective = LearningObjective.fromString(objective.trim());
            } catch (Exception e) {
                // Invalid objective is ignored, falls back to null
                learningObjective = null;
            }
        }
        
        // Sanitize and validate target domains using config-based whitelist
        List<String> targetDomains = null;
        if (domains != null && domains.length > 0) {
            // Get valid domains from configuration (tag-domain mapping values)
            Set<String> validDomains = new HashSet<>(userProfilingProperties.getTagDomainMapping().values());
            
            targetDomains = Arrays.stream(domains)
                .filter(domain -> domain != null && !domain.trim().isEmpty())
                .map(domain -> domain.trim().toLowerCase()) // Normalize case
                .filter(validDomains::contains) // Security: config-based whitelist validation
                .distinct()
                .limit(10) // Limit to prevent abuse
                .collect(Collectors.toList());
            
            // If no valid domains after filtering, set to null
            if (targetDomains.isEmpty()) {
                targetDomains = null;
            }
        }
        
        // Parse and validate difficulty preference
        DifficultyPreference difficultyPreference = null;
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            try {
                difficultyPreference = DifficultyPreference.fromString(difficulty.trim());
            } catch (Exception e) {
                // Invalid difficulty is ignored, falls back to null
                difficultyPreference = null;
            }
        }

        AIRecommendationResponse body = aiRecommendationService.getRecommendations(
            uid, 
            safeLimit,
            learningObjective,
            targetDomains,
            difficultyPreference,
            safeTimebox
        );

        HttpHeaders headers = new HttpHeaders();
        if (body.getMeta() != null) {
            if (body.getMeta().getTraceId() != null) headers.add("X-Trace-Id", body.getMeta().getTraceId());
            headers.add("X-Cache-Hit", String.valueOf(body.getMeta().isCached()));
            String recSource;
            if (body.getMeta().isBusy()) {
                recSource = "DEFAULT";
            } else if ("fsrs_fallback".equalsIgnoreCase(body.getMeta().getStrategy())) {
                recSource = "FSRS";
            } else if (body.getItems() != null && !body.getItems().isEmpty()) {
                String src = body.getItems().get(0).getSource();
                recSource = src != null ? src : "LLM";
            } else {
                recSource = "LLM";
            }
            headers.add("X-Rec-Source", recSource);
            List<String> hops = body.getMeta().getChainHops();
            if (hops != null && !hops.isEmpty()) {
                headers.add("X-Provider-Chain", String.join(">", hops));
            }
            if (body.getMeta().getFallbackReason() != null) {
                headers.add("X-Fallback-Reason", body.getMeta().getFallbackReason());
            }
            if (body.getMeta().getChainId() != null) headers.add("X-Chain-Id", body.getMeta().getChainId());
            if (body.getMeta().getChainVersion() != null) headers.add("X-Chain-Version", body.getMeta().getChainVersion());
            if (body.getMeta().getPolicyId() != null) headers.add("X-Policy-Id", body.getMeta().getPolicyId());
            if (body.getMeta().getUserProfileSummary() != null) headers.add("X-User-Profile", body.getMeta().getUserProfileSummary());
        }

        return ResponseEntity.ok().headers(headers).body(body);
    }

    @PostMapping("/{id}/recommendation-feedback")
    public ResponseEntity<java.util.Map<String, Object>> submitFeedback(
            @PathVariable("id") Long problemId,
            @Valid @org.springframework.web.bind.annotation.RequestBody RecommendationFeedbackRequest request
    ) {
        feedbackService.submit(problemId, request);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("status", "ok");
        resp.put("recordedAt", java.time.Instant.now().toString());
        return ResponseEntity.ok(resp);
    }
}
