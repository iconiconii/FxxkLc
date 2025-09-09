package com.codetop.recommendation.chain;

import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.service.RequestContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderChain {
    private final Map<String, LlmProvider> catalog = new HashMap<>();
    private final LlmProperties properties;
    private final LlmProvider defaultProvider;

    public ProviderChain(List<LlmProvider> providers, LlmProperties properties, LlmProvider defaultProvider) {
        for (LlmProvider p : providers) {
            catalog.put(p.name(), p);
        }
        this.properties = properties;
        this.defaultProvider = defaultProvider;
    }

    public Result execute(RequestContext ctx, List<LlmProvider.ProblemCandidate> candidates, LlmProvider.PromptOptions options) {
        List<String> hops = new ArrayList<>();
        if (!properties.isEnabled()) {
            return Result.defaulted("llm_disabled", hops);
        }

        if (properties.getChain() == null || properties.getChain().getNodes() == null || properties.getChain().getNodes().isEmpty()) {
            return Result.defaulted("chain_empty", hops);
        }

        for (LlmProperties.Node node : properties.getChain().getNodes()) {
            if (!node.isEnabled()) continue;
            LlmProvider provider = catalog.getOrDefault(node.getName(), null);
            if (provider == null) continue;
            hops.add(provider.name());
            LlmProvider.LlmResult res = provider.rankCandidates(ctx, candidates, options);
            if (res != null && res.success) {
                return Result.success(res, hops);
            }
        }
        hops.add("default");
        LlmProvider.LlmResult def = defaultProvider.rankCandidates(ctx, candidates, options);
        return Result.defaulted(def != null ? def.error : "default", hops);
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

