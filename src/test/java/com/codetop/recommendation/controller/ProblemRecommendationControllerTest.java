package com.codetop.recommendation.controller;

import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.service.AIRecommendationService;
import com.codetop.recommendation.service.RecommendationFeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller contract test for ProblemRecommendationController.
 * Validates response JSON structure and required headers.
 */
@WebMvcTest(ProblemRecommendationController.class)
class ProblemRecommendationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AIRecommendationService aiRecommendationService;
    
    @MockBean
    private RecommendationFeedbackService feedbackService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void getAiRecommendations_shouldReturnValidResponse() throws Exception {
        // Given
        AIRecommendationResponse mockResponse = createMockRecommendationResponse(false, "LLM", "normal");
        when(aiRecommendationService.getRecommendations(anyLong(), anyInt())).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/problems/ai-recommendations")
                .param("userId", "1")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                
                // Verify required headers exist
                .andExpect(header().exists("X-Cache-Hit"))
                .andExpect(header().exists("X-Rec-Source"))
                .andExpect(header().exists("X-Provider-Chain"))
                .andExpect(header().string("X-Cache-Hit", "false"))
                .andExpect(header().string("X-Rec-Source", "LLM"))
                .andExpect(header().string("X-Provider-Chain", "provider1>provider2"))
                
                // Verify JSON structure
                .andExpect(jsonPath("$.items", notNullValue()))
                .andExpect(jsonPath("$.items", isA(java.util.List.class)))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].problemId", is(101)))
                .andExpect(jsonPath("$.items[0].reason", notNullValue()))
                .andExpect(jsonPath("$.items[0].confidence", notNullValue()))
                .andExpect(jsonPath("$.items[0].source", is("LLM")))
                
                // Verify meta structure
                .andExpect(jsonPath("$.meta", notNullValue()))
                .andExpect(jsonPath("$.meta.traceId", notNullValue()))
                .andExpect(jsonPath("$.meta.generatedAt", notNullValue()))
                .andExpect(jsonPath("$.meta.cached", is(false)))
                .andExpect(jsonPath("$.meta.strategy", is("normal")))
                .andExpect(jsonPath("$.meta.chainHops", isA(java.util.List.class)));
    }
    
    @Test
    void getAiRecommendations_withCachedResponse_shouldShowCacheHeaders() throws Exception {
        // Given - cached response
        AIRecommendationResponse mockResponse = createMockRecommendationResponse(true, "LLM", "normal");
        when(aiRecommendationService.getRecommendations(anyLong(), anyInt())).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/problems/ai-recommendations")
                .param("userId", "1")
                .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache-Hit", "true"))
                .andExpect(header().string("X-Rec-Source", "LLM"))
                .andExpect(jsonPath("$.meta.cached", is(true)));
    }
    
    @Test
    void getAiRecommendations_withFsrsFallback_shouldShowCorrectSource() throws Exception {
        // Given - FSRS fallback response
        AIRecommendationResponse mockResponse = createMockRecommendationResponse(false, "FSRS", "fsrs_fallback");
        when(aiRecommendationService.getRecommendations(anyLong(), anyInt())).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/problems/ai-recommendations")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Rec-Source", "FSRS"))
                .andExpect(jsonPath("$.meta.strategy", is("fsrs_fallback")))
                .andExpect(jsonPath("$.items[0].source", is("FSRS")));
    }
    
    @Test
    void getAiRecommendations_withBusyResponse_shouldShowDefaultSource() throws Exception {
        // Given - busy response
        AIRecommendationResponse mockResponse = createBusyMockResponse();
        when(aiRecommendationService.getRecommendations(anyLong(), anyInt())).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/problems/ai-recommendations")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Rec-Source", "DEFAULT"))
                .andExpect(jsonPath("$.meta.busy", is(true)))
                .andExpect(jsonPath("$.meta.strategy", is("busy_message")))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }
    
    @Test
    void getAiRecommendations_withOptionalHeaders_shouldIncludeWhenPresent() throws Exception {
        // Given - response with optional metadata
        AIRecommendationResponse mockResponse = createMockRecommendationResponse(false, "LLM", "normal");
        mockResponse.getMeta().setFallbackReason("test_reason");
        mockResponse.getMeta().setChainId("chain-123");
        mockResponse.getMeta().setChainVersion("v1.0");
        mockResponse.getMeta().setPolicyId("policy-456");
        mockResponse.getMeta().setUserProfileSummary("profile-summary");
        
        when(aiRecommendationService.getRecommendations(anyLong(), anyInt())).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(get("/problems/ai-recommendations")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Fallback-Reason", "test_reason"))
                .andExpect(header().string("X-Chain-Id", "chain-123"))
                .andExpect(header().string("X-Chain-Version", "v1.0"))
                .andExpect(header().string("X-Policy-Id", "policy-456"))
                .andExpect(header().string("X-User-Profile", "profile-summary"));
    }
    
    @Test
    void getAiRecommendations_withDefaultParams_shouldWork() throws Exception {
        // Given
        AIRecommendationResponse mockResponse = createMockRecommendationResponse(false, "LLM", "normal");
        when(aiRecommendationService.getRecommendations(anyLong(), anyInt())).thenReturn(mockResponse);
        
        // When & Then - no parameters provided, should use defaults
        mockMvc.perform(get("/problems/ai-recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()))
                .andExpect(jsonPath("$.meta", notNullValue()));
    }
    
    @Test
    void submitFeedback_shouldReturnSuccessResponse() throws Exception {
        // Given
        doNothing().when(feedbackService).submit(anyLong(), org.mockito.ArgumentMatchers.any());
        
        String feedbackJson = """
                {
                    "userId": 1,
                    "rating": 5,
                    "feedback": "Great recommendation!",
                    "actionType": "ACCEPTED"
                }
                """;
        
        // When & Then
        mockMvc.perform(post("/problems/123/recommendation-feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(feedbackJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.recordedAt", notNullValue()))
                .andExpect(jsonPath("$.recordedAt", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z")));
    }
    
    @Test
    void submitFeedback_withInvalidJson_shouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/problems/123/recommendation-feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }
    
    /**
     * Create mock recommendation response for testing.
     */
    private AIRecommendationResponse createMockRecommendationResponse(boolean cached, String source, String strategy) {
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
    
    /**
     * Create busy mock response for testing.
     */
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