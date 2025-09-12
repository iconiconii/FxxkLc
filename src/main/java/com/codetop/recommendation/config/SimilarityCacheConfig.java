package com.codetop.recommendation.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Redis cache configuration for similarity components.
 * Provides specific TTL settings for similarity-related caches.
 */
@Configuration
public class SimilarityCacheConfig {
    
    @Autowired
    private SimilarityProperties similarityProperties;
    
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> {
            Map<String, RedisCacheConfiguration> configurationMap = new HashMap<>();
            
            // Configure similarProblems cache with custom TTL
            long ttlMinutes = similarityProperties.getCache().getTtlMinutes();
            RedisCacheConfiguration similarProblemsConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(ttlMinutes))
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                            new org.springframework.data.redis.serializer.StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                            new GenericJackson2JsonRedisSerializer()))
                    .disableCachingNullValues();
            
            configurationMap.put("similarProblems", similarProblemsConfig);
            
            builder.withCacheConfiguration("similarProblems", similarProblemsConfig);
        };
    }
}
