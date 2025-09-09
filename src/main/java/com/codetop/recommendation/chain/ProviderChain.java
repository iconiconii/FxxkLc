package com.codetop.recommendation.chain;

import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.service.RequestContext;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ProviderChain {
    private final Map<String, LlmProvider> catalog = new HashMap<>();
    private final LlmProperties properties;
    private final LlmProvider defaultProvider;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final RetryRegistry retryRegistry;

    public ProviderChain(List<LlmProvider> providers,
                         LlmProperties properties,
                         LlmProvider defaultProvider,
                         RateLimiterRegistry rateLimiterRegistry,
                         RetryRegistry retryRegistry) {
        for (LlmProvider p : providers) {
            catalog.put(p.name(), p);
        }
        this.properties = properties;
        this.defaultProvider = defaultProvider;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.retryRegistry = retryRegistry;
    }

    public Result execute(RequestContext ctx, List<LlmProvider.ProblemCandidate> candidates, LlmProvider.PromptOptions options) {
        List<String> hops = new ArrayList<>();
        if (!properties.isEnabled()) {
            return Result.defaulted("llm_disabled", hops);
        }

        if (properties.getChain() == null || properties.getChain().getNodes() == null || properties.getChain().getNodes().isEmpty()) {
            return Result.defaulted("chain_empty", hops);
        }

        // Prepare resilience components
        RateLimiter globalLimiter = getOrCreateGlobalLimiter();
        RateLimiter perUserLimiter = getOrCreatePerUserLimiter(ctx);
        Retry retry = retryRegistry != null ? retryRegistry.retry("llm-retry") : null;

        for (LlmProperties.Node node : properties.getChain().getNodes()) {
            if (!node.isEnabled()) continue;
            LlmProvider provider = catalog.getOrDefault(node.getName(), null);
            if (provider == null) continue;
            hops.add(provider.name());

            Supplier<LlmProvider.LlmResult> call = () -> provider.rankCandidates(ctx, candidates, options);
            if (retry != null) call = io.github.resilience4j.retry.Retry.decorateSupplier(retry, call);
            if (perUserLimiter != null) call = io.github.resilience4j.ratelimiter.RateLimiter.decorateSupplier(perUserLimiter, call);
            if (globalLimiter != null) call = io.github.resilience4j.ratelimiter.RateLimiter.decorateSupplier(globalLimiter, call);

            try {
                LlmProvider.LlmResult res = call.get();
                if (res != null && res.success) {
                    return Result.success(res, hops);
                }
            } catch (Exception ex) {
                // suppressed, try next provider
            }
        }
        hops.add("default");
        LlmProvider.LlmResult def = defaultProvider.rankCandidates(ctx, candidates, options);
        return Result.defaulted(def != null ? def.error : "default", hops);
    }

    private RateLimiter getOrCreateGlobalLimiter() {
        if (rateLimiterRegistry == null) return null;
        try {
            return rateLimiterRegistry.rateLimiter("llm-global");
        } catch (Exception e) {
            return RateLimiter.of("llm-global-local", RateLimiterConfig.custom()
                    .timeoutDuration(Duration.ZERO)
                    .limitRefreshPeriod(Duration.ofSeconds(1))
                    .limitForPeriod(5)
                    .build());
        }
    }

    private RateLimiter getOrCreatePerUserLimiter(RequestContext ctx) {
        if (rateLimiterRegistry == null || ctx == null || ctx.getUserId() == null) return null;
        String name = "llm-user-" + ctx.getUserId();
        try {
            // create with a conservative default if not exists
            return rateLimiterRegistry.rateLimiter(name,
                    RateLimiterConfig.custom()
                            .timeoutDuration(Duration.ZERO)
                            .limitRefreshPeriod(Duration.ofSeconds(1))
                            .limitForPeriod(1)
                            .build());
        } catch (Exception e) {
            return null;
        }
    }

    public static class Result {
        public boolean success;
        public LlmProvider.LlmResult result;
        public List<String> hops;
        public String defaultReason;

        static Result success(LlmProvider.LlmResult r, List<String> hops) {
            Result out = new Result();
            out.success = true;
            out.result = r;
            out.hops = hops;
            return out;
        }

        static Result defaulted(String reason, List<String> hops) {
            Result out = new Result();
            out.success = false;
            out.defaultReason = reason;
            out.hops = hops;
            return out;
        }
    }
}
