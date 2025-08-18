package com.codetop.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户上下文工具类
 * 通过ThreadLocal管理当前请求的用户信息
 * 
 * @author CodeTop Team
 */
@Slf4j
public class UserContext {
    
    private static final ThreadLocal<Long> userIdThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> emailThreadLocal = new ThreadLocal<>();
    
    /**
     * 设置当前用户ID
     */
    public static void setUserId(Long userId) {
        userIdThreadLocal.set(userId);
        log.debug("Set user ID in ThreadLocal: {}", userId);
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        return userIdThreadLocal.get();
    }
    
    /**
     * 设置当前用户名
     */
    public static void setUsername(String username) {
        usernameThreadLocal.set(username);
    }
    
    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        return usernameThreadLocal.get();
    }
    
    /**
     * 设置当前用户邮箱
     */
    public static void setEmail(String email) {
        emailThreadLocal.set(email);
    }
    
    /**
     * 获取当前用户邮箱
     */
    public static String getCurrentEmail() {
        return emailThreadLocal.get();
    }
    
    /**
     * 设置用户完整信息
     */
    public static void setUserInfo(Long userId, String username, String email) {
        setUserId(userId);
        setUsername(username);
        setEmail(email);
    }
    
    /**
     * 检查是否有用户登录
     */
    public static boolean hasUser() {
        return getCurrentUserId() != null;
    }
    
    /**
     * 清理ThreadLocal，防止内存泄漏
     * 必须在请求结束时调用
     */
    public static void clear() {
        userIdThreadLocal.remove();
        usernameThreadLocal.remove();
        emailThreadLocal.remove();
        log.debug("Cleared UserContext ThreadLocal");
    }
}