package com.codetop.recommendation.controller;

import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.dto.RecommendationFeedbackRequest;
import com.codetop.recommendation.service.AIRecommendationService;
import com.codetop.recommendation.service.RecommendationFeedbackService;
import com.codetop.recommendation.service.LearningObjective;
import com.codetop.recommendation.service.DifficultyPreference;
import com.codetop.recommendation.service.RecommendationType;
import com.codetop.recommendation.config.UserProfilingProperties;
import com.codetop.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Lightweight unit tests for ProblemRecommendationController.
 * 
 * Tests parameter parsing, alias mapping, and response headers without loading
 * full security/application context for fast and stable execution.
 */
@ExtendWith(MockitoExtension.class)
class ProblemRecommendationControllerTest {

    @Mock
    private AIRecommendationService aiRecommendationService;
    
    @Mock
    private RecommendationFeedbackService feedbackService;
    
    @Mock
    private UserProfilingProperties userProfilingProperties;
    
    @InjectMocks
    private ProblemRecommendationController controller;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        
        // Mock user profiling properties with valid domain mapping
        Map<String, String> domainMap = new HashMap<>();
        domainMap.put("array", "arrays");
        domainMap.put("graph", "graphs");
        domainMap.put("dp", "dynamic_programming");
        
        lenient().when(userProfilingProperties.getTagDomainMapping()).thenReturn(domainMap);
    }
    
    @Test
    void testParameterAliasMapping_TopicFilter() throws Exception {
        // Given - Mock AI recommendation response
        AIRecommendationResponse mockResponse = createMockResponse();
        lenient().when(aiRecommendationService.getRecommendations(anyLong(), anyInt(), 
            any(), any(), any(), any(), any()))
            .thenReturn(mockResponse);
        
        // When - Request with topic_filter alias instead of domains
        mockMvc.perform(get("/problems/ai-recommendations")
                .param("userId", "123")
                .param("limit", "5")
                .param("topic_filter", "arrays", "graphs") // Using alias
                .param("difficulty", "MEDIUM")
                .param("recommendation_type", "AI"))
                
        // Then - Should accept topic_filter as alias for domains
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(header().string("X-Cache-Hit", "false"))
        .andExpect(header().string("X-Rec-Source", "AI"))
        .andExpect(header().string("X-Recommendation-Type", "ai"));
        
        // Verify service called with correct domain list (converted from topic_filter)
        verify(aiRecommendationService).getRecommendations(
            eq(123L), eq(5), isNull(), 
            argThat(domains -> domains != null && domains.contains("arrays") && domains.contains("graphs")),
            eq(DifficultyPreference.MEDIUM), isNull(), eq(RecommendationType.AI));
    }
    
    @Test 
    void testParameterAliasMapping_DifficultyPreference() throws Exception {
        // Given
        AIRecommendationResponse mockResponse = createMockResponse();
        lenient().when(aiRecommendationService.getRecommendations(anyLong(), anyInt(), 
            any(), any(), any(), any(), any()))
            .thenReturn(mockResponse);
        
        // When - Request with difficulty_preference alias instead of difficulty
        mockMvc.perform(get("/problems/ai-recommendations")
                .param("userId", "456")
                .param("limit", "3")
                .param("domains", "arrays")
                .param("difficulty_preference", "HARD") // Using alias
                .param("recommendation_type", "HYBRID"))
                
        // Then - Should accept difficulty_preference as alias for difficulty
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(header().string("X-Recommendation-Type", "hybrid"));
        
        // Verify service called with correct difficulty (converted from difficulty_preference)
        verify(aiRecommendationService).getRecommendations(
            eq(456L), eq(3), isNull(), 
            argThat(domains -> domains != null && domains.contains("arrays")),
            eq(DifficultyPreference.HARD), isNull(), eq(RecommendationType.HYBRID));
    }
    
    @Test
    void testParameterPrecedence_PreferOriginalOverAlias() throws Exception {
        // Given
        AIRecommendationResponse mockResponse = createMockResponse();
        lenient().when(aiRecommendationService.getRecommendations(anyLong(), anyInt(), 
            any(), any(), any(), any(), any()))
            .thenReturn(mockResponse);
        
        // When - Request with both original and alias parameters
        mockMvc.perform(get("/problems/ai-recommendations")
                .param("userId", "789")
                .param("limit", "10")
                .param("domains", "arrays") // Original parameter
                .param("topic_filter", "graphs") // Alias parameter
                .param("difficulty", "EASY") // Original parameter
                .param("difficulty_preference", "HARD") // Alias parameter
                .param("recommendation_type", "AUTO"))
                
        // Then - Should prefer original parameters over aliases
        .andExpect(status().isOk());
        
        // Verify service called with original parameters (not alias values)
        verify(aiRecommendationService).getRecommendations(
            eq(789L), eq(10), isNull(), 
            argThat(domains -> domains != null && domains.contains("arrays") && !domains.contains("graphs")),
            eq(DifficultyPreference.EASY), isNull(), eq(RecommendationType.AUTO));
    }
    
    @Test
    void testResponseHeaders_CacheAndProviderChain() throws Exception {
        // Given - Mock response with cache and provider chain metadata
        AIRecommendationResponse mockResponse = createMockResponseWithMetadata();
        lenient().when(aiRecommendationService.getRecommendations(anyLong(), anyInt(), 
            any(), any(), any(), any(), any()))
            .thenReturn(mockResponse);
        
        // When
        mockMvc.perform(get("/problems/ai-recommendations")
                .param("userId", "999")
                .param("limit", "5"))
                
        // Then - Verify all expected response headers are present
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(header().string("X-Cache-Hit", "true"))
        .andExpect(header().string("X-Rec-Source", "AI"))
        .andExpect(header().string("X-Provider-Chain", "openai>anthropic"))
        .andExpect(header().string("X-Recommendation-Type", "hybrid"))
        .andExpect(jsonPath("$.meta.cached").value(true))
        .andExpect(jsonPath("$.meta.strategy").value("ai"))
        .andExpect(jsonPath("$.meta.chainHops").isArray());
    }
    
    @Test
    void testFeedbackEndpoint_UserIdFromSecurityContext() throws Exception {
        // Given - Mock authenticated user in security context
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCurrentUserId).thenReturn(123L);
            
            RecommendationFeedbackRequest request = new RecommendationFeedbackRequest();
            request.setUserId(123L); // Matching authenticated user
            request.setFeedback("helpful");
            request.setNote("Great recommendation!");
            
            // When
            mockMvc.perform(post("/problems/123/recommendation-feedback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    
            // Then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.recordedAt").exists());
            
            // Verify feedback service called with correct problem ID and request
            verify(feedbackService).submit(eq(123L), any(RecommendationFeedbackRequest.class));
        }
    }
    
    @Test
    void testFeedbackEndpoint_UserIdMismatch_ThrowsSecurityException() throws Exception {
        // Given - Mock authenticated user that doesn't match request
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getCurrentUserId).thenReturn(123L);
            
            RecommendationFeedbackRequest request = new RecommendationFeedbackRequest();
            request.setUserId(999L); // Different from authenticated user
            request.setFeedback("helpful");
            
            // When/Then - Should throw security exception  
            try {
                mockMvc.perform(post("/problems/456/recommendation-feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
                // If we reach here, the test should fail
                assert false : "Expected SecurityException to be thrown";
            } catch (Exception e) {
                // Verify the cause is SecurityException
                assert e.getCause() instanceof SecurityException : "Expected SecurityException";
            }
            
            // Verify feedback service NOT called due to security violation
            verify(feedbackService, never()).submit(anyLong(), any());
        }
    }
    
    @Test
    void testInputValidation_LimitClamping() throws Exception {
        // Given
        AIRecommendationResponse mockResponse = createMockResponse();
        lenient().when(aiRecommendationService.getRecommendations(anyLong(), anyInt(), 
            any(), any(), any(), any(), any()))
            .thenReturn(mockResponse);
        
        // When - Request with limit outside safe range
        mockMvc.perform(get("/problems/ai-recommendations")
                .param("userId", "123")
                .param("limit", "100")) // Over maximum of 50
                
        // Then - Should clamp limit to maximum
        .andExpect(status().isOk());
        
        // Verify service called with clamped limit
        verify(aiRecommendationService).getRecommendations(
            eq(123L), eq(50), // Clamped to maximum
            isNull(), isNull(), isNull(), isNull(), any(RecommendationType.class));
    }
    
    @Test
    void testDomainWhitelistValidation() throws Exception {
        // Given
        AIRecommendationResponse mockResponse = createMockResponse();
        lenient().when(aiRecommendationService.getRecommendations(anyLong(), anyInt(), 
            any(), any(), any(), any(), any()))
            .thenReturn(mockResponse);
        
        // When - Request with mix of valid and invalid domains
        mockMvc.perform(get("/problems/ai-recommendations")
                .param("userId", "123")
                .param("domains", "arrays", "invalid_domain", "graphs"))
                
        // Then - Should filter out invalid domains
        .andExpect(status().isOk());
        
        // Verify service called with only valid domains
        verify(aiRecommendationService).getRecommendations(
            eq(123L), anyInt(), isNull(), 
            argThat(domains -> domains != null && 
                   domains.contains("arrays") && 
                   domains.contains("graphs") && 
                   !domains.contains("invalid_domain")),
            isNull(), isNull(), any(RecommendationType.class));
    }
    
    private AIRecommendationResponse createMockResponse() {
        AIRecommendationResponse response = new AIRecommendationResponse();
        
        // Create mock recommendation items
        RecommendationItemDTO item1 = new RecommendationItemDTO();
        item1.setProblemId(1L);
        item1.setReason("Good for practicing arrays");
        item1.setConfidence(0.85);
        item1.setStrategy("personalized");
        item1.setSource("AI");
        
        RecommendationItemDTO item2 = new RecommendationItemDTO();
        item2.setProblemId(2L);
        item2.setReason("Linked list practice");
        item2.setConfidence(0.92);
        item2.setStrategy("progressive");
        item2.setSource("AI");
        
        response.setItems(Arrays.asList(item1, item2));
        
        // Create mock metadata
        AIRecommendationResponse.Meta meta = new AIRecommendationResponse.Meta();
        meta.setTraceId("test-trace-123");
        meta.setGeneratedAt(Instant.now());
        meta.setCached(false);
        meta.setStrategy("ai");
        meta.setRecommendationType("AI");
        meta.setChainHops(Arrays.asList("openai"));
        
        response.setMeta(meta);
        return response;
    }
    
    private AIRecommendationResponse createMockResponseWithMetadata() {
        AIRecommendationResponse response = createMockResponse();
        
        // Update metadata to simulate cache hit and provider chain
        AIRecommendationResponse.Meta meta = response.getMeta();
        meta.setCached(true);
        meta.setChainHops(Arrays.asList("openai", "anthropic"));
        meta.setChainId("multi-provider");
        
        return response;
    }
}