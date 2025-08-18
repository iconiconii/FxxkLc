package com.codetop.logging;

import org.slf4j.MDC;
import org.springframework.lang.Nullable;

import java.util.UUID;

/**
 * 请求追踪上下文管理
 * 提供TraceId生成和MDC管理功能
 * 
 * @author CodeTop Team
 * @version 1.0.0
 */
public class TraceContext {
    
    // MDC键名常量
    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String OPERATION = "operation";
    public static final String CLIENT_IP = "clientIp";
    public static final String USER_AGENT = "userAgent";
    public static final String REQUEST_URI = "requestUri";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String SESSION_ID = "sessionId";
    
    /**
     * 生成新的TraceId
     * 
     * @return 32位UUID字符串（去掉连字符）
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 设置TraceId到MDC
     * 
     * @param traceId 追踪ID
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }
    
    /**
     * 获取当前TraceId
     * 
     * @return 当前追踪ID，如果不存在则返回null
     */
    @Nullable
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }
    
    /**
     * 设置用户ID到MDC
     * 
     * @param userId 用户ID
     */
    public static void setUserId(@Nullable Long userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId.toString());
        }
    }
    
    /**
     * 获取当前用户ID
     * 
     * @return 当前用户ID，如果不存在则返回null
     */
    @Nullable
    public static String getUserId() {
        return MDC.get(USER_ID);
    }
    
    /**
     * 设置操作名称到MDC
     * 
     * @param operation 操作名称
     */
    public static void setOperation(@Nullable String operation) {
        if (operation != null && !operation.trim().isEmpty()) {
            MDC.put(OPERATION, operation);
        }
    }
    
    /**
     * 获取当前操作名称
     * 
     * @return 当前操作名称，如果不存在则返回null
     */
    @Nullable
    public static String getOperation() {
        return MDC.get(OPERATION);
    }
    
    /**
     * 设置客户端IP到MDC
     * 
     * @param clientIp 客户端IP地址
     */
    public static void setClientIp(@Nullable String clientIp) {
        if (clientIp != null && !clientIp.trim().isEmpty()) {
            MDC.put(CLIENT_IP, clientIp);
        }
    }
    
    /**
     * 获取客户端IP
     * 
     * @return 客户端IP地址，如果不存在则返回null
     */
    @Nullable
    public static String getClientIp() {
        return MDC.get(CLIENT_IP);
    }
    
    /**
     * 设置User-Agent到MDC
     * 
     * @param userAgent 用户代理字符串
     */
    public static void setUserAgent(@Nullable String userAgent) {
        if (userAgent != null && !userAgent.trim().isEmpty()) {
            // 截断过长的User-Agent字符串
            String truncatedUserAgent = userAgent.length() > 200 ? 
                userAgent.substring(0, 200) + "..." : userAgent;
            MDC.put(USER_AGENT, truncatedUserAgent);
        }
    }
    
    /**
     * 获取User-Agent
     * 
     * @return User-Agent字符串，如果不存在则返回null
     */
    @Nullable
    public static String getUserAgent() {
        return MDC.get(USER_AGENT);
    }
    
    /**
     * 设置请求URI到MDC
     * 
     * @param requestUri 请求URI
     */
    public static void setRequestUri(@Nullable String requestUri) {
        if (requestUri != null && !requestUri.trim().isEmpty()) {
            MDC.put(REQUEST_URI, requestUri);
        }
    }
    
    /**
     * 获取请求URI
     * 
     * @return 请求URI，如果不存在则返回null
     */
    @Nullable
    public static String getRequestUri() {
        return MDC.get(REQUEST_URI);
    }
    
    /**
     * 设置请求方法到MDC
     * 
     * @param requestMethod 请求方法
     */
    public static void setRequestMethod(@Nullable String requestMethod) {
        if (requestMethod != null && !requestMethod.trim().isEmpty()) {
            MDC.put(REQUEST_METHOD, requestMethod);
        }
    }
    
    /**
     * 获取请求方法
     * 
     * @return 请求方法，如果不存在则返回null
     */
    @Nullable
    public static String getRequestMethod() {
        return MDC.get(REQUEST_METHOD);
    }
    
    /**
     * 设置会话ID到MDC
     * 
     * @param sessionId 会话ID
     */
    public static void setSessionId(@Nullable String sessionId) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            MDC.put(SESSION_ID, sessionId);
        }
    }
    
    /**
     * 获取会话ID
     * 
     * @return 会话ID，如果不存在则返回null
     */
    @Nullable
    public static String getSessionId() {
        return MDC.get(SESSION_ID);
    }
    
    /**
     * 设置请求相关的上下文信息
     * 
     * @param traceId 追踪ID
     * @param userId 用户ID
     * @param operation 操作名称
     * @param clientIp 客户端IP
     * @param userAgent User-Agent
     * @param requestUri 请求URI
     * @param requestMethod 请求方法
     */
    public static void setRequestContext(
            @Nullable String traceId,
            @Nullable Long userId,
            @Nullable String operation,
            @Nullable String clientIp,
            @Nullable String userAgent,
            @Nullable String requestUri,
            @Nullable String requestMethod) {
        
        if (traceId != null) setTraceId(traceId);
        setUserId(userId);
        setOperation(operation);
        setClientIp(clientIp);
        setUserAgent(userAgent);
        setRequestUri(requestUri);
        setRequestMethod(requestMethod);
    }
    
    /**
     * 清除当前线程的所有MDC上下文
     */
    public static void clear() {
        MDC.clear();
    }
    
    /**
     * 清除特定的MDC键
     * 
     * @param key MDC键名
     */
    public static void remove(String key) {
        MDC.remove(key);
    }
    
    /**
     * 获取当前MDC的所有内容的快照
     * 
     * @return MDC内容映射
     */
    public static java.util.Map<String, String> getCopyOfContextMap() {
        return MDC.getCopyOfContextMap();
    }
}