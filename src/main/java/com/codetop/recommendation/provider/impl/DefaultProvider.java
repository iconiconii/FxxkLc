package com.codetop.recommendation.provider.impl;

import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.service.RequestContext;

import java.util.Collections;

public class DefaultProvider implements LlmProvider {
    private final LlmProperties.DefaultProvider strategy;

    public DefaultProvider(LlmProperties.DefaultProvider strategy) {
        this.strategy = strategy;
    }

    @Override
    public String name() {
        return "default";
    }

    @Override
    public LlmResult rankCandidates(RequestContext ctx, java.util.List<ProblemCandidate> candidates, PromptOptions options) {
        LlmResult res = new LlmResult();
        res.provider = name();
        res.model = "default";
        res.success = false; // indicates not an LLM success
        res.error = strategy.getStrategy();
        res.items = Collections.emptyList();
        res.latencyMs = 0;
        return res;
    }
}

