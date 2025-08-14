package com.codetop.controller;

import com.codetop.entity.Problem;
import com.codetop.entity.User;
import com.codetop.util.RedisCacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis Cache Test Controller for validating serialization/deserialization.
 * 
 * @author CodeTop Team
 */
@RestController
@RequestMapping("/api/v1/test/redis")
@RequiredArgsConstructor
@Slf4j
public class RedisCacheTestController {

    private final RedisCacheUtil redisCacheUtil;

    /**
     * Test basic serialization with User entity
     */
    @GetMapping("/test-user-serialization")
    public ResponseEntity<RedisCacheUtil.SerializationTestResult> testUserSerialization() {
        // Create a test user
        User testUser = new User();
        testUser.setId(999L);
        testUser.setUsername("redis_test_user");
        testUser.setEmail("test@redis.com");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        RedisCacheUtil.SerializationTestResult result = 
                redisCacheUtil.testSerialization("test:user:serialization", testUser, User.class);
        
        log.info("User serialization test result: {}", result);
        return ResponseEntity.ok(result);
    }

    /**
     * Test basic serialization with Problem entity
     */
    @GetMapping("/test-problem-serialization")
    public ResponseEntity<RedisCacheUtil.SerializationTestResult> testProblemSerialization() {
        // Create a test problem
        Problem testProblem = new Problem();
        testProblem.setId(999L);
        testProblem.setTitle("Redis Test Problem");
        testProblem.setTags("[\"数组\", \"哈希表\", \"Redis测试\"]");
        testProblem.setProblemUrl("https://test.redis.com/problem/999");
        testProblem.setLeetcodeId("redis-999");
        testProblem.setCreatedAt(LocalDateTime.now());
        testProblem.setUpdatedAt(LocalDateTime.now());

        RedisCacheUtil.SerializationTestResult result = 
                redisCacheUtil.testSerialization("test:problem:serialization", testProblem, Problem.class);
        
        log.info("Problem serialization test result: {}", result);
        return ResponseEntity.ok(result);
    }

    /**
     * Test bulk serialization performance
     */
    @GetMapping("/test-bulk-serialization")
    public ResponseEntity<RedisCacheUtil.BulkSerializationTestResult> testBulkSerialization() {
        // Create multiple test users
        List<User> testUsers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setId((long) (1000 + i));
            user.setUsername("bulk_test_user_" + i);
            user.setEmail("bulk_test_" + i + "@redis.com");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            testUsers.add(user);
        }

        RedisCacheUtil.BulkSerializationTestResult result = 
                redisCacheUtil.testBulkSerialization("test:bulk:user", testUsers, User.class);
        
        log.info("Bulk serialization test result: {}", result);
        return ResponseEntity.ok(result);
    }

    /**
     * Get Redis cache statistics
     */
    @GetMapping("/cache-stats")
    public ResponseEntity<RedisCacheUtil.RedisCacheStats> getCacheStats() {
        RedisCacheUtil.RedisCacheStats stats = redisCacheUtil.getCacheStats();
        log.info("Redis cache stats: {}", stats);
        return ResponseEntity.ok(stats);
    }

    /**
     * Test complex object serialization with nested structures
     */
    @GetMapping("/test-complex-serialization")
    public ResponseEntity<String> testComplexSerialization() {
        StringBuilder results = new StringBuilder();
        
        try {
            // Test User
            User user = new User();
            user.setId(777L);
            user.setUsername("complex_test");
            user.setEmail("complex@test.com");
            user.setCreatedAt(LocalDateTime.now());
            
            RedisCacheUtil.SerializationTestResult userResult = 
                    redisCacheUtil.testSerialization("test:complex:user", user, User.class);
            results.append("User Test: ").append(userResult.isSuccess() ? "✅" : "❌").append("\n");
            results.append("  - Serialization: ").append(String.format("%.2fms", userResult.getSerializationTimeMs())).append("\n");
            results.append("  - Deserialization: ").append(String.format("%.2fms", userResult.getDeserializationTimeMs())).append("\n");
            results.append("  - Data Integrity: ").append(userResult.isDataIntegrityOk() ? "✅" : "❌").append("\n\n");

            // Test Problem
            Problem problem = new Problem();
            problem.setId(777L);
            problem.setTitle("Complex Serialization Test");
            problem.setTags("[\"Redis\", \"序列化\", \"测试\"]");
            problem.setCreatedAt(LocalDateTime.now());
            
            RedisCacheUtil.SerializationTestResult problemResult = 
                    redisCacheUtil.testSerialization("test:complex:problem", problem, Problem.class);
            results.append("Problem Test: ").append(problemResult.isSuccess() ? "✅" : "❌").append("\n");
            results.append("  - Serialization: ").append(String.format("%.2fms", problemResult.getSerializationTimeMs())).append("\n");
            results.append("  - Deserialization: ").append(String.format("%.2fms", problemResult.getDeserializationTimeMs())).append("\n");
            results.append("  - Data Integrity: ").append(problemResult.isDataIntegrityOk() ? "✅" : "❌").append("\n\n");

            // Test List of mixed objects
            List<Object> mixedList = List.of(user, problem, "String value", 12345, LocalDateTime.now());
            RedisCacheUtil.SerializationTestResult listResult = 
                    redisCacheUtil.testSerialization("test:complex:list", mixedList, List.class);
            results.append("Mixed List Test: ").append(listResult.isSuccess() ? "✅" : "❌").append("\n");
            results.append("  - Serialization: ").append(String.format("%.2fms", listResult.getSerializationTimeMs())).append("\n");
            results.append("  - Deserialization: ").append(String.format("%.2fms", listResult.getDeserializationTimeMs())).append("\n");
            results.append("  - Data Integrity: ").append(listResult.isDataIntegrityOk() ? "✅" : "❌").append("\n\n");

            // Cache stats
            RedisCacheUtil.RedisCacheStats stats = redisCacheUtil.getCacheStats();
            results.append("Cache Stats:\n");
            results.append("  - Total Keys: ").append(stats.getTotalKeys()).append("\n");
            results.append("  - Memory Used: ").append(stats.getMemoryUsed()).append("\n");
            results.append("  - Connection Active: ").append(stats.isConnectionPoolActive() ? "✅" : "❌").append("\n");

        } catch (Exception e) {
            results.append("❌ Complex serialization test failed: ").append(e.getMessage());
            log.error("Complex serialization test failed", e);
        }
        
        return ResponseEntity.ok(results.toString());
    }
}