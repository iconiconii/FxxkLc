package com.codetop.recommendation.dto;

import java.time.Instant;
import java.util.List;

/**
 * API wrapper response for AI recommendations that matches contract test expectations.
 * Provides a 'data' wrapper around the core AIRecommendationResponse structure.
 */
public class ApiRecommendationResponse {
    
    private Data data;
    
    public static class Data {
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
    
    public static class Meta {
        private Integer total;
        private Integer limit;
        private String traceId;
        private Instant generatedAt;
        private Boolean cached;
        private Boolean busy;
        private String strategy;
        private String chainId;
        private String chainVersion;
        private String policyId;
        private List<String> chainHops;
        private String fallbackReason;
        private String userProfileSummary;
        private String recommendationType;
        
        public Integer getTotal() {
            return total;
        }
        
        public void setTotal(Integer total) {
            this.total = total;
        }
        
        public Integer getLimit() {
            return limit;
        }
        
        public void setLimit(Integer limit) {
            this.limit = limit;
        }
        
        public String getTraceId() {
            return traceId;
        }
        
        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }
        
        public Instant getGeneratedAt() {
            return generatedAt;
        }
        
        public void setGeneratedAt(Instant generatedAt) {
            this.generatedAt = generatedAt;
        }
        
        public Boolean getCached() {
            return cached;
        }
        
        public void setCached(Boolean cached) {
            this.cached = cached;
        }
        
        public Boolean getBusy() {
            return busy;
        }
        
        public void setBusy(Boolean busy) {
            this.busy = busy;
        }
        
        public String getStrategy() {
            return strategy;
        }
        
        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }
        
        public String getChainId() {
            return chainId;
        }
        
        public void setChainId(String chainId) {
            this.chainId = chainId;
        }
        
        public String getChainVersion() {
            return chainVersion;
        }
        
        public void setChainVersion(String chainVersion) {
            this.chainVersion = chainVersion;
        }
        
        public String getPolicyId() {
            return policyId;
        }
        
        public void setPolicyId(String policyId) {
            this.policyId = policyId;
        }
        
        public List<String> getChainHops() {
            return chainHops;
        }
        
        public void setChainHops(List<String> chainHops) {
            this.chainHops = chainHops;
        }
        
        public String getFallbackReason() {
            return fallbackReason;
        }
        
        public void setFallbackReason(String fallbackReason) {
            this.fallbackReason = fallbackReason;
        }
        
        public String getUserProfileSummary() {
            return userProfileSummary;
        }
        
        public void setUserProfileSummary(String userProfileSummary) {
            this.userProfileSummary = userProfileSummary;
        }
        
        public String getRecommendationType() {
            return recommendationType;
        }
        
        public void setRecommendationType(String recommendationType) {
            this.recommendationType = recommendationType;
        }
    }
    
    public Data getData() {
        return data;
    }
    
    public void setData(Data data) {
        this.data = data;
    }
    
    /**
     * Convert AIRecommendationResponse to API format with data wrapper
     */
    public static ApiRecommendationResponse from(AIRecommendationResponse source, int requestedLimit) {
        ApiRecommendationResponse response = new ApiRecommendationResponse();
        
        Data data = new Data();
        data.setItems(source.getItems());
        
        // Convert meta
        Meta meta = new Meta();
        if (source.getMeta() != null) {
            AIRecommendationResponse.Meta sourceMeta = source.getMeta();
            meta.setTotal(source.getItems() != null ? source.getItems().size() : 0);
            meta.setLimit(requestedLimit);
            meta.setTraceId(sourceMeta.getTraceId());
            meta.setGeneratedAt(sourceMeta.getGeneratedAt());
            meta.setCached(sourceMeta.isCached());
            meta.setBusy(sourceMeta.isBusy());
            meta.setStrategy(sourceMeta.getStrategy());
            meta.setChainId(sourceMeta.getChainId());
            meta.setChainVersion(sourceMeta.getChainVersion());
            meta.setPolicyId(sourceMeta.getPolicyId());
            meta.setChainHops(sourceMeta.getChainHops());
            meta.setFallbackReason(sourceMeta.getFallbackReason());
            meta.setUserProfileSummary(sourceMeta.getUserProfileSummary());
            meta.setRecommendationType(sourceMeta.getRecommendationType());
        }
        
        data.setMeta(meta);
        response.setData(data);
        
        return response;
    }
}