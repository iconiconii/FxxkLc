package com.codetop.recommendation.chain;

import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.provider.impl.DefaultProvider;
import com.codetop.recommendation.service.RequestContext;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProviderChainTest {

    private LlmProperties props;
    private RateLimiterRegistry rateLimiterRegistry;
    private RetryRegistry retryRegistry;

    @BeforeEach
    void setUp() {
        props = new LlmProperties();
        props.setEnabled(true);
        LlmProperties.Chain chain = new LlmProperties.Chain();
        chain.setDefaultProvider(new LlmProperties.DefaultProvider());
        props.setChain(chain);

        rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        // Configure a permissive global limiter for most tests
        RateLimiterConfig generous = RateLimiterConfig.custom()
                .limitForPeriod(1000)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ZERO)
                .build();
        rateLimiterRegistry.rateLimiter("llm-global", generous);

        RetryConfig rcfg = RetryConfig.custom()
                .maxAttempts(2) // 1 retry
                .waitDuration(Duration.ofMillis(10))
                .build();
        retryRegistry = RetryRegistry.of(rcfg);
    }

    @Test
    void shouldRecoverWithRetryOnTransientFailure() {
        // chain: [flaky]
        LlmProperties.Node node = new LlmProperties.Node();
        node.setName("flaky");
        node.setEnabled(true);
        props.getChain().setNodes(List.of(node));

        FlakyProvider flaky = new FlakyProvider();
        DefaultProvider defaultProvider = new DefaultProvider(props.getChain().getDefaultProvider());
        ProviderChain chain = new ProviderChain(List.of(flaky), props, defaultProvider, rateLimiterRegistry, retryRegistry);

        RequestContext ctx = new RequestContext();
        ctx.setUserId(1L);
        LlmProvider.PromptOptions options = new LlmProvider.PromptOptions();
        options.limit = 3;

        ProviderChain.Result result = chain.execute(ctx, new ArrayList<>(), options);
        assertTrue(result.success, "Should succeed via retry");
        assertEquals("flaky", result.result.provider);
        assertEquals(2, flaky.attempts, "Flaky provider should be called twice (retry)");
        assertEquals(List.of("flaky"), result.hops);
        assertNotNull(result.result.items);
        assertEquals(3, result.result.items.size());
    }

    @Test
    void perUserRateLimitShouldFallbackToDefault() {
        // chain: [ok]
        LlmProperties.Node node = new LlmProperties.Node();
        node.setName("ok");
        node.setEnabled(true);
        props.getChain().setNodes(List.of(node));

        AlwaysSuccessProvider ok = new AlwaysSuccessProvider("ok");
        DefaultProvider defaultProvider = new DefaultProvider(props.getChain().getDefaultProvider());
        ProviderChain chain = new ProviderChain(List.of(ok), props, defaultProvider, rateLimiterRegistry, retryRegistry);

        // user 100: first call allowed
        RequestContext ctx = new RequestContext();
        ctx.setUserId(100L);
        LlmProvider.PromptOptions options = new LlmProvider.PromptOptions();
        options.limit = 1;

        ProviderChain.Result first = chain.execute(ctx, List.of(), options);
        assertTrue(first.success, "First call should pass per-user rate limiter");

        // user 100: second call immediately should be rate-limited (per-user limiter is 1/s)
        ProviderChain.Result second = chain.execute(ctx, List.of(), options);
        assertFalse(second.success, "Second call should be rate-limited and fallback to default");
        assertTrue(second.hops.contains("default"), "Should hit default provider when limited");

        // user 101: different user should not be blocked by per-user limiter of user 100
        RequestContext ctx2 = new RequestContext();
        ctx2.setUserId(101L);
        ProviderChain.Result third = chain.execute(ctx2, List.of(), options);
        assertTrue(third.success, "Different user should be allowed");
    }

    @Test
    void allProvidersFailShouldUseDefault() {
        // chain: [failer]
        LlmProperties.Node node = new LlmProperties.Node();
        node.setName("failer");
        node.setEnabled(true);
        props.getChain().setNodes(List.of(node));

        AlwaysFailProvider failer = new AlwaysFailProvider("failer");
        DefaultProvider defaultProvider = new DefaultProvider(props.getChain().getDefaultProvider());
        ProviderChain chain = new ProviderChain(List.of(failer), props, defaultProvider, rateLimiterRegistry, retryRegistry);

        RequestContext ctx = new RequestContext();
        ctx.setUserId(1L);
        ProviderChain.Result res = chain.execute(ctx, List.of(), new LlmProvider.PromptOptions());
        assertFalse(res.success);
        assertEquals(Arrays.asList("failer", "default"), res.hops);
    }

    // --- test providers ---

    static class FlakyProvider implements LlmProvider {
        int attempts = 0;
        @Override
        public String name() { return "flaky"; }
        @Override
        public LlmResult rankCandidates(RequestContext ctx, List<ProblemCandidate> candidates, PromptOptions options) {
            attempts++;
            if (attempts == 1) {
                throw new RuntimeException("transient");
            }
            LlmResult out = new LlmResult();
            out.success = true;
            out.provider = name();
            out.model = "mock";
            out.items = new ArrayList<>();
            for (int i = 0; i < options.limit; i++) {
                RankedItem r = new RankedItem();
                r.problemId = (long) (i + 1);
                r.reason = "r" + i;
                r.confidence = 0.9;
                r.score = 0.9;
                r.strategy = "retry";
                out.items.add(r);
            }
            return out;
        }
    }

    static class AlwaysSuccessProvider implements LlmProvider {
        private final String name;
        AlwaysSuccessProvider(String name) { this.name = name; }
        @Override
        public String name() { return name; }
        @Override
        public LlmResult rankCandidates(RequestContext ctx, List<ProblemCandidate> candidates, PromptOptions options) {
            LlmResult out = new LlmResult();
            out.success = true;
            out.provider = name();
            out.model = "mock";
            out.items = new ArrayList<>();
            for (int i = 0; i < options.limit; i++) {
                RankedItem r = new RankedItem();
                r.problemId = (long) (i + 1);
                r.reason = "ok";
                r.confidence = 0.8;
                r.score = 0.8;
                r.strategy = "ok";
                out.items.add(r);
            }
            return out;
        }
    }

    static class AlwaysFailProvider implements LlmProvider {
        private final String name;
        AlwaysFailProvider(String name) { this.name = name; }
        @Override
        public String name() { return name; }
        @Override
        public LlmResult rankCandidates(RequestContext ctx, List<ProblemCandidate> candidates, PromptOptions options) {
            LlmResult out = new LlmResult();
            out.success = false;
            out.provider = name();
            out.model = "mock";
            out.items = List.of();
            out.error = "fail";
            return out;
        }
    }
}

