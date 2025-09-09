package com.codetop.recommendation.service;

import com.codetop.recommendation.chain.ProviderChain;
import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.provider.LlmProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AIRecommendationService {
    private final ProviderChain providerChain;

    public AIRecommendationService(ProviderChain providerChain) {
        this.providerChain = providerChain;
    }

    public AIRecommendationResponse getRecommendations(Long userId, int limit) {
        // Build request context using fixed tier and abGroup for now
        RequestContext ctx = new RequestContext();
        ctx.setUserId(userId);
        ctx.setTier("BRONZE"); // fixed value until user tier feature exists
        ctx.setAbGroup("default");
        ctx.setRoute("ai-recommendations");
        ctx.setTraceId(UUID.randomUUID().toString());

        // Candidates: placeholder empty list (integration with FSRS/candidates later)
        List<LlmProvider.ProblemCandidate> candidates = new ArrayList<>();

        LlmProvider.PromptOptions options = new LlmProvider.PromptOptions();
        options.limit = Math.max(1, Math.min(50, limit));

        ProviderChain.Result res = providerChain.execute(ctx, candidates, options);

        AIRecommendationResponse out = new AIRecommendationResponse();
        AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
        meta.setTraceId(ctx.getTraceId());
        meta.setGeneratedAt(Instant.now());
        meta.setCached(false);

        List<RecommendationItemDTO> items = new ArrayList<>();
        if (res.success && res.result != null && res.result.items != null) {
            for (LlmProvider.RankedItem r : res.result.items) {
                RecommendationItemDTO dto = new RecommendationItemDTO();
                dto.setProblemId(r.problemId);
                dto.setReason(r.reason);
                dto.setConfidence(r.confidence);
                dto.setScore(r.score);
                dto.setStrategy(r.strategy);
                dto.setSource("LLM");
                dto.setModel(res.result.model);
                dto.setPromptVersion("v1");
                items.add(dto);
            }
            meta.setBusy(false);
            meta.setStrategy("normal");
        } else {
            // default fallback (busy or fsrs_fallback) — no items here yet
            meta.setBusy(true);
            meta.setStrategy(res.defaultReason != null ? res.defaultReason : "busy_message");
        }

        out.setItems(items);
        out.setMeta(meta);
        return out;
    }
}

