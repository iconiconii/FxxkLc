package com.codetop.aspect;

import com.codetop.annotation.Idempotent;
import com.codetop.security.UserPrincipal;
import com.codetop.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.util.DigestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * 幂等性切面处理器
 * 
 * 拦截带有@Idempotent注解的方法，实现统一的幂等性处理逻辑
 * 
 * @author CodeTop Team
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotentAspect {
    
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        
        // 获取当前用户ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            log.warn("Cannot get current user ID for idempotent check: {}", methodName);
            return joinPoint.proceed(); // 无用户信息时直接执行
        }
        
        // 确定操作类型
        String operation = idempotent.operation().isEmpty() ? methodName : idempotent.operation();
        
        try {
            return handleIdempotent(joinPoint, idempotent, userId, operation);
        } catch (Exception e) {
            log.error("Idempotent processing error for userId={}, operation={}: {}", 
                    userId, operation, e.getMessage(), e);
            // 幂等性处理失败时，继续执行原方法，确保业务不被阻断
            return joinPoint.proceed();
        }
    }
    
    private Object handleIdempotent(ProceedingJoinPoint joinPoint, Idempotent idempotent, 
                                   Long userId, String operation) throws Throwable {
        
        switch (idempotent.strategy()) {
            case TOKEN_BASED:
                return handleTokenBased(joinPoint, idempotent, userId, operation);
            case BUSINESS_KEY_BASED:
                return handleBusinessKeyBased(joinPoint, idempotent, userId, operation);
            case DISTRIBUTED_LOCK_BASED:
                return handleDistributedLockBased(joinPoint, idempotent, userId, operation);
            default:
                log.warn("Unknown idempotent strategy: {}", idempotent.strategy());
                return joinPoint.proceed();
        }
    }
    
    /**
     * 处理基于令牌的幂等性
     */
    private Object handleTokenBased(ProceedingJoinPoint joinPoint, Idempotent idempotent,
                                   Long userId, String operation) throws Throwable {
        
        String token = extractTokenFromRequest(joinPoint);
        if (token == null || token.trim().isEmpty()) {
            log.debug("No idempotent token found for userId={}, operation={}, executing directly", 
                    userId, operation);
            return joinPoint.proceed();
        }
        
        // 尝试获取幂等性锁
        Boolean lockResult = idempotencyService.tryLockIdempotent(userId, operation, token);
        
        if (lockResult == null) {
            log.warn("Invalid idempotent token for userId={}, operation={}, token={}", 
                    userId, operation, token);
            return joinPoint.proceed(); // token无效时继续执行
        }
        
        if (!lockResult) {
            // 重复请求，尝试返回缓存结果
            if (idempotent.returnCachedResult()) {
                Object cachedResult = idempotencyService.getCachedResult(userId, operation, token);
                if (cachedResult != null) {
                    log.info("Returning cached result for duplicate request: userId={}, operation={}, token={}", 
                            userId, operation, token);
                    return cachedResult;
                }
            }
            
            // 无缓存结果时返回标准重复请求响应
            log.info("Duplicate request detected: userId={}, operation={}, token={}", 
                    userId, operation, token);
            return ResponseEntity.ok().body(createDuplicateRequestResponse());
        }
        
        // 执行业务逻辑
        Object result = joinPoint.proceed();
        
        // 完成幂等性处理并缓存结果
        if (idempotent.returnCachedResult()) {
            idempotencyService.completeIdempotent(userId, operation, token, result);
        } else {
            idempotencyService.completeIdempotent(userId, operation, token, null);
        }
        
        if (idempotent.logEnabled()) {
            log.info("Idempotent operation completed: userId={}, operation={}, token={}", 
                    userId, operation, token);
        }
        
        return result;
    }
    
    /**
     * 处理基于业务参数的幂等性
     */
    private Object handleBusinessKeyBased(ProceedingJoinPoint joinPoint, Idempotent idempotent,
                                         Long userId, String operation) throws Throwable {
        
        // 生成业务参数hash作为幂等性key
        String businessKey = generateBusinessKey(joinPoint, userId, operation);
        
        // 检查是否重复请求
        boolean isDuplicate = idempotencyService.checkBusinessDuplicate(businessKey, idempotent.expireTime());
        
        if (isDuplicate) {
            log.info("Duplicate business request detected: userId={}, operation={}, businessKey={}", 
                    userId, operation, businessKey);
            return ResponseEntity.ok().body(createDuplicateRequestResponse());
        }
        
        // 执行业务逻辑
        Object result = joinPoint.proceed();
        
        if (idempotent.logEnabled()) {
            log.info("Business idempotent operation completed: userId={}, operation={}, businessKey={}", 
                    userId, operation, businessKey);
        }
        
        return result;
    }
    
    /**
     * 处理基于分布式锁的幂等性
     */
    private Object handleDistributedLockBased(ProceedingJoinPoint joinPoint, Idempotent idempotent,
                                             Long userId, String operation) throws Throwable {
        
        String lockKey = generateBusinessKey(joinPoint, userId, operation);
        
        // 这里应该集成分布式锁服务（如Redisson）
        // 简化实现，使用Redis的SET NX命令
        String businessKey = "lock:" + lockKey;
        boolean isDuplicate = idempotencyService.checkBusinessDuplicate(businessKey, idempotent.expireTime());
        
        if (isDuplicate) {
            log.info("Concurrent request detected, locked: userId={}, operation={}, lockKey={}", 
                    userId, operation, lockKey);
            return ResponseEntity.ok().body(createDuplicateRequestResponse());
        }
        
        try {
            // 执行业务逻辑
            Object result = joinPoint.proceed();
            
            if (idempotent.logEnabled()) {
                log.info("Distributed lock idempotent operation completed: userId={}, operation={}, lockKey={}", 
                        userId, operation, lockKey);
            }
            
            return result;
        } finally {
            // 在实际实现中，这里应该释放分布式锁
            // 当前简化实现依赖Redis过期自动释放
        }
    }
    
    /**
     * 从请求参数中提取幂等性令牌
     */
    private String extractTokenFromRequest(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg != null) {
                String token = extractTokenFromObject(arg);
                if (token != null) {
                    return token;
                }
            }
        }
        return null;
    }
    
    /**
     * 从对象中提取令牌字段
     */
    private String extractTokenFromObject(Object obj) {
        try {
            Class<?> clazz = obj.getClass();
            
            // 尝试常见的token字段名
            String[] tokenFields = {"requestId", "token", "idempotentToken", "idempotentKey"};
            
            for (String fieldName : tokenFields) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value instanceof String && !((String) value).trim().isEmpty()) {
                        return (String) value;
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                    // 字段不存在或无法访问，继续尝试下一个
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract token from object: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 生成业务幂等性key
     */
    private String generateBusinessKey(ProceedingJoinPoint joinPoint, Long userId, String operation) {
        try {
            Object[] args = joinPoint.getArgs();
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("operation", operation);
            params.put("args", args);
            
            String json = objectMapper.writeValueAsString(params);
            return DigestUtils.md5DigestAsHex(json.getBytes());
        } catch (Exception e) {
            log.warn("Failed to generate business key, using fallback: {}", e.getMessage());
            return userId + ":" + operation + ":" + System.currentTimeMillis();
        }
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
     * 创建重复请求的响应
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