package com.codetop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 业务幂等性服务
 * 
 * 基于Redis实现分布式幂等性保障，支持：
 * - 请求令牌生成和验证
 * - 原子性状态管理
 * - 业务结果缓存
 * - 自动过期清理
 * 
 * @author CodeTop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";
    private static final String RESULT_KEY_PREFIX = "idempotent:result:";
    private static final long DEFAULT_EXPIRE_TIME = 300; // 5分钟过期
    
    /**
     * 生成幂等性令牌
     */
    public String generateIdempotentToken(Long userId, String operation) {
        String token = generateULID();
        String key = buildIdempotentKey(userId, operation, token);
        redisTemplate.opsForValue().set(key, "created", DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
        log.debug("Generated idempotent token: userId={}, operation={}, token={}", userId, operation, token);
        return token;
    }
    
    /**
     * 尝试获取幂等性锁
     * @param userId 用户ID
     * @param operation 操作类型
     * @param token 幂等性令牌
     * @return true=获取成功可以执行, false=重复请求, null=令牌无效
     */
    public Boolean tryLockIdempotent(Long userId, String operation, String token) {
        String key = buildIdempotentKey(userId, operation, token);
        
        String luaScript = """
            if redis.call('exists', KEYS[1]) == 1 then
                local status = redis.call('get', KEYS[1])
                if status == 'created' then
                    redis.call('set', KEYS[1], 'processing', ARGV[1])
                    return 1
                elseif status == 'processing' then
                    return 0  -- 正在处理中
                elseif status == 'completed' then
                    return 0  -- 已经处理完成
                end
            else
                return -1  -- 令牌不存在或已过期
            end
            """;
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = redisTemplate.execute(script, List.of(key), DEFAULT_EXPIRE_TIME);
        
        log.debug("Try lock idempotent: userId={}, operation={}, token={}, result={}", 
                userId, operation, token, result);
        
        if (result == null) return null;
        if (result == 1) return true;
        return false;
    }
    
    /**
     * 完成幂等性处理并缓存结果
     */
    public void completeIdempotent(Long userId, String operation, String token, Object result) {
        String key = buildIdempotentKey(userId, operation, token);
        String resultKey = buildResultKey(userId, operation, token);
        
        String luaScript = """
            redis.call('set', KEYS[1], 'completed', ARGV[1])
            if ARGV[2] ~= nil then
                redis.call('set', KEYS[2], ARGV[2], ARGV[1])
            end
            return 1
            """;
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        redisTemplate.execute(script, List.of(key, resultKey), 
                DEFAULT_EXPIRE_TIME, 
                result != null ? result.toString() : null);
        
        log.debug("Completed idempotent: userId={}, operation={}, token={}", userId, operation, token);
    }
    
    /**
     * 获取缓存的处理结果
     */
    public Object getCachedResult(Long userId, String operation, String token) {
        String resultKey = buildResultKey(userId, operation, token);
        Object result = redisTemplate.opsForValue().get(resultKey);
        log.debug("Get cached result: userId={}, operation={}, token={}, hasResult={}", 
                userId, operation, token, result != null);
        return result;
    }
    
    /**
     * 检查是否为重复请求
     */
    public boolean isDuplicateRequest(Long userId, String operation, String token) {
        String key = buildIdempotentKey(userId, operation, token);
        String status = (String) redisTemplate.opsForValue().get(key);
        boolean isDuplicate = "completed".equals(status) || "processing".equals(status);
        log.debug("Check duplicate request: userId={}, operation={}, token={}, status={}, isDuplicate={}", 
                userId, operation, token, status, isDuplicate);
        return isDuplicate;
    }
    
    /**
     * 基于业务参数生成幂等性key（用于无token场景）
     */
    public String generateBusinessKey(Long userId, String operation, String... params) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(userId).append(":").append(operation);
        for (String param : params) {
            keyBuilder.append(":").append(param);
        }
        return keyBuilder.toString();
    }
    
    /**
     * 基于业务key检查是否重复（用于数据库约束场景）
     */
    public boolean checkBusinessDuplicate(String businessKey, long windowSeconds) {
        String key = "business:" + businessKey;
        String luaScript = """
            if redis.call('exists', KEYS[1]) == 1 then
                return 0  -- 已存在，是重复请求
            else
                redis.call('set', KEYS[1], '1', 'EX', ARGV[1])
                return 1  -- 不存在，可以执行
            end
            """;
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = redisTemplate.execute(script, List.of(key), windowSeconds);
        
        boolean canExecute = result != null && result == 1;
        log.debug("Check business duplicate: businessKey={}, windowSeconds={}, canExecute={}", 
                businessKey, windowSeconds, canExecute);
        return !canExecute;
    }
    
    /**
     * 清理过期的幂等性记录
     */
    public void cleanupExpiredRecords() {
        // Redis自动过期，这里主要用于监控和统计
        log.info("Idempotency cleanup completed at {}", LocalDateTime.now());
    }
    
    private String buildIdempotentKey(Long userId, String operation, String token) {
        return IDEMPOTENT_KEY_PREFIX + userId + ":" + operation + ":" + token;
    }
    
    private String buildResultKey(Long userId, String operation, String token) {
        return RESULT_KEY_PREFIX + userId + ":" + operation + ":" + token;
    }
    
    /**
     * 生成ULID格式的唯一标识符
     * 简化版实现，实际项目可以使用专门的ULID库
     */
    private String generateULID() {
        long timestamp = System.currentTimeMillis();
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return String.format("%013x%s", timestamp, randomPart);
    }
}