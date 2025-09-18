package com.codetop.recommendation.controller;

import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.ApiRecommendationResponse;
import com.codetop.recommendation.service.RecommendationStrategyResolver;
import com.codetop.recommendation.service.RecommendationStrategy;
import com.codetop.recommendation.dto.RecommendationRequest;
import com.codetop.recommendation.service.LearningObjective;
import com.codetop.recommendation.service.DifficultyPreference;
import com.codetop.recommendation.service.RecommendationType;
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
import com.codetop.recommendation.service.AsyncRecommendationService;
import com.codetop.recommendation.config.UserProfilingProperties;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/problems")
public class ProblemRecommendationController {

    private final RecommendationStrategyResolver strategyResolver;
    private final RecommendationFeedbackService feedbackService;
    private final UserProfilingProperties userProfilingProperties;
    private final Environment environment;
    private final AsyncRecommendationService asyncRecommendationService;
    
    @Value("${app.security.demo-user-id:999999}")
    private Long demoUserId;

    public ProblemRecommendationController(RecommendationStrategyResolver strategyResolver,
                                           RecommendationFeedbackService feedbackService,
                                           UserProfilingProperties userProfilingProperties,
                                           Environment environment,
                                           AsyncRecommendationService asyncRecommendationService) {
        this.strategyResolver = strategyResolver;
        this.feedbackService = feedbackService;
        this.userProfilingProperties = userProfilingProperties;
        this.environment = environment;
        this.asyncRecommendationService = asyncRecommendationService;
    }

    @GetMapping("/ai-recommendations")
    public ResponseEntity<AIRecommendationResponse> getAiRecommendations(
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(value = "objective", required = false) String objective,
            @RequestParam(value = "domains", required = false) String[] domains,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "timebox", required = false) Integer timebox,
            @RequestParam(value = "recommendation_type", required = false) String recommendationType,
            @RequestParam(value = "ab_group", required = false) String abGroup,
            @RequestParam(value = "forceRefresh", required = false, defaultValue = "false") Boolean forceRefresh
    ) {
        // Security: Handle authentication with dev environment support
        Long uid = getUserId();
        
        // Simplified parameter handling - no aliases for MVP
        
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
        
        // Validate ab_group parameter using whitelist - don't trust frontend completely
        String validatedAbGroup = validateAbGroup(abGroup);
        if (validatedAbGroup != null && !validatedAbGroup.equals(abGroup)) {
            // Log potential abuse attempt
            org.slf4j.LoggerFactory.getLogger(ProblemRecommendationController.class)
                    .warn("Invalid ab_group sanitized - userId={}, original={}, validated={}", 
                            uid, abGroup, validatedAbGroup);
        }
        
        // ForceRefresh anti-abuse protection - only allow for specific users or in dev environment
        boolean allowedForceRefresh = false;
        if (forceRefresh != null && forceRefresh) {
            // Check if running in development environment
            String profile = System.getProperty("spring.profiles.active", "");
            boolean isDev = profile.contains("dev") || profile.contains("test");
            
            // Allow force refresh in dev environment or for specific A/B groups
            if (isDev) {
                allowedForceRefresh = true;
            } else if ("experimental".equals(validatedAbGroup) || "debug".equals(validatedAbGroup)) {
                // Allow for experimental/debug groups in production
                allowedForceRefresh = true;
            } else {
                // Log potential abuse attempt
                org.slf4j.LoggerFactory.getLogger(ProblemRecommendationController.class)
                        .warn("ForceRefresh requested by non-privileged user - userId={}, abGroup={}, profile={}", 
                                uid, validatedAbGroup, profile);
            }
            
            if (!allowedForceRefresh) {
                forceRefresh = false; // Override to false to prevent abuse
            }
        }
        
        // Parse and validate difficulty preference
        DifficultyPreference parsedDifficultyPreference = null;
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            try {
                parsedDifficultyPreference = DifficultyPreference.fromString(difficulty.trim());
            } catch (Exception e) {
                // Invalid difficulty is ignored, falls back to null
                parsedDifficultyPreference = null;
            }
        }

        // Parse and validate recommendation type
        RecommendationType recType = RecommendationType.getDefault(); // Default to HYBRID
        if (recommendationType != null && !recommendationType.trim().isEmpty()) {
            try {
                recType = RecommendationType.fromString(recommendationType.trim());
            } catch (Exception e) {
                // Invalid recommendation type falls back to default (HYBRID)
                recType = RecommendationType.getDefault();
            }
        }

        // Use strategy resolver to select and execute the appropriate strategy
        RecommendationStrategy strategy = strategyResolver.resolveStrategy(recType, uid, learningObjective);
        
        // Build recommendation request with enhanced parameters
        RecommendationRequest request = RecommendationRequest.builder()
            .userId(uid)
            .limit(safeLimit)
            .objective(learningObjective)
            .domains(targetDomains)
            .difficultyPreference(parsedDifficultyPreference)
            .timebox(safeTimebox)
            .requestedType(recType)
            .abGroup(validatedAbGroup)
            .forceRefresh(forceRefresh != null ? forceRefresh : false)
            .build();
        
        // Execute strategy
        AIRecommendationResponse body = strategy.getRecommendations(request);

        HttpHeaders headers = new HttpHeaders();
        
        // Ensure meta object exists for consistent header handling
        if (body.getMeta() == null) {
            body.setMeta(new AIRecommendationResponse.Meta());
        }
        
        // Always add essential headers with fallback values
        headers.add("X-Trace-Id", body.getMeta().getTraceId() != null ? body.getMeta().getTraceId() : java.util.UUID.randomUUID().toString());
        headers.add("X-Cache-Hit", String.valueOf(body.getMeta().isCached()));
            
        // Set consistent recommendation source for frontend badges
        String recSource;
        if (body.getMeta().isBusy()) {
            recSource = "DEFAULT";
        } else if ("fsrs_fallback".equalsIgnoreCase(body.getMeta().getStrategy())) {
            recSource = "FSRS";
        } else if (body.getMeta().getFinalProvider() != null) {
            // Use finalProvider from chain execution metadata for accuracy
            recSource = body.getMeta().getFinalProvider().toUpperCase();
        } else if (body.getItems() != null && !body.getItems().isEmpty()) {
            String src = body.getItems().get(0).getSource();
            recSource = src != null ? src.toUpperCase() : strategy.getType().name();
        } else {
            recSource = strategy.getType().name();
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
        
        // Always add recommendation type headers (essential for frontend)
        headers.add("X-Recommendation-Type", recType.getValue());
        headers.add("X-Strategy-Used", strategy.getType().getValue());
        body.getMeta().setRecommendationType(recType.getValue());

        // Return AIRecommendationResponse directly to match frontend expectations
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @PostMapping("/ai-recommendations/async")
    public ResponseEntity<AIRecommendationResponse.AsyncTaskResponse> startAsyncRecommendation(
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(value = "objective", required = false) String objective,
            @RequestParam(value = "domains", required = false) String[] domains,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "timebox", required = false) Integer timebox,
            @RequestParam(value = "recommendation_type", required = false) String recommendationType,
            @RequestParam(value = "ab_group", required = false) String abGroup
    ) {
        // 获取用户ID
        Long uid = getUserId();
        
        // 参数验证和处理 (与同步接口相同)
        int safeLimit = Math.max(1, Math.min(50, limit != null ? limit : 10));
        
        Integer safeTimebox = null;
        if (timebox != null) {
            safeTimebox = Math.max(5, Math.min(240, timebox));
        }

        LearningObjective learningObjective = null;
        if (objective != null && !objective.trim().isEmpty()) {
            try {
                learningObjective = LearningObjective.fromString(objective.trim());
            } catch (Exception e) {
                learningObjective = null;
            }
        }
        
        List<String> targetDomains = null;
        if (domains != null && domains.length > 0) {
            Set<String> validDomains = new HashSet<>(userProfilingProperties.getTagDomainMapping().values());
            targetDomains = Arrays.stream(domains)
                .filter(domain -> domain != null && !domain.trim().isEmpty())
                .map(domain -> domain.trim().toLowerCase())
                .filter(validDomains::contains)
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
            
            if (targetDomains.isEmpty()) {
                targetDomains = null;
            }
        }
        
        String validatedAbGroup = validateAbGroup(abGroup);
        
        DifficultyPreference parsedDifficultyPreference = null;
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            try {
                parsedDifficultyPreference = DifficultyPreference.fromString(difficulty.trim());
            } catch (Exception e) {
                parsedDifficultyPreference = null;
            }
        }

        RecommendationType recType = RecommendationType.getDefault();
        if (recommendationType != null && !recommendationType.trim().isEmpty()) {
            try {
                recType = RecommendationType.fromString(recommendationType.trim());
            } catch (Exception e) {
                recType = RecommendationType.getDefault();
            }
        }

        // 构建推荐请求
        RecommendationRequest request = RecommendationRequest.builder()
            .userId(uid)
            .limit(safeLimit)
            .objective(learningObjective)
            .domains(targetDomains)
            .difficultyPreference(parsedDifficultyPreference)
            .timebox(safeTimebox)
            .requestedType(recType)
            .abGroup(validatedAbGroup)
            .forceRefresh(false) // 异步推荐不支持强制刷新
            .build();
        
        // 启动异步任务
        String taskId = asyncRecommendationService.startAsyncRecommendation(request);
        
        // 获取任务状态
        AIRecommendationResponse.AsyncTaskStatus taskStatus = asyncRecommendationService.getTaskStatus(taskId);
        
        // 构建响应
        AIRecommendationResponse.AsyncTaskResponse response = AIRecommendationResponse.AsyncTaskResponse.builder()
            .taskId(taskId)
            .status(taskStatus.getStatus())
            .message(taskStatus.getMessage())
            .userId(uid)
            .createdAt(taskStatus.getCreatedAt())
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Task-Id", taskId);
        headers.add("X-User-Id", uid.toString());
        headers.add("Location", "/api/v1/problems/ai-recommendations/async/" + taskId);
        
        return ResponseEntity.accepted().headers(headers).body(response);
    }

    // Support taskIds containing dots or special characters by using a greedy regex
    @GetMapping("/ai-recommendations/async/{taskId:.+}")
    public ResponseEntity<AIRecommendationResponse.AsyncTaskStatus> getAsyncRecommendationStatus(
            @PathVariable("taskId") String taskId
    ) {
        // 获取用户ID
        Long uid = getUserId();
        
        // 获取任务状态
        AIRecommendationResponse.AsyncTaskStatus status = asyncRecommendationService.getTaskStatus(taskId);
        
        if ("NOT_FOUND".equals(status.getStatus())) {
            return ResponseEntity.notFound().build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Task-Id", taskId);
        headers.add("X-User-Id", uid.toString());
        
        // 如果任务完成，添加结果相关的头部信息
        if ("COMPLETED".equals(status.getStatus()) && status.getResult() != null) {
            AIRecommendationResponse result = status.getResult();
            if (result.getMeta() != null) {
                headers.add("X-Cache-Hit", String.valueOf(result.getMeta().isCached()));
                headers.add("X-Trace-Id", result.getMeta().getTraceId() != null ? 
                    result.getMeta().getTraceId() : java.util.UUID.randomUUID().toString());
                if (result.getMeta().getFinalProvider() != null) {
                    headers.add("X-Rec-Source", result.getMeta().getFinalProvider().toUpperCase());
                }
            }
        }
        
        return ResponseEntity.ok().headers(headers).body(status);
    }

    @GetMapping("/ai-recommendations/daily-limit")
    public ResponseEntity<java.util.Map<String, Object>> checkDailyLimit() {
        Long uid = getUserId();
        boolean canGenerate = asyncRecommendationService.checkDailyLimit(uid);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("userId", uid);
        response.put("canGenerate", canGenerate);
        response.put("dailyLimit", 1);
        response.put("message", canGenerate ? 
            "You can generate AI recommendations today" : 
            "Daily limit reached. You can generate 1 AI recommendation per day");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/recommendation-feedback")
    public ResponseEntity<java.util.Map<String, Object>> submitFeedback(
            @PathVariable("id") Long problemId,
            @Valid @org.springframework.web.bind.annotation.RequestBody RecommendationFeedbackRequest request
    ) {
        // Get authenticated user ID from security context
        Long authenticatedUserId = UserContext.getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new IllegalArgumentException("User not authenticated");
        }
        
        // Security: Validate that request userId matches authenticated user
        if (request.getUserId() != null && !authenticatedUserId.equals(request.getUserId())) {
            throw new SecurityException("User ID mismatch - cannot submit feedback for another user");
        }
        
        // Override request userId with authenticated user ID for security
        request.setUserId(authenticatedUserId);
        
        feedbackService.submit(problemId, request);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("status", "ok");
        resp.put("recordedAt", java.time.Instant.now().toString());
        return ResponseEntity.ok(resp);
    }

    /**
     * Validate and sanitize ab_group parameter using whitelist
     * Security: Don't trust frontend parameters completely
     */
    private String validateAbGroup(String abGroup) {
        if (abGroup == null || abGroup.trim().isEmpty()) {
            return null;
        }
        
        // Whitelist of allowed ab_group values
        Set<String> allowedGroups = Set.of("A", "B", "experimental", "debug", "control");
        
        String sanitized = abGroup.trim().toLowerCase();
        
        // Check if it matches allowed groups (case-insensitive)
        for (String allowed : allowedGroups) {
            if (allowed.equalsIgnoreCase(sanitized)) {
                return allowed; // Return the canonical form
            }
        }
        
        // Invalid ab_group - return null and let caller handle logging
        return null;
    }

    /**
     * Get current user ID with dev environment anonymous access support
     * Security: Handle anonymous access gracefully in development environment
     */
    private Long getUserId() {
        Long uid = UserContext.getCurrentUserId();
        
        // In production, authentication is always required
        if (uid != null) {
            return uid;
        }
        
        // Check if we're in development environment and allow anonymous access
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDev = false;
        for (String profile : activeProfiles) {
            if ("dev".equals(profile) || "test".equals(profile)) {
                isDev = true;
                break;
            }
        }
        
        if (isDev) {
            // Log anonymous access in dev environment
            org.slf4j.LoggerFactory.getLogger(ProblemRecommendationController.class)
                    .info("Anonymous access allowed in dev environment, using demo user ID: {}", demoUserId);
            return demoUserId;
        }
        
        // Production or non-dev environment requires authentication
        throw new IllegalArgumentException("User not authenticated");
    }
}
