package com.codetop.recommendation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.dto.DomainSkill;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.config.MixingStrategyProperties;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecommendationMixer.
 * 
 * Tests multi-dimensional strategy mixing functionality that allocates
 * recommendations across different learning objectives with configurable quotas.
 */
public class RecommendationMixerTest {
    
    @Mock
    private MixingStrategyProperties mixingConfig;
    
    private RecommendationMixer recommendationMixer;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock enabled configuration
        when(mixingConfig.isEnabled()).thenReturn(true);
        
        recommendationMixer = new RecommendationMixer(mixingConfig);
    }
    
    @Test
    void testMixRecommendationsWithWeaknessFocus() {
        // Given
        List<RecommendationItemDTO> hybridItems = createTestRecommendations(10);
        Map<Long, LlmProvider.ProblemCandidate> candidateMap = createTestCandidates();
        UserProfile userProfile = createTestUserProfile();
        LearningObjective objective = LearningObjective.WEAKNESS_FOCUS;
        int totalLimit = 5;
        
        // When
        List<RecommendationItemDTO> result = recommendationMixer.mixRecommendations(
            hybridItems, candidateMap, userProfile, objective, totalLimit);
        
        // Then
        assertNotNull(result);
        assertTrue(result.size() <= totalLimit);
        
        // Verify that items have strategy source tags
        assertTrue(result.stream().anyMatch(item -> 
            item.getSource() != null && item.getSource().contains("WEAKNESS_FOCUS")));
    }
    
    @Test
    void testMixRecommendationsDisabled() {
        // Given - disabled configuration
        when(mixingConfig.isEnabled()).thenReturn(false);
        
        List<RecommendationItemDTO> hybridItems = createTestRecommendations(10);
        Map<Long, LlmProvider.ProblemCandidate> candidateMap = createTestCandidates();
        UserProfile userProfile = createTestUserProfile();
        LearningObjective objective = LearningObjective.WEAKNESS_FOCUS;
        int totalLimit = 5;
        
        // When
        List<RecommendationItemDTO> result = recommendationMixer.mixRecommendations(
            hybridItems, candidateMap, userProfile, objective, totalLimit);
        
        // Then - should return top items when disabled
        assertNotNull(result);
        assertEquals(totalLimit, result.size());
    }
    
    @Test
    void testMixRecommendationsWithEmptyItems() {
        // Given
        List<RecommendationItemDTO> emptyItems = new ArrayList<>();
        Map<Long, LlmProvider.ProblemCandidate> candidateMap = new HashMap<>();
        UserProfile userProfile = createTestUserProfile();
        LearningObjective objective = LearningObjective.PROGRESSIVE_DIFFICULTY;
        
        // When
        List<RecommendationItemDTO> result = recommendationMixer.mixRecommendations(
            emptyItems, candidateMap, userProfile, objective, 5);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    private List<RecommendationItemDTO> createTestRecommendations(int count) {
        List<RecommendationItemDTO> items = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            RecommendationItemDTO item = new RecommendationItemDTO();
            item.setProblemId((long) i);
            item.setScore(0.9 - (i * 0.05)); // Decreasing scores
            item.setReason("Test recommendation " + i);
            item.setSource("HYBRID");
            items.add(item);
        }
        
        return items;
    }
    
    private Map<Long, LlmProvider.ProblemCandidate> createTestCandidates() {
        Map<Long, LlmProvider.ProblemCandidate> candidateMap = new HashMap<>();
        
        for (long i = 1; i <= 10; i++) {
            LlmProvider.ProblemCandidate candidate = new LlmProvider.ProblemCandidate();
            candidate.id = i;
            candidate.urgencyScore = 0.5 + (i * 0.05);
            candidate.tags = Arrays.asList("arrays", "dynamic_programming");
            candidate.difficulty = i <= 3 ? "EASY" : i <= 7 ? "MEDIUM" : "HARD";
            candidateMap.put(i, candidate);
        }
        
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