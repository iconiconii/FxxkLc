package com.codetop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify Redis serialization fix
 */
@SpringBootTest
@ActiveProfiles("test")
public class RedisSerializationTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Test
    public void testRedisTemplateSerialization() {
        // Test that RedisTemplate can serialize and deserialize objects properly
        String testKey = "test-serialization-key";
        String testValue = "test-value";
        
        // Test basic string serialization
        redisTemplate.opsForValue().set(testKey, testValue);
        Object retrievedValue = redisTemplate.opsForValue().get(testKey);
        
        assertThat(retrievedValue).isEqualTo(testValue);
        assertThat(retrievedValue).isInstanceOf(String.class);
        
        // Clean up
        redisTemplate.delete(testKey);
        
        System.out.println("✅ Redis template serialization test passed");
    }

    @Test
    public void testCacheManagerConfiguration() {
        // Test that cache manager is properly configured
        assertThat(cacheManager).isNotNull();
        
        String cacheName = "test-cache";
        assertThat(cacheManager.getCache(cacheName)).isNotNull();
        
        System.out.println("✅ Cache manager configuration test passed");
    }
}