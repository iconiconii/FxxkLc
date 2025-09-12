package com.codetop.recommendation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.dto.DomainSkill;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.config.ConfidenceProperties;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConfidenceCalibrator.
 * 
 * Tests confidence calibration functionality that assesses recommendation quality
 * using multiple data signals including LLM quality, FSRS completeness, and user relevance.
 */
public class ConfidenceCalibratorTest {
    
    private ConfidenceCalibrator confidenceCalibrator;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Build real configuration to exercise logic instead of a Mockito mock
        ConfidenceProperties realConfig = new ConfidenceProperties();
        realConfig.setEnabled(true);

        // Configure thresholds to avoid filtering items in test
        realConfig.getThresholds().setMinimumShow(0.0);

        // Configure weights
        realConfig.getWeights().setLlmQuality(0.25);
        realConfig.getWeights().setFsrsData(0.20);
        realConfig.getWeights().setProfileRelevance(0.20);
        realConfig.getWeights().setHistoricalAccuracy(0.15);
        realConfig.getWeights().setCrossSignalConsensus(0.10);
        realConfig.getWeights().setContextQuality(0.10);

        // Configure display flags
        realConfig.getDisplay().setIncludeInReason(true);
        realConfig.getDisplay().setIncludeFactors(false);

        confidenceCalibrator = new ConfidenceCalibrator(realConfig);
    }
    
    @Test
    void testCalibrateConfidenceWithValidInputs() {
        // Given
        List<RecommendationItemDTO> recommendations = createTestRecommendations();
        Map<Long, LlmProvider.ProblemCandidate> candidateMap = createTestCandidates();
        UserProfile userProfile = createTestUserProfile();
        ConfidenceCalibrator.LlmResponseMetadata llmMetadata = createTestLlmMetadata();
        
        // When
        List<RecommendationItemDTO> result = confidenceCalibrator.calibrateConfidence(
            recommendations, candidateMap, userProfile, llmMetadata);
        
        // Then
        assertNotNull(result);
        assertEquals(recommendations.size(), result.size());
        
        // Verify calibrated confidence is set on items
        assertTrue(result.stream().allMatch(item -> 
            item.getConfidence() != null && item.getConfidence() >= 0.0 && item.getConfidence() <= 1.0));

        // Verify reason contains confidence label when enabled
        assertTrue(result.stream().anyMatch(item -> 
            item.getReason() != null && item.getReason().contains("Confidence]")));
    }
    
    @Test
    void testCalibrateConfidenceDisabled() {
        // Given - disabled configuration using a new calibrator instance
        ConfidenceProperties disabledConfig = new ConfidenceProperties();
        disabledConfig.setEnabled(false);
        ConfidenceCalibrator disabledCalibrator = new ConfidenceCalibrator(disabledConfig);

        List<RecommendationItemDTO> recommendations = createTestRecommendations();
        Map<Long, LlmProvider.ProblemCandidate> candidateMap = createTestCandidates();
        UserProfile userProfile = createTestUserProfile();
        ConfidenceCalibrator.LlmResponseMetadata llmMetadata = createTestLlmMetadata();
        
        // When
        List<RecommendationItemDTO> result = disabledCalibrator.calibrateConfidence(
            recommendations, candidateMap, userProfile, llmMetadata);
        
        // Then - should return original items when disabled
        assertNotNull(result);
        assertEquals(recommendations.size(), result.size());
        assertEquals(recommendations, result);
    }
    
    @Test
    void testCalibrateConfidenceWithEmptyItems() {
        // Given
        List<RecommendationItemDTO> emptyRecommendations = new ArrayList<>();
        Map<Long, LlmProvider.ProblemCandidate> candidateMap = new HashMap<>();
        UserProfile userProfile = createTestUserProfile();
        ConfidenceCalibrator.LlmResponseMetadata llmMetadata = createTestLlmMetadata();
        
        // When
        List<RecommendationItemDTO> result = confidenceCalibrator.calibrateConfidence(
            emptyRecommendations, candidateMap, userProfile, llmMetadata);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    private List<RecommendationItemDTO> createTestRecommendations() {
        List<RecommendationItemDTO> items = new ArrayList<>();
        
        RecommendationItemDTO item1 = new RecommendationItemDTO();
        item1.setProblemId(1L);
        item1.setScore(0.8);
        item1.setReason("Test recommendation with good FSRS data");
        item1.setSource("HYBRID");
        items.add(item1);
        
        RecommendationItemDTO item2 = new RecommendationItemDTO();
        item2.setProblemId(2L);
        item2.setScore(0.6);
        item2.setReason("Test recommendation with limited data");
        item2.setSource("HYBRID");
        items.add(item2);
        
        return items;
    }
    
    private Map<Long, LlmProvider.ProblemCandidate> createTestCandidates() {
        Map<Long, LlmProvider.ProblemCandidate> candidateMap = new HashMap<>();
        
        LlmProvider.ProblemCandidate candidate1 = new LlmProvider.ProblemCandidate();
        candidate1.id = 1L;
        candidate1.urgencyScore = 0.7;
        candidate1.retentionProbability = 0.8;
        candidate1.attempts = 15; // Good history depth
        candidate1.recentAccuracy = 0.75;
        candidate1.tags = Arrays.asList("arrays", "dynamic_programming");
        candidateMap.put(1L, candidate1);
        
        LlmProvider.ProblemCandidate candidate2 = new LlmProvider.ProblemCandidate();
        candidate2.id = 2L;
        candidate2.urgencyScore = 0.4;
        candidate2.retentionProbability = 0.6;
        candidate2.attempts = 3; // Limited history
        candidate2.recentAccuracy = 0.5;
        candidate2.tags = Arrays.asList("graphs");
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
        profile.setOverallMastery(0.65); // Good mastery level
        return profile;
    }
    
    private ConfidenceCalibrator.LlmResponseMetadata createTestLlmMetadata() {
        return new ConfidenceCalibrator.LlmResponseMetadata(
            "openai",     // provider
            1500L,        // responseTime
            2000,         // inputTokens  
            500           // outputTokens
        );
    }
}
