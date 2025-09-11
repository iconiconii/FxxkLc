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
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    return execute(ctx, candidates, options, properties.getChain());
}

public Result execute(RequestContext ctx, List<LlmProvider.ProblemCandidate> candidates, LlmProvider.PromptOptions options, LlmProperties.Chain selectedChain) {
    List<String> hops = new ArrayList<>();
    if (!properties.isEnabled()) {
        return Result.defaulted("llm_disabled", hops);
    }

    LlmProperties.Chain chainToUse = selectedChain != null ? selectedChain : properties.getChain();
    if (chainToUse == null || chainToUse.getNodes() == null || chainToUse.getNodes().isEmpty()) {
        return Result.defaulted("chain_empty", hops);
    }

    // Prepare resilience components
    RateLimiter globalLimiter = getOrCreateGlobalLimiter();
    RateLimiter perUserLimiter = getOrCreatePerUserLimiter(ctx);

    for (LlmProperties.Node node : chainToUse.getNodes()) {
        if (!node.isEnabled()) continue;
        LlmProvider provider = catalog.getOrDefault(node.getName(), null);
        if (provider == null) continue;
        hops.add(provider.name());

        // Apply node-specific retry configuration, fallback to global retry
        Retry nodeRetry = getNodeRetry(node);
        if (nodeRetry == null && retryRegistry != null) {
            try {
                nodeRetry = retryRegistry.retry("llm-retry");
            } catch (Exception e) {
                // Ignore if no global retry configured
            }
        }
        
        // Apply node-specific rate limiting if configured
        RateLimiter nodeGlobalLimiter = getNodeGlobalLimiter(node, globalLimiter);
        RateLimiter nodePerUserLimiter = getNodePerUserLimiter(node, ctx, perUserLimiter);

        Supplier<LlmProvider.LlmResult> call = () -> provider.rankCandidates(ctx, candidates, options);
        
        // Apply decorations in order: timeout -> retry -> rate limiting
        if (nodeRetry != null) call = io.github.resilience4j.retry.Retry.decorateSupplier(nodeRetry, call);
        if (nodePerUserLimiter != null) call = io.github.resilience4j.ratelimiter.RateLimiter.decorateSupplier(nodePerUserLimiter, call);
        if (nodeGlobalLimiter != null) call = io.github.resilience4j.ratelimiter.RateLimiter.decorateSupplier(nodeGlobalLimiter, call);

        try {
            LlmProvider.LlmResult res = call.get();
            if (res != null && res.success) {
                return Result.success(res, hops);
            }
            // Check if error should trigger fallback to next node
            if (res != null && !shouldFallbackToNext(node, res.error)) {
                // This error type should not fallback - return default result
                hops.add("default");
                LlmProvider.LlmResult def = defaultProvider.rankCandidates(ctx, candidates, options);
                return Result.defaulted(res.error, hops, def != null ? def.error : null);
            }
        } catch (Exception ex) {
            String errorType = classifyException((Throwable)ex);
            if (!shouldFallbackToNext(node, errorType)) {
                // This error type should not fallback - return default result
                hops.add("default");
                LlmProvider.LlmResult def = defaultProvider.rankCandidates(ctx, candidates, options);
                return Result.defaulted(errorType, hops, def != null ? def.error : null);
            }
            // Continue to next node for fallback errors
        }
    }
    
    // All nodes failed or no nodes configured - use default provider
    hops.add("default");
    LlmProvider.LlmResult def = defaultProvider.rankCandidates(ctx, candidates, options);
    return Result.defaulted("all_nodes_failed", hops, def != null ? def.error : null);
}

    public java.util.concurrent.CompletableFuture<Result> executeAsync(RequestContext ctx,
                                                               List<LlmProvider.ProblemCandidate> candidates,
                                                               LlmProvider.PromptOptions options) {
    return executeAsync(ctx, candidates, options, properties.getChain());
}

public java.util.concurrent.CompletableFuture<Result> executeAsync(RequestContext ctx,
                                                               List<LlmProvider.ProblemCandidate> candidates,
                                                               LlmProvider.PromptOptions options,
                                                               LlmProperties.Chain selectedChain) {
    List<String> hops = new ArrayList<>();
    if (!properties.isEnabled()) {
        return java.util.concurrent.CompletableFuture.completedFuture(Result.defaulted("llm_disabled", hops));
    }

    LlmProperties.Chain chainToUse = selectedChain != null ? selectedChain : properties.getChain();
    if (chainToUse == null || chainToUse.getNodes() == null || chainToUse.getNodes().isEmpty()) {
        return java.util.concurrent.CompletableFuture.completedFuture(Result.defaulted("chain_empty", hops));
    }

    java.util.List<LlmProperties.Node> nodes = chainToUse.getNodes();
    // Prepare resilience components for async execution as well
    RateLimiter globalLimiter = getOrCreateGlobalLimiter();
    RateLimiter perUserLimiter = getOrCreatePerUserLimiter(ctx);

    java.util.function.Function<Integer, java.util.concurrent.CompletableFuture<Result>> attempt = new java.util.function.Function<>() {
        @Override
        public java.util.concurrent.CompletableFuture<Result> apply(Integer idx) {
            if (idx >= nodes.size()) {
                hops.add("default");
                LlmProvider.LlmResult def = defaultProvider.rankCandidates(ctx, candidates, options);
                return java.util.concurrent.CompletableFuture.completedFuture(Result.defaulted("all_nodes_failed", hops, def != null ? def.error : null));
            }
            
            LlmProperties.Node node = nodes.get(idx);
            if (!node.isEnabled()) {
                return this.apply(idx + 1);
            }
            
            LlmProvider provider = catalog.getOrDefault(node.getName(), null);
            if (provider == null) {
                return this.apply(idx + 1);
            }
            
            hops.add(provider.name());
            
            // Resolve node-specific rate limiters
            RateLimiter nodeGlobalLimiter = getNodeGlobalLimiter(node, globalLimiter);
            RateLimiter nodePerUserLimiter = getNodePerUserLimiter(node, ctx, perUserLimiter);

            // Build async supplier and decorate with rate limiters if present
            java.util.function.Supplier<java.util.concurrent.CompletionStage<LlmProvider.LlmResult>> supplier =
                    () -> provider.rankCandidatesAsync(ctx, candidates, options);
            if (nodePerUserLimiter != null) {
                supplier = io.github.resilience4j.ratelimiter.RateLimiter.decorateCompletionStage(nodePerUserLimiter, supplier);
            }
            if (nodeGlobalLimiter != null) {
                supplier = io.github.resilience4j.ratelimiter.RateLimiter.decorateCompletionStage(nodeGlobalLimiter, supplier);
            }
            java.util.concurrent.CompletableFuture<LlmProvider.LlmResult> providerCall;
            
            // Apply node-specific retry logic using Resilience4j (before timeout)
            if (node.getRetry() != null && node.getRetry().getAttempts() > 0) {
                final var finalSupplier = supplier; // Create final copy for lambda
                providerCall = applyAsyncRetry(() -> finalSupplier.get().toCompletableFuture(), node.getRetry().getAttempts(), node.getName());
            } else {
                providerCall = supplier.get().toCompletableFuture();
            }
            
            // Apply node-specific timeout
            if (node.getTimeoutMs() != null && node.getTimeoutMs() > 0) {
                providerCall = providerCall.orTimeout(node.getTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
            }
            
            return providerCall.handle((r, ex) -> {
                if (ex != null) {
                    Throwable cause = ex instanceof java.util.concurrent.CompletionException && ex.getCause() != null ? ex.getCause() : ex;
                    String errorType = classifyException(cause);
                    if (!shouldFallbackToNext(node, errorType)) {
                        // This error type should not fallback - return default result
                        hops.add("default");
                        LlmProvider.LlmResult def = defaultProvider.rankCandidates(ctx, candidates, options);
                        return Result.defaulted(errorType, hops, def != null ? def.error : null);
                    }
                    // Continue to next node for fallback errors
                    return null;
                }
                
                if (r != null && r.success) {
                    return Result.success(r, hops);
                }
                
                // Check if error should trigger fallback to next node
                if (r != null && !shouldFallbackToNext(node, r.error)) {
                    // This error type should not fallback - return default result
                    hops.add("default");
                    LlmProvider.LlmResult def = defaultProvider.rankCandidates(ctx, candidates, options);
                    return Result.defaulted(r.error, hops, def != null ? def.error : null);
                }
                
                // Continue to next node
                return null;
            }).thenCompose(result -> {
                if (result != null) {
                    return java.util.concurrent.CompletableFuture.completedFuture(result);
                }
                return this.apply(idx + 1);
            });
        }
    };

    return attempt.apply(0);
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

    private Retry getNodeRetry(LlmProperties.Node node) {
        if (retryRegistry == null || node.getRetry() == null || node.getRetry().getAttempts() <= 0) {
            return null;
        }
        
        String retryName = "llm-node-" + node.getName();
        try {
            return retryRegistry.retry(retryName, io.github.resilience4j.retry.RetryConfig.custom()
                    .maxAttempts(node.getRetry().getAttempts() + 1) // Resilience4j counts initial attempt
                    .waitDuration(Duration.ofMillis(150))
                    .build());
        } catch (Exception e) {
            return null;
        }
    }

    private RateLimiter getNodeGlobalLimiter(LlmProperties.Node node, RateLimiter defaultLimiter) {
        if (node.getRateLimit() == null) {
            return defaultLimiter;
        }
        
        if (rateLimiterRegistry == null) return defaultLimiter;
        
        String limiterName = "llm-node-global-" + node.getName();
        try {
            return rateLimiterRegistry.rateLimiter(limiterName,
                    RateLimiterConfig.custom()
                            .timeoutDuration(Duration.ZERO)
                            .limitRefreshPeriod(Duration.ofSeconds(1))
                            .limitForPeriod(node.getRateLimit().getRps())
                            .build());
        } catch (Exception e) {
            return defaultLimiter;
        }
    }

    private RateLimiter getNodePerUserLimiter(LlmProperties.Node node, RequestContext ctx, RateLimiter defaultLimiter) {
        if (node.getRateLimit() == null || ctx == null || ctx.getUserId() == null) {
            return defaultLimiter;
        }
        
        if (rateLimiterRegistry == null) return defaultLimiter;
        
        String limiterName = "llm-node-user-" + node.getName() + "-" + ctx.getUserId();
        try {
            return rateLimiterRegistry.rateLimiter(limiterName,
                    RateLimiterConfig.custom()
                            .timeoutDuration(Duration.ZERO)
                            .limitRefreshPeriod(Duration.ofSeconds(1))
                            .limitForPeriod(node.getRateLimit().getPerUserRps())
                            .build());
        } catch (Exception e) {
            return defaultLimiter;
        }
    }

    private boolean shouldFallbackToNext(LlmProperties.Node node, String errorType) {
        if (node.getOnErrorsToNext() == null || node.getOnErrorsToNext().isEmpty()) {
            // Default behavior: fallback on common retry-able errors
            return isRetryableError(errorType);
        }
        return node.getOnErrorsToNext().contains(errorType);
    }

    private boolean isRetryableError(String errorType) {
        if (errorType == null) return true;
        return errorType.contains("TIMEOUT") || 
               errorType.contains("HTTP_429") || 
               errorType.contains("HTTP_5") ||  // 5xx errors
               errorType.contains("CIRCUIT_OPEN") ||
               errorType.contains("PARSING_ERROR");
    }

    private String classifyException(Throwable ex) {
        if (ex == null) return "UNKNOWN_ERROR";
        
        String className = ex.getClass().getSimpleName();
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        
        if (className.contains("RequestNotPermitted") || message.contains("request not permitted")) {
            return "HTTP_429";
        }
        if (className.contains("Timeout") || message.contains("timeout")) {
            return "TIMEOUT";
        }
        if (className.contains("CircuitBreaker") || message.contains("circuit")) {
            return "CIRCUIT_OPEN";
        }
        if (className.contains("RateLimiter") || message.contains("rate limit")) {
            return "HTTP_429";
        }
        if (message.contains("http")) {
            if (message.contains("429")) return "HTTP_429";
            if (message.contains("5")) return "HTTP_5XX";
            if (message.contains("4")) return "HTTP_4XX";
        }
        
        return className.toUpperCase();
    }

    /**
     * Applies Resilience4j async retry to a provider call supplier.
     * This allows proper retry by reconstructing the call instead of trying to retry a completed future.
     */
    private java.util.concurrent.CompletableFuture<LlmProvider.LlmResult> applyAsyncRetry(
            Supplier<java.util.concurrent.CompletableFuture<LlmProvider.LlmResult>> supplier, 
            int maxAttempts, 
            String nodeName) {
        
        if (maxAttempts <= 0) {
            return supplier.get();
        }
        
        // Create a retry configuration for this node
        String retryName = "llm-node-" + nodeName;
        Retry retry = retryRegistry.retry(retryName, io.github.resilience4j.retry.RetryConfig.custom()
            .maxAttempts(maxAttempts)
            .waitDuration(Duration.ofMillis(100)) // Short wait between retries for LLM calls
            .retryOnResult(result -> {
                if (result == null) return true;
                LlmProvider.LlmResult llmResult = (LlmProvider.LlmResult) result;
                return !llmResult.success;
            }) // Retry on unsuccessful results
            .retryOnException(throwable -> {
                // Retry on timeouts, network issues, but not on authentication errors
                if (throwable instanceof java.util.concurrent.TimeoutException) {
                    return true;
                }
                if (throwable instanceof java.io.IOException) {
                    return true;
                }
                if (throwable instanceof java.net.ConnectException) {
                    return true;
                }
                // Don't retry on authentication/authorization errors (4xx responses)
                return false;
            })
            .build());
        
        // Use the simpler approach with decorateSupplier since we're working with CompletableFuture
        Supplier<LlmProvider.LlmResult> syncSupplier = () -> {
            try {
                return supplier.get().join(); // Convert async to sync for retry
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        
        Supplier<LlmProvider.LlmResult> retryableSupplier = Retry.decorateSupplier(retry, syncSupplier);
        
        // Convert back to CompletableFuture
        return java.util.concurrent.CompletableFuture.supplyAsync(retryableSupplier);
    }

    public static class Result {
    public boolean success;
    public LlmProvider.LlmResult result;
    public List<String> hops;
    public String defaultReason;
    public String fallbackDetail; // Additional detail for fallback scenarios

    static Result success(LlmProvider.LlmResult r, List<String> hops) {
        Result out = new Result();
        out.success = true;
        out.result = r;
        out.hops = hops;
        return out;
    }

    static Result defaulted(String reason, List<String> hops) {
        return defaulted(reason, hops, null);
    }

    static Result defaulted(String reason, List<String> hops, String fallbackDetail) {
        Result out = new Result();
        out.success = false;
        out.defaultReason = reason;
        out.hops = hops;
        out.fallbackDetail = fallbackDetail;
        return out;
    }
}
}
