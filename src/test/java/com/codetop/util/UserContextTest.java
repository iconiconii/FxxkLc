package com.codetop.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserContext单元测试
 * 
 * 测试用户上下文工具类的基本功能，不需要Spring上下文
 */
public class UserContextTest {

    @BeforeEach
    void setUp() {
        // 每个测试前清理
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        // 每个测试后清理
        UserContext.clear();
    }

    @Test
    void testSetAndGetUserId() {
        // 测试设置和获取用户ID
        assertNull(UserContext.getCurrentUserId(), "初始状态用户ID应为null");
        
        UserContext.setUserId(123L);
        assertEquals(Long.valueOf(123), UserContext.getCurrentUserId(), "用户ID应正确设置");
        
        UserContext.setUserId(456L);
        assertEquals(Long.valueOf(456), UserContext.getCurrentUserId(), "用户ID应能更新");
    }

    @Test
    void testSetAndGetUsername() {
        // 测试设置和获取用户名
        assertNull(UserContext.getCurrentUsername(), "初始状态用户名应为null");
        
        UserContext.setUsername("testuser");
        assertEquals("testuser", UserContext.getCurrentUsername(), "用户名应正确设置");
        
        UserContext.setUsername("newuser");
        assertEquals("newuser", UserContext.getCurrentUsername(), "用户名应能更新");
    }

    @Test
    void testSetAndGetEmail() {
        // 测试设置和获取邮箱
        assertNull(UserContext.getCurrentEmail(), "初始状态邮箱应为null");
        
        UserContext.setEmail("test@example.com");
        assertEquals("test@example.com", UserContext.getCurrentEmail(), "邮箱应正确设置");
        
        UserContext.setEmail("new@example.com");
        assertEquals("new@example.com", UserContext.getCurrentEmail(), "邮箱应能更新");
    }

    @Test
    void testSetUserInfo() {
        // 测试一次性设置所有用户信息
        UserContext.setUserInfo(123L, "fulluser", "full@example.com");
        
        assertEquals(Long.valueOf(123), UserContext.getCurrentUserId(), "用户ID应正确设置");
        assertEquals("fulluser", UserContext.getCurrentUsername(), "用户名应正确设置");
        assertEquals("full@example.com", UserContext.getCurrentEmail(), "邮箱应正确设置");
    }

    @Test
    void testHasUser() {
        // 测试检查用户登录状态
        assertFalse(UserContext.hasUser(), "初始状态应无用户登录");
        
        // 只设置ID
        UserContext.setUserId(123L);
        assertTrue(UserContext.hasUser(), "设置ID后应有用户登录");
        
        UserContext.clear();
        assertFalse(UserContext.hasUser(), "清理后应无用户登录");
        
        // 只设置用户名
        UserContext.setUsername("testuser");
        assertFalse(UserContext.hasUser(), "只设置用户名不算登录");
        
        UserContext.clear();
        
        // 只设置邮箱
        UserContext.setEmail("test@example.com");
        assertFalse(UserContext.hasUser(), "只设置邮箱不算登录");
    }

    @Test
    void testClear() {
        // 测试清理功能
        // 设置所有信息
        UserContext.setUserInfo(123L, "testuser", "test@example.com");
        
        // 验证设置成功
        assertNotNull(UserContext.getCurrentUserId());
        assertNotNull(UserContext.getCurrentUsername());
        assertNotNull(UserContext.getCurrentEmail());
        assertTrue(UserContext.hasUser());
        
        // 清理
        UserContext.clear();
        
        // 验证清理成功
        assertNull(UserContext.getCurrentUserId(), "清理后用户ID应为null");
        assertNull(UserContext.getCurrentUsername(), "清理后用户名应为null");
        assertNull(UserContext.getCurrentEmail(), "清理后邮箱应为null");
        assertFalse(UserContext.hasUser(), "清理后应无用户登录");
    }

    @Test
    void testNullValues() {
        // 测试null值处理
        UserContext.setUserId(null);
        assertNull(UserContext.getCurrentUserId());
        assertFalse(UserContext.hasUser());
        
        UserContext.setUsername(null);
        assertNull(UserContext.getCurrentUsername());
        
        UserContext.setEmail(null);
        assertNull(UserContext.getCurrentEmail());
        
        // 即使设置为null，也不影响已设置的其他值
        UserContext.setUserId(123L);
        UserContext.setUsername(null);
        assertTrue(UserContext.hasUser());
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        // 测试多线程环境下的隔离性
        final int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        final boolean[] results = new boolean[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    // 在每个线程中设置不同的用户信息
                    UserContext.setUserId((long) (threadId + 1));
                    UserContext.setUsername("threaduser" + (threadId + 1));
                    UserContext.setEmail("thread" + (threadId + 1) + "@example.com");
                    
                    // 验证设置正确
                    assertEquals(Long.valueOf(threadId + 1), UserContext.getCurrentUserId());
                    assertEquals("threaduser" + (threadId + 1), UserContext.getCurrentUsername());
                    assertEquals("thread" + (threadId + 1) + "@example.com", UserContext.getCurrentEmail());
                    assertTrue(UserContext.hasUser());
                    
                    // 短暂休眠，增加线程交错的可能性
                    Thread.sleep(50);
                    
                    // 再次验证，确保没有被其他线程修改
                    assertEquals(Long.valueOf(threadId + 1), UserContext.getCurrentUserId());
                    assertEquals("threaduser" + (threadId + 1), UserContext.getCurrentUsername());
                    assertEquals("thread" + (threadId + 1) + "@example.com", UserContext.getCurrentEmail());
                    
                    results[threadId] = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    results[threadId] = false;
                } finally {
                    // 清理当前线程的上下文
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
        
        // 验证所有线程都成功执行
        for (int i = 0; i < threadCount; i++) {
            assertTrue(results[i], "线程 " + i + " 执行失败");
        }
        
        // 验证主线程的上下文仍然为空
        assertNull(UserContext.getCurrentUserId(), "主线程用户ID应为null");
        assertNull(UserContext.getCurrentUsername(), "主线程用户名应为null");
        assertNull(UserContext.getCurrentEmail(), "主线程邮箱应为null");
        assertFalse(UserContext.hasUser(), "主线程应无用户登录");
    }

    @Test
    void testMultipleClearCalls() {
        // 测试多次调用clear不会出错
        UserContext.setUserInfo(123L, "testuser", "test@example.com");
        
        // 多次调用clear
        UserContext.clear();
        UserContext.clear();
        UserContext.clear();
        
        // 验证状态正确
        assertNull(UserContext.getCurrentUserId());
        assertNull(UserContext.getCurrentUsername());
        assertNull(UserContext.getCurrentEmail());
        assertFalse(UserContext.hasUser());
    }

    @Test
    void testPartialInformation() {
        // 测试部分设置用户信息
        // 只设置ID
        UserContext.setUserId(123L);
        assertTrue(UserContext.hasUser());
        assertNull(UserContext.getCurrentUsername());
        assertNull(UserContext.getCurrentEmail());
        
        UserContext.clear();
        
        // 设置ID和用户名
        UserContext.setUserId(123L);
        UserContext.setUsername("testuser");
        assertTrue(UserContext.hasUser());
        assertEquals("testuser", UserContext.getCurrentUsername());
        assertNull(UserContext.getCurrentEmail());
        
        UserContext.clear();
        
        // 设置所有信息
        UserContext.setUserInfo(123L, "testuser", "test@example.com");
        assertTrue(UserContext.hasUser());
        assertNotNull(UserContext.getCurrentUserId());
        assertNotNull(UserContext.getCurrentUsername());
        assertNotNull(UserContext.getCurrentEmail());
    }
}