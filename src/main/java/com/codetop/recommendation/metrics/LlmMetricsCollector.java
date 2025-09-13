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
}