package com.codetop.service;

import com.codetop.config.TestContainerConfiguration;
import com.codetop.entity.Problem;
import com.codetop.entity.User;
import com.codetop.event.Events;
import com.codetop.mapper.ProblemMapper;
import com.codetop.mapper.UserMapper;
import com.codetop.util.RedisCacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify the cache consistency fixes:
 * 1. Synchronous cache invalidation (no more @Async)
 * 2. Unified serialization with UniversalJsonRedisSerializer
 * 3. Cache-first invalidation strategy (delete cache before DB update)
 * 
 * @author CodeTop Team
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestContainerConfiguration.class})
@Slf4j
public class CacheConsistencyFixVerificationTest {

    @Autowired
    private CacheInvalidationStrategy cacheInvalidationStrategy;
    
    @Autowired
    private RedisCacheUtil redisCacheUtil;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private ProblemMapper problemMapper;
    
    @Autowired
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        // Clear any existing cache data
        redisTemplate.getConnectionFactory().getConnection().flushDb();
        log.info("Cleared Redis database for test");
    }

    @Test
    @Transactional
    public void testSynchronousCacheInvalidation() {
        log.info("=== Testing Synchronous Cache Invalidation ===");
        
        // Step 1: Create test data and cache it
        String cacheKey = "codetop:problem:single:999";
        Problem testProblem = createTestProblem();
        
        // Cache the problem
        redisTemplate.opsForValue().set(cacheKey, testProblem, 300, TimeUnit.SECONDS);
        
        // Verify cache exists
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(cached, "Cache should contain the test problem");
        log.info("Cache populated successfully with test problem");
        
        // Step 2: Test synchronous cache invalidation
        long startTime = System.currentTimeMillis();
        cacheInvalidationStrategy.invalidateProblemCachesSync();
        long duration = System.currentTimeMillis() - startTime;
        
        // Step 3: Verify cache is immediately cleared
        Object afterInvalidation = redisTemplate.opsForValue().get(cacheKey);
        assertNull(afterInvalidation, "Cache should be cleared immediately after synchronous invalidation");
        
        log.info("✅ Synchronous cache invalidation completed in {}ms", duration);
        assertTrue(duration < 1000, "Synchronous invalidation should complete within 1 second");
    }

    @Test
    public void testSerializationConsistency() {
        log.info("=== Testing Serialization Consistency ===");
        
        // Test with User entity
        User testUser = createTestUser();
        
        RedisCacheUtil.SerializationTestResult userResult = 
            redisCacheUtil.testSerialization("test:user:serialization", testUser, User.class);
        
        log.info("User serialization test result: {}", userResult);
        assertTrue(userResult.isSuccess(), "User serialization should succeed");
        assertTrue(userResult.isDataIntegrityOk(), "User data integrity should be maintained");
        
        // Test with Problem entity
        Problem testProblem = createTestProblem();
        
        RedisCacheUtil.SerializationTestResult problemResult = 
            redisCacheUtil.testSerialization("test:problem:serialization", testProblem, Problem.class);
        
        log.info("Problem serialization test result: {}", problemResult);
        assertTrue(problemResult.isSuccess(), "Problem serialization should succeed");
        assertTrue(problemResult.isDataIntegrityOk(), "Problem data integrity should be maintained");
        
        log.info("✅ Serialization consistency verification passed");
    }

    @Test
    @Transactional
    public void testCacheFirstInvalidationStrategy() {
        log.info("=== Testing Cache-First Invalidation Strategy ===");
        
        // Step 1: Setup - populate cache with test data
        String userCacheKey = "codetop:user:profile:123";
        User testUser = createTestUser();
        testUser.setId(123L);
        
        redisTemplate.opsForValue().set(userCacheKey, testUser, 300, TimeUnit.SECONDS);
        
        // Verify cache exists
        Object cachedBefore = redisTemplate.opsForValue().get(userCacheKey);
        assertNotNull(cachedBefore, "Cache should contain user data before invalidation");
        
        // Step 2: Publish user event which should trigger BEFORE_COMMIT cache invalidation
        Events.UserEvent userEvent = new Events.UserEvent(
            Events.UserEvent.UserEventType.PROFILE_UPDATED, 
            123L, 
            "testuser"
        );
        
        // This should trigger synchronous cache invalidation BEFORE transaction commit
        eventPublisher.publishEvent(userEvent);
        
        // Step 3: Verify cache is cleared immediately (cache-first strategy)
        Object cachedAfter = redisTemplate.opsForValue().get(userCacheKey);
        assertNull(cachedAfter, "Cache should be cleared immediately by cache-first strategy");
        
        log.info("✅ Cache-first invalidation strategy working correctly");
    }

    @Test
    public void testNoAsyncDelayInCacheInvalidation() {
        log.info("=== Testing No Async Delay in Cache Invalidation ===");
        
        // Step 1: Setup cache data
        String problemCacheKey = "codetop:problem:list:difficulty_EASY:page_1:size_20";
        String testData = "cached_problem_list_data";
        
        redisTemplate.opsForValue().set(problemCacheKey, testData, 300, TimeUnit.SECONDS);
        
        // Step 2: Trigger problem event and measure invalidation timing
        long startTime = System.nanoTime();
        
        Events.ProblemEvent problemEvent = new Events.ProblemEvent(
            Events.ProblemEvent.ProblemEventType.UPDATED,
            999L,
            "Test Problem"
        );
        
        eventPublisher.publishEvent(problemEvent);
        
        // Step 3: Check if cache is immediately cleared (no async delay)
        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        
        Object cachedData = redisTemplate.opsForValue().get(problemCacheKey);
        assertNull(cachedData, "Cache should be cleared immediately without async delay");
        
        log.info("✅ Cache invalidation completed in {:.2f}ms (no async delay)", durationMs);
        assertTrue(durationMs < 100, "Synchronous invalidation should complete within 100ms");
    }

    @Test
    public void testRedisCacheStatsConsistency() {
        log.info("=== Testing Redis Cache Stats Consistency ===");
        
        RedisCacheUtil.RedisCacheStats stats = redisCacheUtil.getCacheStats();
        
        log.info("Redis cache stats: {}", stats);
        assertTrue(stats.isConnectionPoolActive(), "Redis connection pool should be active");
        
        // Test cache operations
        redisTemplate.opsForValue().set("test:stats:key1", "value1", 60, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("test:stats:key2", "value2", 60, TimeUnit.SECONDS);
        
        RedisCacheUtil.RedisCacheStats updatedStats = redisCacheUtil.getCacheStats();
        log.info("Updated Redis cache stats: {}", updatedStats);
        
        assertTrue(updatedStats.getTotalKeys() >= 2, "Redis should show at least 2 keys after operations");
        
        // Clean up
        redisTemplate.delete("test:stats:key1");
        redisTemplate.delete("test:stats:key2");
        
        log.info("✅ Redis cache stats consistency verified");
    }

    private User createTestUser() {
        User user = new User();
        user.setId(999L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        // User entity likely uses Boolean deleted field or different approach
        return user;
    }

    private Problem createTestProblem() {
        Problem problem = new Problem();
        problem.setId(999L);
        problem.setTitle("Test Algorithm Problem");
        // Problem entity likely doesn't have setDescription method
        problem.setDifficulty(com.codetop.enums.Difficulty.MEDIUM);  // Use enum instead of string
        problem.setCreatedAt(LocalDateTime.now());
        problem.setUpdatedAt(LocalDateTime.now());
        // Problem entity likely uses Boolean deleted field or different approach
        return problem;
    }
}