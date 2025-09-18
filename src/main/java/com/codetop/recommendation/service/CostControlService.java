package com.codetop.recommendation.service;

import com.codetop.recommendation.metrics.LlmMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cost control and budget management service for LLM recommendations.
 * Implements token budgets, cost estimation, and auto-degradation strategies.
 */
@Service
public class CostControlService {
    
    private static final Logger log = LoggerFactory.getLogger(CostControlService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final LlmMetricsCollector metricsCollector;
    
    // Configuration from application.yml
    @Value("${llm.cost-control.daily-budget-usd:10.0}")
    private double dailyBudgetUsd;
    
    @Value("${llm.cost-control.monthly-budget-usd:300.0}")
    private double monthlyBudgetUsd;
    
    @Value("${llm.cost-control.per-user-daily-limit:100}")
    private int perUserDailyTokenLimit;
    
    @Value("${llm.cost-control.chain-level-budget-usd:5.0}")
    private double chainLevelBudgetUsd;
    
    @Value("${llm.cost-control.emergency-threshold:0.9}")
    private double emergencyThreshold;
    
    @Value("${llm.cost-control.warning-threshold:0.8}")
    private double warningThreshold;
    
    // Token cost estimates (per 1K tokens) - should be externalized to config
    private static final Map<String, Double> TOKEN_COSTS = Map.of(
        "deepseek-chat", 0.00014,  // DeepSeek pricing
        "gpt-3.5-turbo", 0.0015,   // OpenAI pricing
        "gpt-4", 0.03,
        "gpt-4-turbo", 0.01
    );
    
    public CostControlService(RedisTemplate<String, String> redisTemplate, 
                            LlmMetricsCollector metricsCollector) {
        this.redisTemplate = redisTemplate;
        this.metricsCollector = metricsCollector;
    }
    
    /**
     * Check if request is within budget constraints.
     * Returns recommendation for processing or degradation.
     */
    public BudgetCheckResult checkBudgetConstraints(Long userId, String chainId, String model, 
                                                  int estimatedPromptTokens, int candidateCount) {
        try {
            // Estimate total token usage
            TokenEstimate estimate = estimateTokenUsage(model, estimatedPromptTokens, candidateCount);
            double estimatedCost = estimate.estimatedCostUsd;
            
            // Check daily budget
            double dailySpent = getDailySpentAmount();
            if (dailySpent + estimatedCost > dailyBudgetUsd * emergencyThreshold) {
                log.warn("Daily budget emergency threshold exceeded. Daily spent: {}, estimated cost: {}, budget: {}", 
                    dailySpent, estimatedCost, dailyBudgetUsd);
                recordBudgetEvent("DAILY_EMERGENCY_THRESHOLD", chainId, estimatedCost);
                return BudgetCheckResult.emergencyFallback("Daily budget emergency threshold exceeded");
            }
            
            if (dailySpent + estimatedCost > dailyBudgetUsd * warningThreshold) {
                log.warn("Daily budget warning threshold exceeded. Recommending candidate reduction.");
                recordBudgetEvent("DAILY_WARNING_THRESHOLD", chainId, estimatedCost);
                return BudgetCheckResult.reduceCandidates("Daily budget warning", Math.min(candidateCount / 2, 5));
            }
            
            // Check monthly budget
            double monthlySpent = getMonthlySpentAmount();
            if (monthlySpent + estimatedCost > monthlyBudgetUsd * emergencyThreshold) {
                log.warn("Monthly budget emergency threshold exceeded");
                recordBudgetEvent("MONTHLY_EMERGENCY_THRESHOLD", chainId, estimatedCost);
                return BudgetCheckResult.emergencyFallback("Monthly budget emergency threshold exceeded");
            }
            
            // Check chain-level budget
            double chainSpent = getChainSpentAmount(chainId);
            if (chainSpent + estimatedCost > chainLevelBudgetUsd * emergencyThreshold) {
                log.warn("Chain-level budget emergency threshold exceeded for chain: {}", chainId);
                recordBudgetEvent("CHAIN_EMERGENCY_THRESHOLD", chainId, estimatedCost);
                return BudgetCheckResult.emergencyFallback("Chain budget emergency threshold exceeded");
            }
            
            // Check per-user limits  
            if (userId != null) {
                int userTokensToday = getUserDailyTokenUsage(userId);
                if (userTokensToday + estimate.totalTokens > perUserDailyTokenLimit) {
                    log.warn("User {} daily token limit would be exceeded. Current: {}, estimated: {}, limit: {}", 
                        userId, userTokensToday, estimate.totalTokens, perUserDailyTokenLimit);
                    recordBudgetEvent("USER_TOKEN_LIMIT", chainId, estimatedCost);
                    return BudgetCheckResult.emergencyFallback("User daily token limit exceeded");
                }
            }
            
            recordBudgetEvent("APPROVED", chainId, estimatedCost);
            return BudgetCheckResult.approved();
            
        } catch (Exception e) {
            log.error("Error checking budget constraints", e);
            recordBudgetEvent("CHECK_ERROR", chainId, 0.0);
            return BudgetCheckResult.approved(); // Fail open for availability
        }
    }
    
    /**
     * Record actual usage after LLM call completion.
     */
    public void recordActualUsage(Long userId, String chainId, String provider, String model, 
                                 int promptTokens, int completionTokens, double actualCost) {
        try {
            String dateKey = getCurrentDateKey();
            String monthKey = getCurrentMonthKey();
            
            // Update daily totals
            String dailyKey = "llm:cost:daily:" + dateKey;
            redisTemplate.opsForValue().increment(dailyKey, actualCost);
            redisTemplate.expire(dailyKey, Duration.ofDays(7)); // Keep for 7 days
            
            // Update monthly totals
            String monthlyKey = "llm:cost:monthly:" + monthKey;
            redisTemplate.opsForValue().increment(monthlyKey, actualCost);
            redisTemplate.expire(monthlyKey, Duration.ofDays(60)); // Keep for 2 months
            
            // Update chain-level totals
            String chainKey = "llm:cost:chain:" + chainId + ":" + dateKey;
            redisTemplate.opsForValue().increment(chainKey, actualCost);
            redisTemplate.expire(chainKey, Duration.ofDays(7));
            
            // Update user token usage
            if (userId != null) {
                String userTokenKey = "llm:tokens:user:" + userId + ":" + dateKey;
                redisTemplate.opsForValue().increment(userTokenKey, promptTokens + completionTokens);
                redisTemplate.expire(userTokenKey, Duration.ofDays(2));
            }
            
            // Record metrics
            metricsCollector.recordTokenUsage(provider, model, chainId, 
                promptTokens, completionTokens, promptTokens + completionTokens, actualCost);
                
            log.debug("Recorded usage - Provider: {}, Model: {}, Chain: {}, Cost: ${}, Tokens: {}/{}", 
                provider, model, chainId, actualCost, promptTokens, completionTokens);
                
        } catch (Exception e) {
            log.error("Error recording actual usage", e);
        }
    }
    
    /**
     * Estimate token usage based on prompt length and candidate count.
     */
    public TokenEstimate estimateTokenUsage(String model, int promptTokens, int candidateCount) {
        // Rough estimates based on typical response patterns
        int systemMessageTokens = 50; // Fixed system message overhead
        int candidateOverheadTokens = candidateCount * 20; // ~20 tokens per candidate in prompt
        int totalPromptTokens = promptTokens + systemMessageTokens + candidateOverheadTokens;
        
        // Estimate completion tokens based on candidate count
        // Each recommendation item: ~30-50 tokens (problemId + score + explanation + confidence)
        int estimatedCompletionTokens = candidateCount * 40; // Conservative estimate
        
        int totalTokens = totalPromptTokens + estimatedCompletionTokens;
        
        // Calculate cost based on model
        double costPer1k = TOKEN_COSTS.getOrDefault(model, 0.002); // Default fallback cost
        double estimatedCost = (totalTokens / 1000.0) * costPer1k;
        
        return new TokenEstimate(totalPromptTokens, estimatedCompletionTokens, totalTokens, estimatedCost);
    }
    
    /**
     * Get current daily spent amount.
     */
    public double getDailySpentAmount() {
        try {
            String dailyKey = "llm:cost:daily:" + getCurrentDateKey();
            String value = redisTemplate.opsForValue().get(dailyKey);
            return value != null ? Double.parseDouble(value) : 0.0;
        } catch (Exception e) {
            log.warn("Error getting daily spent amount", e);
            return 0.0;
        }
    }
    
    /**
     * Get current monthly spent amount.
     */
    public double getMonthlySpentAmount() {
        try {
            String monthlyKey = "llm:cost:monthly:" + getCurrentMonthKey();
            String value = redisTemplate.opsForValue().get(monthlyKey);
            return value != null ? Double.parseDouble(value) : 0.0;
        } catch (Exception e) {
            log.warn("Error getting monthly spent amount", e);
            return 0.0;
        }
    }
    
    /**
     * Get chain-level spent amount for today.
     */
    public double getChainSpentAmount(String chainId) {
        try {
            String chainKey = "llm:cost:chain:" + chainId + ":" + getCurrentDateKey();
            String value = redisTemplate.opsForValue().get(chainKey);
            return value != null ? Double.parseDouble(value) : 0.0;
        } catch (Exception e) {
            log.warn("Error getting chain spent amount for {}", chainId, e);
            return 0.0;
        }
    }
    
    /**
     * Get user daily token usage.
     */
    public int getUserDailyTokenUsage(Long userId) {
        try {
            String userKey = "llm:tokens:user:" + userId + ":" + getCurrentDateKey();
            String value = redisTemplate.opsForValue().get(userKey);
            return value != null ? Integer.parseInt(value) : 0;
        } catch (Exception e) {
            log.warn("Error getting user token usage for {}", userId, e);
            return 0;
        }
    }
    
    private void recordBudgetEvent(String eventType, String chainId, double estimatedCost) {
        try {
            metricsCollector.recordErrorReasons("budget_control", "cost_check", chainId, 
                eventType, "BUDGET_CONTROL", null, Duration.ZERO);
        } catch (Exception e) {
            log.warn("Error recording budget event", e);
        }
    }
    
    private String getCurrentDateKey() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
    
    private String getCurrentMonthKey() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
    
    /**
     * Result of budget constraint checking.
     */
    public static class BudgetCheckResult {
        public final boolean approved;
        public final String reason;
        public final String action; // APPROVE, REDUCE_CANDIDATES, EMERGENCY_FALLBACK
        public final Integer reducedCandidateCount;
        
        private BudgetCheckResult(boolean approved, String reason, String action, Integer reducedCandidateCount) {
            this.approved = approved;
            this.reason = reason;
            this.action = action;
            this.reducedCandidateCount = reducedCandidateCount;
        }
        
        public static BudgetCheckResult approved() {
            return new BudgetCheckResult(true, "Within budget", "APPROVE", null);
        }
        
        public static BudgetCheckResult reduceCandidates(String reason, int newCount) {
            return new BudgetCheckResult(true, reason, "REDUCE_CANDIDATES", newCount);
        }
        
        public static BudgetCheckResult emergencyFallback(String reason) {
            return new BudgetCheckResult(false, reason, "EMERGENCY_FALLBACK", null);
        }
    }
    
    /**
     * Token usage estimation result.
     */
    public static class TokenEstimate {
        public final int promptTokens;
        public final int completionTokens;
        public final int totalTokens;
        public final double estimatedCostUsd;
        
        public TokenEstimate(int promptTokens, int completionTokens, int totalTokens, double estimatedCostUsd) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
            this.estimatedCostUsd = estimatedCostUsd;
        }
    }
}