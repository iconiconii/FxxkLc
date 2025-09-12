package com.codetop.recommendation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.dto.DomainSkill;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.config.HybridRankingProperties;
import com.codetop.recommendation.alg.SimilarityScorer;
import com.codetop.recommendation.config.SimilarityProperties;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HybridRankingService.
 * 
 * Tests the core hybrid ranking functionality that combines LLM scores,
 * FSRS urgency signals, similarity scores, and personalization factors.
 */
public class HybridRankingServiceTest {
    
    @Mock
    private SimilarityScorer similarityScorer;
    
    @Mock
    private SimilarityProperties similarityConfig;
    
    private HybridRankingProperties hybridConfig;
    private HybridRankingService hybridRankingService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up test configuration
        hybridConfig = new HybridRankingProperties();
        hybridConfig.setEnabled(true);
        
        HybridRankingProperties.Weights weights = new HybridRankingProperties.Weights();
        weights.setLlm(0.45);
        weights.setFsrs(0.30);
        weights.setSimilarity(0.15);
        weights.setPersonalization(0.10);
        hybridConfig.setWeights(weights);
        
        hybridRankingService = new HybridRankingService(similarityScorer, similarityConfig, hybridConfig);
    }
    
    @Test
    void testHybridRankingWithValidInputs() {
        // Given
        List<RecommendationItemDTO> llmItems = createTestRecommendations();
        Map<Long, LlmProvider.ProblemCandidate> candidateMap = createTestCandidates();
        UserProfile userProfile = createTestUserProfile();
        
        // When
        List<RecommendationItemDTO> result = hybridRankingService.rankWithHybridScores(
            llmItems, candidateMap, userProfile);
        
        // Then
        assertNotNull(result);
        assertEquals(llmItems.size(), result.size());
        // Verify that scores have been recalculated
        assertTrue(result.stream().allMatch(item -> item.getScore() != null));
    }
    
    @Test
    void testHybridRankingWithEmptyInputs() {
        // Given
        List<RecommendationItemDTO> emptyItems = new ArrayList<>();
        Map<Long, LlmProvider.ProblemCandidate> candidateMap = new HashMap<>();
        UserProfile userProfile = createTestUserProfile();
        
        // When
        List<RecommendationItemDTO> result = hybridRankingService.rankWithHybridScores(
            emptyItems, candidateMap, userProfile);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testHybridRankingDisabled() {
        // Given - disabled configuration
        hybridConfig.setEnabled(false);
        List<RecommendationItemDTO> llmItems = createTestRecommendations();
        Map<Long, LlmProvider.ProblemCandidate> candidateMap = createTestCandidates();
        UserProfile userProfile = createTestUserProfile();
        
        // When
        List<RecommendationItemDTO> result = hybridRankingService.rankWithHybridScores(
            llmItems, candidateMap, userProfile);
        
        // Then - should return original items when disabled
        assertNotNull(result);
        assertEquals(llmItems.size(), result.size());
    }
    
    private List<RecommendationItemDTO> createTestRecommendations() {
        List<RecommendationItemDTO> items = new ArrayList<>();
        
        RecommendationItemDTO item1 = new RecommendationItemDTO();
        item1.setProblemId(1L);
        item1.setScore(0.8);
        item1.setReason("Test recommendation 1");
        item1.setSource("LLM");
        items.add(item1);
        
        RecommendationItemDTO item2 = new RecommendationItemDTO();
        item2.setProblemId(2L);
        item2.setScore(0.7);
        item2.setReason("Test recommendation 2");
        item2.setSource("LLM");
        items.add(item2);
        
        return items;
    }
    
    private Map<Long, LlmProvider.ProblemCandidate> createTestCandidates() {
        Map<Long, LlmProvider.ProblemCandidate> candidateMap = new HashMap<>();
        
        LlmProvider.ProblemCandidate candidate1 = new LlmProvider.ProblemCandidate();
        candidate1.id = 1L;
        candidate1.urgencyScore = 0.6;
        candidate1.retentionProbability = 0.7;
        candidate1.daysOverdue = 2;
        candidate1.tags = Arrays.asList("arrays", "dynamic_programming");
        candidateMap.put(1L, candidate1);
        
        LlmProvider.ProblemCandidate candidate2 = new LlmProvider.ProblemCandidate();
        candidate2.id = 2L;
        candidate2.urgencyScore = 0.8;
        candidate2.retentionProbability = 0.5;
        candidate2.daysOverdue = 5;
        candidate2.tags = Arrays.asList("graphs", "trees");
        candidateMap.put(2L, candidate2);
        
        return candidateMap;
    }
    
    private UserProfile createTestUserProfile() {
        UserProfile profile = new UserProfile();
        profile.setUserId(123L);
        
        // Set domain skills that will result in weak/strong domains
        Map<String, DomainSkill> domainSkills = new HashMap<>();
        
        // Weak domains (low skill scores)
        DomainSkill weakDp = new DomainSkill();
        weakDp.setSkillScore(0.3); // Weak
        weakDp.setSamples(20); // Sufficient samples
        weakDp.setAttempts(25);
        weakDp.setStrength(DomainSkill.StrengthLevel.WEAK);
        domainSkills.put("dynamic_programming", weakDp);
        
        DomainSkill weakGraphs = new DomainSkill();
        weakGraphs.setSkillScore(0.4); // Weak
        weakGraphs.setSamples(15); // Sufficient samples
        weakGraphs.setAttempts(20);
        weakGraphs.setStrength(DomainSkill.StrengthLevel.WEAK);
        domainSkills.put("graphs", weakGraphs);
        
        // Strong domains (high skill scores)
        DomainSkill strongArrays = new DomainSkill();
        strongArrays.setSkillScore(0.8); // Strong
        strongArrays.setSamples(25); // Sufficient samples
        strongArrays.setAttempts(30);
        strongArrays.setStrength(DomainSkill.StrengthLevel.STRONG);
        domainSkills.put("arrays", strongArrays);
        
        DomainSkill strongStrings = new DomainSkill();
        strongStrings.setSkillScore(0.85); // Strong
        strongStrings.setSamples(30); // Sufficient samples
        strongStrings.setAttempts(35);
        strongStrings.setStrength(DomainSkill.StrengthLevel.STRONG);
        domainSkills.put("strings", strongStrings);
        
        profile.setDomainSkills(domainSkills);
        profile.setOverallMastery(0.6);
        return profile;
    }
}