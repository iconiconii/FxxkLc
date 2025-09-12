package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.DifficultyPref;
import com.codetop.recommendation.dto.DomainSkill;
import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.provider.LlmProvider.ProblemCandidate;
import com.codetop.recommendation.provider.LlmProvider.PromptOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Tests for PromptTemplateService to verify prompt engineering templates work correctly.
 */
@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @Mock
    private ExternalPromptTemplateService externalTemplateService;
    
    private PromptTemplateService promptTemplateService;
    private RequestContext testContext;
    private List<ProblemCandidate> testCandidates;
    private PromptOptions testOptions;

    @BeforeEach
    void setUp() {
        promptTemplateService = new PromptTemplateService(externalTemplateService);
        
        // Setup mock behavior to trigger fallback with lenient stubbing
        lenient().when(externalTemplateService.buildSystemMessage(anyString(), any()))
            .thenThrow(new RuntimeException("Template not found"));
        lenient().when(externalTemplateService.buildUserMessage(any(), any(), any()))
            .thenThrow(new RuntimeException("Template not found"));
        lenient().when(externalTemplateService.getCurrentPromptVersion(any()))
            .thenThrow(new RuntimeException("Template service not available"));
        
        // Set up test context
        testContext = new RequestContext();
        testContext.setUserId(12345L);
        testContext.setTier("BRONZE");
        testContext.setAbGroup("A");
        testContext.setRoute("ai-recommendations");
        
        // Set up test candidates
        testCandidates = new ArrayList<>();
        testCandidates.add(createCandidate(1L, "数组", "EASY", List.of("数组", "哈希表"), 0.85, 2));
        testCandidates.add(createCandidate(2L, "字符串", "MEDIUM", List.of("滑动窗口"), 0.60, 3));
        testCandidates.add(createCandidate(3L, "链表", "HARD", List.of("递归"), 0.30, 5));
        
        // Set up options
        testOptions = new PromptOptions();
        testOptions.limit = 5;
    }

    @Test
    void testSystemMessageV1() {
        String systemMessage = promptTemplateService.buildSystemMessage("v1");
        
        assertNotNull(systemMessage);
        assertTrue(systemMessage.contains("JSON"));
        assertTrue(systemMessage.contains("problemId"));
        assertTrue(systemMessage.contains("confidence"));
        assertTrue(systemMessage.contains("score"));
    }

    @Test
    void testSystemMessageV2() {
        String systemMessage = promptTemplateService.buildSystemMessage("v2");
        
        assertNotNull(systemMessage);
        assertTrue(systemMessage.contains("recommendation engine"));
        assertTrue(systemMessage.contains("weak areas"));
        assertTrue(systemMessage.contains("difficulty progression"));
        assertTrue(systemMessage.contains("weakness_focus"));
        assertTrue(systemMessage.contains("topic_coverage"));
    }

    @Test
    void testBasicUserMessage() {
        String userMessage = promptTemplateService.buildUserMessage(testContext, testCandidates, testOptions, "v1");
        
        assertNotNull(userMessage);
        assertTrue(userMessage.contains("12345")); // userId
        assertTrue(userMessage.contains("ai-recommendations")); // route
        assertTrue(userMessage.contains("数组")); // topic from candidates
        assertTrue(userMessage.contains("0.85")); // accuracy from candidates
    }

    @Test
    void testAdvancedUserMessage() {
        String userMessage = promptTemplateService.buildUserMessage(testContext, testCandidates, testOptions, "v2");
        
        assertNotNull(userMessage);
        assertTrue(userMessage.contains("User Profile"));
        assertTrue(userMessage.contains("BRONZE")); // tier
        assertTrue(userMessage.contains("Learning Pattern"));
        assertTrue(userMessage.contains("Weak Topics"));
        assertTrue(userMessage.contains("Strong Topics"));
        assertTrue(userMessage.contains("Available Problems"));
        assertTrue(userMessage.contains("Recommendation Strategy"));
    }

    @Test
    void testGetCurrentPromptVersion() {
        String version = promptTemplateService.getCurrentPromptVersion();
        assertEquals("v2", version);
    }

    @Test
    void testEmptyCandidatesHandling() {
        List<ProblemCandidate> emptyCandidates = new ArrayList<>();
        String userMessage = promptTemplateService.buildUserMessage(testContext, emptyCandidates, testOptions, "v2");
        
        assertNotNull(userMessage);
        assertTrue(userMessage.contains("insufficient_data"));
        assertTrue(userMessage.contains("exploration"));
    }

    @Test
    void testNullContextHandling() {
        String userMessage = promptTemplateService.buildUserMessage(null, testCandidates, testOptions, "v1");
        
        assertNotNull(userMessage);
        assertTrue(userMessage.contains("unknown")); // default userId
    }

    @Test
    void testUserInsightsGeneration() {
        // Test with varied performance candidates
        List<ProblemCandidate> variedCandidates = new ArrayList<>();
        variedCandidates.add(createCandidate(1L, "数组", "EASY", List.of("数组"), 0.9, 1));  // strong
        variedCandidates.add(createCandidate(2L, "图", "MEDIUM", List.of("BFS"), 0.3, 4)); // weak
        variedCandidates.add(createCandidate(3L, "动态规划", "HARD", List.of("DP"), 0.2, 6)); // weak
        
        String userMessage = promptTemplateService.buildUserMessage(testContext, variedCandidates, testOptions, "v2");
        
        assertTrue(userMessage.contains("图") || userMessage.contains("动态规划")); // should identify weak topics
        assertTrue(userMessage.contains("数组")); // should identify strong topics
    }

    @Test
    void testAdvancedUserMessage_WithUserProfile_ShouldUseProfileData() {
        // Given - Context with UserProfile
        RequestContext contextWithProfile = new RequestContext();
        contextWithProfile.setUserId(123L);
        contextWithProfile.setTier("GOLD");
        contextWithProfile.setAbGroup("experimental");
        
        UserProfile userProfile = createMockUserProfile();
        contextWithProfile.setUserProfile(userProfile);

        // When
        String result = promptTemplateService.buildUserMessage(contextWithProfile, testCandidates, testOptions, "v2");

        // Then - Verify that profile-specific information is included
        assertThat(result).contains("Learning Pattern: ADVANCED");
        assertThat(result).contains("Overall Mastery: 85.5%");
        assertThat(result).contains("Average Accuracy: 92.3%");
        assertThat(result).contains("Weak Domains: graph, dynamic_programming");
        assertThat(result).contains("Strong Domains: arrays, strings");
        assertThat(result).contains("Learning Approach: CHALLENGE_FOCUSED");
        assertThat(result).contains("Data Quality:");
        
        // Verify strategy section uses profile data
        assertThat(result).contains("Focus on: CHALLENGE_FOCUSED");
        assertThat(result).contains("Address weak domains: graph, dynamic_programming");
        assertThat(result).contains("Build on strong domains: arrays, strings");
    }

    @Test
    void testAdvancedUserMessage_WithoutUserProfile_ShouldUseCandidateInsights() {
        // Given - Context without UserProfile
        RequestContext contextWithoutProfile = new RequestContext();
        contextWithoutProfile.setUserId(456L);
        contextWithoutProfile.setTier("BRONZE");
        contextWithoutProfile.setAbGroup("default");
        // No UserProfile set

        // When
        String result = promptTemplateService.buildUserMessage(contextWithoutProfile, testCandidates, testOptions, "v2");

        // Then - Verify fallback to candidate-derived insights
        assertThat(result).contains("Weak Topics:");
        assertThat(result).contains("Strong Topics:");
        assertThat(result).contains("Difficulty Preference:");
        
        // Should not contain profile-specific data
        assertThat(result).doesNotContain("Overall Mastery:");
        assertThat(result).doesNotContain("Data Quality:");
        assertThat(result).doesNotContain("Learning Approach:");
    }

    @Test
    void testAdvancedUserMessage_WithNullDifficultyPref_ShouldHandleGracefully() {
        // Given - Profile with null difficulty preference
        RequestContext contextWithNullDiff = new RequestContext();
        contextWithNullDiff.setUserId(123L);
        contextWithNullDiff.setTier("SILVER");
        
        UserProfile profileWithNullDiff = UserProfile.builder()
                .userId(123L)
                .generatedAt(Instant.now())
                .window("recent90d")
                .domainSkills(Map.of("arrays", createMockDomainSkill("arrays")))
                .difficultyPref(null) // Null difficulty preference
                .tagAffinity(new HashMap<>())
                .overallMastery(0.7)
                .totalProblemsReviewed(15)
                .totalReviewAttempts(25)
                .averageAccuracy(0.8)
                .learningPattern(UserProfile.LearningPattern.STEADY_PROGRESS)
                .build();
        contextWithNullDiff.setUserProfile(profileWithNullDiff);

        // When
        String result = promptTemplateService.buildUserMessage(contextWithNullDiff, testCandidates, testOptions, "v2");

        // Then - Should handle null gracefully with fallback values
        assertThat(result).contains("Difficulty Distribution: balanced");
        assertThat(result).contains("Match difficulty preference: BALANCED");
        assertThat(result).doesNotContain("null");
    }

    @Test
    void testAdvancedUserMessage_WithLowDataQuality_ShouldIncludeExplorationNote() {
        // Given - Profile with low data quality (few problems reviewed)
        RequestContext contextWithLowQuality = new RequestContext();
        contextWithLowQuality.setUserId(789L);
        contextWithLowQuality.setTier("BRONZE");
        
        UserProfile lowQualityProfile = UserProfile.builder()
                .userId(789L)
                .generatedAt(Instant.now())
                .window("recent90d")
                .domainSkills(new HashMap<>()) // Empty skills = low quality
                .difficultyPref(DifficultyPref.builder()
                        .easy(0.3).medium(0.5).hard(0.2)
                        .trend(DifficultyPref.DifficultyTrend.STABLE)
                        .preferredLevel(DifficultyPref.PreferredLevel.BALANCED)
                        .build())
                .tagAffinity(new HashMap<>())
                .overallMastery(0.5)
                .totalProblemsReviewed(2) // Low number = low quality
                .totalReviewAttempts(3)
                .averageAccuracy(0.6)
                .learningPattern(UserProfile.LearningPattern.STRUGGLING)
                .build();
        contextWithLowQuality.setUserProfile(lowQualityProfile);

        // When
        String result = promptTemplateService.buildUserMessage(contextWithLowQuality, testCandidates, testOptions, "v2");

        // Then - Should include note about limited data when quality is low
        assertThat(result).contains("Limited data available");
        assertThat(result).contains("broader exploration strategy");
    }

    private UserProfile createMockUserProfile() {
        Map<String, DomainSkill> domainSkills = new HashMap<>();
        // Strong domains
        domainSkills.put("arrays", createMockDomainSkill("arrays"));
        domainSkills.put("strings", createMockDomainSkill("strings"));
        // Weak domains expected by assertions
        domainSkills.put("graph", DomainSkill.builder()
                .domain("graph")
                .samples(15)
                .accuracy(0.35)
                .retention(0.4)
                .lapseRate(0.4)
                .avgRtMs(180000L)
                .attempts(20)
                .skillScore(0.3)
                .strength(DomainSkill.StrengthLevel.WEAK)
                .build());
        domainSkills.put("dynamic_programming", DomainSkill.builder()
                .domain("dynamic_programming")
                .samples(15)
                .accuracy(0.4)
                .retention(0.45)
                .lapseRate(0.35)
                .avgRtMs(150000L)
                .attempts(18)
                .skillScore(0.35)
                .strength(DomainSkill.StrengthLevel.WEAK)
                .build());

        return UserProfile.builder()
                .userId(123L)
                .generatedAt(Instant.now())
                .window("recent90d")
                .domainSkills(domainSkills)
                .difficultyPref(DifficultyPref.builder()
                        .easy(0.2).medium(0.6).hard(0.2)
                        .trend(DifficultyPref.DifficultyTrend.INCREASING)
                        .preferredLevel(DifficultyPref.PreferredLevel.SEEKING_CHALLENGE)
                        .build())
                .tagAffinity(Map.of("array", 0.8, "string", 0.6, "graph", 0.3))
                .overallMastery(0.855)
                .totalProblemsReviewed(50)
                .totalReviewAttempts(85)
                .averageAccuracy(0.923)
                .learningPattern(UserProfile.LearningPattern.ADVANCED)
                .build();
    }

    private DomainSkill createMockDomainSkill(String domain) {
        return DomainSkill.builder()
                .domain(domain)
                .samples(15)
                .accuracy(0.85)
                .retention(0.9)
                .lapseRate(0.1)
                .avgRtMs(4500L)
                .attempts(25)
                .skillScore(0.8)
                .strength(DomainSkill.StrengthLevel.STRONG)
                .build();
    }

    private ProblemCandidate createCandidate(Long id, String topic, String difficulty, 
                                           List<String> tags, Double accuracy, Integer attempts) {
        ProblemCandidate candidate = new ProblemCandidate();
        candidate.id = id;
        candidate.topic = topic;
        candidate.difficulty = difficulty;
        candidate.tags = tags;
        candidate.recentAccuracy = accuracy;
        candidate.attempts = attempts;
        return candidate;
    }
}
