package com.codetop.aspect;

import com.codetop.annotation.SimpleIdempotent;
import com.codetop.security.UserPrincipal;
import com.codetop.service.SimpleIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 简化的幂等性切面处理器
 * 
 * 基于MySQL唯一约束的幂等性保障，逻辑简单清晰
 * 
 * @author CodeTop Team
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class SimpleIdempotentAspect {
    
    private final SimpleIdempotencyService simpleIdempotencyService;
    
    @Around("@annotation(simpleIdempotent)")
    public Object around(ProceedingJoinPoint joinPoint, SimpleIdempotent simpleIdempotent) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        
        // 获取当前用户ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            log.debug("No user context for idempotent check: {}", methodName);
            return joinPoint.proceed(); // 无用户信息时直接执行
        }
        
        // 确定操作类型
        String operation = simpleIdempotent.operation().isEmpty() ? methodName : simpleIdempotent.operation();
        
        // 提取requestId
        String requestId = extractRequestIdFromArgs(joinPoint.getArgs());
        if (requestId == null || requestId.trim().isEmpty()) {
            log.debug("No requestId found for operation: {}, proceeding without idempotency check", operation);
            return joinPoint.proceed();
        }
        
        try {
            // 检查是否重复请求
            boolean isDuplicate = simpleIdempotencyService.checkAndRecordRequest(requestId, userId, operation);
            
            if (isDuplicate) {
                log.info("Duplicate request detected: requestId={}, userId={}, operation={}", 
                        requestId, userId, operation);
                
                // 尝试返回缓存结果
                if (simpleIdempotent.returnCachedResult()) {
                    Object cachedResult = simpleIdempotencyService.getCachedResult(requestId, userId, operation);
                    if (cachedResult != null) {
                        log.info("Returning cached result for duplicate request: requestId={}", requestId);
                        return cachedResult;
                    }
                }
                
                // 返回标准重复请求响应
                return ResponseEntity.ok().body(createDuplicateRequestResponse());
            }
            
            // 执行业务逻辑
            Object result = null;
            boolean success = false;
            try {
                result = joinPoint.proceed();
                success = true;
                return result;
            } catch (Exception e) {
                log.error("Business logic execution failed: requestId={}, userId={}, operation={}, error={}", 
                        requestId, userId, operation, e.getMessage());
                throw e;
            } finally {
                // 记录处理结果
                if (simpleIdempotent.returnCachedResult() || !success) {
                    simpleIdempotencyService.completeRequest(requestId, userId, operation, result, success);
                }
            }
            
        } catch (Exception e) {
            log.error("Idempotent processing error: requestId={}, userId={}, operation={}, error={}", 
                    requestId, userId, operation, e.getMessage(), e);
            // 幂等性处理失败时继续执行业务逻辑，确保不阻断正常流程
            return joinPoint.proceed();
        }
    }
    
    /**
     * 从请求参数中提取requestId
     */
    private String extractRequestIdFromArgs(Object[] args) {
        for (Object arg : args) {
            if (arg != null) {
                String requestId = extractRequestIdFromObject(arg);
                if (requestId != null && !requestId.trim().isEmpty()) {
                    return requestId;
                }
            }
        }
        return null;
    }
    
    /**
     * 从对象中提取requestId字段
     */
    private String extractRequestIdFromObject(Object obj) {
        try {
            Class<?> clazz = obj.getClass();
            String[] requestIdFields = {"requestId", "idempotentToken", "idempotentKey", "token"};
            
            for (String fieldName : requestIdFields) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value instanceof String && !((String) value).trim().isEmpty()) {
                        return ((String) value).trim();
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                    // 字段不存在或无法访问，继续尝试下一个
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract requestId from object: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                return ((UserPrincipal) authentication.getPrincipal()).getId();
            }
        } catch (Exception e) {
            log.debug("Failed to get current user ID: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 创建重复请求响应
     */
    private Map<String, Object> createDuplicateRequestResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Request already processed");
        response.put("code", "DUPLICATE_REQUEST");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}