package com.codetop.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IdempotencyService单元测试
 * 
 * @author CodeTop Team
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.redis.host=localhost",
    "spring.redis.port=6379",
    "spring.redis.database=1"
})
public class IdempotencyServiceTest {
    
    @Autowired
    private IdempotencyService idempotencyService;
    
    @Test
    void testGenerateIdempotentToken() {
        Long userId = 1L;
        String operation = "TEST_OPERATION";
        
        String token = idempotencyService.generateIdempotentToken(userId, operation);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.length() > 20); // ULID应该比较长
    }
    
    @Test
    void testTryLockIdempotent() {
        Long userId = 1L;
        String operation = "TEST_LOCK";
        String token = idempotencyService.generateIdempotentToken(userId, operation);
        
        // 第一次获取锁应该成功
        Boolean firstLock = idempotencyService.tryLockIdempotent(userId, operation, token);
        assertTrue(firstLock != null && firstLock);
        
        // 第二次获取锁应该失败（已在处理中）
        Boolean secondLock = idempotencyService.tryLockIdempotent(userId, operation, token);
        assertTrue(secondLock != null && !secondLock);
    }
    
    @Test
    void testCompleteIdempotentAndGetCachedResult() {
        Long userId = 1L;
        String operation = "TEST_COMPLETE";
        String token = idempotencyService.generateIdempotentToken(userId, operation);
        
        // 获取锁
        Boolean lockResult = idempotencyService.tryLockIdempotent(userId, operation, token);
        assertTrue(lockResult != null && lockResult);
        
        // 完成处理并缓存结果
        String testResult = "test-result-data";
        idempotencyService.completeIdempotent(userId, operation, token, testResult);
        
        // 验证可以获取缓存结果
        Object cachedResult = idempotencyService.getCachedResult(userId, operation, token);
        assertEquals(testResult, cachedResult);
        
        // 验证重复请求检查
        assertTrue(idempotencyService.isDuplicateRequest(userId, operation, token));
    }
    
    @Test
    void testGenerateBusinessKey() {
        Long userId = 1L;
        String operation = "TEST_BUSINESS";
        String[] params = {"param1", "param2", "param3"};
        
        String businessKey = idempotencyService.generateBusinessKey(userId, operation, params);
        
        assertNotNull(businessKey);
        assertTrue(businessKey.contains(userId.toString()));
        assertTrue(businessKey.contains(operation));
        
        // 相同参数应该生成相同的key
        String businessKey2 = idempotencyService.generateBusinessKey(userId, operation, params);
        assertEquals(businessKey, businessKey2);
        
        // 不同参数应该生成不同的key
        String[] differentParams = {"param1", "param2", "param4"};
        String businessKey3 = idempotencyService.generateBusinessKey(userId, operation, differentParams);
        assertNotEquals(businessKey, businessKey3);
    }
    
    @Test
    void testCheckBusinessDuplicate() {
        String businessKey = "test-business-key-" + System.currentTimeMillis();
        long windowSeconds = 60;
        
        // 第一次检查应该允许执行
        boolean firstCheck = idempotencyService.checkBusinessDuplicate(businessKey, windowSeconds);
        assertFalse(firstCheck, "First check should allow execution");
        
        // 第二次检查应该识别为重复
        boolean secondCheck = idempotencyService.checkBusinessDuplicate(businessKey, windowSeconds);
        assertTrue(secondCheck, "Second check should detect duplicate");
    }
    
    @Test
    void testInvalidToken() {
        Long userId = 1L;
        String operation = "TEST_INVALID";
        String invalidToken = "invalid-token-123";
        
        // 使用无效token应该返回null
        Boolean lockResult = idempotencyService.tryLockIdempotent(userId, operation, invalidToken);
        assertNull(lockResult);
        
        // 无效token不应该被认为是重复请求
        assertFalse(idempotencyService.isDuplicateRequest(userId, operation, invalidToken));
        
        // 无效token应该没有缓存结果
        assertNull(idempotencyService.getCachedResult(userId, operation, invalidToken));
    }
}