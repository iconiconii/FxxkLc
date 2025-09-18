package com.codetop.recommendation.provider.impl;

import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.service.RequestContext;

import java.util.ArrayList;
import java.util.List;

public class MockProvider implements LlmProvider {
    @Override
    public String name() {
        return "mock";
    }

    @Override
    public LlmResult rankCandidates(RequestContext ctx, List<ProblemCandidate> candidates, PromptOptions options) {
        long start = System.currentTimeMillis();
        LlmResult res = new LlmResult();
        res.provider = name();
        res.model = "mock";
        res.success = true;
        res.items = new ArrayList<>();
        int count = Math.min(options != null ? options.limit : 10, candidates != null ? candidates.size() : 0);
        for (int i = 0; i < count; i++) {
            ProblemCandidate c = candidates.get(i);
            RankedItem item = new RankedItem();
            item.problemId = c.id;
            item.confidence = 0.5;
            item.score = 0.5;
            item.strategy = "mock";
            item.reason = "Mock recommendation for candidate " + c.id;
            res.items.add(item);
        }
        res.latencyMs = (int) (System.currentTimeMillis() - start);
        return res;
    }
}

