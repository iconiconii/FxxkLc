package com.codetop.recommendation.service;

import com.codetop.recommendation.chain.ProviderChain;
import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.alg.CandidateBuilder;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.service.CacheKeyBuilder;
import com.codetop.service.cache.CacheService;
import com.codetop.util.CacheHelper;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class AIRecommendationService {
    private static final Logger log = LoggerFactory.getLogger(AIRecommendationService.class);
    
    private final ProviderChain providerChain;
    private final CandidateBuilder candidateBuilder;
    private final CacheHelper cacheHelper;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final PromptTemplateService promptTemplateService;
    private final LlmProperties llmProperties;
    private final ChainSelector chainSelector;
    private final LlmToggleService llmToggleService;
    
    // Async rate limiting with semaphores (configured via properties)
    private final Semaphore globalAsyncSemaphore;
    private final Semaphore perUserAsyncSemaphore;

    @org.springframework.beans.factory.annotation.Autowired
public AIRecommendationService(ProviderChain providerChain, CandidateBuilder candidateBuilder,
                               CacheHelper cacheHelper, CacheService cacheService,
                               ObjectMapper objectMapper, PromptTemplateService promptTemplateService,
                               LlmProperties llmProperties, ChainSelector chainSelector,
                               LlmToggleService llmToggleService) {
    this.providerChain = providerChain;
    this.candidateBuilder = candidateBuilder;
    this.cacheHelper = cacheHelper;
    this.cacheService = cacheService;
    this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    this.promptTemplateService = promptTemplateService;
    this.llmProperties = llmProperties;
    this.chainSelector = chainSelector;
    this.llmToggleService = llmToggleService;
    
    // Initialize semaphores with configured limits
    int globalLimit = llmProperties != null && llmProperties.getAsyncLimits() != null 
        ? llmProperties.getAsyncLimits().getGlobalConcurrency() : 10;
    int perUserLimit = llmProperties != null && llmProperties.getAsyncLimits() != null 
        ? llmProperties.getAsyncLimits().getPerUserConcurrency() : 2;
    
    this.globalAsyncSemaphore = new Semaphore(globalLimit);
    this.perUserAsyncSemaphore = new Semaphore(perUserLimit);
    
    log.debug("Initialized AIRecommendationService with async limits: global={}, perUser={}", 
        globalLimit, perUserLimit);
}

    // Backward-compatible constructor for existing tests
    public AIRecommendationService(ProviderChain providerChain) {
        this.providerChain = providerChain;
        this.candidateBuilder = null;
        this.cacheHelper = null;
        this.cacheService = null;
        this.objectMapper = new ObjectMapper();
        this.promptTemplateService = null;
        this.llmProperties = null;
        this.chainSelector = null;
        this.llmToggleService = null;
        this.globalAsyncSemaphore = new Semaphore(10);
        this.perUserAsyncSemaphore = new Semaphore(2);
    }
    
    // Additional backward-compatible constructor for existing tests
public AIRecommendationService(ProviderChain providerChain, CandidateBuilder candidateBuilder,
                               CacheHelper cacheHelper, CacheService cacheService,
                               ObjectMapper objectMapper, PromptTemplateService promptTemplateService,
                               LlmProperties llmProperties) {
    this.providerChain = providerChain;
    this.candidateBuilder = candidateBuilder;
    this.cacheHelper = cacheHelper;
    this.cacheService = cacheService;
    this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    this.promptTemplateService = promptTemplateService;
    this.llmProperties = llmProperties;
    this.chainSelector = null; // No chain selection in legacy mode
    this.llmToggleService = null; // No toggle service in legacy mode
    
    // Initialize semaphores with configured limits
    int globalLimit = llmProperties != null && llmProperties.getAsyncLimits() != null 
        ? llmProperties.getAsyncLimits().getGlobalConcurrency() : 10;
    int perUserLimit = llmProperties != null && llmProperties.getAsyncLimits() != null 
        ? llmProperties.getAsyncLimits().getPerUserConcurrency() : 2;
    
    this.globalAsyncSemaphore = new Semaphore(globalLimit);
    this.perUserAsyncSemaphore = new Semaphore(perUserLimit);
    
    log.debug("Initialized AIRecommendationService in legacy mode with async limits: global={}, perUser={}", 
        globalLimit, perUserLimit);
}

    public AIRecommendationResponse getRecommendations(Long userId, int limit) {
    // Build request context with proper tier and abGroup resolution
    RequestContext ctx = buildRequestContext(userId);
    String chainId = chainSelector != null ? chainSelector.getSelectedChainId(ctx, llmProperties) : "legacy";

    // Check feature toggles first
    if (llmToggleService != null && !llmToggleService.isEnabled(ctx, llmProperties)) {
        String disabledReason = llmToggleService.getDisabledReason(ctx, llmProperties);
        log.debug("LLM disabled for userId={}, reason={}", userId, disabledReason);
        return buildFsrsFallbackResponse(ctx, userId, limit, disabledReason);
    }

    // Build cache key with segment information including chainId
    String promptVersion = getCurrentPromptVersion();
    String segmentSuffix = buildSegmentSuffix(ctx, chainId);
    String cacheKey = CacheKeyBuilder.buildUserKey("rec-ai", userId, 
        String.format("limit_%d_pv_%s_%s", limit, promptVersion, segmentSuffix));
    
    List<RecommendationItemDTO> items = null;
    boolean cacheHit = false;
    List<String> chainHops = new ArrayList<>();
    String strategy = "normal";
    String fallbackReason = null;
    
    if (cacheService != null) {
        items = cacheService.getList(cacheKey, RecommendationItemDTO.class);
        if (items != null && !items.isEmpty() && !(items.get(0) instanceof RecommendationItemDTO)) {
            try {
                items = objectMapper.convertValue(items, new TypeReference<List<RecommendationItemDTO>>(){});
            } catch (Exception ignore) {
                items = null;
            }
            // If still not typed, evict stale cache
            if (items == null || items.isEmpty() || !(items.get(0) instanceof RecommendationItemDTO)) {
                try { cacheService.delete(cacheKey); } catch (Exception ignored) {}
            }
        }
        cacheHit = (items != null && !items.isEmpty());
    }
    
    if (items == null || items.isEmpty()) {
        ComputeResult result = computeItems(ctx, userId, limit);
        items = result.items;
        chainHops = result.chainHops != null ? result.chainHops : new ArrayList<>();
        strategy = result.strategy;
        chainId = result.chainId; // Use actual chainId from computation
        
        if (cacheService != null && items != null && !items.isEmpty()) {
            cacheService.put(cacheKey, items, java.time.Duration.ofHours(1));
        }
        // Capture fallback reason for meta
        fallbackReason = result.fallbackReason;
    } else {
        chainHops.add("cache"); // Indicate cache hit in hops
    }
    
    AIRecommendationResponse out = new AIRecommendationResponse();
    AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
    meta.setTraceId(ctx.getTraceId());
    meta.setGeneratedAt(Instant.now());
    meta.setCached(cacheHit);
    meta.setChainHops(chainHops);
    meta.setFallbackReason(fallbackReason);
    meta.setChainId(chainId);
    
    if (items != null && !items.isEmpty()) {
        boolean isFsrs = false;
        Object first = items.get(0);
        if (first instanceof RecommendationItemDTO dto) {
            isFsrs = "FSRS".equalsIgnoreCase(dto.getSource());
        } else if (first instanceof java.util.Map<?, ?> map) {
            Object src = map.get("source");
            isFsrs = src != null && "FSRS".equalsIgnoreCase(src.toString());
        }
        meta.setBusy(false);
        meta.setStrategy(isFsrs ? "fsrs_fallback" : strategy);
    } else {
        meta.setBusy(true);
        meta.setStrategy("busy_message");
    }
    
    out.setItems(items != null ? items : new ArrayList<>());
    out.setMeta(meta);
    return out;
}

    public java.util.concurrent.CompletableFuture<AIRecommendationResponse> getRecommendationsAsync(Long userId, int limit) {
    RequestContext ctx = buildRequestContext(userId);

    // Check feature toggles first
    if (llmToggleService != null && !llmToggleService.isEnabled(ctx, llmProperties)) {
        String disabledReason = llmToggleService.getDisabledReason(ctx, llmProperties);
        log.debug("LLM disabled for userId={}, reason={}", userId, disabledReason);
        AIRecommendationResponse fallbackResponse = buildFsrsFallbackResponse(ctx, userId, limit, disabledReason);
        return java.util.concurrent.CompletableFuture.completedFuture(fallbackResponse);
    }

    // Build cache key with segment information including chainId
    String chainId = chainSelector != null ? chainSelector.getSelectedChainId(ctx, llmProperties) : "legacy";
    String promptVersion = getCurrentPromptVersion();
    String segmentSuffix = buildSegmentSuffix(ctx, chainId);
    String cacheKey = CacheKeyBuilder.buildUserKey("rec-ai", userId, 
        String.format("limit_%d_pv_%s_%s", limit, promptVersion, segmentSuffix));
    List<RecommendationItemDTO> cached = null;
    boolean cacheHit = false;
    
    if (cacheService != null) {
        cached = cacheService.getList(cacheKey, RecommendationItemDTO.class);
        if (cached != null && !cached.isEmpty() && !(cached.get(0) instanceof RecommendationItemDTO)) {
            try {
                cached = objectMapper.convertValue(cached, new TypeReference<List<RecommendationItemDTO>>(){});
            } catch (Exception ignore) {
                cached = null;
            }
            if (cached == null || cached.isEmpty() || !(cached.get(0) instanceof RecommendationItemDTO)) {
                try { cacheService.delete(cacheKey); } catch (Exception ignored) {}
            }
        }
        cacheHit = (cached != null && !cached.isEmpty());
    }
    
    if (cacheHit) {
        AIRecommendationResponse out = new AIRecommendationResponse();
        AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
        meta.setTraceId(ctx.getTraceId());
        meta.setGeneratedAt(Instant.now());
        meta.setCached(true);
        meta.setBusy(false);
        meta.setStrategy("normal");
        meta.setChainHops(List.of("cache")); // Indicate cache hit
        meta.setChainId(chainId);
        out.setMeta(meta);
        out.setItems(cached);
        return java.util.concurrent.CompletableFuture.completedFuture(out);
    }

    int candidateCap = Math.min(50, Math.max(10, limit * 3));
    List<LlmProvider.ProblemCandidate> candidates = candidateBuilder != null
            ? candidateBuilder.buildForUser(userId, candidateCap)
            : buildMockCandidates();

    LlmProvider.PromptOptions options = new LlmProvider.PromptOptions();
    options.limit = Math.max(1, Math.min(50, limit));

    // Apply async rate limiting with semaphores
    int acquireTimeout = llmProperties != null && llmProperties.getAsyncLimits() != null 
        ? llmProperties.getAsyncLimits().getAcquireTimeoutMs() : 100;
    
    return CompletableFuture.supplyAsync(() -> {
        try {
            // Try to acquire global rate limit (with timeout to avoid blocking)
            if (!globalAsyncSemaphore.tryAcquire(acquireTimeout, TimeUnit.MILLISECONDS)) {
                log.warn("Global async rate limit exceeded for userId={}, traceId={}", 
                    userId, ctx.getTraceId());
                throw new RuntimeException("Global rate limit exceeded");
            }
            try {
                // Also apply per-user rate limiting
                if (!perUserAsyncSemaphore.tryAcquire(acquireTimeout / 2, TimeUnit.MILLISECONDS)) {
                    log.warn("Per-user async rate limit exceeded for userId={}, traceId={}", 
                        userId, ctx.getTraceId());
                    throw new RuntimeException("Per-user rate limit exceeded");
                }
                try {
                    log.debug("Async LLM request acquired permits for userId={}, traceId={}", 
                        userId, ctx.getTraceId());
                    // Select the appropriate chain
                    LlmProperties.Chain selectedChain = chainSelector != null 
                        ? chainSelector.selectChain(ctx, llmProperties) 
                        : null;
                    // Execute the actual async call with selected chain
                    return providerChain.executeAsync(ctx, candidates, options, selectedChain);
                } finally {
                    perUserAsyncSemaphore.release();
                }
            } finally {
                globalAsyncSemaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Rate limiting interrupted for userId={}, traceId={}", userId, ctx.getTraceId(), e);
            throw new RuntimeException("Rate limiting interrupted", e);
        }
    }).thenCompose(future -> future) // Flatten the nested CompletableFuture
            .handle((res, ex) -> {
                List<RecommendationItemDTO> items = new ArrayList<>();
                List<String> chainHops = new ArrayList<>();
                String strategy = "normal";
                String fallbackReason = null;
                
                if (ex == null && res != null && res.success && res.result != null && res.result.items != null && !res.result.items.isEmpty()) {
                    for (LlmProvider.RankedItem r : res.result.items) {
                        RecommendationItemDTO dto = new RecommendationItemDTO();
                        dto.setProblemId(r.problemId);
                        dto.setReason(r.reason);
                        dto.setConfidence(r.confidence);
                        dto.setScore(r.score);
                        dto.setStrategy(r.strategy);
                        dto.setSource("LLM");
                        dto.setModel(res.result.model);
                        dto.setPromptVersion(getCurrentPromptVersion());
                        items.add(dto);
                    }
                    chainHops = res.hops != null ? res.hops : new ArrayList<>();
                } else {
                    List<LlmProvider.ProblemCandidate> base = (candidates != null && !candidates.isEmpty()) ? candidates : buildMockCandidates();
                    items.addAll(buildFsrsFallback(base, limit));
                    strategy = "fsrs_fallback";
                    chainHops = res != null && res.hops != null ? res.hops : new ArrayList<>();
                    fallbackReason = res != null ? res.defaultReason : "unknown_error";
                }

                AIRecommendationResponse out = new AIRecommendationResponse();
                AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
                meta.setTraceId(ctx.getTraceId());
                meta.setGeneratedAt(Instant.now());
                meta.setCached(false);
                meta.setChainHops(chainHops);
                meta.setFallbackReason(fallbackReason);
                meta.setChainId(chainId);
                
                if (!items.isEmpty() && "FSRS".equalsIgnoreCase(items.get(0).getSource())) {
                    meta.setBusy(false);
                    meta.setStrategy("fsrs_fallback");
                } else {
                    meta.setBusy(false);
                    meta.setStrategy(strategy);
                }
                out.setItems(items);
                out.setMeta(meta);
                return out;
            })
            .whenComplete((resp, throwable) -> {
                if (throwable == null && resp != null && cacheService != null && resp.getItems() != null && !resp.getItems().isEmpty()) {
                    cacheService.put(cacheKey, resp.getItems(), java.time.Duration.ofHours(1));
                }
            });
}

    /**
     * Builds a request context with proper tier and AB group resolution.
     * TODO: Integrate with actual user service to get real tier information.
     */
    private RequestContext buildRequestContext(Long userId) {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(userId);
        ctx.setTier(getUserTier(userId)); // TODO: Get from user service
        ctx.setAbGroup(getUserAbGroup(userId)); // TODO: Get from A/B testing service
        ctx.setRoute("ai-recommendations");
        ctx.setTraceId(UUID.randomUUID().toString());
        return ctx;
    }

    /**
 * TODO: Replace with actual user tier lookup from user service/JWT claims.
 * Returns normalized tier name (uppercase).
 */
private String getUserTier(Long userId) {
    // For now, return BRONZE as default. In real implementation:
    // 1. Check JWT claims for user tier
    // 2. Query user service for subscription level  
    // 3. Apply business logic for tier assignment
    String tier = "BRONZE";
    return tier.toUpperCase(); // Normalize to uppercase
}

    /**
 * TODO: Replace with actual A/B testing service integration.
 * Uses consistent hashing to ensure stable assignment across deployments.
 */
private String getUserAbGroup(Long userId) {
    // For now, use consistent hash-based assignment. In real implementation:
    // 1. Check feature flag service for user-specific overrides
    // 2. Use consistent hashing based on user ID for stable assignment
    // 3. Consider user preferences and business rules
    if (userId == null) return "default";
    
    // Simple consistent hashing using Java's hashCode
    int hash = Math.abs(userId.hashCode());
    String[] groups = {"A", "B"}; // Available AB groups
    return groups[hash % groups.length];
}

    /**
 * Builds a segment suffix for cache keys to isolate different user segments and chains.
 */
private String buildSegmentSuffix(RequestContext ctx, String chainId) {
    return String.format("t_%s_ab_%s_chain_%s", 
        ctx.getTier() != null ? ctx.getTier() : "unknown",
        ctx.getAbGroup() != null ? ctx.getAbGroup() : "unknown",
        chainId != null ? chainId : "unknown");
}

    /**
     * Builds FSRS fallback response when LLM is disabled by feature toggles.
     */
    private AIRecommendationResponse buildFsrsFallbackResponse(RequestContext ctx, Long userId, int limit, String disabledReason) {
        int candidateCap = Math.min(50, Math.max(10, limit * 3));
        List<LlmProvider.ProblemCandidate> candidates = candidateBuilder != null
                ? candidateBuilder.buildForUser(userId, candidateCap)
                : buildMockCandidates();

        List<RecommendationItemDTO> items = buildFsrsFallback(candidates, limit);

        AIRecommendationResponse out = new AIRecommendationResponse();
        AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
        meta.setTraceId(ctx.getTraceId());
        meta.setGeneratedAt(Instant.now());
        meta.setCached(false);
        meta.setChainHops(List.of("toggle_off"));
        meta.setFallbackReason(disabledReason);
        meta.setBusy(false);
        meta.setStrategy("fsrs_fallback");
        meta.setChainId("disabled");
        
        out.setItems(items);
        out.setMeta(meta);
        return out;
    }

    private List<RecommendationItemDTO> buildFsrsFallback(List<LlmProvider.ProblemCandidate> candidates, int limit) {
    // Sort by lowest accuracy first (needs attention), then by fewer attempts
    // Create a mutable copy to avoid UnsupportedOperationException
    List<LlmProvider.ProblemCandidate> sortableCandidates = new ArrayList<>(candidates);
    sortableCandidates.sort(java.util.Comparator
            .comparing((LlmProvider.ProblemCandidate c) -> c.recentAccuracy == null ? 1.0 : c.recentAccuracy)
            .thenComparing(c -> c.attempts == null ? 0 : c.attempts));

    List<RecommendationItemDTO> list = new ArrayList<>();
    int k = Math.max(1, Math.min(50, limit));
    for (int i = 0; i < Math.min(k, sortableCandidates.size()); i++) {
        LlmProvider.ProblemCandidate c = sortableCandidates.get(i);
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
        dto.setPromptVersion(getCurrentPromptVersion());
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

    private ComputeResult computeItems(RequestContext ctx, Long userId, int limit) {
    int candidateCap = Math.min(50, Math.max(10, limit * 3));
    List<LlmProvider.ProblemCandidate> candidates = candidateBuilder != null
            ? candidateBuilder.buildForUser(userId, candidateCap)
            : buildMockCandidates();

    LlmProvider.PromptOptions options = new LlmProvider.PromptOptions();
    options.limit = Math.max(1, Math.min(50, limit));

    // Select the appropriate chain based on request context
    LlmProperties.Chain selectedChain = chainSelector != null 
        ? chainSelector.selectChain(ctx, llmProperties) 
        : null;
    
    String chainId = chainSelector != null ? chainSelector.getSelectedChainId(ctx, llmProperties) : "unknown";
    
    ProviderChain.Result res = providerChain.execute(ctx, candidates, options, selectedChain);

    List<RecommendationItemDTO> items = new ArrayList<>();
    String strategy = "normal";
    String fallbackReason = null;
    
    if (res != null && res.success && res.result != null && res.result.items != null && !res.result.items.isEmpty()) {
        for (LlmProvider.RankedItem r : res.result.items) {
            RecommendationItemDTO dto = new RecommendationItemDTO();
            dto.setProblemId(r.problemId);
            dto.setReason(r.reason);
            dto.setConfidence(r.confidence);
            dto.setScore(r.score);
            dto.setStrategy(r.strategy);
            dto.setSource("LLM");
            dto.setModel(res.result.model);
            dto.setPromptVersion(getCurrentPromptVersion());
            items.add(dto);
        }
    } else {
        List<LlmProvider.ProblemCandidate> base = (candidates != null && !candidates.isEmpty())
                ? candidates
                : buildMockCandidates();
        items.addAll(buildFsrsFallback(base, limit));
        strategy = "fsrs_fallback";
        fallbackReason = res != null ? res.defaultReason : "provider_chain_null";
    }
    
    return new ComputeResult(items, res != null ? res.hops : new ArrayList<>(), strategy, fallbackReason, chainId);
}

    private String getCurrentPromptVersion() {
        return promptTemplateService != null 
            ? promptTemplateService.getCurrentPromptVersion() 
            : "v1"; // fallback for tests or when service is not available
    }

private static class ComputeResult {
    final List<RecommendationItemDTO> items;
    final List<String> chainHops;
    final String strategy;
    final String fallbackReason;
    final String chainId;
    
    ComputeResult(List<RecommendationItemDTO> items, List<String> chainHops, String strategy, String fallbackReason, String chainId) {
        this.items = items;
        this.chainHops = chainHops;
        this.strategy = strategy;
        this.fallbackReason = fallbackReason;
        this.chainId = chainId;
    }
}
}
