package com.codetop.service.cache.impl;

import com.codetop.service.cache.CacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis缓存服务实现
 * 
 * 基于RedisTemplate实现统一的缓存服务接口，提供高性能的缓存操作，
 * 支持对象序列化、统计信息收集和异常处理。
 * 
 * @author CodeTop FSRS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // 默认TTL - 1小时
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    
    // 统计计数器
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    
    @Override
    public <T> void put(String key, T value, Duration ttl) {
        try {
            if (key == null || value == null) {
                log.warn("Cache put operation ignored - key or value is null: key={}", key);
                return;
            }
            
            redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("Cache PUT: key={}, ttl={}ms", key, ttl.toMillis());
            
        } catch (Exception e) {
            log.error("Cache put operation failed: key={}, error={}", key, e.getMessage(), e);
            // 缓存失败不应该影响业务逻辑，这里只记录错误日志
        }
    }
    
    @Override
    public <T> void put(String key, T value) {
        put(key, value, DEFAULT_TTL);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            if (key == null) {
                log.warn("Cache get operation ignored - key is null");
                missCount.incrementAndGet();
                return null;
            }
            
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                log.debug("Cache MISS: key={}", key);
                missCount.incrementAndGet();
                return null;
            }
            
            // 如果缓存的对象已经是目标类型，直接返回
            if (type.isInstance(cached)) {
                log.debug("Cache HIT (direct): key={}", key);
                hitCount.incrementAndGet();
                return (T) cached;
            }
            
            // 否则尝试转换类型
            T result = objectMapper.convertValue(cached, type);
            log.debug("Cache HIT (converted): key={}", key);
            hitCount.incrementAndGet();
            return result;
            
        } catch (Exception e) {
            log.error("Cache get operation failed: key={}, type={}, error={}", key, type.getSimpleName(), e.getMessage(), e);
            missCount.incrementAndGet();
            return null;
        }
    }
    
    @Override
    public <T> List<T> getList(String key, TypeReference<List<T>> typeRef) {
        try {
            if (key == null) {
                log.warn("Cache getList operation ignored - key is null");
                missCount.incrementAndGet();
                return null;
            }
            
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                log.debug("Cache MISS (list): key={}", key);
                missCount.incrementAndGet();
                return null;
            }
            
            List<T> result = objectMapper.convertValue(cached, typeRef);
            log.debug("Cache HIT (list): key={}, size={}", key, result != null ? result.size() : 0);
            hitCount.incrementAndGet();
            return result;
            
        } catch (Exception e) {
            log.error("Cache getList operation failed: key={}, error={}", key, e.getMessage(), e);
            missCount.incrementAndGet();
            return null;
        }
    }
    
    @Override
    public <T> List<T> getList(String key, Class<T> elementType) {
        return getList(key, new TypeReference<List<T>>() {});
    }
    
    @Override
    public boolean delete(String key) {
        try {
            if (key == null) {
                log.warn("Cache delete operation ignored - key is null");
                return false;
            }
            
            Boolean deleted = redisTemplate.delete(key);
            boolean result = deleted != null && deleted;
            log.debug("Cache DELETE: key={}, deleted={}", key, result);
            
            if (result) {
                evictionCount.incrementAndGet();
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Cache delete operation failed: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public long delete(Set<String> keys) {
        try {
            if (keys == null || keys.isEmpty()) {
                log.warn("Cache batch delete operation ignored - keys is null or empty");
                return 0;
            }
            
            Long deleted = redisTemplate.delete(keys);
            long result = deleted != null ? deleted : 0;
            log.debug("Cache BATCH DELETE: keys={}, deleted={}", keys.size(), result);
            
            evictionCount.addAndGet(result);
            
            return result;
            
        } catch (Exception e) {
            log.error("Cache batch delete operation failed: keys={}, error={}", keys.size(), e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public long deleteByPattern(String pattern) {
        try {
            if (pattern == null || pattern.trim().isEmpty()) {
                log.warn("Cache deleteByPattern operation ignored - pattern is null or empty");
                return 0;
            }
            // Use SCAN to avoid blocking the Redis server
            java.util.Set<String> keys = new java.util.HashSet<>();
            org.springframework.data.redis.connection.RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            org.springframework.data.redis.core.ScanOptions options = org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match(pattern)
                    .count(2000)
                    .build();
            try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            if (keys.isEmpty()) {
                log.debug("Cache DELETE BY PATTERN (SCAN): pattern={}, no keys found", pattern);
                return 0;
            }
            Long deleted = redisTemplate.delete(keys);
            long result = deleted != null ? deleted : 0;
            log.debug("Cache DELETE BY PATTERN (SCAN): pattern={}, keys={}, deleted={}", pattern, keys.size(), result);
            evictionCount.addAndGet(result);
            return result;
            
        } catch (Exception e) {
            log.error("Cache deleteByPattern operation failed (SCAN): pattern={}, error={}", pattern, e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public boolean exists(String key) {
        try {
            if (key == null) {
                return false;
            }
            
            Boolean exists = redisTemplate.hasKey(key);
            boolean result = exists != null && exists;
            log.debug("Cache EXISTS: key={}, exists={}", key, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("Cache exists operation failed: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean expire(String key, Duration ttl) {
        try {
            if (key == null || ttl == null) {
                log.warn("Cache expire operation ignored - key or ttl is null");
                return false;
            }
            
            Boolean expired = redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
            boolean result = expired != null && expired;
            log.debug("Cache EXPIRE: key={}, ttl={}ms, success={}", key, ttl.toMillis(), result);
            
            return result;
            
        } catch (Exception e) {
            log.error("Cache expire operation failed: key={}, ttl={}, error={}", key, ttl, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public long getExpire(String key) {
        try {
            if (key == null) {
                return -2;
            }
            
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            long result = ttl != null ? ttl : -2;
            log.debug("Cache GET EXPIRE: key={}, ttl={}ms", key, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("Cache getExpire operation failed: key={}, error={}", key, e.getMessage(), e);
            return -2;
        }
    }
    
    @Override
    public Set<String> keys(String pattern) {
        try {
            if (pattern == null) {
                log.warn("Cache keys operation ignored - pattern is null");
                return Set.of();
            }

            java.util.Set<String> results = new java.util.HashSet<>();
            org.springframework.data.redis.connection.RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            org.springframework.data.redis.core.ScanOptions options = org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match(pattern)
                    .count(1000)
                    .build();
            try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    results.add(new String(cursor.next(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            log.debug("Cache KEYS (SCAN): pattern={}, count={}", pattern, results.size());
            return results;

        } catch (Exception e) {
            log.error("Cache keys operation failed (SCAN): pattern={}, error={}", pattern, e.getMessage(), e);
            return Set.of();
        }
    }
    
    @Override
    public CacheStats getStats() {
        return new CacheStatsImpl(
            hitCount.get(),
            missCount.get(),
            evictionCount.get()
        );
    }
    
    /**
     * 缓存统计信息实现
     */
    private static class CacheStatsImpl implements CacheStats {
        private final long hitCount;
        private final long missCount;
        private final long evictionCount;
        
        public CacheStatsImpl(long hitCount, long missCount, long evictionCount) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.evictionCount = evictionCount;
        }
        
        @Override
        public long getHitCount() {
            return hitCount;
        }
        
        @Override
        public long getMissCount() {
            return missCount;
        }
        
        @Override
        public double getHitRate() {
            long total = getTotalRequests();
            return total > 0 ? (double) hitCount / total : 0.0;
        }
        
        @Override
        public long getTotalRequests() {
            return hitCount + missCount;
        }
        
        @Override
        public long getEvictionCount() {
            return evictionCount;
        }
    }
}
