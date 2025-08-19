package com.codetop;

import com.codetop.dto.CodeTopFilterResponse;
import com.codetop.dto.ProblemRankingDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Redis serialization/deserialization of CodeTopFilterResponse
 */
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class RedisCodeTopFilterResponseTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testCodeTopFilterResponseSerialization() {
        // Create a sample CodeTopFilterResponse with nested objects
        ProblemRankingDTO problem1 = ProblemRankingDTO.builder()
                .problemId(1L)
                .title("Two Sum")
                .difficulty("EASY")
                .frequencyScore(java.math.BigDecimal.valueOf(85.5))
                .addedDate(LocalDate.now())
                .build();

        CodeTopFilterResponse.FilterSummary summary = CodeTopFilterResponse.FilterSummary.builder()
                .totalProblems(100)
                .hotProblems(25)
                .trendingProblems(10)
                .avgFrequencyScore(75.5)
                .mostCommonDifficulty("MEDIUM")
                .mostActiveCompany("Google")
                .build();

        CodeTopFilterResponse originalResponse = CodeTopFilterResponse.builder()
                .problems(Arrays.asList(problem1))
                .totalElements(100L)
                .totalPages(5L)
                .currentPage(1L)
                .pageSize(20L)
                .summary(summary)
                .build();

        String testKey = "test:codetop-response:serialization";

        try {
            // Test Redis serialization/deserialization
            log.info("Testing Redis serialization for CodeTopFilterResponse...");
            
            // Store in Redis
            redisTemplate.opsForValue().set(testKey, originalResponse);
            
            // Retrieve from Redis
            Object retrievedObject = redisTemplate.opsForValue().get(testKey);
            
            log.info("Retrieved object type: {}", retrievedObject.getClass().getName());
            log.info("Retrieved object: {}", retrievedObject);
            
            // This should work without ClassCastException
            assertNotNull(retrievedObject);
            assertTrue(retrievedObject instanceof CodeTopFilterResponse, 
                "Retrieved object should be CodeTopFilterResponse, but was: " + retrievedObject.getClass().getName());
            
            CodeTopFilterResponse retrievedResponse = (CodeTopFilterResponse) retrievedObject;
            
            // Verify the data integrity
            assertEquals(originalResponse.getTotalElements(), retrievedResponse.getTotalElements());
            assertEquals(originalResponse.getProblems().size(), retrievedResponse.getProblems().size());
            assertNotNull(retrievedResponse.getSummary());
            assertEquals(originalResponse.getSummary().getTotalProblems(), retrievedResponse.getSummary().getTotalProblems());
            
            log.info("✅ CodeTopFilterResponse Redis serialization test passed!");
            
        } catch (Exception e) {
            log.error("❌ CodeTopFilterResponse Redis serialization test failed", e);
            fail("CodeTopFilterResponse serialization failed: " + e.getMessage());
        } finally {
            // Cleanup
            redisTemplate.delete(testKey);
        }
    }

    @Test
    public void testDirectJacksonSerialization() {
        // Test Jackson serialization directly
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        try {
            ProblemRankingDTO problem1 = ProblemRankingDTO.builder()
                    .problemId(1L)
                    .title("Two Sum")
                    .difficulty("EASY")
                    .addedDate(LocalDate.now())
                    .build();

            CodeTopFilterResponse.FilterSummary summary = CodeTopFilterResponse.FilterSummary.builder()
                    .totalProblems(100)
                    .hotProblems(25)
                    .trendingProblems(10)
                    .avgFrequencyScore(75.5)
                    .mostCommonDifficulty("MEDIUM")
                    .mostActiveCompany("Google")
                    .build();

            CodeTopFilterResponse originalResponse = CodeTopFilterResponse.builder()
                    .problems(Arrays.asList(problem1))
                    .totalElements(100L)
                    .totalPages(5L)
                    .currentPage(1L)
                    .pageSize(20L)
                    .summary(summary)
                    .build();

            // Test JSON serialization
            String json = objectMapper.writeValueAsString(originalResponse);
            log.info("JSON serialization result: {}", json);
            
            // Test JSON deserialization
            CodeTopFilterResponse deserialized = objectMapper.readValue(json, CodeTopFilterResponse.class);
            
            assertEquals(originalResponse.getTotalElements(), deserialized.getTotalElements());
            assertEquals(originalResponse.getProblems().size(), deserialized.getProblems().size());
            assertNotNull(deserialized.getSummary());
            
            log.info("✅ Direct Jackson serialization test passed!");
            
        } catch (Exception e) {
            log.error("❌ Direct Jackson serialization test failed", e);
            fail("Direct Jackson serialization failed: " + e.getMessage());
        }
    }

    @Test
    public void testGenericJackson2JsonRedisSerializerDirectly() {
        // Test the Redis serializer directly
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        
        try {
            CodeTopFilterResponse.FilterSummary summary = CodeTopFilterResponse.FilterSummary.builder()
                    .totalProblems(100)
                    .hotProblems(25)
                    .trendingProblems(10)
                    .avgFrequencyScore(75.5)
                    .mostCommonDifficulty("MEDIUM")
                    .mostActiveCompany("Google")
                    .build();

            CodeTopFilterResponse originalResponse = CodeTopFilterResponse.builder()
                    .problems(Arrays.asList())
                    .totalElements(100L)
                    .totalPages(5L)
                    .currentPage(1L)
                    .pageSize(20L)
                    .summary(summary)
                    .build();

            // Serialize
            byte[] serialized = serializer.serialize(originalResponse);
            assertNotNull(serialized);
            log.info("Serialized size: {} bytes", serialized.length);
            
            // Deserialize
            Object deserialized = serializer.deserialize(serialized);
            assertNotNull(deserialized);
            log.info("Deserialized object type: {}", deserialized.getClass().getName());
            
            assertTrue(deserialized instanceof CodeTopFilterResponse);
            CodeTopFilterResponse deserializedResponse = (CodeTopFilterResponse) deserialized;
            
            assertEquals(originalResponse.getTotalElements(), deserializedResponse.getTotalElements());
            assertNotNull(deserializedResponse.getSummary());
            
            log.info("✅ GenericJackson2JsonRedisSerializer direct test passed!");
            
        } catch (Exception e) {
            log.error("❌ GenericJackson2JsonRedisSerializer direct test failed", e);
            fail("GenericJackson2JsonRedisSerializer test failed: " + e.getMessage());
        }
    }
}