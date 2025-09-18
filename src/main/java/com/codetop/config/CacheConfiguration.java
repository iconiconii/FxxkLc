package com.codetop.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置管理类
 * 
 * 统一管理各种缓存的TTL、键前缀、监控等配置信息，
 * 支持不同缓存域的个性化配置和热更新。
 * 
 * @author CodeTop FSRS Team
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cache.config")
public class CacheConfiguration {
    
    /**
     * 默认缓存配置
     */
    private CacheConfig defaultConfig = new CacheConfig();
    
    /**
     * 特定缓存域的配置映射
     * key: 缓存域名 (如 user-profile, problem-list 等)
     * value: 该域的特定配置
     */
    private Map<String, CacheConfig> caches = new HashMap<>();
    
    /**
     * 全局缓存开关
     */
    private boolean enabled = true;
    
    /**
     * 缓存预热配置
     */
    private WarmupConfig warmup = new WarmupConfig();
    
    /**
     * 缓存监控配置
     */
    private MonitoringConfig monitoring = new MonitoringConfig();
    
    public CacheConfiguration() {
        initializeDefaultCaches();
    }
    
    /**
     * 获取特定缓存域的配置，如果不存在则返回默认配置
     * 
     * @param cacheName 缓存名称
     * @return 缓存配置
     */
    public CacheConfig getCacheConfig(String cacheName) {
        return caches.getOrDefault(cacheName, defaultConfig);
    }
    
    /**
     * 初始化默认的缓存配置
     */
    private void initializeDefaultCaches() {
        // 用户相关缓存 - 1小时TTL
        CacheConfig userConfig = new CacheConfig();
        userConfig.setTtl(Duration.ofHours(1));
        userConfig.setKeyPrefix("user");
        userConfig.setDescription("User profile and related data");
        caches.put("user-profile", userConfig);
        caches.put("user-progress", userConfig);
        caches.put("user-mastery", userConfig);
        
        // FSRS相关缓存
        CacheConfig fsrsQueueConfig = new CacheConfig();
        fsrsQueueConfig.setTtl(Duration.ofMinutes(5));
        fsrsQueueConfig.setKeyPrefix("fsrs-queue");
        fsrsQueueConfig.setDescription("FSRS review queue");
        caches.put("fsrs-queue", fsrsQueueConfig);
        
        CacheConfig fsrsStatsConfig = new CacheConfig();
        fsrsStatsConfig.setTtl(Duration.ofMinutes(10));
        fsrsStatsConfig.setKeyPrefix("fsrs-stats");
        fsrsStatsConfig.setDescription("FSRS user statistics");
        caches.put("fsrs-stats", fsrsStatsConfig);
        
        CacheConfig fsrsMetricsConfig = new CacheConfig();
        fsrsMetricsConfig.setTtl(Duration.ofHours(1));
        fsrsMetricsConfig.setKeyPrefix("fsrs-metrics");
        fsrsMetricsConfig.setDescription("FSRS system metrics");
        caches.put("fsrs-metrics", fsrsMetricsConfig);
        
        // 问题相关缓存 - 30分钟TTL
        CacheConfig problemConfig = new CacheConfig();
        problemConfig.setTtl(Duration.ofMinutes(30));
        problemConfig.setKeyPrefix("problem");
        problemConfig.setDescription("Problem data and statistics");
        caches.put("problem-single", problemConfig);
        caches.put("problem-list", problemConfig);
        caches.put("problem-search", problemConfig);
        caches.put("problem-advanced", problemConfig);
        caches.put("problem-enhanced", problemConfig);
        caches.put("problem-difficulty", problemConfig);
        caches.put("problem-hot", problemConfig);
        caches.put("problem-recent", problemConfig);
        caches.put("problem-stats", problemConfig);
        
        // 标签统计缓存 - 1小时TTL
        CacheConfig tagConfig = new CacheConfig();
        tagConfig.setTtl(Duration.ofHours(1));
        tagConfig.setKeyPrefix("tag-stats");
        tagConfig.setDescription("Tag usage statistics");
        caches.put("tag-stats", tagConfig);
        
        // 排行榜缓存 - 5分钟TTL
        CacheConfig leaderboardConfig = new CacheConfig();
        leaderboardConfig.setTtl(Duration.ofMinutes(5));
        leaderboardConfig.setKeyPrefix("leaderboard");
        leaderboardConfig.setDescription("Leaderboard data");
        caches.put("leaderboard-global", leaderboardConfig);
        caches.put("leaderboard-weekly", leaderboardConfig);
        caches.put("leaderboard-monthly", leaderboardConfig);
        caches.put("leaderboard-streak", leaderboardConfig);
    }
    
    /**
     * 单个缓存的配置
     */
    @Data
    public static class CacheConfig {
        /**
         * 缓存存活时间
         */
        private Duration ttl = Duration.ofHours(1);
        
        /**
         * 缓存键前缀
         */
        private String keyPrefix;
        
        /**
         * 是否启用缓存指标收集
         */
        private boolean enableMetrics = true;
        
        /**
         * 是否启用缓存预热
         */
        private boolean enableWarmup = false;
        
        /**
         * 最大缓存大小 (条目数)
         */
        private long maxSize = -1; // -1 表示无限制
        
        /**
         * 缓存描述
         */
        private String description;
        
        /**
         * 自定义配置属性
         */
        private Map<String, Object> properties = new HashMap<>();
    }
    
    /**
     * 缓存预热配置
     */
    @Data
    public static class WarmupConfig {
        /**
         * 是否启用缓存预热
         */
        private boolean enabled = false;
        
        /**
         * 预热延迟时间 (应用启动后)
         */
        private Duration delay = Duration.ofMinutes(1);
        
        /**
         * 预热任务执行间隔
         */
        private Duration interval = Duration.ofHours(6);
        
        /**
         * 需要预热的缓存列表
         */
        private Map<String, Object> caches = new HashMap<>();
    }
    
    /**
     * 缓存监控配置
     */
    @Data
    public static class MonitoringConfig {
        /**
         * 是否启用监控
         */
        private boolean enabled = true;
        
        /**
         * 监控数据收集间隔
         */
        private Duration collectInterval = Duration.ofMinutes(1);
        
        /**
         * 监控数据保留时间
         */
        private Duration retentionPeriod = Duration.ofHours(24);
        
        /**
         * 慢查询阈值
         */
        private Duration slowQueryThreshold = Duration.ofMillis(100);
        
        /**
         * 命中率告警阈值
         */
        private double hitRateAlertThreshold = 0.8;
        
        /**
         * 是否导出到 Prometheus
         */
        private boolean exportToPrometheus = false;
    }
    
    /**
     * 获取所有缓存配置的概要信息
     */
    public Map<String, String> getCacheConfigSummary() {
        Map<String, String> summary = new HashMap<>();
        
        for (Map.Entry<String, CacheConfig> entry : caches.entrySet()) {
            String cacheName = entry.getKey();
            CacheConfig config = entry.getValue();
            
            String configInfo = String.format(
                "TTL: %ds, Prefix: %s, Metrics: %s, Warmup: %s", 
                config.getTtl().getSeconds(),
                config.getKeyPrefix(),
                config.isEnableMetrics(),
                config.isEnableWarmup()
            );
            
            if (config.getDescription() != null) {
                configInfo += ", Desc: " + config.getDescription();
            }
            
            summary.put(cacheName, configInfo);
        }
        
        return summary;
    }
    
    /**
     * 验证配置是否有效
     */
    public boolean validateConfiguration() {
        if (defaultConfig == null) {
            return false;
        }
        
        if (defaultConfig.getTtl() == null || defaultConfig.getTtl().isNegative()) {
            return false;
        }
        
        for (CacheConfig config : caches.values()) {
            if (config.getTtl() == null || config.getTtl().isNegative()) {
                return false;
            }
        }
        
        return true;
    }
}