package com.codetop.recommendation.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

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
}