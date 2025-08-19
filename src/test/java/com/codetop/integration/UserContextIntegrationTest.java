package com.codetop.integration;

import com.codetop.CodetopFsrsApplication;
import com.codetop.controller.InterviewReportController;
import com.codetop.dto.SubmitReportRequest;
import com.codetop.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户上下文管理集成测试
 * 
 * 验证重构后的用户上下文管理功能，包括：
 * - ThreadLocal正确设置和清理
 * - @CurrentUserId注解正确解析
 * - Controller方法正常工作
 * - 异常处理正确
 */
@SpringBootTest(classes = CodetopFsrsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserContextIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InterviewReportController interviewReportController;

    @BeforeEach
    void setUp() {
        // 重置ThreadLocal
        UserContext.clear();
    }

    @Test
    void testUserContextBasicOperations() {
        // 测试UserContext基本操作
        
        // 1. 测试设置用户ID
        UserContext.setUserId(123L);
        assertEquals(Long.valueOf(123), UserContext.getCurrentUserId());
        
        // 2. 测试设置用户名和邮箱
        UserContext.setUsername("testuser");
        UserContext.setEmail("test@example.com");
        
        assertEquals("testuser", UserContext.getCurrentUsername());
        assertEquals("test@example.com", UserContext.getCurrentEmail());
        
        // 3. 测试检查用户登录状态
        assertTrue(UserContext.hasUser());
        
        // 4. 测试清理
        UserContext.clear();
        assertNull(UserContext.getCurrentUserId());
        assertNull(UserContext.getCurrentUsername());
        assertNull(UserContext.getCurrentEmail());
        assertFalse(UserContext.hasUser());
    }

    @Test
    void testUserContextThreadSafety() throws Exception {
        // 测试多线程环境下的用户上下文隔离
        
        final int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        final boolean[] results = new boolean[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    // 模拟设置用户上下文
                    UserContext.setUserId((long) (threadId + 1));
                    UserContext.setUsername("user" + (threadId + 1));
                    UserContext.setEmail("user" + (threadId + 1) + "@example.com");
                    
                    // 验证当前线程的上下文
                    assertEquals(Long.valueOf(threadId + 1), UserContext.getCurrentUserId());
                    assertEquals("user" + (threadId + 1), UserContext.getCurrentUsername());
                    assertEquals("user" + (threadId + 1) + "@example.com", UserContext.getCurrentEmail());
                    
                    // 模拟处理时间
                    Thread.sleep(50);
                    
                    // 再次验证
                    assertEquals(Long.valueOf(threadId + 1), UserContext.getCurrentUserId());
                    assertTrue(UserContext.hasUser());
                    
                    results[threadId] = true;
                } catch (Exception e) {
                    results[threadId] = false;
                    e.printStackTrace();
                } finally {
                    // 清理
                    UserContext.clear();
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证所有线程都成功
        for (boolean result : results) {
            assertTrue(result);
        }
        
        // 验证主线程的上下文仍然为空
        assertNull(UserContext.getCurrentUserId());
        assertNull(UserContext.getCurrentUsername());
        assertNull(UserContext.getCurrentEmail());
    }

    @Test
    void testUserContextWithValidToken() throws Exception {
        // 测试有效Token的用户上下文管理
        
        // 1. 准备测试数据
        SubmitReportRequest request = SubmitReportRequest.builder()
                .company("测试公司")
                .department("技术部")
                .position("后端开发")
                .problemSearch("两数之和")
                .date("2024-01-01")
                .additionalNotes("测试面试报告")
                .difficultyRating(3)
                .build();
        
        // 2. 模拟设置用户上下文（在实际场景中由JwtAuthenticationFilter设置）
        UserContext.setUserId(1L);
        UserContext.setUsername("testuser");
        
        // 3. 执行请求
        MvcResult result = mockMvc.perform(post("/api/v1/interview-reports")
                .header("Authorization", "Bearer valid-token")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        
        // 4. 验证响应
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("success") || response.contains("reportId"));
        
        // 5. 验证ThreadLocal已被清理
        assertNull(UserContext.getCurrentUserId());
    }

    @Test
    void testUserContextWithoutToken() throws Exception {
        // 测试无Token的处理
        
        SubmitReportRequest request = SubmitReportRequest.builder()
                .company("测试公司")
                .build();
        
        mockMvc.perform(post("/api/v1/interview-reports")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        
        // 验证ThreadLocal未被设置
        assertNull(UserContext.getCurrentUserId());
    }

    @Test
    void testUserContextMemoryLeak() throws Exception {
        // 测试用户上下文内存泄漏
        
        // 模拟大量请求
        for (int i = 0; i < 1000; i++) {
            try {
                UserContext.setUserId((long) i);
                UserContext.setUsername("user" + i);
                UserContext.setEmail("user" + i + "@example.com");
                
                assertNotNull(UserContext.getCurrentUserId());
                assertNotNull(UserContext.getCurrentUsername());
                assertNotNull(UserContext.getCurrentEmail());
                assertTrue(UserContext.hasUser());
                
                UserContext.clear();
            } catch (Exception e) {
                fail("用户上下文管理出现异常: " + e.getMessage());
            }
        }
        
        // 最终验证
        assertNull(UserContext.getCurrentUserId());
        assertNull(UserContext.getCurrentUsername());
        assertNull(UserContext.getCurrentEmail());
        assertFalse(UserContext.hasUser());
    }

    @Test
    void testUserContextWithDifferentEndpoints() throws Exception {
        // 测试不同端点的用户上下文
        
        // 模拟用户登录
        UserContext.setUserId(1L);
        UserContext.setUsername("testuser");
        
        // 测试获取用户排名信息
        MvcResult result = mockMvc.perform(get("/api/v1/leaderboard/user/rank")
                .header("Authorization", "Bearer rank-test-token")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        // 验证响应包含预期的字段
        assertTrue(response.contains("rank") || response.contains("totalSolved") || 
                   response.contains("userId") || response.contains("username"));
        
        // 验证ThreadLocal已清理
        assertNull(UserContext.getCurrentUserId());
    }

    @Test
    void testUserContextEdgeCases() {
        // 测试边界情况
        
        // 1. 测试null值
        UserContext.setUserId(null);
        assertNull(UserContext.getCurrentUserId());
        assertFalse(UserContext.hasUser());
        
        // 2. 测试重复设置
        UserContext.setUserId(1L);
        UserContext.setUserId(2L);
        assertEquals(Long.valueOf(2), UserContext.getCurrentUserId());
        
        // 3. 测试重复清理
        UserContext.clear();
        UserContext.clear(); // 应该不会出错
        assertNull(UserContext.getCurrentUserId());
        
        // 4. 测试部分设置
        UserContext.setUserId(1L);
        // 只设置ID，不设置username和email
        assertTrue(UserContext.hasUser());
        assertNull(UserContext.getCurrentUsername());
        assertNull(UserContext.getCurrentEmail());
        
        UserContext.clear();
    }

    @Test
    void testUserContextSetUserInfo() {
        // 测试setUserInfo方法
        
        // 一次性设置所有用户信息
        UserContext.setUserInfo(123L, "fulluser", "full@example.com");
        
        // 验证所有信息都正确设置
        assertEquals(Long.valueOf(123), UserContext.getCurrentUserId());
        assertEquals("fulluser", UserContext.getCurrentUsername());
        assertEquals("full@example.com", UserContext.getCurrentEmail());
        assertTrue(UserContext.hasUser());
        
        // 清理
        UserContext.clear();
        
        // 验证清理后的状态
        assertNull(UserContext.getCurrentUserId());
        assertNull(UserContext.getCurrentUsername());
        assertNull(UserContext.getCurrentEmail());
        assertFalse(UserContext.hasUser());
    }
}