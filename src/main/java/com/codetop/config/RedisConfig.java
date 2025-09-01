package com.codetop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;


/**
 * Redis配置类 - 手动缓存实现
 * 
 * 使用自定义的序列化方案：
 * 1. Key: String序列化器（可读性好）
 * 2. Value: UniversalJsonRedisSerializer（增强的类型信息处理）
 * 3. 配置RedisTemplate用于手动缓存管理
 * 
 * @author CodeTop FSRS Team
 */
@Configuration
@Slf4j
public class RedisConfig {

    /**
     * 配置RedisTemplate - 用于手动缓存操作
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
}