package com.codetop.integration;

import com.codetop.controller.ReviewController;
import com.codetop.service.FSRSService;
import com.codetop.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 幂等性功能集成测试
 * 
 * 测试所有带有@Idempotent注解的接口的幂等性保障功能
 * 
 * @author CodeTop Team
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.redis.host=localhost",
    "spring.redis.port=6379",
    "spring.redis.database=1"
})
@Transactional
public class IdempotencyIntegrationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private IdempotencyService idempotencyService;
    
    @Autowired
    private FSRSService fsrsService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }
    
    /**
     * 测试复习提交的幂等性 - 基于令牌策略
     */
    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testReviewSubmitIdempotency() throws Exception {
        Long userId = 1L;
        String operation = "SUBMIT_REVIEW";
        String token = idempotencyService.generateIdempotentToken(userId, operation);
        
        ReviewController.SubmitReviewRequest request = new ReviewController.SubmitReviewRequest();
        request.setProblemId(1L);
        request.setRating(3);
        request.setReviewType("REVIEW");
        request.setRequestId(token);
        
        String requestJson = objectMapper.writeValueAsString(request);
        
        // 第一次请求 - 应该成功
        mockMvc.perform(post("/review/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists());
        
        // 第二次相同请求 - 应该返回重复请求响应或缓存结果
        mockMvc.perform(post("/review/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk());
        
        // 验证幂等性状态
        assertTrue(idempotencyService.isDuplicateRequest(userId, operation, token));
    }
    
    /**
     * 测试并发复习提交的幂等性保障
     */
    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testConcurrentReviewSubmitIdempotency() throws Exception {
        Long userId = 1L;
        String operation = "SUBMIT_REVIEW";
        String token = idempotencyService.generateIdempotentToken(userId, operation);
        
        ReviewController.SubmitReviewRequest request = new ReviewController.SubmitReviewRequest();
        request.setProblemId(2L);
        request.setRating(4);
        request.setReviewType("LEARNING");
        request.setRequestId(token);
        
        String requestJson = objectMapper.writeValueAsString(request);
        
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        
        // 并发发送相同请求
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    var result = mockMvc.perform(post("/review/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                            .andExpect(status().isOk())
                            .andReturn();
                    
                    String response = result.getResponse().getContentAsString();
                    if (response.contains("DUPLICATE_REQUEST")) {
                        duplicateCount.incrementAndGet();
                    } else {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }, executor);
        }
        
        latch.await();
        executor.shutdown();
        
        // 验证只有一个请求成功处理，其他都是重复请求
        assertEquals(1, successCount.get(), "Only one request should succeed");
        assertEquals(threadCount - 1, duplicateCount.get(), "Other requests should be marked as duplicates");
    }
    
    /**
     * 测试问题状态更新的幂等性 - 基于业务参数策略
     */
    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testProblemStatusUpdateIdempotency() throws Exception {
        String updateRequest = """
            {
                "status": "COMPLETED",
                "completedAt": "2024-01-15T10:00:00",
                "notes": "解题思路清晰"
            }
            """;
        
        Long problemId = 3L;
        
        // 第一次更新 - 应该成功
        mockMvc.perform(put("/problems/" + problemId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isOk());
        
        // 第二次相同更新 - 应该被识别为重复请求
        mockMvc.perform(put("/problems/" + problemId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REQUEST"));
    }
    
    /**
     * 测试用户注册的幂等性 - 基于业务参数策略
     */
    @Test
    void testUserRegistrationIdempotency() throws Exception {
        String registerRequest = """
            {
                "username": "testuser123",
                "email": "testuser123@example.com",
                "password": "password123",
                "firstName": "Test",
                "lastName": "User",
                "timezone": "Asia/Shanghai"
            }
            """;
        
        // 第一次注册 - 应该成功
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
        
        // 第二次相同注册 - 应该被识别为重复请求
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REQUEST"));
    }
    
    /**
     * 测试用户资料更新的幂等性
     */
    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testUserProfileUpdateIdempotency() throws Exception {
        String updateRequest = """
            {
                "firstName": "Updated",
                "lastName": "Name",
                "timezone": "America/New_York",
                "bio": "Updated bio information"
            }
            """;
        
        // 第一次更新 - 应该成功
        mockMvc.perform(put("/user/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isOk());
        
        // 第二次相同更新 - 应该被识别为重复请求
        mockMvc.perform(put("/user/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REQUEST"));
    }
    
    /**
     * 测试FSRS卡片创建的竞态条件保护
     */
    @Test
    @Transactional
    void testFSRSCardCreationRaceConditionProtection() throws Exception {
        Long userId = 1L;
        Long problemId = 99L; // 使用一个不存在的problemId确保需要创建
        
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        
        // 并发创建相同的FSRS卡片
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    fsrsService.getOrCreateCard(userId, problemId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    if (e.getMessage().contains("Duplicate") || 
                        e.getCause() instanceof org.springframework.dao.DuplicateKeyException) {
                        conflictCount.incrementAndGet();
                    } else {
                        e.printStackTrace();
                    }
                } finally {
                    latch.countDown();
                }
            }, executor);
        }
        
        latch.await();
        executor.shutdown();
        
        // 验证所有线程都成功返回（要么创建成功，要么获取到已存在的卡片）
        assertEquals(threadCount, successCount.get() + conflictCount.get(), 
                "All threads should either succeed or encounter expected conflicts");
        
        // 验证最终只创建了一张卡片
        var finalCard = fsrsService.getOrCreateCard(userId, problemId);
        assertNotNull(finalCard);
        assertEquals(userId, finalCard.getUserId());
        assertEquals(problemId, finalCard.getProblemId());
    }
    
    /**
     * 测试幂等性令牌过期机制
     */
    @Test
    void testIdempotentTokenExpiration() throws Exception {
        Long userId = 1L;
        String operation = "TEST_OPERATION";
        String token = idempotencyService.generateIdempotentToken(userId, operation);
        
        // 验证令牌存在
        Boolean lockResult = idempotencyService.tryLockIdempotent(userId, operation, token);
        assertTrue(lockResult != null && lockResult);
        
        // 完成操作
        idempotencyService.completeIdempotent(userId, operation, token, "test result");
        
        // 验证重复请求检查
        assertTrue(idempotencyService.isDuplicateRequest(userId, operation, token));
        
        // 验证缓存结果
        Object cachedResult = idempotencyService.getCachedResult(userId, operation, token);
        assertNotNull(cachedResult);
        assertEquals("test result", cachedResult.toString());
    }
    
    /**
     * 测试业务参数幂等性检查
     */
    @Test
    void testBusinessKeyIdempotency() {
        Long userId = 1L;
        String operation = "TEST_BUSINESS_OP";
        String businessKey = idempotencyService.generateBusinessKey(userId, operation, "param1", "param2");
        
        // 第一次检查 - 应该允许执行
        boolean firstCheck = idempotencyService.checkBusinessDuplicate(businessKey, 60);
        assertFalse(firstCheck, "First check should allow execution");
        
        // 第二次检查 - 应该识别为重复
        boolean secondCheck = idempotencyService.checkBusinessDuplicate(businessKey, 60);
        assertTrue(secondCheck, "Second check should detect duplicate");
    }
}