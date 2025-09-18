package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.chain.ProviderChain;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.metrics.LlmMetricsCollector;
import com.codetop.recommendation.service.RequestContext;
import com.codetop.recommendation.dto.UserProfile;
import com.codetop.service.cache.CacheService;
import com.codetop.entity.Problem;
import com.codetop.entity.FSRSCard;
import com.codetop.mapper.ProblemMapper;
import com.codetop.mapper.FSRSCardMapper;
import com.codetop.service.ProblemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIRecommendationService {
    
    private final UserProfilingService userProfilingService;
    private final ProviderChain providerChain;
    private final ChainSelector chainSelector;
    private final LlmToggleService toggleService;
    private final HybridRankingService hybridRankingService;
    private final CandidateEnhancer candidateEnhancer;
    private final ConfidenceCalibrator confidenceCalibrator;
    private final ProblemService problemService;
    private final com.codetop.service.FSRSService fsrsService;
    private final RecommendationMixer mixer;
    private final LlmMetricsCollector metricsCollector;
    private final CacheService cacheService;
    private final LlmProperties llmProperties;
    private final ProblemMapper problemMapper;
    private final ObjectMapper objectMapper;
    
    /**
     * Core AI recommendation orchestration with intelligent fallback.
     * Simplified implementation that focuses on working functionality.
     */
    public AIRecommendationResponse getRecommendations(Long userId, int limit, 
                                                     LearningObjective objective, 
                                                     List<String> targetDomains,
                                                     DifficultyPreference desiredDifficulty, 
                                                     Integer timeboxMinutes, 
                                                     boolean forceRefresh, 
                                                     String abGroup) {
        
        long startTime = System.currentTimeMillis();
        String traceId = java.util.UUID.randomUUID().toString();
        
        log.info("Starting AI recommendation generation - userId={}, limit={}, traceId={}", 
                userId, limit, traceId);
        
        try {
            // Step 1: Create request context for LLM routing and user profiling
            RequestContext context = createRequestContext(userId, objective, targetDomains, desiredDifficulty, timeboxMinutes, forceRefresh, abGroup);
            
            // Step 1.5: Synchronize prompt version in context to match actual execution flow
            // This ensures cache key consistency with the prompt version that will be used by providers
            String actualPromptVersion = determinePromptVersion(context);
            context.setPromptVersion(actualPromptVersion);
            log.debug("Synchronized context prompt version to: {} for userId={}", actualPromptVersion, userId);
            
            // Step 2: Build enhanced cache key with all parameters for proper cache isolation
            String domainsHash = "null";
            if (targetDomains != null && !targetDomains.isEmpty()) {
                // Create stable hash of sorted domains for consistent cache keys
                String sortedDomains = targetDomains.stream()
                        .sorted()
                        .collect(java.util.stream.Collectors.joining(","));
                domainsHash = String.valueOf(sortedDomains.hashCode()).replace("-", "n"); // Remove negative sign
            }
            
            String cacheKey = com.codetop.service.CacheKeyBuilder.buildUserKey("rec-ai", userId, 
                    "limit_" + limit, 
                    "obj_" + (objective != null ? objective.name() : "null"),
                    "diff_" + (desiredDifficulty != null ? desiredDifficulty.name() : "null"),
                    "domains_" + domainsHash,
                    "timebox_" + (timeboxMinutes != null ? timeboxMinutes : "null"),
                    "pv_" + context.getPromptVersion());
            
            if (!forceRefresh) {
                try {
                    String cached = cacheService.get(cacheKey, String.class);
                    if (cached != null && !cached.isEmpty()) {
                        // Attempt to deserialize cached response
                        try {
                            AIRecommendationResponse cachedResponse = objectMapper.readValue(cached, AIRecommendationResponse.class);
                            // Update response metadata to indicate cache hit and preserve important fields
                            if (cachedResponse.getMeta() != null) {
                                cachedResponse.getMeta().setCached(true);
                                cachedResponse.getMeta().setTraceId(context.getTraceId());
                                // Preserve generatedAt from original response
                                // Don't overwrite other metadata fields as they contain valuable chain execution info
                            } else {
                                // Create meta if missing
                                AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
                                meta.setCached(true);
                                meta.setTraceId(context.getTraceId());
                                meta.setGeneratedAt(java.time.Instant.now());
                                cachedResponse.setMeta(meta);
                            }
                            
                            metricsCollector.recordCacheAccess(context.getTier(), context.getAbGroup(), 
                                    context.getTraceId(), true);
                            log.info("Cache hit - returning cached AI recommendations - userId={}, count={}", 
                                    userId, cachedResponse.getItems() != null ? cachedResponse.getItems().size() : 0);
                            return cachedResponse;
                        } catch (JsonProcessingException e) {
                            log.warn("Failed to deserialize cached response for userId={}, proceeding with fresh generation: {}", 
                                    userId, e.getMessage());
                            // Continue with fresh generation if deserialization fails
                        }
                    }
                } catch (Exception e) {
                    log.debug("Cache retrieval failed for key={}: {}", cacheKey, e.getMessage());
                }
            } else {
                log.info("Force refresh requested, skipping cache check - userId={}", userId);
            }
            
            metricsCollector.recordCacheAccess(context.getTier(), context.getAbGroup(), 
                    context.getTraceId(), false);
            
            // Step 3: Check if LLM is enabled for this user/context
            // Note: Simplified version without LlmProperties parameter
            try {
                // For now, assume LLM is enabled - can be enhanced with proper toggle logic
                boolean llmEnabled = true; // toggleService.isEnabled(context, llmProperties);
                
                if (!llmEnabled) {
                    log.info("LLM disabled for userId={}, falling back to FSRS", userId);
                    return createFallbackResponse(userId, limit, "llm_disabled", context);
                }
            } catch (Exception e) {
                log.debug("LLM toggle check failed, proceeding with LLM enabled: {}", e.getMessage());
            }
            
            // Step 4: Generate problem candidates using enhanced approach with caching
            // Generate more candidates for FSRS pre-filtering, then limit what goes to LLM
            int candidateLimit = Math.min(60, limit * 3); // More candidates for better FSRS selection
            
            // Try to get candidates from cache first
            String candidateCacheKey = com.codetop.service.CacheKeyBuilder.buildUserKey("rec-candidates", userId,
                    "domains_" + domainsHash,
                    "timebox_" + (timeboxMinutes != null ? timeboxMinutes : "null"),
                    "diff_" + (desiredDifficulty != null ? desiredDifficulty.name() : "null"));
            
            List<LlmProvider.ProblemCandidate> candidates = null;
            try {
                String cachedCandidates = cacheService.get(candidateCacheKey, String.class);
                if (cachedCandidates != null && !cachedCandidates.isEmpty()) {
                    // Try to deserialize cached candidates
                    try {
                        candidates = objectMapper.readValue(cachedCandidates, 
                                new TypeReference<List<LlmProvider.ProblemCandidate>>() {});
                        log.debug("Cache hit for candidates - userId={}, count={}", userId, candidates.size());
                    } catch (JsonProcessingException e) {
                        log.debug("Failed to deserialize cached candidates, generating fresh: {}", e.getMessage());
                        candidates = null;
                    }
                }
            } catch (Exception e) {
                log.debug("Candidate cache retrieval failed: {}", e.getMessage());
            }
            
            // Generate fresh candidates if not cached
            if (candidates == null) {
                candidates = generateSimplifiedCandidates(userId, targetDomains, desiredDifficulty, candidateLimit);
                
                // Cache the candidates for reuse
                try {
                    String serializedCandidates = objectMapper.writeValueAsString(candidates);
                    cacheService.put(candidateCacheKey, serializedCandidates, Duration.ofMinutes(1));
                    log.debug("Cached candidates for userId={}, count={}", userId, candidates.size());
                } catch (Exception e) {
                    log.debug("Failed to cache candidates: {}", e.getMessage());
                }
            }
            
            if (candidates.isEmpty()) {
                log.warn("No candidates generated for userId={}", userId);
                return createFallbackResponse(userId, limit, "no_candidates", context);
            }
            
            // Step 5: Enhance candidates with domain affinity and signals using CandidateEnhancer
            List<LlmProvider.ProblemCandidate> enhancedCandidates = enhanceCandidates(candidates, context);
            
            // Step 5.5: Apply token reduction by limiting candidates sent to LLM
            // Keep top candidates based on CandidateEnhancer ranking, limit to reduce token usage
            int llmCandidateLimit = Math.min(20, limit * 2); // Target 30%+ token reduction
            List<LlmProvider.ProblemCandidate> llmCandidates = enhancedCandidates.size() > llmCandidateLimit ? 
                    enhancedCandidates.subList(0, llmCandidateLimit) : enhancedCandidates;
            
            log.debug("Token reduction: {} total candidates -> {} sent to LLM ({}% reduction)", 
                    enhancedCandidates.size(), llmCandidates.size(), 
                    enhancedCandidates.size() > 0 ? (100 - (llmCandidates.size() * 100 / enhancedCandidates.size())) : 0);
            
            // Step 6: Select and execute LLM provider chain
            long chainStartTime = System.currentTimeMillis();
            String selectedChainId = null;
            String finalProvider = null;
            List<String> chainHops = new ArrayList<>();
            
            try {
                // Select chain based on user context
                com.codetop.recommendation.config.LlmProperties.Chain selectedChain = chainSelector.selectChain(context, llmProperties);
                selectedChainId = chainSelector.getSelectedChainId(context, llmProperties);
                
                // Record chain selection
                metricsCollector.recordChainSelection(selectedChainId, context.getTier(), context.getAbGroup(), context.getRoute());
                
                // Create prompt options with enhanced versioning
                LlmProvider.PromptOptions promptOptions = new LlmProvider.PromptOptions();
                promptOptions.limit = Math.min(limit * 2, 50);
                // Use the synchronized prompt version from context for consistency
                promptOptions.promptVersion = context.getPromptVersion();
                
                // Execute provider chain with real LLM calls
                // Use async chain with enforced timeout to avoid long waits on providers
                ProviderChain.Result chainResult;
                // Enhanced timeout guard: Calculate dynamically based on chain configuration
                // Target: Keep total response under 2.3s even with processing overhead
                long guardMs = calculateChainTimeoutGuard(selectedChain, llmProperties);
                log.debug("Using dynamic guard timeout: {}ms for chain: {}", guardMs, selectedChainId);
                try {
                    java.util.concurrent.CompletableFuture<ProviderChain.Result> future =
                            providerChain.executeAsync(context, llmCandidates, promptOptions, selectedChain);
                    chainResult = future.get(guardMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                } catch (java.util.concurrent.TimeoutException te) {
                    log.warn("Provider chain overall timeout after {}ms for userId={}, falling back", guardMs, userId);
                    ProviderChain.Result r = new ProviderChain.Result();
                    r.success = false;
                    r.defaultReason = "TIMEOUT";
                    r.hops = java.util.List.of("openai", "default");
                    chainResult = r;
                }
                
                if (chainResult.success && chainResult.result != null && chainResult.result.success) {
                    // Successful LLM execution
                    finalProvider = extractFinalProvider(chainResult.hops);
                    chainHops = chainResult.hops;
                    
                    // Record chain success telemetry
                    Duration totalChainLatency = Duration.ofMillis(System.currentTimeMillis() - chainStartTime);
                    metricsCollector.recordChainHops(selectedChainId, context.getTier(), context.getAbGroup(), 
                            chainHops.size(), finalProvider, new String[0], totalChainLatency);
                    
                    // Convert LLM results to recommendation DTOs
                    List<RecommendationItemDTO> recommendations = convertLlmResultToRecommendationItems(chainResult.result, limit);
                    
                    // Enrich with real problem data (titles, difficulties)
                    enrichRecommendationsWithProblemData(recommendations);
                    
                    // Build successful response with complete metadata
                    AIRecommendationResponse response = createSuccessResponse(recommendations, context, selectedChainId, chainHops, finalProvider);
                    
                    // Cache the full serialized response with 1-hour TTL
                    try {
                        String serializedResponse = objectMapper.writeValueAsString(response);
                        cacheService.put(cacheKey, serializedResponse, Duration.ofHours(1));
                        log.debug("Successfully cached AI recommendations - userId={}, cacheKey={}, size={} bytes", 
                                userId, cacheKey, serializedResponse.length());
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to serialize AI recommendations for caching - userId={}: {}", userId, e.getMessage());
                    } catch (Exception e) {
                        log.debug("Failed to cache AI recommendations - userId={}: {}", userId, e.getMessage());
                    }
                    
                    // Record final metrics
                    long duration = System.currentTimeMillis() - startTime;
                    metricsCollector.recordProviderChainResult(selectedChainId, chainHops.size(), true, null);
                    
                    log.info("AI recommendations generated successfully - userId={}, count={}, duration={}ms, chain={}, provider={}", 
                            userId, recommendations.size(), duration, selectedChainId, finalProvider);
                    
                    return response;
                } else {
                    // Chain execution failed or returned default result
                    chainHops = chainResult.hops;
                    String fallbackReason = chainResult.defaultReason;
                    
                    log.warn("Provider chain failed for userId={}, reason={}, hops={}", userId, fallbackReason, chainHops);
                    
                    // Record chain failure telemetry
                    Duration totalChainLatency = Duration.ofMillis(System.currentTimeMillis() - chainStartTime);
                    metricsCollector.recordChainHops(selectedChainId, context.getTier(), context.getAbGroup(), 
                            chainHops.size(), null, chainHops.toArray(new String[0]), totalChainLatency);
                    
                    // Record fallback event
                    metricsCollector.recordFallback("chain_execution_failed", selectedChainId, "ai_to_fsrs");
                    
                    // Set context for fallback response with full candidate set for better FSRS ranking
                    context.setChainId(selectedChainId);
                    context.setChainHops(chainHops);
                    context.setFallbackReason(fallbackReason);
                    
                    // Store full enhanced candidates for fallback to use
                    // This ensures FSRS fallback has access to all candidates, not just the LLM subset
                    log.debug("Fallback will use {} candidates instead of LLM-limited {}", 
                            enhancedCandidates.size(), llmCandidates.size());
                }
            } catch (Exception e) {
                log.error("Exception during provider chain execution for userId={}: {}", userId, e.getMessage(), e);
                
                // Record chain failure telemetry
                Duration totalChainLatency = Duration.ofMillis(System.currentTimeMillis() - chainStartTime);
                metricsCollector.recordChainHops(selectedChainId != null ? selectedChainId : "unknown", 
                        context.getTier(), context.getAbGroup(), 
                        chainHops.size(), null, chainHops.toArray(new String[0]), totalChainLatency);
                
                // Record fallback event
                metricsCollector.recordFallback("chain_exception", selectedChainId, "ai_to_fsrs");
                
                // Set context for fallback response
                context.setChainId(selectedChainId);
                context.setChainHops(chainHops);
                context.setFallbackReason("chain_exception");
            }
            
            // Step 10: Fallback if LLM failed
            return createFallbackResponse(userId, limit, "llm_execution_failed", context);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Error in AI recommendations - userId={}, duration={}ms, error={}", 
                    userId, duration, e.getMessage(), e);
            
            return createFallbackResponse(userId, limit, "service_error", null);
        }
    }
    
    /**
     * Generate problem candidates using optimized bulk FSRS card fetching.
     * Avoids N+1 queries by bulk-fetching FSRS cards and enriching them.
     */
    private List<LlmProvider.ProblemCandidate> generateSimplifiedCandidates(Long userId, List<String> targetDomains, 
                                                           DifficultyPreference difficulty, int candidateLimit) {
        List<LlmProvider.ProblemCandidate> candidates = new ArrayList<>();
        
        try {
            // Step 1: Bulk-fetch FSRS due cards (avoids N+1 queries)
            List<com.codetop.mapper.FSRSCardMapper.ReviewQueueCard> fsrsCards = fsrsService.getDueCards(userId, candidateLimit);
            
            if (fsrsCards != null && !fsrsCards.isEmpty()) {
                // Convert FSRS cards to problem candidates with actual FSRS data
                for (com.codetop.mapper.FSRSCardMapper.ReviewQueueCard fsrsCard : fsrsCards) {
                    LlmProvider.ProblemCandidate candidate = new LlmProvider.ProblemCandidate();
                    candidate.id = fsrsCard.getProblemId();
                    candidate.topic = targetDomains != null && !targetDomains.isEmpty() ? targetDomains.get(0) : "fsrs";
                    candidate.difficulty = fsrsCard.getDifficulty() != null ? fsrsCard.getDifficulty().toString() : "MEDIUM";
                    candidate.tags = targetDomains != null ? new ArrayList<>(targetDomains) : new ArrayList<>();
                    
                    // Use actual FSRS data for accurate urgency scoring
                    candidate.recentAccuracy = 0.8; // Could be enhanced with actual accuracy from review logs
                    candidate.attempts = fsrsCard.getReviewCount() != null ? fsrsCard.getReviewCount() : 0;
                    
                    // Calculate retention probability from FSRS stability
                    if (fsrsCard.getStability() != null) {
                        double stability = fsrsCard.getStability().doubleValue();
                        candidate.retentionProbability = stability > 0 ? 1.0 / (1.0 + Math.exp(-stability)) : 0.5;
                    } else {
                        candidate.retentionProbability = 0.5; // Default for new cards
                    }
                    
                    // Calculate days overdue from next review date
                    if (fsrsCard.getNextReview() != null) {
                        java.time.LocalDateTime nextReview = fsrsCard.getNextReview();
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(nextReview, now);
                        candidate.daysOverdue = Math.max(0, (int) daysDiff);
                    } else {
                        candidate.daysOverdue = 0; // Not overdue
                    }
                    
                    // Calculate urgency score based on FSRS urgency factors
                    double retentionUrgency = 1.0 - candidate.retentionProbability;
                    double overdueUrgency = candidate.daysOverdue * 0.1; // 0.1 per day overdue
                    candidate.urgencyScore = Math.min(1.0, retentionUrgency + overdueUrgency);
                    
                    candidates.add(candidate);
                }
                
                log.debug("Generated {} FSRS-based candidates for userId={}", candidates.size(), userId);
            }
            
            // Step 2: If we have insufficient FSRS candidates, log and proceed with what we have
            if (candidates.size() < candidateLimit) {
                log.debug("Only {} FSRS candidates available for userId={}, requested {}", 
                        candidates.size(), userId, candidateLimit);
            }
            
        } catch (Exception e) {
            log.error("Error generating FSRS candidates for userId={}: {}", userId, e.getMessage(), e);
            // Return empty list if FSRS candidate generation fails - no fake data
        }
        
        log.info("Generated total {} candidates for userId={} (FSRS-based + additional)", candidates.size(), userId);
        return candidates;
    }
    
    /**
     * Enhance candidates with domain affinity and signals using CandidateEnhancer.
     */
    private List<LlmProvider.ProblemCandidate> enhanceCandidates(List<LlmProvider.ProblemCandidate> candidates, RequestContext context) {
        try {
            // Use CandidateEnhancer to add domain affinity and additional signals
            // This reduces the number of candidates sent to LLM and improves relevance
            for (LlmProvider.ProblemCandidate candidate : candidates) {
                // Enhance with domain affinity scores
                if (context.getTargetDomains() != null && !context.getTargetDomains().isEmpty()) {
                    String candidateTopic = candidate.topic != null ? candidate.topic : "general";
                    boolean domainMatch = context.getTargetDomains().contains(candidateTopic);
                    
                    // Boost urgency score for domain-matching candidates
                    if (domainMatch) {
                        candidate.urgencyScore = Math.min(1.0, candidate.urgencyScore + 0.2);
                    }
                }
                
                // Enhance with objective-based signals
                if (context.getObjective() != null) {
                    switch (context.getObjective()) {
                        case WEAKNESS_FOCUS:
                            // Boost candidates with lower recent accuracy
                            if (candidate.recentAccuracy != null && candidate.recentAccuracy < 0.7) {
                                candidate.urgencyScore = Math.min(1.0, candidate.urgencyScore + 0.3);
                            }
                            break;
                        case PROGRESSIVE_DIFFICULTY:
                            // Adjust based on difficulty progression
                            // This could be enhanced with actual difficulty ordering
                            break;
                        case REFRESH_MASTERED:
                            // Boost overdue candidates for refresh
                            if (candidate.daysOverdue != null && candidate.daysOverdue > 0) {
                                candidate.urgencyScore = Math.min(1.0, candidate.urgencyScore + (candidate.daysOverdue * 0.1));
                            }
                            break;
                    }
                }
            }
            
            // Sort by enhanced urgency score and return top candidates
            return candidates.stream()
                    .sorted((a, b) -> Double.compare(b.urgencyScore, a.urgencyScore))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.debug("Failed to enhance candidates: {}", e.getMessage());
            return candidates; // Return original candidates if enhancement fails
        }
    }
    
    
    /**
     * Determine prompt version based on context and A/B testing.
     */
    private String determinePromptVersion(RequestContext context) {
        // Use A/B group for prompt versioning
        String abGroup = context.getAbGroup();
        if ("B".equals(abGroup)) {
            return "v1.1"; // Testing version with enhanced instructions
        } else {
            return "v1.0"; // Stable version
        }
    }
    
    
    /**
     * Extract the final provider name from chain hops.
     */
    private String extractFinalProvider(List<String> chainHops) {
        if (chainHops == null || chainHops.isEmpty()) {
            return "unknown";
        }
        
        // Find the last non-default provider in the chain
        for (int i = chainHops.size() - 1; i >= 0; i--) {
            String hop = chainHops.get(i);
            if (!"default".equals(hop)) {
                return hop;
            }
        }
        
        return chainHops.get(chainHops.size() - 1); // Return last hop if all are default
    }
    
    /**
     * Convert ProviderChain LlmResult to recommendation DTOs.
     */
    private List<RecommendationItemDTO> convertLlmResultToRecommendationItems(LlmProvider.LlmResult llmResult, int limit) {
        if (llmResult == null || llmResult.items == null || llmResult.items.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<RecommendationItemDTO> recommendations = new ArrayList<>();
        
        for (int i = 0; i < Math.min(llmResult.items.size(), limit); i++) {
            LlmProvider.RankedItem item = llmResult.items.get(i);
            
            RecommendationItemDTO dto = new RecommendationItemDTO();
            dto.setProblemId(item.problemId);
            dto.setTitle("Problem " + item.problemId); // Could be enhanced with actual titles
            dto.setDifficulty("MEDIUM"); // Default, could be enhanced
            dto.setEstimatedTime(30); // Default 30 minutes
            dto.setScore(item.score);
            dto.setConfidence(item.confidence);
            dto.setReason(item.reason != null ? item.reason : "AI recommendation");
            dto.setSource("AI_LLM");
            
            recommendations.add(dto);
        }
        
        return recommendations;
    }
    
    /**
     * Convert LLM ranked items to recommendation DTOs (legacy method for compatibility).
     */
    private List<RecommendationItemDTO> convertToRecommendationItems(List<LlmProvider.RankedItem> rankedItems, int limit) {
        List<RecommendationItemDTO> recommendations = new ArrayList<>();
        
        for (int i = 0; i < Math.min(rankedItems.size(), limit); i++) {
            LlmProvider.RankedItem item = rankedItems.get(i);
            
            RecommendationItemDTO dto = new RecommendationItemDTO();
            dto.setProblemId(item.problemId);
            dto.setTitle("Problem " + item.problemId); // Could be enhanced with actual titles
            dto.setDifficulty("MEDIUM"); // Default, could be enhanced
            dto.setEstimatedTime(30); // Default 30 minutes
            dto.setScore(item.score);
            dto.setConfidence(item.confidence);
            dto.setReason(item.reason != null ? item.reason : "AI recommendation");
            dto.setSource("AI_LLM");
            
            recommendations.add(dto);
        }
        
        return recommendations;
    }
    
    /**
     * Calculate dynamic timeout guard based on chain configuration.
     * Aggregates node timeouts and adds safety margin.
     * 
     * @param selectedChain The chain configuration to analyze
     * @param llmProperties LLM properties for fallback values
     * @return Calculated guard timeout in milliseconds
     */
    private long calculateChainTimeoutGuard(LlmProperties.Chain selectedChain, LlmProperties llmProperties) {
        long maxNodeTimeoutMs = 0L;
        
        // Find the maximum timeout among enabled nodes in the chain
        if (selectedChain != null && selectedChain.getNodes() != null) {
            for (LlmProperties.Node node : selectedChain.getNodes()) {
                if (node.isEnabled() && node.getTimeoutMs() != null) {
                    maxNodeTimeoutMs = Math.max(maxNodeTimeoutMs, node.getTimeoutMs());
                }
            }
        }
        
        // Fallback to provider-level timeout if no node timeouts found
        if (maxNodeTimeoutMs == 0L) {
            if (llmProperties != null && llmProperties.getOpenai() != null && 
                llmProperties.getOpenai().getTimeoutMs() != null) {
                maxNodeTimeoutMs = llmProperties.getOpenai().getTimeoutMs();
            } else {
                maxNodeTimeoutMs = 1500L; // Default fallback
            }
        }
        
        // Add safety margin (2000ms) but cap at 47000ms to allow for DeepSeek API response time
        long guardMs = Math.min(47000L, Math.max(500L, maxNodeTimeoutMs + 2000L));
        
        log.trace("Chain timeout calculation: maxNodeTimeout={}ms, guardTimeout={}ms", 
                maxNodeTimeoutMs, guardMs);
        
        return guardMs;
    }
    
    /**
     * Create request context for LLM routing and user profiling (simplified).
     */
    private RequestContext createRequestContext(Long userId, LearningObjective objective, 
                                              List<String> targetDomains, DifficultyPreference difficulty, 
                                              Integer timeboxMinutes, boolean forceRefresh, String abGroup) {
        RequestContext context = new RequestContext();
        context.setUserId(userId);
        context.setTier("FREE"); // Default tier
        context.setAbGroup(abGroup != null && !abGroup.trim().isEmpty() ? abGroup.trim() : "A"); // Use provided abGroup or default
        context.setRoute("ai-recommendations");
        context.setTraceId(java.util.UUID.randomUUID().toString());
        
        // Set intelligent recommendation fields
        context.setObjective(objective);
        context.setTargetDomains(targetDomains);
        context.setDesiredDifficulty(difficulty);
        context.setTimeboxMinutes(timeboxMinutes);
        context.setPromptVersion("v1");
        context.setUserProfileSummary("simplified_profile");
        context.setForceRefresh(forceRefresh);
        
        return context;
    }
    
    /**
     * Create successful AI recommendation response with complete metadata.
     */
    private AIRecommendationResponse createSuccessResponse(List<RecommendationItemDTO> recommendations, 
                                                         RequestContext context, String chainId, List<String> chainHops, String finalProvider) {
        AIRecommendationResponse response = new AIRecommendationResponse();
        response.setItems(recommendations);
        
        AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
        meta.setTraceId(context.getTraceId());
        meta.setGeneratedAt(Instant.now());
        meta.setCached(false);
        meta.setBusy(false);
        meta.setStrategy(finalProvider != null ? finalProvider : "llm");
        meta.setUserProfileSummary(context.getUserProfileSummary());
        
        // Set chain metadata for observability
        meta.setChainId(chainId);
        meta.setChainHops(chainHops);
        meta.setFinalProvider(finalProvider);
        
        response.setMeta(meta);
        
        return response;
    }
    
    /**
     * Create successful AI recommendation response (legacy overload for compatibility).
     */
    private AIRecommendationResponse createSuccessResponse(List<RecommendationItemDTO> recommendations, 
                                                         RequestContext context, String strategy) {
        return createSuccessResponse(recommendations, context, "default", new ArrayList<>(), strategy);
    }
    
    /**
     * Create fallback response with meaningful recommendations instead of empty list.
     */
    private AIRecommendationResponse createFallbackResponse(Long userId, int limit, String reason, RequestContext context) {
        List<RecommendationItemDTO> fallbackItems = new ArrayList<>();
        
        try {
            // Use real FSRS recommendations as fallback
            List<com.codetop.mapper.FSRSCardMapper.ReviewQueueCard> fsrsCards = fsrsService.getDueCards(userId, limit);
            
            if (fsrsCards != null && !fsrsCards.isEmpty()) {
                for (com.codetop.mapper.FSRSCardMapper.ReviewQueueCard fsrsCard : fsrsCards) {
                    RecommendationItemDTO item = new RecommendationItemDTO();
                    item.setProblemId(fsrsCard.getProblemId());
                    item.setTitle(fsrsCard.getProblemTitle() != null ? fsrsCard.getProblemTitle() : "Problem " + fsrsCard.getProblemId());
                    item.setDifficulty(fsrsCard.getProblemDifficulty() != null ? fsrsCard.getProblemDifficulty().toLowerCase() : "medium");
                    item.setEstimatedTime(30); // Default estimated time since FSRS doesn't track this
                    
                    // Calculate FSRS-based scores
                    double urgency = calculateFsrsUrgency(fsrsCard);
                    item.setScore(urgency);
                    item.setConfidence(0.85); // FSRS has high confidence for due items
                    
                    // Build FSRS-specific reason
                    String fsrsReason = buildFsrsReason(fsrsCard, reason);
                    item.setReason(fsrsReason);
                    item.setSource("FSRS");
                    
                    fallbackItems.add(item);
                }
                
                // Enrich FSRS fallback items with real problem data
                enrichRecommendationsWithProblemData(fallbackItems);
            }
        } catch (Exception e) {
            log.warn("FSRS fallback failed for userId={}: {}", userId, e.getMessage());
        }
        
        // If FSRS fallback also fails, return empty list with appropriate metadata
        if (fallbackItems.isEmpty()) {
            log.warn("Both AI and FSRS fallback failed for userId={}, returning empty recommendations", userId);
        }
        
        AIRecommendationResponse response = new AIRecommendationResponse();
        response.setItems(fallbackItems);
        
        AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
        meta.setTraceId(context != null ? context.getTraceId() : java.util.UUID.randomUUID().toString());
        meta.setGeneratedAt(Instant.now());
        meta.setCached(false);
        meta.setBusy(false);
        meta.setStrategy("fsrs_fallback");
        meta.setFallbackReason(reason);
        meta.setUserProfileSummary(context != null ? context.getUserProfileSummary() : "unknown");
        
        // Set chain metadata from context if available
        if (context != null) {
            meta.setChainId(context.getChainId());
            meta.setChainHops(context.getChainHops());
        }
        
        response.setMeta(meta);
        
        return response;
    }
    
    /**
     * Calculate FSRS urgency score based on card state and overdue days.
     */
    private double calculateFsrsUrgency(com.codetop.mapper.FSRSCardMapper.ReviewQueueCard fsrsCard) {
        double urgency = 0.5; // Base urgency
        
        // Factor in stability (lower stability = higher urgency)
        if (fsrsCard.getStability() != null) {
            double stability = fsrsCard.getStability().doubleValue();
            double retention = 1.0 / (1.0 + Math.exp(-stability));
            urgency += (1.0 - retention) * 0.3; // Boost urgency for lower retention
        }
        
        // Factor in days overdue
        if (fsrsCard.getNextReview() != null) {
            java.time.LocalDateTime nextReview = fsrsCard.getNextReview();
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(nextReview, now);
            if (daysDiff > 0) {
                urgency += Math.min(0.4, daysDiff * 0.05); // Add urgency for overdue items
            }
        }
        
        return Math.min(1.0, urgency);
    }
    
    /**
     * Build FSRS-specific reason explaining why this problem was recommended.
     */
    private String buildFsrsReason(com.codetop.mapper.FSRSCardMapper.ReviewQueueCard fsrsCard, String fallbackContext) {
        StringBuilder reason = new StringBuilder("FSRS fallback: ");
        
        // Add urgency explanation
        double urgency = calculateFsrsUrgency(fsrsCard);
        reason.append("urgency ").append(String.format("%.2f", urgency));
        
        // Add review history context
        if (fsrsCard.getReviewCount() != null && fsrsCard.getReviewCount() > 0) {
            // Estimate accuracy from review count and current state
            double estimatedAccuracy = Math.min(0.95, 0.4 + (fsrsCard.getReviewCount() * 0.1));
            reason.append(", ").append(String.format("%.0f%%", estimatedAccuracy * 100)).append(" accuracy");
        }
        
        // Add overdue information
        if (fsrsCard.getNextReview() != null) {
            java.time.LocalDateTime nextReview = fsrsCard.getNextReview();
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(nextReview, now);
            if (daysDiff > 0) {
                reason.append(", ").append(daysDiff).append(" days overdue");
            } else if (daysDiff == 0) {
                reason.append(", due today");
            }
        }
        
        // Add fallback context
        if (fallbackContext != null && !fallbackContext.isEmpty()) {
            reason.append(" (").append(fallbackContext).append(")");
        }
        
        return reason.toString();
    }
    
    /**
     * Enrich recommendation items with real problem data (titles, difficulties).
     * Performs batch lookup to avoid N+1 queries.
     */
    private void enrichRecommendationsWithProblemData(List<RecommendationItemDTO> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return;
        }
        
        try {
            // Deduplicate recommendations by problemId while preserving order
            Map<Long, RecommendationItemDTO> seenProblems = new LinkedHashMap<>();
            int duplicateCount = 0;
            
            for (RecommendationItemDTO item : recommendations) {
                Long problemId = item.getProblemId();
                if (problemId != null) {
                    if (seenProblems.containsKey(problemId)) {
                        duplicateCount++;
                        log.debug("Duplicate problem ID {} found in recommendations, keeping first occurrence", problemId);
                    } else {
                        seenProblems.put(problemId, item);
                    }
                }
            }
            
            if (duplicateCount > 0) {
                log.warn("Removed {} duplicate problems from recommendations", duplicateCount);
            }
            
            // Replace original list with deduplicated items
            List<RecommendationItemDTO> deduplicatedList = new ArrayList<>(seenProblems.values());
            
            // Collect all problem IDs for batch fetch
            List<Long> problemIds = deduplicatedList.stream()
                    .map(RecommendationItemDTO::getProblemId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            if (problemIds.isEmpty()) {
                recommendations.clear();
                return;
            }
            
            // Batch fetch all problems to avoid N+1 queries
            List<Problem> problems = problemMapper.selectBatchIds(problemIds);
            
            // Create a map for O(1) lookup
            Map<Long, Problem> problemMap = problems.stream()
                    .collect(Collectors.toMap(Problem::getId, problem -> problem));
            
            // Filter out non-existent problems and enrich existing ones
            List<RecommendationItemDTO> filteredRecommendations = new ArrayList<>();
            int filteredCount = 0;
            
            for (RecommendationItemDTO item : deduplicatedList) {
                Problem problem = problemMap.get(item.getProblemId());
                if (problem != null) {
                    // Set real title instead of placeholder
                    if (problem.getTitle() != null && !problem.getTitle().trim().isEmpty()) {
                        item.setTitle(problem.getTitle());
                    }
                    
                    // Set real difficulty and validate consistency
                    if (problem.getDifficulty() != null) {
                        String frontendDifficulty = problem.getDifficulty().name().toLowerCase();
                        String originalDifficulty = item.getDifficulty();
                        
                        // Validate difficulty consistency (warn if mismatch)
                        if (originalDifficulty != null && !originalDifficulty.equalsIgnoreCase(frontendDifficulty)) {
                            log.warn("Difficulty mismatch for problem {}: backend={}, frontend={}, using backend value", 
                                    item.getProblemId(), frontendDifficulty, originalDifficulty);
                        }
                        
                        item.setDifficulty(frontendDifficulty);
                    }
                    
                    // Optionally set estimated time based on difficulty
                    if (problem.getDifficulty() != null) {
                        switch (problem.getDifficulty()) {
                            case EASY:
                                item.setEstimatedTime(20); // 20 minutes for easy problems
                                break;
                            case MEDIUM:
                                item.setEstimatedTime(30); // 30 minutes for medium problems
                                break;
                            case HARD:
                                item.setEstimatedTime(45); // 45 minutes for hard problems
                                break;
                            default:
                                item.setEstimatedTime(30); // Default 30 minutes
                                break;
                        }
                    }
                    
                    filteredRecommendations.add(item);
                } else {
                    filteredCount++;
                    log.warn("Problem with ID {} not found, filtering from recommendations", item.getProblemId());
                }
            }
            
            // Replace the original list with filtered and enriched results
            recommendations.clear();
            recommendations.addAll(filteredRecommendations);
            
            if (filteredCount > 0) {
                log.warn("Filtered {} non-existent problems from recommendations", filteredCount);
            }
            
            log.debug("Processed {} recommendations: {} duplicates removed, {} non-existent filtered, {} final results", 
                    seenProblems.size() + duplicateCount, duplicateCount, filteredCount, recommendations.size());
            
        } catch (Exception e) {
            log.warn("Failed to enrich recommendations with problem data: {}", e.getMessage());
            // Don't fail the entire operation if enrichment fails - just log and continue
        }
    }
}
