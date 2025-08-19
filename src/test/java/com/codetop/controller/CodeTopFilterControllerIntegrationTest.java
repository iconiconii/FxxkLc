package com.codetop.controller;

import com.codetop.service.CodeTopFilterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify CodeTop API endpoints work correctly with Redis caching
 */
@SpringBootTest
@ActiveProfiles("test")
public class CodeTopFilterControllerIntegrationTest {

    @Autowired
    private CodeTopFilterService codeTopFilterService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Test
    public void testCodeTopFilterServiceWithCaching() {
        // Test that CodeTopFilterService can handle caching without ClassCastException
        try {
            // Clear any existing cache
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            
            // This should work without throwing ClassCastException
            var response = codeTopFilterService.getGlobalProblems(1, 10, "frequency_score", "desc");
            
            assertThat(response).isNotNull();
            assertThat(response.getProblems()).isNotNull();
            assertThat(response.getCurrentPage()).isEqualTo(1L);
            assertThat(response.getPageSize()).isEqualTo(10L);
            
            System.out.println("✅ CodeTop filter service test passed - no ClassCastException");
            
        } catch (Exception e) {
            System.err.println("❌ CodeTop filter service test failed: " + e.getMessage());
            assertThat(false).isTrue(); // Force test failure
        }
    }

    @Test
    public void testRedisCacheOperations() {
        // Test Redis cache operations
        String testKey = "test-codetop-cache";
        String testValue = "test-value";
        
        // Test basic operations
        redisTemplate.opsForValue().set(testKey, testValue);
        Object retrieved = redisTemplate.opsForValue().get(testKey);
        
        assertThat(retrieved).isEqualTo(testValue);
        
        // Clean up
        redisTemplate.delete(testKey);
        
        System.out.println("✅ Redis cache operations test passed");
    }

    @Test
    public void testCacheManagerAvailability() {
        // Test that cache manager is available and configured
        assertThat(cacheManager).isNotNull();
        
        String cacheName = "codetop-global-problems";
        var cache = cacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();
        
        System.out.println("✅ Cache manager availability test passed");
    }
}