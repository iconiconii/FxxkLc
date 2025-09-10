package com.codetop.recommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    private boolean enabled = false;
    private String provider = "chain"; // chain | openai | mock
    private Chain chain = new Chain();
    private OpenAi openai = new OpenAi();
    private Azure azure = new Azure();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Chain getChain() { return chain; }
    public void setChain(Chain chain) { this.chain = chain; }
    public OpenAi getOpenai() { return openai; }
    public void setOpenai(OpenAi openai) { this.openai = openai; }
    public Azure getAzure() { return azure; }
    public void setAzure(Azure azure) { this.azure = azure; }

    public static class Chain {
        private List<Node> nodes;
        private DefaultProvider defaultProvider = new DefaultProvider();
        public List<Node> getNodes() { return nodes; }
        public void setNodes(List<Node> nodes) { this.nodes = nodes; }
        public DefaultProvider getDefaultProvider() { return defaultProvider; }
        public void setDefaultProvider(DefaultProvider defaultProvider) { this.defaultProvider = defaultProvider; }
    }

    public static class Node {
        private String name; // provider key: openai | azure | mock
        private boolean enabled = true;
        private Map<String, Object> conditions; // reserved for future
        private Integer timeoutMs;
        private Retry retry;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Map<String, Object> getConditions() { return conditions; }
        public void setConditions(Map<String, Object> conditions) { this.conditions = conditions; }
        public Integer getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
        public Retry getRetry() { return retry; }
        public void setRetry(Retry retry) { this.retry = retry; }
    }

    public static class Retry {
        private int attempts = 0;
        public int getAttempts() { return attempts; }
        public void setAttempts(int attempts) { this.attempts = attempts; }
    }

    public static class DefaultProvider {
        private String strategy = "busy_message"; // busy_message | fsrs_fallback | empty
        private String message = "系统繁忙，请稍后重试";
        private int httpStatus = 503;
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getHttpStatus() { return httpStatus; }
        public void setHttpStatus(int httpStatus) { this.httpStatus = httpStatus; }
    }

    public static class OpenAi {
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o-mini";
        private String apiKeyEnv = "OPENAI_API_KEY";
        private Integer timeoutMs = 1800;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getApiKeyEnv() { return apiKeyEnv; }
        public void setApiKeyEnv(String apiKeyEnv) { this.apiKeyEnv = apiKeyEnv; }
        public Integer getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class Azure {
        private String endpoint;
        private String deployment;
        private String apiKeyEnv = "AZURE_OPENAI_KEY";
        private Integer timeoutMs = 1800;
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getDeployment() { return deployment; }
        public void setDeployment(String deployment) { this.deployment = deployment; }
        public String getApiKeyEnv() { return apiKeyEnv; }
        public void setApiKeyEnv(String apiKeyEnv) { this.apiKeyEnv = apiKeyEnv; }
        public Integer getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    }
}
