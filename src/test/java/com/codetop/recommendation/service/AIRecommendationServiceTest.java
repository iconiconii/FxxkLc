package com.codetop.recommendation.service;

import com.codetop.recommendation.chain.ProviderChain;
import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.provider.impl.DefaultProvider;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AIRecommendationServiceTest {

    @Test
    void serviceReturnsItemsOnSuccess() {
        // Build provider chain with a success provider
        TestSuccessProvider provider = new TestSuccessProvider();
        ProviderChain chain = buildChain(List.of(provider), true);
        AIRecommendationService service = new AIRecommendationService(chain);

        var resp = service.getRecommendations(1L, 3);
        assertNotNull(resp);
        assertNotNull(resp.getMeta());
        assertFalse(resp.getMeta().isBusy());
        assertEquals("normal", resp.getMeta().getStrategy());
        assertNotNull(resp.getMeta().getTraceId());
        assertEquals(3, resp.getItems().size());
        assertEquals("LLM", resp.getItems().get(0).getSource());
    }

    @Test
    void serviceFallsBackToBusyWhenAllFail() {
        // Build provider chain with a failing provider
        TestFailProvider provider = new TestFailProvider();
        ProviderChain chain = buildChain(List.of(provider), true);
        AIRecommendationService service = new AIRecommendationService(chain);

        var resp = service.getRecommendations(1L, 5);
        assertNotNull(resp);
        assertTrue(resp.getMeta().isBusy());
        assertTrue(resp.getItems() == null || resp.getItems().isEmpty());
    }

    private ProviderChain buildChain(List<LlmProvider> providers, boolean enabled) {
        LlmProperties props = new LlmProperties();
        props.setEnabled(enabled);
        LlmProperties.Chain chainProps = new LlmProperties.Chain();
        List<LlmProperties.Node> nodes = new ArrayList<>();
        for (LlmProvider p : providers) {
            LlmProperties.Node n = new LlmProperties.Node();
            n.setName(p.name());
            n.setEnabled(true);
            nodes.add(n);
        }
        chainProps.setNodes(nodes);
        chainProps.setDefaultProvider(new LlmProperties.DefaultProvider());
        props.setChain(chainProps);

        RateLimiterRegistry rlr = RateLimiterRegistry.ofDefaults();
        rlr.rateLimiter("llm-global", RateLimiterConfig.custom()
                .limitForPeriod(1000)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ZERO)
                .build());
        RetryRegistry rr = RetryRegistry.of(RetryConfig.custom().maxAttempts(1).build());

        DefaultProvider defaultProvider = new DefaultProvider(chainProps.getDefaultProvider());
        return new ProviderChain(providers, props, defaultProvider, rlr, rr);
    }

    static class TestSuccessProvider implements LlmProvider {
        @Override
        public String name() { return "success"; }
        @Override
        public LlmResult rankCandidates(RequestContext ctx, List<ProblemCandidate> candidates, PromptOptions options) {
            LlmResult out = new LlmResult();
            out.success = true;
            out.provider = name();
            out.model = "mock";
            out.items = new ArrayList<>();
            for (int i = 0; i < options.limit; i++) {
                RankedItem r = new RankedItem();
                r.problemId = (long) i + 1;
                r.reason = "ok";
                r.confidence = 0.9;
                r.score = 0.9;
                r.strategy = "ok";
                out.items.add(r);
            }
            return out;
        }
    }

    static class TestFailProvider implements LlmProvider {
        @Override
        public String name() { return "fail"; }
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

