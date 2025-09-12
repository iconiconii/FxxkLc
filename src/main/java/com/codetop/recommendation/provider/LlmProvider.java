package com.codetop.recommendation.provider;

import com.codetop.recommendation.service.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LlmProvider {
    String name();

    LlmResult rankCandidates(RequestContext ctx, List<ProblemCandidate> candidates, PromptOptions options);

    default CompletableFuture<LlmResult> rankCandidatesAsync(RequestContext ctx,
                                                             List<ProblemCandidate> candidates,
                                                             PromptOptions options) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> rankCandidates(ctx, candidates, options));
    }

    class ProblemCandidate {
        public Long id;
        public String topic;
        public String difficulty;
        public List<String> tags;
        public Double recentAccuracy;
        public Integer attempts;
        
        // FSRS urgency signals (v3 enhancement)
        public Double retentionProbability;  // Calculated retention probability from FSRS
        public Integer daysOverdue;          // Days overdue for review
        public Double urgencyScore;          // Normalized urgency: 1-retention + overdue bonus
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
