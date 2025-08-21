package com.codetop.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis Cache Utility for testing serialization/deserialization and monitoring cache performance.
 * 
 * Simplified to work with standard Spring Redis serialization.
 * 
 * @author CodeTop Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisCacheUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Test serialization and deserialization for any object using RedisTemplate
     */
    public <T> SerializationTestResult testSerialization(String key, T object, Class<T> clazz) {
        SerializationTestResult result = new SerializationTestResult();
        result.setKey(key);
        result.setObjectType(clazz.getSimpleName());
        
        try {
            // Test Redis round-trip
            long redisStart = System.nanoTime();
            redisTemplate.opsForValue().set(key, object, 60, TimeUnit.SECONDS);
            Object retrieved = redisTemplate.opsForValue().get(key);
            long redisTime = System.nanoTime() - redisStart;
            result.setSerializationTimeMs(redisTime / 1_000_000.0);
            
            if (retrieved != null && clazz.isInstance(retrieved)) {
                result.setSuccess(true);
                result.setMessage("Redis serialization/deserialization successful");
                
                // Compare using JSON for deep comparison
                try {
                    String originalJson = objectMapper.writeValueAsString(object);
                    String retrievedJson = objectMapper.writeValueAsString(retrieved);
                    
                    boolean match = originalJson.equals(retrievedJson);
                    result.setDataIntegrityOk(match);
                    
                    if (!match) {
                        result.setMessage("Data integrity check failed");
                    }
                } catch (JsonProcessingException e) {
                    result.setDataIntegrityOk(false);
                    result.setMessage("Failed to verify data integrity: " + e.getMessage());
                }
            } else {
                result.setSuccess(false);
                result.setMessage("Redis deserialization failed");
            }
            
            // Clean up test key
            redisTemplate.delete(key);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Redis test failed: " + e.getMessage());
            log.error("Redis test failed for key: {} with object type: {}", key, clazz.getSimpleName(), e);
        }
        
        return result;
    }

    /**
     * Test bulk serialization performance
     */
    public <T> BulkSerializationTestResult testBulkSerialization(String keyPrefix, List<T> objects, Class<T> clazz) {
        BulkSerializationTestResult result = new BulkSerializationTestResult();
        result.setKeyPrefix(keyPrefix);
        result.setObjectType(clazz.getSimpleName());
        result.setObjectCount(objects.size());
        
        long totalTime = 0;
        int successCount = 0;
        
        try {
            for (int i = 0; i < objects.size(); i++) {
                String key = keyPrefix + ":" + i;
                T object = objects.get(i);
                
                long start = System.nanoTime();
                redisTemplate.opsForValue().set(key, object, 60, TimeUnit.SECONDS);
                Object retrieved = redisTemplate.opsForValue().get(key);
                totalTime += (System.nanoTime() - start);
                
                if (retrieved != null && clazz.isInstance(retrieved)) {
                    successCount++;
                }
                
                // Clean up
                redisTemplate.delete(key);
            }
            
            result.setSuccessCount(successCount);
            result.setAvgSerializationTimeMs(totalTime / (objects.size() * 1_000_000.0));
            result.setSuccessRate((double) successCount / objects.size() * 100);
            result.setMessage(String.format("Bulk Redis test success rate: %.1f%%", result.getSuccessRate()));
            
        } catch (Exception e) {
            result.setMessage("Bulk Redis test failed: " + e.getMessage());
            log.error("Bulk Redis test failed for keyPrefix: {}", keyPrefix, e);
        }
        
        return result;
    }

    /**
     * Get Redis cache statistics
     */
    public RedisCacheStats getCacheStats() {
        RedisCacheStats stats = new RedisCacheStats();
        
        try {
            // Basic Redis info
            Long dbSize = redisTemplate.getConnectionFactory().getConnection().dbSize();
            stats.setTotalKeys(dbSize != null ? dbSize : 0);
            
            // Memory usage (if available)
            try {
                String memoryInfo = redisTemplate.getConnectionFactory().getConnection().info("memory").getProperty("used_memory");
                stats.setMemoryUsed(memoryInfo);
            } catch (Exception e) {
                stats.setMemoryUsed("N/A");
            }
            
            stats.setConnectionPoolActive(true);
            
        } catch (Exception e) {
            log.error("Failed to get Redis cache stats", e);
            stats.setConnectionPoolActive(false);
        }
        
        return stats;
    }

    /**
     * Serialization test result
     */
    public static class SerializationTestResult {
        private String key;
        private String objectType;
        private boolean success;
        private boolean dataIntegrityOk;
        private double serializationTimeMs;
        private double deserializationTimeMs;
        private String message;
        
        // Getters and setters
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        
        public String getObjectType() { return objectType; }
        public void setObjectType(String objectType) { this.objectType = objectType; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public boolean isDataIntegrityOk() { return dataIntegrityOk; }
        public void setDataIntegrityOk(boolean dataIntegrityOk) { this.dataIntegrityOk = dataIntegrityOk; }
        
        public double getSerializationTimeMs() { return serializationTimeMs; }
        public void setSerializationTimeMs(double serializationTimeMs) { this.serializationTimeMs = serializationTimeMs; }
        
        public double getDeserializationTimeMs() { return deserializationTimeMs; }
        public void setDeserializationTimeMs(double deserializationTimeMs) { this.deserializationTimeMs = deserializationTimeMs; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        @Override
        public String toString() {
            return String.format("SerializationTest[key=%s, type=%s, success=%s, integrity=%s, time=%.2fms, message=%s]",
                    key, objectType, success, dataIntegrityOk, serializationTimeMs, message);
        }
    }

    /**
     * Bulk serialization test result
     */
    public static class BulkSerializationTestResult {
        private String keyPrefix;
        private String objectType;
        private int objectCount;
        private int successCount;
        private double successRate;
        private double avgSerializationTimeMs;
        private double avgDeserializationTimeMs;
        private String message;
        
        // Getters and setters
        public String getKeyPrefix() { return keyPrefix; }
        public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
        
        public String getObjectType() { return objectType; }
        public void setObjectType(String objectType) { this.objectType = objectType; }
        
        public int getObjectCount() { return objectCount; }
        public void setObjectCount(int objectCount) { this.objectCount = objectCount; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public double getAvgSerializationTimeMs() { return avgSerializationTimeMs; }
        public void setAvgSerializationTimeMs(double avgSerializationTimeMs) { this.avgSerializationTimeMs = avgSerializationTimeMs; }
        
        public double getAvgDeserializationTimeMs() { return avgDeserializationTimeMs; }
        public void setAvgDeserializationTimeMs(double avgDeserializationTimeMs) { this.avgDeserializationTimeMs = avgDeserializationTimeMs; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        @Override
        public String toString() {
            return String.format("BulkSerializationTest[prefix=%s, type=%s, count=%d, success=%d/%.1f%%, avgTime=%.2fms]",
                    keyPrefix, objectType, objectCount, successCount, successRate, avgSerializationTimeMs);
        }
    }

    /**
     * Redis cache statistics
     */
    public static class RedisCacheStats {
        private long totalKeys;
        private String memoryUsed;
        private boolean connectionPoolActive;
        
        // Getters and setters
        public long getTotalKeys() { return totalKeys; }
        public void setTotalKeys(long totalKeys) { this.totalKeys = totalKeys; }
        
        public String getMemoryUsed() { return memoryUsed; }
        public void setMemoryUsed(String memoryUsed) { this.memoryUsed = memoryUsed; }
        
        public boolean isConnectionPoolActive() { return connectionPoolActive; }
        public void setConnectionPoolActive(boolean connectionPoolActive) { this.connectionPoolActive = connectionPoolActive; }
        
        @Override
        public String toString() {
            return String.format("RedisCacheStats[keys=%d, memory=%s, poolActive=%s]", 
                    totalKeys, memoryUsed, connectionPoolActive);
        }
    }
}