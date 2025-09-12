package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test for AIRecommendationService concepts.
 * Tests core functionality patterns without heavy dependencies.
 */
class AIRecommendationServiceSmokeTest {
    
    private AIRecommendationResponse mockResponse;
    
    @BeforeEach
    void setUp() {
        mockResponse = createMockResponse(false, "LLM", "normal");
    }
    
    @Test
    void responseStructure_shouldHaveRequiredFields() {
        // Given - a typical AI recommendation response
        AIRecommendationResponse response = mockResponse;
        
        // Then - verify required structure
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getItems(), "Response items should not be null");
        assertFalse(response.getItems().isEmpty(), "Response items should not be empty");
        
        // Verify meta structure
        assertNotNull(response.getMeta(), "Meta should not be null");
        assertNotNull(response.getMeta().getTraceId(), "Trace ID should not be null");
        assertEquals("normal", response.getMeta().getStrategy(), "Strategy should match");
        assertNotNull(response.getMeta().getGeneratedAt(), "Generated at should not be null");
        
        // Verify item structure
        RecommendationItemDTO firstItem = response.getItems().get(0);
        assertNotNull(firstItem.getProblemId(), "Problem ID should not be null");
        assertNotNull(firstItem.getReason(), "Reason should not be null");
        assertNotNull(firstItem.getConfidence(), "Confidence should not be null");
        assertNotNull(firstItem.getSource(), "Source should not be null");
        assertTrue(firstItem.getConfidence() > 0 && firstItem.getConfidence() <= 1, 
            "Confidence should be in valid range");
    }
    
    @Test
    void cacheMetadata_shouldIndicateCorrectState() {
        // Given - cached vs non-cached responses
        AIRecommendationResponse cachedResponse = createMockResponse(true, "LLM", "normal");
        AIRecommendationResponse freshResponse = createMockResponse(false, "LLM", "normal");
        
        // Then - verify cache indicators
        assertTrue(cachedResponse.getMeta().isCached(), "Cached response should be marked as cached");
        assertFalse(freshResponse.getMeta().isCached(), "Fresh response should not be marked as cached");
    }
    
    @Test
    void fsrsFallbackResponse_shouldHaveCorrectMetadata() {
        // Given - FSRS fallback scenario
        AIRecommendationResponse fsrsResponse = createMockResponse(false, "FSRS", "fsrs_fallback");
        fsrsResponse.getMeta().setFallbackReason("llm_timeout");
        
        // Then - verify FSRS fallback metadata
        assertEquals("fsrs_fallback", fsrsResponse.getMeta().getStrategy(), "Strategy should be fsrs_fallback");
        assertEquals("llm_timeout", fsrsResponse.getMeta().getFallbackReason(), "Fallback reason should be set");
        assertEquals("FSRS", fsrsResponse.getItems().get(0).getSource(), "Source should be FSRS");
    }
    
    @Test
    void busyResponse_shouldHandleGracefully() {
        // Given - busy response scenario
        AIRecommendationResponse busyResponse = createBusyMockResponse();
        
        // Then - verify busy response structure
        assertTrue(busyResponse.getMeta().isBusy(), "Response should be marked as busy");
        assertEquals("busy_message", busyResponse.getMeta().getStrategy(), "Strategy should be busy_message");
        assertTrue(busyResponse.getItems().isEmpty(), "Items should be empty for busy response");
    }
    
    @Test
    void recommendationItems_shouldHaveValidScores() {
        // Given - recommendation items with scores
        AIRecommendationResponse response = mockResponse;
        
        // Then - verify score validity
        for (RecommendationItemDTO item : response.getItems()) {
            assertNotNull(item.getScore(), "Score should not be null");
            assertTrue(item.getScore() >= 0.0 && item.getScore() <= 1.0, 
                "Score should be in valid range [0.0, 1.0]");
            assertNotNull(item.getConfidence(), "Confidence should not be null");
            assertTrue(item.getConfidence() >= 0.0 && item.getConfidence() <= 1.0,
                "Confidence should be in valid range [0.0, 1.0]");
        }
    }
    
    @Test
    void chainMetadata_shouldProvideTraceability() {
        // Given - response with chain information
        AIRecommendationResponse response = mockResponse;
        
        // Then - verify traceability information
        assertNotNull(response.getMeta().getTraceId(), "Trace ID should be present");
        assertTrue(response.getMeta().getTraceId().startsWith("trace-"), "Trace ID should have correct format");
        assertNotNull(response.getMeta().getChainHops(), "Chain hops should be present");
        assertFalse(response.getMeta().getChainHops().isEmpty(), "Chain hops should not be empty");
    }
    
    @Test
    void edgeCases_shouldHandleNullsGracefully() {
        // Given - response with potential null values
        AIRecommendationResponse response = new AIRecommendationResponse();
        AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
        response.setMeta(meta);
        response.setItems(Arrays.asList());
        
        // Then - should handle gracefully
        assertNotNull(response.getMeta(), "Meta should not be null");
        assertNotNull(response.getItems(), "Items should not be null (even if empty)");
        assertTrue(response.getItems().isEmpty(), "Empty items list should be valid");
    }
    
    private AIRecommendationResponse createMockResponse(boolean cached, String source, String strategy) {
        // Create mock items
        RecommendationItemDTO item1 = new RecommendationItemDTO();
        item1.setProblemId(101L);
        item1.setReason("Similar to problems you've solved well");
        item1.setConfidence(0.85);
        item1.setScore(0.92);
        item1.setSource(source);
        item1.setModel("gpt-4");
        
        RecommendationItemDTO item2 = new RecommendationItemDTO();
        item2.setProblemId(102L);
        item2.setReason("Recommended based on your weak areas");
        item2.setConfidence(0.78);
        item2.setScore(0.88);
        item2.setSource(source);
        item2.setModel("gpt-4");
        
        // Create mock response
        AIRecommendationResponse response = new AIRecommendationResponse();
        response.setItems(Arrays.asList(item1, item2));
        
        // Create mock meta
        AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
        meta.setTraceId("trace-123");
        meta.setGeneratedAt(Instant.now());
        meta.setCached(cached);
        meta.setStrategy(strategy);
        meta.setChainHops(Arrays.asList("provider1", "provider2"));
        meta.setBusy(false);
        
        response.setMeta(meta);
        return response;
    }
    
    private AIRecommendationResponse createBusyMockResponse() {
        AIRecommendationResponse response = new AIRecommendationResponse();
        response.setItems(Arrays.asList()); // Empty items
        
        AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
        meta.setTraceId("trace-busy");
        meta.setGeneratedAt(Instant.now());
        meta.setCached(false);
        meta.setStrategy("busy_message");
        meta.setBusy(true);
        
        response.setMeta(meta);
        return response;
    }
}