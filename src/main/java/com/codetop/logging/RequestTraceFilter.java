package com.codetop.logging;

import com.codetop.security.UserPrincipal;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 请求追踪过滤器
 * 为每个HTTP请求生成TraceId，并记录请求信息到MDC
 * 
 * @author CodeTop Team
 * @version 1.0.0
 */
@Component
@Order(1)
public class RequestTraceFilter implements Filter {
    
    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);
    private static final Logger accessLog = LoggerFactory.getLogger("com.codetop.access");
    
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String REQUEST_START_TIME = "requestStartTime";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 设置请求追踪上下文
            setupTraceContext(httpRequest, startTime);
            
            // 记录请求开始
            logRequestStart(httpRequest);
            
            // 继续执行过滤器链
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            // 记录异常
            log.error("Request processing failed for {} {}: {}", 
                httpRequest.getMethod(), httpRequest.getRequestURI(), e.getMessage(), e);
            throw e;
        } finally {
            // 记录请求完成并清理上下文
            logRequestEnd(httpRequest, httpResponse, startTime);
            TraceContext.clear();
        }
    }
    
    /**
     * 设置请求追踪上下文
     */
    private void setupTraceContext(HttpServletRequest request, long startTime) {
        // 生成或获取TraceId
        String traceId = getOrGenerateTraceId(request);
        
        // 获取客户端IP
        String clientIp = getClientIpAddress(request);
        
        // 获取用户信息
        Long userId = getCurrentUserId();
        
        // 获取操作名称
        String operation = getOperationName(request);
        
        // 设置追踪上下文
        TraceContext.setRequestContext(
            traceId,
            userId,
            operation,
            clientIp,
            request.getHeader("User-Agent"),
            request.getRequestURI(),
            request.getMethod()
        );
        
        // 设置会话ID
        if (request.getSession(false) != null) {
            TraceContext.setSessionId(request.getSession().getId());
        }
        
        // 将开始时间存储在请求属性中
        request.setAttribute(REQUEST_START_TIME, startTime);
        
        log.debug("Request trace context initialized - TraceId: {}, UserId: {}, Operation: {}, ClientIP: {}", 
            traceId, userId, operation, clientIp);
    }
    
    /**
     * 获取或生成TraceId
     */
    private String getOrGenerateTraceId(HttpServletRequest request) {
        // 首先检查请求头中是否有TraceId
        String traceId = request.getHeader(TRACE_ID_HEADER);
        
        if (traceId == null || traceId.trim().isEmpty()) {
            // 生成新的TraceId
            traceId = TraceContext.generateTraceId();
        }
        
        return traceId;
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 如果是多个IP，取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                return userPrincipal.getId();
            }
        } catch (Exception e) {
            log.debug("Failed to get current user ID: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 根据请求路径生成操作名称
     */
    private String getOperationName(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        // 移除上下文路径
        String contextPath = request.getContextPath();
        if (uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        
        // 生成简化的操作名称
        if (uri.startsWith("/api/v1")) {
            uri = uri.substring(7); // 移除 "/api/v1"
        }
        
        // 根据路径模式生成操作名称
        String operation = generateOperationFromPath(method, uri);
        
        return operation;
    }
    
    /**
     * 根据HTTP方法和路径生成操作名称
     */
    private String generateOperationFromPath(String method, String path) {
        // 移除查询参数
        if (path.contains("?")) {
            path = path.substring(0, path.indexOf("?"));
        }
        
        // 特殊路径映射
        if (path.equals("/auth/login")) return "USER_LOGIN";
        if (path.equals("/auth/logout")) return "USER_LOGOUT";
        if (path.equals("/auth/register")) return "USER_REGISTER";
        if (path.equals("/auth/refresh")) return "TOKEN_REFRESH";
        
        if (path.startsWith("/codetop/problems")) return "PROBLEM_" + method;
        if (path.startsWith("/review")) return "REVIEW_" + method;
        if (path.startsWith("/analytics")) return "ANALYTICS_" + method;
        if (path.startsWith("/leaderboard")) return "LEADERBOARD_" + method;
        if (path.startsWith("/filter")) return "FILTER_" + method;
        
        // 默认格式：METHOD_PATH
        String sanitizedPath = path.replaceAll("[^a-zA-Z0-9/_-]", "")
                                  .replaceAll("/+", "_")
                                  .replaceAll("^_|_$", "")
                                  .toUpperCase();
        
        return method + "_" + sanitizedPath;
    }
    
    /**
     * 记录请求开始
     */
    private void logRequestStart(HttpServletRequest request) {
        if (isAccessLogEnabled(request)) {
            accessLog.info("Request started - {} {} from {} [{}]",
                request.getMethod(),
                request.getRequestURI(),
                TraceContext.getClientIp(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
        }
        
        log.debug("Processing request: {} {} - TraceId: {}", 
            request.getMethod(), request.getRequestURI(), TraceContext.getTraceId());
    }
    
    /**
     * 记录请求完成
     */
    private void logRequestEnd(HttpServletRequest request, HttpServletResponse response, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        
        if (isAccessLogEnabled(request)) {
            accessLog.info("Request completed - {} {} - Status: {} - Duration: {}ms - TraceId: {}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration,
                TraceContext.getTraceId()
            );
        }
        
        // 记录慢请求
        if (duration > 3000) { // 3秒以上的请求
            log.warn("Slow request detected - {} {} - Duration: {}ms - Status: {} - TraceId: {}",
                request.getMethod(),
                request.getRequestURI(),
                duration,
                response.getStatus(),
                TraceContext.getTraceId()
            );
        }
        
        log.debug("Request processing completed - Duration: {}ms - Status: {} - TraceId: {}", 
            duration, response.getStatus(), TraceContext.getTraceId());
    }
    
    /**
     * 判断是否需要记录访问日志
     */
    private boolean isAccessLogEnabled(HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        // 排除静态资源和健康检查
        return !uri.endsWith(".css") && 
               !uri.endsWith(".js") && 
               !uri.endsWith(".ico") && 
               !uri.endsWith(".png") && 
               !uri.endsWith(".jpg") && 
               !uri.endsWith(".gif") &&
               !uri.contains("/actuator/health") &&
               !uri.contains("/swagger-ui") &&
               !uri.contains("/api-docs");
    }
}