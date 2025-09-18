package com.codetop.recommendation.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Metrics collector for LLM recommendation system.
 * Provides observability into chain routing, toggle decisions, and fallback reasons.
 */
@Component
public class LlmMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    public LlmMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * Record LLM toggle decision.
     */
    public void recordToggleDecision(String tier, String abGroup, String route, boolean enabled, String disabledReason) {
        Tags tags = Tags.of(
            "tier", tier != null ? tier : "unknown",
            "ab_group", abGroup != null ? abGroup : "unknown", 
            "route", route != null ? route : "unknown",
            "enabled", String.valueOf(enabled)
        );
        
        if (!enabled && disabledReason != null) {
            tags = tags.and("disabled_reason", disabledReason);
        }
        
        Counter.builder("llm.toggle.decision")
            .description("LLM feature toggle decisions")
            .tags(tags)
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Record chain selection.
     */
    public void recordChainSelection(String chainId, String tier, String abGroup, String route) {
        Counter.builder("llm.routing.chain_selected")
            .description("LLM chain selections by routing rules")
            .tags(Tags.of(
                "chain_id", chainId != null ? chainId : "unknown",
                "tier", tier != null ? tier : "unknown",
                "ab_group", abGroup != null ? abGroup : "unknown",
                "route", route != null ? route : "unknown"
            ))
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Record provider chain execution results.
     */
    public void recordProviderChainResult(String chainId, int hopsCount, boolean success, String fallbackReason) {
        Tags tags = Tags.of(
            "chain_id", chainId != null ? chainId : "unknown",
            "hops_count", String.valueOf(hopsCount),
            "success", String.valueOf(success)
        );
        
        if (!success && fallbackReason != null) {
            tags = tags.and("fallback_reason", fallbackReason);
        }
        
        Counter.builder("llm.provider.execution")
            .description("LLM provider chain execution results")
            .tags(tags)
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Record fallback events.
     */
    public void recordFallback(String reason, String chainId, String strategy) {
        Counter.builder("llm.fallback.event")
            .description("LLM fallback events by reason")
            .tags(Tags.of(
                "reason", reason != null ? reason : "unknown",
                "chain_id", chainId != null ? chainId : "unknown",
                "strategy", strategy != null ? strategy : "unknown"
            ))
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Record cache hit/miss.
     */
    public void recordCacheAccess(String tier, String abGroup, String chainId, boolean hit) {
        Counter.builder("llm.cache.access")
            .description("LLM recommendation cache access")
            .tags(Tags.of(
                "tier", tier != null ? tier : "unknown",
                "ab_group", abGroup != null ? abGroup : "unknown",
                "chain_id", chainId != null ? chainId : "unknown",
                "result", hit ? "hit" : "miss"
            ))
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Record cache TTL information for AI recommendation keys.
     */
    public void recordCacheTtl(String keyspace, String keyType, Duration remainingTtl, Duration originalTtl) {
        // Record remaining TTL as a gauge
        Gauge.builder("llm.cache.ttl.remaining_seconds", remainingTtl, Duration::getSeconds)
            .description("Remaining TTL for AI recommendation cache keys")
            .tags(Tags.of(
                "keyspace", keyspace != null ? keyspace : "unknown",
                "key_type", keyType != null ? keyType : "unknown"
            ))
            .register(meterRegistry);
        
        // Record TTL utilization ratio (remaining/original)
        if (originalTtl != null && originalTtl.getSeconds() > 0) {
            double utilizationRatio = (double) remainingTtl.getSeconds() / originalTtl.getSeconds();
            Gauge.builder("llm.cache.ttl.utilization_ratio", () -> utilizationRatio)
                .description("TTL utilization ratio for AI cache keys (remaining/original)")
                .tags(Tags.of(
                    "keyspace", keyspace != null ? keyspace : "unknown",
                    "key_type", keyType != null ? keyType : "unknown"
                ))
                .register(meterRegistry);
        }
    }
    
    /**
     * Record cache warming effectiveness metrics.
     */
    public void recordCacheWarmingEffect(String keyspace, boolean warmed, boolean accessed, Duration timeToAccess) {
        // Record warming success
        Counter.builder("llm.cache.warming.effect")
            .description("Cache warming effectiveness for AI keys")
            .tags(Tags.of(
                "keyspace", keyspace != null ? keyspace : "unknown",
                "warmed", String.valueOf(warmed),
                "accessed", String.valueOf(accessed)
            ))
            .register(meterRegistry)
            .increment();
        
        // Record time between warming and access (if both occurred)
        if (warmed && accessed && timeToAccess != null) {
            Timer.builder("llm.cache.warming.time_to_access")
                .description("Time between cache warming and first access")
                .tags(Tags.of("keyspace", keyspace != null ? keyspace : "unknown"))
                .register(meterRegistry)
                .record(timeToAccess.toMillis(), TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Record cache keyspace statistics.
     */
    public void recordCacheKeyspaceStats(String keyspace, int totalKeys, int expiredKeys, double hitRate) {
        // Total keys in keyspace
        Gauge.builder("llm.cache.keyspace.total_keys", () -> (double) totalKeys)
            .description("Total number of keys in AI recommendation keyspace")
            .tags(Tags.of("keyspace", keyspace != null ? keyspace : "unknown"))
            .register(meterRegistry);
        
        // Expired keys
        Gauge.builder("llm.cache.keyspace.expired_keys", () -> (double) expiredKeys)
            .description("Number of expired keys in AI recommendation keyspace")
            .tags(Tags.of("keyspace", keyspace != null ? keyspace : "unknown"))
            .register(meterRegistry);
        
        // Hit rate for this keyspace
        Gauge.builder("llm.cache.keyspace.hit_rate", () -> hitRate)
            .description("Cache hit rate for AI recommendation keyspace")
            .tags(Tags.of("keyspace", keyspace != null ? keyspace : "unknown"))
            .register(meterRegistry);
    }
    
    /**
     * Record detailed cache access with key pattern analysis.
     */
    public void recordDetailedCacheAccess(String keyspace, String keyPattern, boolean hit, Duration responseTime) {
        // Enhanced cache access metrics with key pattern
        Counter.builder("llm.cache.detailed_access")
            .description("Detailed LLM cache access with key pattern analysis")
            .tags(Tags.of(
                "keyspace", keyspace != null ? keyspace : "unknown",
                "key_pattern", keyPattern != null ? keyPattern : "unknown",
                "result", hit ? "hit" : "miss"
            ))
            .register(meterRegistry)
            .increment();
        
        // Response time for cache operations
        if (responseTime != null) {
            Timer.builder("llm.cache.response_time")
                .description("Response time for cache operations")
                .tags(Tags.of(
                    "keyspace", keyspace != null ? keyspace : "unknown",
                    "result", hit ? "hit" : "miss"
                ))
                .register(meterRegistry)
                .record(responseTime.toNanos(), TimeUnit.NANOSECONDS);
        }
    }

    
    /**
     * Record LLM provider latency with percentile tracking (p50/p95).
     */
    public void recordLlmLatency(String provider, String model, String chainId, String abGroup, 
                               String route, long latencyMs, boolean success, String errorReason) {
        Tags tags = Tags.of(
            "provider", provider != null ? provider : "unknown",
            "model", model != null ? model : "unknown", 
            "chain_id", chainId != null ? chainId : "unknown",
            "ab_group", abGroup != null ? abGroup : "unknown",
            "route", route != null ? route : "unknown",
            "success", String.valueOf(success)
        );
        
        if (!success && errorReason != null) {
            tags = tags.and("error_reason", errorReason);
        }
        
        Timer.builder("llm.latency_ms")
            .description("LLM provider latency in milliseconds")
            .tags(tags)
            .register(meterRegistry)
            .record(latencyMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Record fallback ratio by provider and reason.
     */
    public void recordFallbackRatio(String provider, String model, String chainId, String reason) {
        // Track total requests
        Counter.builder("llm.requests.total")
            .description("Total LLM requests by provider")
            .tags(Tags.of(
                "provider", provider != null ? provider : "unknown",
                "model", model != null ? model : "unknown",
                "chain_id", chainId != null ? chainId : "unknown"
            ))
            .register(meterRegistry)
            .increment();
            
        // Track fallback requests  
        Counter.builder("llm.fallback.total")
            .description("Total LLM fallback events by provider and reason")
            .tags(Tags.of(
                "provider", provider != null ? provider : "unknown",
                "model", model != null ? model : "unknown", 
                "chain_id", chainId != null ? chainId : "unknown",
                "reason", reason != null ? reason : "unknown"
            ))
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Record cache hit ratio with enhanced granularity.
     */
    public void recordCacheHitRatio(String tier, String abGroup, String route, String chainId, boolean hit) {
        // Track total cache requests
        Counter.builder("rec.cache.requests.total")
            .description("Total recommendation cache requests")
            .tags(Tags.of(
                "tier", tier != null ? tier : "unknown",
                "ab_group", abGroup != null ? abGroup : "unknown",
                "route", route != null ? route : "unknown",
                "chain_id", chainId != null ? chainId : "unknown"
            ))
            .register(meterRegistry)
            .increment();
            
        // Track cache hits
        if (hit) {
            Counter.builder("rec.cache.hits.total")
                .description("Total recommendation cache hits")
                .tags(Tags.of(
                    "tier", tier != null ? tier : "unknown",
                    "ab_group", abGroup != null ? abGroup : "unknown",
                    "route", route != null ? route : "unknown",
                    "chain_id", chainId != null ? chainId : "unknown"
                ))
                .register(meterRegistry)
                .increment();
        }
    }
    
    /**
     * Record chain hop counts and routing decisions.
     */
    public void recordChainHops(String chainId, String tier, String abGroup, int hopCount, 
                               String finalProvider, String[] failedProviders, Duration totalLatency) {
        // Record chain hop count
        Timer.builder("llm.chain.hops")
            .description("Number of hops in LLM provider chain")
            .tags(Tags.of(
                "chain_id", chainId != null ? chainId : "unknown",
                "tier", tier != null ? tier : "unknown",
                "ab_group", abGroup != null ? abGroup : "unknown",
                "final_provider", finalProvider != null ? finalProvider : "none",
                "hop_count", String.valueOf(hopCount)
            ))
            .register(meterRegistry)
            .record(hopCount, TimeUnit.SECONDS); // Using seconds as unit for hop count
            
        // Record total chain latency 
        if (totalLatency != null) {
            Timer.builder("llm.chain.total_latency")
                .description("Total latency for complete provider chain execution")
                .tags(Tags.of(
                    "chain_id", chainId != null ? chainId : "unknown",
                    "hop_count", String.valueOf(hopCount),
                    "success", finalProvider != null ? "true" : "false"
                ))
                .register(meterRegistry)
                .record(totalLatency.toMillis(), TimeUnit.MILLISECONDS);
        }
        
        // Record failed providers in chain
        if (failedProviders != null) {
            for (String failedProvider : failedProviders) {
                Counter.builder("llm.chain.provider_failure")
                    .description("Provider failures in chain execution")
                    .tags(Tags.of(
                        "chain_id", chainId != null ? chainId : "unknown",
                        "provider", failedProvider != null ? failedProvider : "unknown"
                    ))
                    .register(meterRegistry)
                    .increment();
            }
        }
    }
    
    /**
     * Record detailed error reasons with dimensional breakdown.
     */
    public void recordErrorReasons(String provider, String model, String chainId, String errorCode, 
                                 String errorCategory, String httpStatus, Duration latency) {
        Tags tags = Tags.of(
            "provider", provider != null ? provider : "unknown",
            "model", model != null ? model : "unknown",
            "chain_id", chainId != null ? chainId : "unknown",
            "error_code", errorCode != null ? errorCode : "unknown",
            "error_category", errorCategory != null ? errorCategory : "unknown"
        );
        
        if (httpStatus != null) {
            tags = tags.and("http_status", httpStatus);
        }
        
        Counter.builder("llm.errors.total")
            .description("LLM errors by detailed reason codes")
            .tags(tags)
            .register(meterRegistry)
            .increment();
            
        // Record error latency (time to fail)
        if (latency != null) {
            Timer.builder("llm.errors.latency")
                .description("Time to failure for LLM requests")
                .tags(tags)
                .register(meterRegistry)
                .record(latency.toMillis(), TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Record recommendation quality metrics.
     */
    public void recordRecommendationQuality(String provider, String model, String chainId,
                                          int recommendationCount, double avgConfidence, 
                                          double avgScore, String userTier) {
        // Record recommendation count
        Gauge.builder("llm.recommendations.count", () -> (double) recommendationCount)
            .description("Number of recommendations returned")
            .tags(Tags.of(
                "provider", provider != null ? provider : "unknown",
                "model", model != null ? model : "unknown",
                "chain_id", chainId != null ? chainId : "unknown",
                "user_tier", userTier != null ? userTier : "unknown"
            ))
            .register(meterRegistry);
            
        // Record average confidence
        Gauge.builder("llm.recommendations.avg_confidence", () -> avgConfidence)
            .description("Average confidence score of recommendations")
            .tags(Tags.of(
                "provider", provider != null ? provider : "unknown",
                "model", model != null ? model : "unknown", 
                "chain_id", chainId != null ? chainId : "unknown"
            ))
            .register(meterRegistry);
            
        // Record average score
        Gauge.builder("llm.recommendations.avg_score", () -> avgScore)
            .description("Average recommendation score")
            .tags(Tags.of(
                "provider", provider != null ? provider : "unknown",
                "model", model != null ? model : "unknown",
                "chain_id", chainId != null ? chainId : "unknown"
            ))
            .register(meterRegistry);
    }
    
    /**
     * Record token usage and cost estimation metrics.
     */
    public void recordTokenUsage(String provider, String model, String chainId, 
                               int promptTokens, int completionTokens, int totalTokens, 
                               double estimatedCost) {
        Tags baseTags = Tags.of(
            "provider", provider != null ? provider : "unknown",
            "model", model != null ? model : "unknown",
            "chain_id", chainId != null ? chainId : "unknown"
        );
        
        // Record token counts
        Counter.builder("llm.tokens.prompt")
            .description("Prompt tokens consumed")
            .tags(baseTags)
            .register(meterRegistry)
            .increment(promptTokens);
            
        Counter.builder("llm.tokens.completion")
            .description("Completion tokens consumed")
            .tags(baseTags)
            .register(meterRegistry)
            .increment(completionTokens);
            
        Counter.builder("llm.tokens.total")
            .description("Total tokens consumed")
            .tags(baseTags)
            .register(meterRegistry)
            .increment(totalTokens);
            
        // Record estimated cost
        if (estimatedCost > 0) {
            Counter.builder("llm.cost.estimated_usd")
                .description("Estimated cost in USD")
                .tags(baseTags)
                .register(meterRegistry)
                .increment(estimatedCost);
        }
    }
}