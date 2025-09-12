package com.codetop.recommendation.controller;

import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.service.AIRecommendationService;
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

import com.codetop.recommendation.dto.RecommendationFeedbackRequest;
import com.codetop.recommendation.service.RecommendationFeedbackService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/problems")
public class ProblemRecommendationController {

    private final AIRecommendationService aiRecommendationService;
    private final RecommendationFeedbackService feedbackService;

    public ProblemRecommendationController(AIRecommendationService aiRecommendationService,
                                           RecommendationFeedbackService feedbackService) {
        this.aiRecommendationService = aiRecommendationService;
        this.feedbackService = feedbackService;
    }

    @GetMapping("/ai-recommendations")
    public ResponseEntity<AIRecommendationResponse> getAiRecommendations(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit
    ) {
        Long uid = userId != null ? userId : (UserContext.getCurrentUserId() != null ? UserContext.getCurrentUserId() : 1L);

        AIRecommendationResponse body = aiRecommendationService.getRecommendations(uid, limit != null ? limit : 10);

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
