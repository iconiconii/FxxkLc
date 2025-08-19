package com.codetop.util;

import com.codetop.config.UniversalJsonRedisSerializer;
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
 * Updated to use UniversalJsonRedisSerializer for consistency with RedisConfig.
 * This ensures all Redis operations use the same serialization strategy.
 * 
 * @author CodeTop Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisCacheUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Use the same serializer as RedisConfig for consistency
    private final UniversalJsonRedisSerializer universalJsonSerializer = new UniversalJsonRedisSerializer();

    /**
     * Test serialization and deserialization for any object
     * Uses UniversalJsonRedisSerializer for consistency with RedisConfig
     */
    public <T> SerializationTestResult testSerialization(String key, T object, Class<T> clazz) {
        SerializationTestResult result = new SerializationTestResult();
        result.setKey(key);
        result.setObjectType(clazz.getSimpleName());
        
        try {
            // Test direct serialization using UniversalJsonRedisSerializer
            long serializeStart = System.nanoTime();
            byte[] serializedData = universalJsonSerializer.serialize(object);
            long serializeTime = System.nanoTime() - serializeStart;
            result.setSerializationTimeMs(serializeTime / 1_000_000.0);
            
            // Test direct deserialization using UniversalJsonRedisSerializer
            long deserializeStart = System.nanoTime();
            Object retrieved = universalJsonSerializer.deserialize(serializedData);
            long deserializeTime = System.nanoTime() - deserializeStart;
            result.setDeserializationTimeMs(deserializeTime / 1_000_000.0);
            
            // Also test through RedisTemplate to ensure consistency
            redisTemplate.opsForValue().set(key, object, 60, TimeUnit.SECONDS);
            Object redisRetrieved = redisTemplate.opsForValue().get(key);
            
            // Verify correctness - both direct serialization and Redis should work
            if (retrieved != null && redisRetrieved != null && clazz.isInstance(retrieved) && clazz.isInstance(redisRetrieved)) {
                result.setSuccess(true);
                result.setMessage("Serialization/deserialization successful with UniversalJsonRedisSerializer");
                
                // Compare original vs both retrieved objects using JSON for deep comparison
                try {
                    String originalJson = objectMapper.writeValueAsString(object);
                    String directRetrievedJson = objectMapper.writeValueAsString(retrieved);
                    String redisRetrievedJson = objectMapper.writeValueAsString(redisRetrieved);
                    
                    boolean directMatch = originalJson.equals(directRetrievedJson);
                    boolean redisMatch = originalJson.equals(redisRetrievedJson);
                    boolean bothMatch = directRetrievedJson.equals(redisRetrievedJson);
                    
                    result.setDataIntegrityOk(directMatch && redisMatch && bothMatch);
                    
                    if (!result.isDataIntegrityOk()) {
                        result.setMessage(String.format("Data integrity check failed - directMatch=%s, redisMatch=%s, bothMatch=%s", 
                                                       directMatch, redisMatch, bothMatch));
                    }
                } catch (JsonProcessingException e) {
                    result.setDataIntegrityOk(false);
                    result.setMessage("Failed to verify data integrity: " + e.getMessage());
                }
            } else {
                result.setSuccess(false);
                result.setMessage(String.format("Deserialization failed - directOk=%s, redisOk=%s", 
                                               retrieved != null && clazz.isInstance(retrieved),
                                               redisRetrieved != null && clazz.isInstance(redisRetrieved)));
            }
            
            // Clean up test key
            redisTemplate.delete(key);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Serialization test failed: " + e.getMessage());
            log.error("Serialization test failed for key: {} with object type: {}", key, clazz.getSimpleName(), e);
        }
        
        return result;
    }

    /**
     * Test bulk serialization performance using UniversalJsonRedisSerializer
     */
    public <T> BulkSerializationTestResult testBulkSerialization(String keyPrefix, List<T> objects, Class<T> clazz) {
        BulkSerializationTestResult result = new BulkSerializationTestResult();
        result.setKeyPrefix(keyPrefix);
        result.setObjectType(clazz.getSimpleName());
        result.setObjectCount(objects.size());
        
        long totalSerializeTime = 0;
        long totalDeserializeTime = 0;
        long totalRedisTime = 0;
        int successCount = 0;
        int redisSuccessCount = 0;
        
        try {
            for (int i = 0; i < objects.size(); i++) {
                String key = keyPrefix + ":" + i;
                T object = objects.get(i);
                
                // Test direct serialization with UniversalJsonRedisSerializer
                long serializeStart = System.nanoTime();
                byte[] serialized = universalJsonSerializer.serialize(object);
                totalSerializeTime += (System.nanoTime() - serializeStart);
                
                // Test direct deserialization
                long deserializeStart = System.nanoTime();
                Object directRetrieved = universalJsonSerializer.deserialize(serialized);
                totalDeserializeTime += (System.nanoTime() - deserializeStart);
                
                if (directRetrieved != null && clazz.isInstance(directRetrieved)) {
                    successCount++;
                }
                
                // Also test Redis round-trip for verification
                long redisStart = System.nanoTime();
                redisTemplate.opsForValue().set(key, object, 60, TimeUnit.SECONDS);
                Object redisRetrieved = redisTemplate.opsForValue().get(key);
                totalRedisTime += (System.nanoTime() - redisStart);
                
                if (redisRetrieved != null && clazz.isInstance(redisRetrieved)) {
                    redisSuccessCount++;
                }
                
                // Clean up
                redisTemplate.delete(key);
            }
            
            result.setSuccessCount(successCount);
            result.setAvgSerializationTimeMs(totalSerializeTime / (objects.size() * 1_000_000.0));
            result.setAvgDeserializationTimeMs(totalDeserializeTime / (objects.size() * 1_000_000.0));
            result.setSuccessRate((double) successCount / objects.size() * 100);
            
            // Add Redis consistency information
            double redisSuccessRate = (double) redisSuccessCount / objects.size() * 100;
            double avgRedisTimeMs = totalRedisTime / (objects.size() * 1_000_000.0);
            
            result.setMessage(String.format("Direct serialization success: %.1f%%, Redis success: %.1f%%, Redis avg time: %.2fms", 
                                           result.getSuccessRate(), redisSuccessRate, avgRedisTimeMs));
            
        } catch (Exception e) {
            result.setMessage("Bulk serialization test failed: " + e.getMessage());
            log.error("Bulk serialization test failed for keyPrefix: {}", keyPrefix, e);
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
            return String.format("SerializationTest[key=%s, type=%s, success=%s, integrity=%s, serialize=%.2fms, deserialize=%.2fms, message=%s]",
                    key, objectType, success, dataIntegrityOk, serializationTimeMs, deserializationTimeMs, message);
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
            return String.format("BulkSerializationTest[prefix=%s, type=%s, count=%d, success=%d/%.1f%%, avgSerialize=%.2fms, avgDeserialize=%.2fms]",
                    keyPrefix, objectType, objectCount, successCount, successRate, avgSerializationTimeMs, avgDeserializationTimeMs);
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