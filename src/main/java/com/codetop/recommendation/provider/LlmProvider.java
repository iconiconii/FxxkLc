package com.codetop.recommendation.provider;

import com.codetop.recommendation.service.RequestContext;

import java.util.List;

public interface LlmProvider {
    String name();

    LlmResult rankCandidates(RequestContext ctx, List<ProblemCandidate> candidates, PromptOptions options);

    class ProblemCandidate {
        public Long id;
        public String topic;
        public String difficulty;
        public List<String> tags;
        public Double recentAccuracy;
        public Integer attempts;
    }

    class PromptOptions {
        public int limit = 10;
        public String promptVersion = "v1";
    }

    class LlmResult {
        public boolean success;
        public String model;
        public String error; // optional
        public List<RankedItem> items;
        public int latencyMs;
        public String provider;
    }

    class RankedItem {
        public Long problemId;
        public String reason;
        public double confidence;
        public double score;
        public String strategy;
    }
}

