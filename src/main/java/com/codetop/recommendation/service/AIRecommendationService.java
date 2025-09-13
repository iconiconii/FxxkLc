package com.codetop.recommendation.service;

import com.codetop.recommendation.chain.ProviderChain;
import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.alg.CandidateBuilder;
import com.codetop.recommendation.service.CandidateEnhancer;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.metrics.LlmMetricsCollector;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final UserProfilingService userProfilingService;
    private final CandidateEnhancer candidateEnhancer;
    private final HybridRankingService hybridRankingService;
    private final RecommendationMixer recommendationMixer;
    private final ConfidenceCalibrator confidenceCalibrator;
    private final LlmMetricsCollector metricsCollector;
    private final RecommendationStrategyResolver strategyResolver;
    
    // Async rate limiting with semaphores (configured via properties)
    private final Semaphore globalAsyncSemaphore;
    private final Map<Long, Semaphore> perUserSemaphores; // Fixed: per-user semaphores instead of shared
    private final int maxPerUserConcurrency;

    @org.springframework.beans.factory.annotation.Autowired
public AIRecommendationService(ProviderChain providerChain, CandidateBuilder candidateBuilder,
                               CacheHelper cacheHelper, CacheService cacheService,
                               ObjectMapper objectMapper, PromptTemplateService promptTemplateService,
                               LlmProperties llmProperties, ChainSelector chainSelector,
                               LlmToggleService llmToggleService, UserProfilingService userProfilingService,
                               CandidateEnhancer candidateEnhancer, HybridRankingService hybridRankingService,
                               RecommendationMixer recommendationMixer, ConfidenceCalibrator confidenceCalibrator,
                               LlmMetricsCollector metricsCollector, RecommendationStrategyResolver strategyResolver) {
    this.providerChain = providerChain;
    this.candidateBuilder = candidateBuilder;
    this.cacheHelper = cacheHelper;
    this.cacheService = cacheService;
    this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    this.promptTemplateService = promptTemplateService;
    this.llmProperties = llmProperties;
    this.chainSelector = chainSelector;
    this.llmToggleService = llmToggleService;
    this.userProfilingService = userProfilingService;
    this.candidateEnhancer = candidateEnhancer;
    this.hybridRankingService = hybridRankingService;
    this.recommendationMixer = recommendationMixer;
    this.confidenceCalibrator = confidenceCalibrator;
    this.metricsCollector = metricsCollector;
    this.strategyResolver = strategyResolver;
    
    // Initialize semaphores with configured limits
    int globalLimit = llmProperties != null && llmProperties.getAsyncLimits() != null 
        ? llmProperties.getAsyncLimits().getGlobalConcurrency() : 10;
    int perUserLimit = llmProperties != null && llmProperties.getAsyncLimits() != null 
        ? llmProperties.getAsyncLimits().getPerUserConcurrency() : 2;
    
    this.globalAsyncSemaphore = new Semaphore(globalLimit);
    this.perUserSemaphores = new java.util.concurrent.ConcurrentHashMap<>();
    this.maxPerUserConcurrency = perUserLimit;
    
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
        this.userProfilingService = null;
        this.candidateEnhancer = null;
        this.hybridRankingService = null;
        this.recommendationMixer = null;
        this.confidenceCalibrator = null;
        this.metricsCollector = null;
        this.strategyResolver = null;
        this.globalAsyncSemaphore = new Semaphore(10);
        this.perUserSemaphores = new java.util.concurrent.ConcurrentHashMap<>();
        this.maxPerUserConcurrency = 2;
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
    this.userProfilingService = null; // No user profiling in legacy mode
    this.candidateEnhancer = null; // No candidate enhancement in legacy mode
    this.hybridRankingService = null; // No hybrid ranking in legacy mode
    this.recommendationMixer = null; // No recommendation mixing in legacy mode
    this.confidenceCalibrator = null; // No confidence calibration in legacy mode
    this.metricsCollector = null; // No metrics collection in legacy mode
    this.strategyResolver = null; // No strategy resolver in legacy mode
    
    // Initialize semaphores with configured limits
    int globalLimit = llmProperties != null && llmProperties.getAsyncLimits() != null 
        ? llmProperties.getAsyncLimits().getGlobalConcurrency() : 10;
    int perUserLimit = llmProperties != null && llmProperties.getAsyncLimits() != null 
        ? llmProperties.getAsyncLimits().getPerUserConcurrency() : 2;
    
    this.globalAsyncSemaphore = new Semaphore(globalLimit);
    this.perUserSemaphores = new java.util.concurrent.ConcurrentHashMap<>();
    this.maxPerUserConcurrency = perUserLimit;
    
    log.debug("Initialized AIRecommendationService in legacy mode with async limits: global={}, perUser={}", 
        globalLimit, perUserLimit);
}

    public AIRecommendationResponse getRecommendations(Long userId, int limit) {
        // Delegate to the enhanced version with null parameters to avoid duplicate logic
        return getRecommendations(userId, limit, null, null, null, null);
    }

    /**
     * Enhanced getRecommendations method with learning objectives, preferences, and recommendation type.
     */
    public AIRecommendationResponse getRecommendations(
            Long userId, 
            int limit,
            LearningObjective objective,
            List<String> targetDomains,
            DifficultyPreference desiredDifficulty,
            Integer timeboxMinutes,
            RecommendationType recommendationType
    ) {
        // Strategy-based routing using RecommendationStrategyResolver
        if (strategyResolver != null && recommendationType != null) {
            try {
                RecommendationStrategy strategy = strategyResolver.resolveStrategy(recommendationType, userId, objective);
                if (strategy != null && strategy.isAvailable()) {
                    return strategy.getRecommendations(userId, limit, objective, targetDomains, desiredDifficulty, timeboxMinutes);
                }
            } catch (Exception e) {
                log.warn("Failed to use strategy-based routing for type {}, falling back to default: {}", 
                         recommendationType, e.getMessage());
            }
        }
        
        // Fallback to existing implementation for compatibility
        return getRecommendations(userId, limit, objective, targetDomains, desiredDifficulty, timeboxMinutes);
    }

    /**
     * Enhanced getRecommendations method with learning objectives and preferences.
     */
    public AIRecommendationResponse getRecommendations(
            Long userId, 
            int limit,
            LearningObjective objective,
            List<String> targetDomains,
            DifficultyPreference desiredDifficulty,
            Integer timeboxMinutes
    ) {
        // Build enhanced request context with learning objectives
        RequestContext ctx = buildRequestContext(userId, objective, targetDomains, desiredDifficulty, timeboxMinutes);
        String chainId = chainSelector != null ? chainSelector.getSelectedChainId(ctx, llmProperties) : "legacy";

        // Check feature toggles first
        if (llmToggleService != null && !llmToggleService.isEnabled(ctx, llmProperties)) {
            String disabledReason = llmToggleService.getDisabledReason(ctx, llmProperties);
            log.debug("LLM disabled for userId={}, reason={}", userId, disabledReason);
            
            // Record toggle decision metrics
            if (metricsCollector != null) {
                metricsCollector.recordToggleDecision(
                    ctx.getTier(), ctx.getAbGroup(), ctx.getRoute(), false, disabledReason);
            }
            
            return buildFsrsFallbackResponse(ctx, userId, limit, disabledReason);
        }
        
        // Record enabled toggle decision
        if (metricsCollector != null) {
            metricsCollector.recordToggleDecision(
                ctx.getTier(), ctx.getAbGroup(), ctx.getRoute(), true, null);
        }
        
        // Record chain selection
        if (metricsCollector != null) {
            metricsCollector.recordChainSelection(chainId, ctx.getTier(), ctx.getAbGroup(), ctx.getRoute());
        }

        // Build cache key with segment information including chainId and objectives
        String promptVersion = getCurrentPromptVersion();
        String segmentSuffix = buildSegmentSuffix(ctx, chainId);
        String objectiveHash = buildObjectiveHash(objective, targetDomains, desiredDifficulty, timeboxMinutes);
        String cacheKey = CacheKeyBuilder.buildUserKey("rec-ai", userId, 
            String.format("limit_%d_pv_%s_%s_%s", limit, promptVersion, segmentSuffix, objectiveHash));
        
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
        
        // Record cache access metrics
        if (metricsCollector != null) {
            metricsCollector.recordCacheAccess(ctx.getTier(), ctx.getAbGroup(), chainId, cacheHit);
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
        meta.setUserProfileSummary(ctx.getUserProfileSummary());
        
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
        meta.setUserProfileSummary(ctx.getUserProfileSummary());
        out.setMeta(meta);
        out.setItems(cached);
        return java.util.concurrent.CompletableFuture.completedFuture(out);
    }

    int candidateCap = Math.min(50, Math.max(10, limit * 3));
    List<LlmProvider.ProblemCandidate> candidates = candidateBuilder != null
            ? candidateBuilder.buildForUser(userId, candidateCap)
            : buildMockCandidates();
            
    // Enhance candidates with domain-based intelligence if available
    if (candidateEnhancer != null && ctx.getUserProfile() != null) {
        candidates = candidateEnhancer.enhanceCandidates(candidates, ctx.getUserProfile(), limit);
        log.debug("Applied domain-based candidate enhancement for userId={}", userId);
    }

    LlmProvider.PromptOptions options = new LlmProvider.PromptOptions();
    options.limit = Math.max(1, Math.min(50, limit));

    // Apply async rate limiting with semaphores
    int acquireTimeout = llmProperties != null && llmProperties.getAsyncLimits() != null 
        ? llmProperties.getAsyncLimits().getAcquireTimeoutMs() : 100;
    
    final List<LlmProvider.ProblemCandidate> candidatesFinal = candidates;
    final LlmProvider.PromptOptions optionsFinal = options;
    final int limitFinal = limit;
    final String chainIdFinal = chainId;
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
                Semaphore userSemaphore = getUserSemaphore(userId);
                if (!userSemaphore.tryAcquire(acquireTimeout / 2, TimeUnit.MILLISECONDS)) {
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
                    return providerChain.executeAsync(ctx, candidatesFinal, optionsFinal, selectedChain);
                } finally {
                    userSemaphore.release();
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
                    List<LlmProvider.ProblemCandidate> base = (candidatesFinal != null && !candidatesFinal.isEmpty()) ? candidatesFinal : buildMockCandidates();
                    items.addAll(buildFsrsFallback(base, limitFinal));
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
                meta.setChainId(chainIdFinal);
                meta.setUserProfileSummary(ctx.getUserProfileSummary());
                
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
     * Includes user profile data for personalized recommendations.
     */
    private RequestContext buildRequestContext(Long userId) {
        RequestContext ctx = new RequestContext();
        ctx.setUserId(userId);
        ctx.setTier(getUserTier(userId)); // TODO: Get from user service
        ctx.setAbGroup(getUserAbGroup(userId)); // TODO: Get from A/B testing service
        ctx.setRoute("ai-recommendations");
        ctx.setTraceId(UUID.randomUUID().toString());
        
        // Add user profile data for personalized recommendations
        if (userProfilingService != null) {
            try {
                UserProfile userProfile = userProfilingService.getUserProfile(userId, true);
                // Store user profile in context for prompt template service to use
                ctx.setUserProfile(userProfile);
                // Generate diagnostic summary for header
                ctx.setUserProfileSummary(generateUserProfileSummary(userProfile));
                log.debug("Added user profile to request context: userId={}, learningPattern={}, overallMastery={}", 
                         userId, userProfile.getLearningPattern(), userProfile.getOverallMastery());
            } catch (Exception e) {
                log.warn("Failed to load user profile for userId={}: {}", userId, e.getMessage());
                // Continue without profile data - system should gracefully degrade
            }
        }
        
        return ctx;
    }

    /**
     * Enhanced buildRequestContext method with learning objectives and preferences.
     */
    private RequestContext buildRequestContext(
            Long userId, 
            LearningObjective objective,
            List<String> targetDomains,
            DifficultyPreference desiredDifficulty,
            Integer timeboxMinutes
    ) {
        // Start with basic context
        RequestContext ctx = buildRequestContext(userId);
        
        // Add learning objectives
        ctx.setObjective(objective);
        ctx.setTargetDomains(targetDomains);
        ctx.setDesiredDifficulty(desiredDifficulty);
        ctx.setTimeboxMinutes(timeboxMinutes);
        
        return ctx;
    }
    
    /**
     * Builds a hash for objective-related parameters for cache key isolation.
     */
    private String buildObjectiveHash(
            LearningObjective objective,
            List<String> targetDomains,
            DifficultyPreference desiredDifficulty,
            Integer timeboxMinutes
    ) {
        StringBuilder sb = new StringBuilder();
        
        // Add objective
        if (objective != null) {
            sb.append("obj_").append(objective.getValue());
        }
        
        // Add target domains (sorted for consistency)
        if (targetDomains != null && !targetDomains.isEmpty()) {
            List<String> sortedDomains = new ArrayList<>(targetDomains);
            sortedDomains.sort(String::compareToIgnoreCase);
            sb.append("_domains_").append(String.join(",", sortedDomains));
        }
        
        // Add difficulty preference
        if (desiredDifficulty != null) {
            sb.append("_diff_").append(desiredDifficulty.getValue());
        }
        
        // Add timebox
        if (timeboxMinutes != null) {
            sb.append("_time_").append(timeboxMinutes);
        }
        
        // Return stable SHA-256 hash or default if no objectives
        String result = sb.toString();
        return result.isEmpty() ? "default" : calculateStableHash(result);
    }

    /**
     * Calculate stable SHA-256 hash for cache key, truncated to 12 characters for low collision risk
     */
    private String calculateStableHash(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // Convert to hex and truncate to 12 characters for manageable key length
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(6, hash.length); i++) { // 6 bytes = 12 hex chars
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // Fallback to hashCode if SHA-256 is not available (should never happen)
            log.warn("SHA-256 not available, falling back to hashCode for cache key: {}", e.getMessage());
            return Integer.toHexString(input.hashCode());
        }
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
     * Generates a condensed user profile summary for diagnostic headers
     */
    private String generateUserProfileSummary(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        
        try {
            StringBuilder summary = new StringBuilder();
            summary.append(profile.getLearningPattern()).append("|");
            summary.append("M:").append(String.format("%.0f%%", profile.getOverallMastery() * 100)).append("|");
            summary.append("A:").append(String.format("%.0f%%", profile.getAverageAccuracy() * 100)).append("|");
            summary.append("Q:").append(String.format("%.0f%%", profile.getDataQuality() * 100));
            
            if (!profile.getWeakDomains().isEmpty()) {
                summary.append("|W:").append(String.join(",", profile.getWeakDomains().subList(0, 
                    Math.min(2, profile.getWeakDomains().size())))); // Top 2 weak domains
            }
            
            return summary.toString();
        } catch (Exception e) {
            log.debug("Failed to generate profile summary for userId={}: {}", profile.getUserId(), e.getMessage());
            return "ERROR";
        }
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
        meta.setUserProfileSummary(ctx.getUserProfileSummary());
        
        out.setItems(items);
        out.setMeta(meta);
        return out;
    }

    private List<RecommendationItemDTO> buildFsrsFallback(List<LlmProvider.ProblemCandidate> candidates, int limit) {
    // Sort by FSRS urgency signals: urgencyScore desc, daysOverdue desc, then recentAccuracy asc
    // Create a mutable copy to avoid UnsupportedOperationException
    List<LlmProvider.ProblemCandidate> sortableCandidates = new ArrayList<>(candidates);
    sortableCandidates.sort(java.util.Comparator
            // Primary: urgency score descending (higher urgency first)
            .comparing((LlmProvider.ProblemCandidate c) -> c.urgencyScore != null ? c.urgencyScore : 0.0, java.util.Comparator.reverseOrder())
            // Secondary: days overdue descending (more overdue first) using type-safe int comparator
            .thenComparing(java.util.Comparator.comparingInt((LlmProvider.ProblemCandidate c) -> c.daysOverdue != null ? c.daysOverdue : 0).reversed())
            // Tertiary: recent accuracy ascending (lower accuracy = needs more practice)
            .thenComparing(c -> c.recentAccuracy != null ? c.recentAccuracy : 1.0));

    List<RecommendationItemDTO> list = new ArrayList<>();
    int k = Math.max(1, Math.min(50, limit));
    for (int i = 0; i < Math.min(k, sortableCandidates.size()); i++) {
        LlmProvider.ProblemCandidate c = sortableCandidates.get(i);
        RecommendationItemDTO dto = new RecommendationItemDTO();
        dto.setProblemId(c.id);
        
        // Use urgency score as primary ranking signal
        double urgency = c.urgencyScore != null ? c.urgencyScore : 0.0;
        double acc = c.recentAccuracy != null ? c.recentAccuracy : 0.0;
        double daysOverdue = c.daysOverdue != null ? c.daysOverdue : 0.0;
        
        String reason;
        if (urgency > 0) {
            reason = String.format("FSRS fallback: urgency %.2f, %.0f%% accuracy%s", 
                    urgency, acc * 100, daysOverdue > 0 ? String.format(", %.1f days overdue", daysOverdue) : "");
        } else {
            reason = String.format("FSRS fallback: prioritize lower accuracy (%.0f%%)", acc * 100);
        }
        
        dto.setReason(reason);
        // Use urgency score for confidence and score, fallback to accuracy-based scoring
        double finalScore = urgency > 0 ? urgency : (1.0 - acc);
        dto.setConfidence(Math.max(0.0, Math.min(1.0, finalScore)));
        dto.setScore(finalScore);
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
            
    // Enhance candidates with domain-based intelligence if available
    if (candidateEnhancer != null && ctx.getUserProfile() != null) {
        candidates = candidateEnhancer.enhanceCandidates(candidates, ctx.getUserProfile(), limit);
        log.debug("Applied domain-based candidate enhancement for userId={}", userId);
    }

    LlmProvider.PromptOptions options = new LlmProvider.PromptOptions();
    options.limit = Math.max(1, Math.min(50, limit));

    // Select the appropriate chain based on request context
    LlmProperties.Chain selectedChain = chainSelector != null 
        ? chainSelector.selectChain(ctx, llmProperties) 
        : null;
    
    String chainId = chainSelector != null ? chainSelector.getSelectedChainId(ctx, llmProperties) : "unknown";
    
    ProviderChain.Result res = providerChain.execute(ctx, candidates, options, selectedChain);
    
    // Record provider chain execution metrics
    if (metricsCollector != null && res != null) {
        int hopsCount = res.hops != null ? res.hops.size() : 0;
        metricsCollector.recordProviderChainResult(chainId, hopsCount, res.success, res.defaultReason);
    }

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
        
        // Create candidate mapping for downstream processing
        Map<Long, LlmProvider.ProblemCandidate> candidateMap = candidates.stream()
            .collect(Collectors.toMap(c -> c.id, c -> c, (existing, replacement) -> existing));
        
        // Apply hybrid ranking if available (v3 enhancement)
        if (hybridRankingService != null && !items.isEmpty() && ctx.getUserProfile() != null) {
            try {
                // Apply hybrid ranking
                items = hybridRankingService.rankWithHybridScores(items, candidateMap, ctx.getUserProfile());
                
                log.debug("Applied hybrid ranking to {} LLM items for userId={}", items.size(), userId);
            } catch (Exception e) {
                log.warn("Hybrid ranking failed for userId={}: {}, using LLM-only ranking", userId, e.getMessage());
                // Continue with LLM-only results on failure
            }
        }
        
        // Apply multi-dimensional strategy mixing if available (v3 enhancement)
        if (recommendationMixer != null && !items.isEmpty() && ctx.getUserProfile() != null && ctx.getObjective() != null) {
            try {
                items = recommendationMixer.mixRecommendations(
                    items, candidateMap, ctx.getUserProfile(), ctx.getObjective(), limit);
                
                log.debug("Applied multi-dimensional mixing with {} objective to {} items for userId={}", 
                    ctx.getObjective(), items.size(), userId);
            } catch (Exception e) {
                log.warn("Recommendation mixing failed for userId={}: {}, using hybrid-only results", userId, e.getMessage());
                // Continue with hybrid results on failure
            }
        }
        
        // Apply confidence calibration if available (v3 enhancement)
        if (confidenceCalibrator != null && !items.isEmpty() && ctx.getUserProfile() != null) {
            try {
                // Create LLM metadata from provider chain result
                ConfidenceCalibrator.LlmResponseMetadata llmMetadata = createLlmMetadata(res);
                
                items = confidenceCalibrator.calibrateConfidence(
                    items, candidateMap, ctx.getUserProfile(), llmMetadata);
                
                log.debug("Applied confidence calibration to {} items for userId={}", items.size(), userId);
            } catch (Exception e) {
                log.warn("Confidence calibration failed for userId={}: {}, using uncalibrated results", userId, e.getMessage());
                // Continue with mixed results on failure
            }
        }
        
    } else {
        List<LlmProvider.ProblemCandidate> base = (candidates != null && !candidates.isEmpty())
                ? candidates
                : buildMockCandidates();
        items.addAll(buildFsrsFallback(base, limit));
        strategy = "fsrs_fallback";
        fallbackReason = res != null ? res.defaultReason : "provider_chain_null";
        
        // Record fallback event metrics
        if (metricsCollector != null) {
            metricsCollector.recordFallback(fallbackReason, chainId, strategy);
        }
    }
    
    return new ComputeResult(items, res != null ? res.hops : new ArrayList<>(), strategy, fallbackReason, chainId);
}

    private String getCurrentPromptVersion() {
        return promptTemplateService != null 
            ? promptTemplateService.getCurrentPromptVersion() 
            : "v1"; // fallback for tests or when service is not available
    }
    
    /**
     * Create LLM metadata for confidence calibration from provider chain result.
     */
    private ConfidenceCalibrator.LlmResponseMetadata createLlmMetadata(ProviderChain.Result providerResult) {
        if (providerResult == null || providerResult.result == null) {
            return new ConfidenceCalibrator.LlmResponseMetadata("unknown", null, null, null);
        }
        
        // Extract metadata from provider result
        String provider = providerResult.result.provider != null ? providerResult.result.provider : "unknown";
        Long responseTime = (long) providerResult.result.latencyMs; // Convert int to Long
        Integer inputTokens = null; // Not available in current LlmResult structure
        Integer outputTokens = null; // Not available in current LlmResult structure
        
        ConfidenceCalibrator.LlmResponseMetadata metadata = 
            new ConfidenceCalibrator.LlmResponseMetadata(provider, responseTime, inputTokens, outputTokens);
        
        // Set additional metadata if available
        metadata.model = providerResult.result.model;
        metadata.temperature = null; // Not available in current LlmResult structure
        metadata.isComplete = providerResult.success;
        
        return metadata;
    }
    
    /**
     * Get or create per-user semaphore for rate limiting.
     */
    private Semaphore getUserSemaphore(Long userId) {
        return perUserSemaphores.computeIfAbsent(userId, 
            k -> new Semaphore(maxPerUserConcurrency));
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
