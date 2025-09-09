package com.codetop.recommendation.dto;

import java.time.Instant;
import java.util.List;

public class AIRecommendationResponse {
    public static class Meta {
        private boolean cached;
        private String traceId;
        private Instant generatedAt;
        private boolean busy; // default-provider busy message
        private String strategy; // fsrs_fallback | busy_message | normal
        private String chainId;
        private String chainVersion;
        private String policyId;
        private java.util.List<String> chainHops;

        public boolean isCached() { return cached; }
        public void setCached(boolean cached) { this.cached = cached; }
        public String getTraceId() { return traceId; }
        public void setTraceId(String traceId) { this.traceId = traceId; }
        public Instant getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
        public boolean isBusy() { return busy; }
        public void setBusy(boolean busy) { this.busy = busy; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public String getChainId() { return chainId; }
        public void setChainId(String chainId) { this.chainId = chainId; }
        public String getChainVersion() { return chainVersion; }
        public void setChainVersion(String chainVersion) { this.chainVersion = chainVersion; }
        public String getPolicyId() { return policyId; }
        public void setPolicyId(String policyId) { this.policyId = policyId; }
        public java.util.List<String> getChainHops() { return chainHops; }
        public void setChainHops(java.util.List<String> chainHops) { this.chainHops = chainHops; }
    }

    private List<RecommendationItemDTO> items;
    private Meta meta;

    public List<RecommendationItemDTO> getItems() {
        return items;
    }

    public void setItems(List<RecommendationItemDTO> items) {
        this.items = items;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }
}
