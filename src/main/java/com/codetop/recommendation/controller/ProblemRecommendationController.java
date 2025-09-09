package com.codetop.recommendation.controller;

import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.service.AIRecommendationService;
import com.codetop.util.UserContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/problems")
public class ProblemRecommendationController {

    private final AIRecommendationService aiRecommendationService;

    public ProblemRecommendationController(AIRecommendationService aiRecommendationService) {
        this.aiRecommendationService = aiRecommendationService;
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
            String recSource = body.getMeta().isBusy() ? "DEFAULT" : "LLM"; // simple heuristic for now
            headers.add("X-Rec-Source", recSource);
            List<String> hops = body.getMeta().getChainHops();
            if (hops != null && !hops.isEmpty()) {
                headers.add("X-Provider-Chain", String.join(">", hops));
            }
            if (body.getMeta().getChainId() != null) headers.add("X-Chain-Id", body.getMeta().getChainId());
            if (body.getMeta().getChainVersion() != null) headers.add("X-Chain-Version", body.getMeta().getChainVersion());
            if (body.getMeta().getPolicyId() != null) headers.add("X-Policy-Id", body.getMeta().getPolicyId());
        }

        return ResponseEntity.ok().headers(headers).body(body);
    }
}

