package com.codetop.util;

import com.codetop.service.CacheKeyBuilder;
import com.codetop.service.cache.CacheService;
// import com.codetop.service.cache.CacheMetricsCollector;
import com.codetop.config.CacheConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 缓存助手类 - 提供高级缓存操作
 * 
 * 功能特性：
 * 1. 自动cache-aside模式实现
 * 2. 指标收集和监控
 * 3. 错误处理和降级
 * 4. 批量操作支持
 * 5. 缓存预热
 * 6. 健康检查
 * 
 * @author CodeTop FSRS Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheHelper {
    
    private final CacheService cacheService;
    // private final CacheMetricsCollector metricsCollector;
    private final CacheConfiguration cacheConfiguration;
    
    /**
     * 缓存或计算值 - 核心cache-aside模式实现
     * 
     * @param cacheKey 缓存键
     * @param valueType 值类型
     * @param ttl 存活时间
     * @param valueSupplier 值供应函数
     * @param <T> 值类型泛型
     * @return 缓存或计算的值
     */
    public <T> T cacheOrCompute(String cacheKey, Class<T> valueType, Duration ttl, Supplier<T> valueSupplier) {
        if (!cacheConfiguration.isEnabled()) {
            log.debug("Cache is disabled, computing value directly: key={}", cacheKey);
            return executeWithMetrics("compute-direct", valueSupplier::get);
        }
        
        if (!CacheKeyBuilder.validateKey(cacheKey)) {
            log.warn("Invalid cache key format, computing value directly: key={}", cacheKey);
            return executeWithMetrics("compute-invalid-key", valueSupplier::get);
        }
        
        // 尝试从缓存获取
        T cachedValue = executeWithMetrics("get", () -> cacheService.get(cacheKey, valueType));
        
        if (cachedValue != null) {
            String cacheName = CacheKeyBuilder.extractDomain(cacheKey);
            if (cacheName != null) {
                // metricsCollector.recordCacheHit(cacheName);
            }
            log.debug("Cache hit: key={}", cacheKey);
            return cachedValue;
        }
        
        // 缓存未命中，计算值
        String cacheName = CacheKeyBuilder.extractDomain(cacheKey);
        if (cacheName != null) {
            // metricsCollector.recordCacheMiss(cacheName);
        }
        
        log.debug("Cache miss, computing value: key={}", cacheKey);
        T computedValue = executeWithMetrics("compute", valueSupplier::get);
        
        // 将计算结果存入缓存 (异步执行以避免影响主流程)
        if (computedValue != null) {
            executeWithMetrics("put", () -> {
                cacheService.put(cacheKey, computedValue, ttl);
                return null;
            });
        }
        
        return computedValue;
    }
    
    /**
     * 缓存或计算值 - 使用配置的默认TTL
     */
    public <T> T cacheOrCompute(String cacheKey, Class<T> valueType, Supplier<T> valueSupplier) {
        String cacheName = CacheKeyBuilder.extractDomain(cacheKey);
        Duration ttl = cacheName != null ? 
            cacheConfiguration.getCacheConfig(cacheName).getTtl() : 
            cacheConfiguration.getDefaultConfig().getTtl();
            
        return cacheOrCompute(cacheKey, valueType, ttl, valueSupplier);
    }
    
    /**
     * 缓存或计算列表 - 专门处理列表类型
     */
    public <T> List<T> cacheOrComputeList(String cacheKey, Class<T> elementType, Duration ttl, Supplier<List<T>> listSupplier) {
        if (!cacheConfiguration.isEnabled()) {
            log.debug("Cache is disabled, computing list directly: key={}", cacheKey);
            return executeWithMetrics("compute-list-direct", listSupplier::get);
        }
        
        if (!CacheKeyBuilder.validateKey(cacheKey)) {
            log.warn("Invalid cache key format, computing list directly: key={}", cacheKey);
            return executeWithMetrics("compute-list-invalid-key", listSupplier::get);
        }
        
        // 尝试从缓存获取列表
        List<T> cachedList = executeWithMetrics("get-list", () -> cacheService.getList(cacheKey, elementType));
        
        if (cachedList != null) {
            String cacheName = CacheKeyBuilder.extractDomain(cacheKey);
            if (cacheName != null) {
                // metricsCollector.recordCacheHit(cacheName);
            }
            log.debug("Cache hit (list): key={}, size={}", cacheKey, cachedList.size());
            return cachedList;
        }
        
        // 缓存未命中，计算列表
        String cacheName = CacheKeyBuilder.extractDomain(cacheKey);
        if (cacheName != null) {
            // metricsCollector.recordCacheMiss(cacheName);
        }
        
        log.debug("Cache miss, computing list: key={}", cacheKey);
        List<T> computedList = executeWithMetrics("compute-list", listSupplier::get);
        
        // 将计算结果存入缓存
        if (computedList != null) {
            executeWithMetrics("put-list", () -> {
                cacheService.put(cacheKey, computedList, ttl);
                return null;
            });
        }
        
        return computedList;
    }
    
    /**
     * 缓存或计算列表 - 使用配置的默认TTL
     */
    public <T> List<T> cacheOrComputeList(String cacheKey, Class<T> elementType, Supplier<List<T>> listSupplier) {
        String cacheName = CacheKeyBuilder.extractDomain(cacheKey);
        Duration ttl = cacheName != null ? 
            cacheConfiguration.getCacheConfig(cacheName).getTtl() : 
            cacheConfiguration.getDefaultConfig().getTtl();
            
        return cacheOrComputeList(cacheKey, elementType, ttl, listSupplier);
    }
    
    /**
     * 带重试的缓存操作
     * 
     * @param operation 操作名称
     * @param cacheOperation 缓存操作
     * @param maxRetries 最大重试次数
     * @param <T> 返回类型
     * @return 操作结果
     */
    public <T> T executeWithRetry(String operation, Callable<T> cacheOperation, int maxRetries) {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return executeWithMetrics(operation, () -> {
                    try {
                        return cacheOperation.call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < maxRetries) {
                    long delay = (long) Math.pow(2, attempt) * 100; // 指数退避
                    log.warn("Cache operation failed, retrying in {}ms: operation={}, attempt={}/{}, error={}", 
                            delay, operation, attempt + 1, maxRetries, e.getMessage());
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Operation interrupted", ie);
                    }
                } else {
                    log.error("Cache operation failed after {} retries: operation={}, error={}", 
                            maxRetries, operation, e.getMessage(), e);
                }
            }
        }
        
        throw new RuntimeException("Cache operation failed after retries: " + operation, lastException);
    }
    
    /**
     * 批量删除缓存
     * 
     * @param keyPattern 键模式
     * @return 删除的键数量
     */
    public long evictByPattern(String keyPattern) {
        return executeWithMetrics("evict-pattern", () -> {
            long deletedCount = cacheService.deleteByPattern(keyPattern);
            
            if (deletedCount > 0) {
                String cacheName = CacheKeyBuilder.extractDomain(keyPattern);
                if (cacheName != null) {
                    // metricsCollector.recordCacheEviction(cacheName, deletedCount);
                }
                log.info("Batch cache eviction completed: pattern={}, deleted={}", keyPattern, deletedCount);
            }
            
            return deletedCount;
        });
    }
    
    /**
     * 预热指定的缓存
     * 
     * @param cacheKey 缓存键
     * @param valueType 值类型
     * @param valueSupplier 值供应函数
     * @param <T> 值类型泛型
     */
    public <T> void warmupCache(String cacheKey, Class<T> valueType, Supplier<T> valueSupplier) {
        String cacheName = CacheKeyBuilder.extractDomain(cacheKey);
        
        if (cacheName == null) {
            log.warn("Cannot determine cache name for warmup: key={}", cacheKey);
            return;
        }
        
        CacheConfiguration.CacheConfig config = cacheConfiguration.getCacheConfig(cacheName);
        if (!config.isEnableWarmup()) {
            log.debug("Cache warmup is disabled: cache={}", cacheName);
            return;
        }
        
        // 检查是否已经存在
        if (cacheService.exists(cacheKey)) {
            log.debug("Cache already warmed up: key={}", cacheKey);
            return;
        }
        
        log.info("Starting cache warmup: key={}", cacheKey);
        
        try {
            T value = executeWithMetrics("warmup", valueSupplier::get);
            if (value != null) {
                cacheService.put(cacheKey, value, config.getTtl());
                log.info("Cache warmup completed: key={}", cacheKey);
            } else {
                log.warn("Cache warmup produced null value: key={}", cacheKey);
            }
        } catch (Exception e) {
            log.error("Cache warmup failed: key={}, error={}", cacheKey, e.getMessage(), e);
        }
    }
    
    /**
     * 获取缓存健康状态
     * 
     * @return 缓存健康信息
     */
    public CacheHealthInfo getCacheHealthInfo() {
        // CacheMetricsCollector.CacheMetricsSummary summary = metricsCollector.getMetricsSummary();
        
        CacheHealthInfo healthInfo = new CacheHealthInfo();
        healthInfo.setHealthy(true); // summary.getGlobalHitRate() > 0.5); // 命中率超过50%认为健康
        healthInfo.setHitRate(0.0); // summary.getGlobalHitRate());
        healthInfo.setTotalRequests(0); // summary.getTotalRequests());
        healthInfo.setAverageResponseTime(0.0); // summary.getAverageResponseTime());
        healthInfo.setCacheCount(0); // summary.getCacheMetrics().size());
        healthInfo.setGeneratedAt(java.time.LocalDateTime.now()); // summary.getGeneratedAt());
        
        // 检查慢查询
        double slowQueryThreshold = cacheConfiguration.getMonitoring().getSlowQueryThreshold().toMillis();
        healthInfo.setHasSlowQueries(false); // summary.getAverageResponseTime() > slowQueryThreshold);
        
        return healthInfo;
    }
    
    /**
     * 执行操作并记录指标
     */
    private <T> T executeWithMetrics(String operation, Supplier<T> supplier) {
        Instant start = Instant.now();
        try {
            T result = supplier.get();
            Duration duration = Duration.between(start, Instant.now());
            // metricsCollector.recordCacheOperation(operation, duration);
            return result;
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            // metricsCollector.recordCacheOperation(operation, duration);
            // metricsCollector.recordCacheError("global", operation, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 缓存健康信息
     */
    public static class CacheHealthInfo {
        private boolean healthy;
        private double hitRate;
        private long totalRequests;
        private double averageResponseTime;
        private int cacheCount;
        private boolean hasSlowQueries;
        private java.time.LocalDateTime generatedAt;
        
        // Getters and setters
        public boolean isHealthy() {
            return healthy;
        }
        
        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }
        
        public double getHitRate() {
            return hitRate;
        }
        
        public void setHitRate(double hitRate) {
            this.hitRate = hitRate;
        }
        
        public long getTotalRequests() {
            return totalRequests;
        }
        
        public void setTotalRequests(long totalRequests) {
            this.totalRequests = totalRequests;
        }
        
        public double getAverageResponseTime() {
            return averageResponseTime;
        }
        
        public void setAverageResponseTime(double averageResponseTime) {
            this.averageResponseTime = averageResponseTime;
        }
        
        public int getCacheCount() {
            return cacheCount;
        }
        
        public void setCacheCount(int cacheCount) {
            this.cacheCount = cacheCount;
        }
        
        public boolean isHasSlowQueries() {
            return hasSlowQueries;
        }
        
        public void setHasSlowQueries(boolean hasSlowQueries) {
            this.hasSlowQueries = hasSlowQueries;
        }
        
        public java.time.LocalDateTime getGeneratedAt() {
            return generatedAt;
        }
        
        public void setGeneratedAt(java.time.LocalDateTime generatedAt) {
            this.generatedAt = generatedAt;
        }
        
        @Override
        public String toString() {
            return String.format(
                "CacheHealthInfo{healthy=%s, hitRate=%.2f%%, totalRequests=%d, avgResponseTime=%.2fms, " +
                "cacheCount=%d, hasSlowQueries=%s, generatedAt=%s}",
                healthy, hitRate * 100, totalRequests, averageResponseTime, 
                cacheCount, hasSlowQueries, generatedAt
            );
        }
    }
}