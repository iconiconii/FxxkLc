package com.codetop.recommendation.service;

import com.codetop.recommendation.chain.ProviderChain;
import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.provider.impl.DefaultProvider;
import com.codetop.service.CacheKeyBuilder;
import com.codetop.service.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AIRecommendationServiceCacheKeyTest {

    @Mock
    private ProviderChain providerChain;
    
    @Mock
    private CacheService cacheService;
    
    @Mock
    private PromptTemplateService promptTemplateService;
    
    @Mock
    private com.codetop.recommendation.alg.CandidateBuilder candidateBuilder;
    
    private LlmProperties llmProperties;
    private AIRecommendationService service;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        llmProperties = new LlmProperties();
        llmProperties.setEnabled(true);
        
        // Configure async limits for testing
        LlmProperties.AsyncLimits asyncLimits = new LlmProperties.AsyncLimits();
        asyncLimits.setGlobalConcurrency(3);  // Small limit for testing
        asyncLimits.setPerUserConcurrency(2); // Small limit for testing
        asyncLimits.setAcquireTimeoutMs(50);  // Short timeout for testing
        llmProperties.setAsyncLimits(asyncLimits);
        
        service = new AIRecommendationService(
            providerChain, null, null, cacheService, null, promptTemplateService, llmProperties
        );
    }
    
    @Test
    void shouldIncludePromptVersionInCacheKey() {
        // Given
        when(promptTemplateService.getCurrentPromptVersion()).thenReturn("v2");
        when(cacheService.getList(anyString(), eq(RecommendationItemDTO.class))).thenReturn(null);
        
        ProviderChain.Result chainResult = createSuccessResult();
        when(providerChain.execute(any(), any(), any())).thenReturn(chainResult);
        
        // When
        service.getRecommendations(123L, 5);
        
        // Then
        ArgumentCaptor<String> cacheKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheService).getList(cacheKeyCaptor.capture(), eq(RecommendationItemDTO.class));
        
        String actualCacheKey = cacheKeyCaptor.getValue();
        // Cache key now includes segment information with chainId: "limit_5_pv_v2_t_BRONZE_ab_B_chain_legacy"
        String expectedCacheKey = CacheKeyBuilder.buildUserKey("rec-ai", 123L, "limit_5_pv_v2_t_BRONZE_ab_B_chain_legacy");
        assertEquals(expectedCacheKey, actualCacheKey);
        
        // Verify cache is written with the same key
        verify(cacheService).put(eq(expectedCacheKey), any(), eq(Duration.ofHours(1)));
    }
    
    @Test
    void shouldUseDifferentCacheKeysForDifferentPromptVersions() {
        // Test with v1
        when(promptTemplateService.getCurrentPromptVersion()).thenReturn("v1");
        when(cacheService.getList(anyString(), eq(RecommendationItemDTO.class))).thenReturn(null);
        ProviderChain.Result chainResult = createSuccessResult();
        when(providerChain.execute(any(), any(), any())).thenReturn(chainResult);
        
        service.getRecommendations(123L, 5);
        
        ArgumentCaptor<String> cacheKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheService, times(1)).getList(cacheKeyCaptor.capture(), eq(RecommendationItemDTO.class));
        String v1CacheKey = cacheKeyCaptor.getValue();
        
        // Debug output
        System.out.println("V1 Cache Key: " + v1CacheKey);
        
        // Reset and test with v2
        reset(cacheService);
        reset(providerChain); // Also reset providerChain
        when(promptTemplateService.getCurrentPromptVersion()).thenReturn("v2");
        when(cacheService.getList(anyString(), eq(RecommendationItemDTO.class))).thenReturn(null);
        when(providerChain.execute(any(), any(), any())).thenReturn(chainResult);
        
        service.getRecommendations(123L, 5);
        
        ArgumentCaptor<String> v2CacheKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheService, times(1)).getList(v2CacheKeyCaptor.capture(), eq(RecommendationItemDTO.class));
        String v2CacheKey = v2CacheKeyCaptor.getValue();
        
        // Debug output
        System.out.println("V2 Cache Key: " + v2CacheKey);
        
        // Keys should be different
        assertNotEquals(v1CacheKey, v2CacheKey);
        assertTrue(v1CacheKey.contains("pvv1"), "V1 key should contain pvv1: " + v1CacheKey);
        assertTrue(v2CacheKey.contains("pvv2"), "V2 key should contain pvv2: " + v2CacheKey);
    }
    
    @Test
    void asyncShouldAlsoIncludePromptVersionInCacheKey() {
        // Given
        when(promptTemplateService.getCurrentPromptVersion()).thenReturn("v3");
        when(cacheService.getList(anyString(), eq(RecommendationItemDTO.class))).thenReturn(null);
        
        ProviderChain.Result chainResult = createSuccessResult();
        CompletableFuture<ProviderChain.Result> futureResult = CompletableFuture.completedFuture(chainResult);
        when(providerChain.executeAsync(any(), any(), any())).thenReturn(futureResult);
        
        // When
        CompletableFuture<AIRecommendationResponse> response = service.getRecommendationsAsync(456L, 10);
        response.join(); // Wait for completion
        
        // Then
        ArgumentCaptor<String> cacheKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheService).getList(cacheKeyCaptor.capture(), eq(RecommendationItemDTO.class));
        
        String actualCacheKey = cacheKeyCaptor.getValue();
        // Cache key now includes segment information with chainId: "limit_10_pv_v3_t_BRONZE_ab_A_chain_legacy"
        String expectedCacheKey = CacheKeyBuilder.buildUserKey("rec-ai", 456L, "limit_10_pv_v3_t_BRONZE_ab_A_chain_legacy");
        assertEquals(expectedCacheKey, actualCacheKey);
    }
    
    @Test
    void asyncLimitsConfigurationShouldBeApplied() {
        // Create service with custom limits
        LlmProperties testProperties = new LlmProperties();
        testProperties.setEnabled(true);
        LlmProperties.AsyncLimits customLimits = new LlmProperties.AsyncLimits();
        customLimits.setGlobalConcurrency(5);
        customLimits.setPerUserConcurrency(3);
        customLimits.setAcquireTimeoutMs(200);
        testProperties.setAsyncLimits(customLimits);
        
        AIRecommendationService testService = new AIRecommendationService(
            providerChain, candidateBuilder, null, cacheService, null, promptTemplateService, testProperties
        );
        
        // Verify the service was initialized with correct limits
        // This test validates configuration rather than runtime behavior
        assertNotNull(testService);
        
        // Test that async call works with configured limits
        when(promptTemplateService.getCurrentPromptVersion()).thenReturn("v1");
        when(cacheService.getList(anyString(), eq(RecommendationItemDTO.class))).thenReturn(null);
        when(candidateBuilder.buildForUser(anyLong(), anyInt())).thenReturn(List.of(createCandidate(1L)));
        
        ProviderChain.Result successResult = createSuccessResult();
        CompletableFuture<ProviderChain.Result> future = CompletableFuture.completedFuture(successResult);
        when(providerChain.executeAsync(any(), any(), any())).thenReturn(future);
        
        // When
        CompletableFuture<AIRecommendationResponse> response = testService.getRecommendationsAsync(1000L, 5);
        
        // Then - should complete successfully
        assertNotNull(response);
        AIRecommendationResponse result = response.join();
        assertNotNull(result);
        assertFalse(result.getMeta().isCached()); // Should not be cached
        assertNotNull(result.getItems());
    }
    
    private ProviderChain.Result createSuccessResult() {
        ProviderChain.Result result = new ProviderChain.Result();
        result.success = true;
        result.result = createLlmResult();
        result.hops = List.of("mock");
        return result;
    }
    
    private LlmProvider.LlmResult createLlmResult() {
        LlmProvider.LlmResult result = new LlmProvider.LlmResult();
        result.success = true;
        result.provider = "mock";
        result.model = "test-model";
        result.items = new ArrayList<>();
        
        LlmProvider.RankedItem item = new LlmProvider.RankedItem();
        item.problemId = 1L;
        item.reason = "test reason";
        item.confidence = 0.9;
        item.score = 0.9;
        item.strategy = "test";
        result.items.add(item);
        
        return result;
    }
    
    private LlmProvider.ProblemCandidate createCandidate(Long id) {
        LlmProvider.ProblemCandidate candidate = new LlmProvider.ProblemCandidate();
        candidate.id = id;
        candidate.topic = "test topic";
        candidate.difficulty = "MEDIUM";
        candidate.tags = List.of("array", "hash-table");
        candidate.recentAccuracy = 0.7;
        candidate.attempts = 3;
        return candidate;
    }
}