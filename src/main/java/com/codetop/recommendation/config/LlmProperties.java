package com.codetop.recommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    private boolean enabled = false;
    private String provider = "chain"; // chain | openai | mock
    private String defaultChainId = "main"; // Default chain when no routing rules match
    
    // Multiple chains support
    private Map<String, Chain> chains = new HashMap<>();
    
    // Legacy single chain (for backward compatibility)
    private Chain chain = new Chain();
    
    // Routing rules
    private Routing routing = new Routing();
    
    // Feature toggles per segment
    private FeatureToggles toggles = new FeatureToggles();
    
    private OpenAi openai = new OpenAi();
    private Azure azure = new Azure();
    private AsyncLimits asyncLimits = new AsyncLimits();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getDefaultChainId() { return defaultChainId; }
    public void setDefaultChainId(String defaultChainId) { this.defaultChainId = defaultChainId; }
    public Map<String, Chain> getChains() { return chains; }
    public void setChains(Map<String, Chain> chains) { this.chains = chains; }
    public Chain getChain() { return chain; }
    public void setChain(Chain chain) { this.chain = chain; }
    public Routing getRouting() { return routing; }
    public void setRouting(Routing routing) { this.routing = routing; }
    public FeatureToggles getToggles() { return toggles; }
    public void setToggles(FeatureToggles toggles) { this.toggles = toggles; }
    public OpenAi getOpenai() { return openai; }
    public void setOpenai(OpenAi openai) { this.openai = openai; }
    public Azure getAzure() { return azure; }
    public void setAzure(Azure azure) { this.azure = azure; }
    public AsyncLimits getAsyncLimits() { return asyncLimits; }
    public void setAsyncLimits(AsyncLimits asyncLimits) { this.asyncLimits = asyncLimits; }

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
        private List<String> onErrorsToNext; // Error types that trigger fallback to next node
        private RateLimit rateLimit;

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
        public List<String> getOnErrorsToNext() { return onErrorsToNext; }
        public void setOnErrorsToNext(List<String> onErrorsToNext) { this.onErrorsToNext = onErrorsToNext; }
        public RateLimit getRateLimit() { return rateLimit; }
        public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
    }

    public static class Retry {
        private int attempts = 0;
        public int getAttempts() { return attempts; }
        public void setAttempts(int attempts) { this.attempts = attempts; }
    }

    public static class RateLimit {
        private int rps = 5;  // requests per second
        private int perUserRps = 1;  // per user requests per second
        public int getRps() { return rps; }
        public void setRps(int rps) { this.rps = rps; }
        public int getPerUserRps() { return perUserRps; }
        public void setPerUserRps(int perUserRps) { this.perUserRps = perUserRps; }
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

    public static class AsyncLimits {
        private int globalConcurrency = 10;  // Maximum concurrent async requests globally
        private int perUserConcurrency = 2;  // Maximum concurrent async requests per user
        private int acquireTimeoutMs = 100; // Timeout to acquire semaphore in milliseconds
        
        public int getGlobalConcurrency() { return globalConcurrency; }
        public void setGlobalConcurrency(int globalConcurrency) { this.globalConcurrency = globalConcurrency; }
        public int getPerUserConcurrency() { return perUserConcurrency; }
        public void setPerUserConcurrency(int perUserConcurrency) { this.perUserConcurrency = perUserConcurrency; }
        public int getAcquireTimeoutMs() { return acquireTimeoutMs; }
        public void setAcquireTimeoutMs(int acquireTimeoutMs) { this.acquireTimeoutMs = acquireTimeoutMs; }
    }

    // Routing rules for chain selection
    public static class Routing {
        private List<Rule> rules = new ArrayList<>();
        public List<Rule> getRules() { return rules; }
        public void setRules(List<Rule> rules) { this.rules = rules; }
    }

    public static class Rule {
        private Map<String, List<String>> when; // conditions: tier, abGroup, route
        private String useChain; // chain id to use when conditions match
        
        public Map<String, List<String>> getWhen() { return when; }
        public void setWhen(Map<String, List<String>> when) { this.when = when; }
        public String getUseChain() { return useChain; }
        public void setUseChain(String useChain) { this.useChain = useChain; }
    }

    // Feature toggles per segment
    public static class FeatureToggles {
        private Map<String, Boolean> byTier = new HashMap<>(); // tier -> enabled
        private Map<String, Boolean> byAbGroup = new HashMap<>(); // abGroup -> enabled  
        private Map<String, Boolean> byRoute = new HashMap<>(); // route -> enabled
        private Set<Long> allowUserIds = new HashSet<>(); // always allow these users
        private Set<Long> denyUserIds = new HashSet<>(); // always deny these users
        
        /**
         * Allow list mode determines how allow list is evaluated:
         * - "override": Users in allowUserIds bypass all other toggle checks (but still respect denyUserIds) 
         *               Users NOT in allowUserIds continue normal evaluation (default behavior)
         * - "whitelist": Users in allowUserIds are allowed, users NOT in allowUserIds are denied
         *                (traditional whitelist behavior - more restrictive)
         */
        private String allowListMode = "override"; // override | whitelist

        public Map<String, Boolean> getByTier() { return byTier; }
        public void setByTier(Map<String, Boolean> byTier) { this.byTier = byTier; }
        public Map<String, Boolean> getByAbGroup() { return byAbGroup; }
        public void setByAbGroup(Map<String, Boolean> byAbGroup) { this.byAbGroup = byAbGroup; }
        public Map<String, Boolean> getByRoute() { return byRoute; }
        public void setByRoute(Map<String, Boolean> byRoute) { this.byRoute = byRoute; }
        public Set<Long> getAllowUserIds() { return allowUserIds; }
        public void setAllowUserIds(Set<Long> allowUserIds) { this.allowUserIds = allowUserIds; }
        public Set<Long> getDenyUserIds() { return denyUserIds; }
        public void setDenyUserIds(Set<Long> denyUserIds) { this.denyUserIds = denyUserIds; }
        public String getAllowListMode() { return allowListMode; }
        public void setAllowListMode(String allowListMode) { this.allowListMode = allowListMode; }
    }

    /**
     * Post-process configuration to normalize case sensitivity issues.
     * Ensures consistent tier matching by converting tier keys to uppercase.
     */
    @PostConstruct
    public void normalizeConfiguration() {
        if (toggles != null && toggles.getByTier() != null) {
            // Normalize tier keys to uppercase for consistent matching
            Map<String, Boolean> normalizedTiers = toggles.getByTier().entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> entry.getKey().toUpperCase(),
                    Map.Entry::getValue,
                    (existing, replacement) -> existing, // Keep first occurrence in case of duplicates
                    HashMap::new
                ));
            toggles.setByTier(normalizedTiers);
        }

        if (routing != null && routing.getRules() != null) {
            // Normalize tier values in routing rules to uppercase
            for (Rule rule : routing.getRules()) {
                if (rule.getWhen() != null && rule.getWhen().containsKey("tier")) {
                    List<String> normalizedTiers = rule.getWhen().get("tier").stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.toList());
                    rule.getWhen().put("tier", normalizedTiers);
                }
            }
        }
    }
}
