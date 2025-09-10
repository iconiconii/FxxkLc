package com.codetop.recommendation.service;

import com.codetop.recommendation.chain.ProviderChain;
import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.alg.CandidateBuilder;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.service.CacheKeyBuilder;
import com.codetop.service.cache.CacheService;
import com.codetop.util.CacheHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AIRecommendationService {
    private final ProviderChain providerChain;
    private final CandidateBuilder candidateBuilder;
    private final CacheHelper cacheHelper;
    private final CacheService cacheService;

    @org.springframework.beans.factory.annotation.Autowired
    public AIRecommendationService(ProviderChain providerChain, CandidateBuilder candidateBuilder,
                                   CacheHelper cacheHelper, CacheService cacheService) {
        this.providerChain = providerChain;
        this.candidateBuilder = candidateBuilder;
        this.cacheHelper = cacheHelper;
        this.cacheService = cacheService;
    }

    // Backward-compatible constructor for existing tests
    public AIRecommendationService(ProviderChain providerChain) {
        this.providerChain = providerChain;
        this.candidateBuilder = null;
        this.cacheHelper = null;
        this.cacheService = null;
    }

    public AIRecommendationResponse getRecommendations(Long userId, int limit) {
        // Build request context using fixed tier and abGroup for now
        RequestContext ctx = new RequestContext();
        ctx.setUserId(userId);
        ctx.setTier("BRONZE"); // fixed value until user tier feature exists
        ctx.setAbGroup("default");
        ctx.setRoute("ai-recommendations");
        ctx.setTraceId(UUID.randomUUID().toString());

        // Build cache key and attempt cached retrieval (TTL 1h)
        String cacheKey = CacheKeyBuilder.buildUserKey("rec-ai", userId, "limit_" + limit);
        List<RecommendationItemDTO> items = null;
        boolean cacheHit = false;
        if (cacheService != null) {
            items = cacheService.getList(cacheKey, RecommendationItemDTO.class);
            cacheHit = (items != null && !items.isEmpty());
        }
        if (items == null || items.isEmpty()) {
            items = computeItems(ctx, userId, limit);
            if (cacheService != null && items != null && !items.isEmpty()) {
                cacheService.put(cacheKey, items, java.time.Duration.ofHours(1));
            }
        }

        AIRecommendationResponse out = new AIRecommendationResponse();
        AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
        meta.setTraceId(ctx.getTraceId());
        meta.setGeneratedAt(Instant.now());
        meta.setCached(cacheHit);
        if (items != null && !items.isEmpty()) {
            boolean isFsrs = "FSRS".equalsIgnoreCase(items.get(0).getSource());
            meta.setBusy(false);
            meta.setStrategy(isFsrs ? "fsrs_fallback" : "normal");
        } else {
            meta.setBusy(true);
            meta.setStrategy("busy_message");
        }
        out.setItems(items != null ? items : new ArrayList<>());
        out.setMeta(meta);
        return out;
    }

    private List<RecommendationItemDTO> buildFsrsFallback(List<LlmProvider.ProblemCandidate> candidates, int limit) {
        // Sort by lowest accuracy first (needs attention), then by fewer attempts
        candidates.sort(java.util.Comparator
                .comparing((LlmProvider.ProblemCandidate c) -> c.recentAccuracy == null ? 1.0 : c.recentAccuracy)
                .thenComparing(c -> c.attempts == null ? 0 : c.attempts));

        List<RecommendationItemDTO> list = new ArrayList<>();
        int k = Math.max(1, Math.min(50, limit));
        for (int i = 0; i < Math.min(k, candidates.size()); i++) {
            LlmProvider.ProblemCandidate c = candidates.get(i);
            RecommendationItemDTO dto = new RecommendationItemDTO();
            dto.setProblemId(c.id);
            double acc = c.recentAccuracy != null ? c.recentAccuracy : 0.0;
            String reason = String.format("FSRS fallback: prioritize lower accuracy (%.0f%%) and due status", acc * 100);
            dto.setReason(reason);
            dto.setConfidence(Math.max(0.0, Math.min(1.0, 1.0 - acc))); // inverse of accuracy as need score
            dto.setScore(1.0 - acc);
            dto.setStrategy("fsrs");
            dto.setSource("FSRS");
            dto.setModel("local");
            dto.setPromptVersion("v1");
            list.add(dto);
        }
        return list;
    }

    // Minimal mock for legacy tests or when CandidateBuilder is unavailable
    private List<LlmProvider.ProblemCandidate> buildMockCandidates() {
        List<LlmProvider.ProblemCandidate> list = new ArrayList<>();
        list.add(candidate(1L, "数组", "EASY", java.util.List.of("数组", "哈希表"), 0.85, 2));
        list.add(candidate(2L, "字符串", "MEDIUM", java.util.List.of("滑动窗口", "哈希表"), 0.60, 3));
        list.add(candidate(3L, "设计", "MEDIUM", java.util.List.of("链表", "缓存"), 0.55, 4));
        list.add(candidate(4L, "链表", "EASY", java.util.List.of("递归"), 0.75, 1));
        list.add(candidate(5L, "数组", "MEDIUM", java.util.List.of("分治", "堆"), 0.65, 2));
        return list;
    }

    private LlmProvider.ProblemCandidate candidate(Long id, String topic, String difficulty, List<String> tags, Double recentAccuracy, Integer attempts) {
        LlmProvider.ProblemCandidate c = new LlmProvider.ProblemCandidate();
        c.id = id;
        c.topic = topic;
        c.difficulty = difficulty;
        c.tags = tags;
        c.recentAccuracy = recentAccuracy;
        c.attempts = attempts;
        return c;
    }

    private List<RecommendationItemDTO> computeItems(RequestContext ctx, Long userId, int limit) {
        int candidateCap = Math.min(50, Math.max(10, limit * 3));
        List<LlmProvider.ProblemCandidate> candidates = candidateBuilder != null
                ? candidateBuilder.buildForUser(userId, candidateCap)
                : buildMockCandidates();

        LlmProvider.PromptOptions options = new LlmProvider.PromptOptions();
        options.limit = Math.max(1, Math.min(50, limit));

        ProviderChain.Result res = providerChain.execute(ctx, candidates, options);

        List<RecommendationItemDTO> items = new ArrayList<>();
        if (res.success && res.result != null && res.result.items != null && !res.result.items.isEmpty()) {
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
        } else {
            List<LlmProvider.ProblemCandidate> base = (candidates != null && !candidates.isEmpty())
                    ? candidates
                    : buildMockCandidates();
            items.addAll(buildFsrsFallback(base, limit));
        }
        return items;
    }
}
