package com.codetop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis配置类 - Spring Cache最佳实践
 * 
 * 使用自定义的序列化方案：
 * 1. Key: String序列化器（可读性好）
 * 2. Value: UniversalJsonRedisSerializer（增强的类型信息处理）
 * 3. 统一配置RedisTemplate和CacheManager，避免ClassCastException
 * 
 * @author CodeTop FSRS Team
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

    @Value("${cache.redis.ttl:3600}") // 1 hour default
    private long defaultCacheTtl;

    /**
     * 配置RedisTemplate - 使用Spring推荐的序列化器
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // String序列化器用于key
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        // 使用自定义的UniversalJsonRedisSerializer，提供更好的类型信息处理
        UniversalJsonRedisSerializer jsonSerializer = new UniversalJsonRedisSerializer();
        
        // 设置序列化器
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setDefaultSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        
        log.info("Redis template configured with UniversalJsonRedisSerializer for enhanced type handling");
        return template;
    }

    /**
     * 配置缓存管理器 - 与RedisTemplate使用相同的序列化策略
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        
        // 使用与RedisTemplate相同的序列化器配置
        UniversalJsonRedisSerializer jsonSerializer = new UniversalJsonRedisSerializer();
        
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(defaultCacheTtl))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();
        
        log.info("Cache manager configured with TTL: {}s, using UniversalJsonRedisSerializer for enhanced type handling", defaultCacheTtl);
        
        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(connectionFactory)
                .cacheDefaults(defaultConfig)
                .build();
    }
}