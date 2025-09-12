package com.codetop.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.codetop.entity.FSRSCard;
import com.codetop.entity.Problem;
import com.codetop.entity.ReviewLog;
import com.codetop.enums.Difficulty;
import com.codetop.enums.FSRSState;
import com.codetop.mapper.FSRSCardMapper;
import com.codetop.mapper.ProblemMapper;
import com.codetop.mapper.ReviewLogMapper;
import com.codetop.recommendation.dto.DifficultyPref;
import com.codetop.recommendation.dto.DomainSkill;
import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.config.UserProfilingProperties;
import com.codetop.service.cache.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserProfilingServiceTest {
    
    @Mock private ReviewLogMapper reviewLogMapper;
    @Mock private FSRSCardMapper fsrsCardMapper;
    @Mock private ProblemMapper problemMapper;
    @Mock private CacheService cacheService;
    @Mock private ObjectMapper objectMapper;
    @Mock private UserProfilingProperties config;
    
    @InjectMocks private UserProfilingService userProfilingService;
    
    private static final Long TEST_USER_ID = 123L;
    private static final Long PROBLEM_ID_ARRAY = 1L;
    private static final Long PROBLEM_ID_DP = 2L;
    private static final Long PROBLEM_ID_GRAPH = 3L;
    
    @BeforeEach
    void setUp() {
        // Mock ObjectMapper for tag parsing
        try {
            org.mockito.Mockito.lenient().when(objectMapper.readValue(eq("[\"array\"]"), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>>any()))
                    .thenReturn(Arrays.asList("array"));
            org.mockito.Mockito.lenient().when(objectMapper.readValue(eq("[\"dynamic-programming\"]"), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>>any()))
                    .thenReturn(Arrays.asList("dynamic-programming"));
            org.mockito.Mockito.lenient().when(objectMapper.readValue(eq("[\"graph\"]"), org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>>any()))
                    .thenReturn(Arrays.asList("graph"));
        } catch (Exception e) {
            // Should not happen in tests
        }

        // Configure profiling properties defaults
        UserProfilingProperties.Windows windows = new UserProfilingProperties.Windows();
        windows.setRecentDays(90);
        windows.setDefaultReviewLimit(2000);
        windows.setHalfLifeDays(30.0);
        windows.setTrendComparisonDays(30);
        org.mockito.Mockito.lenient().when(config.getWindows()).thenReturn(windows);

        UserProfilingProperties.Thresholds thresholds = new UserProfilingProperties.Thresholds();
        // use defaults; can customize if needed
        org.mockito.Mockito.lenient().when(config.getThresholds()).thenReturn(thresholds);

        UserProfilingProperties.Cache cache = new UserProfilingProperties.Cache();
        cache.setEnabled(true);
        cache.setTtl(Duration.ofHours(1));
        org.mockito.Mockito.lenient().when(config.getCache()).thenReturn(cache);

        java.util.Map<String, String> mapping = new java.util.HashMap<>();
        mapping.put("array", "arrays");
        mapping.put("dynamic-programming", "dynamic_programming");
        mapping.put("graph", "graph");
        org.mockito.Mockito.lenient().when(config.getTagDomainMapping()).thenReturn(mapping);
    }
    
    @Test
    void getUserProfile_WithCacheHit_ShouldReturnCachedProfile() {
        // Given
        UserProfile cachedProfile = UserProfile.builder()
                .userId(TEST_USER_ID)
                .overallMastery(0.75)
                .build();
        
        when(cacheService.get(anyString(), eq(UserProfile.class))).thenReturn(cachedProfile);
        
        // When
        UserProfile result = userProfilingService.getUserProfile(TEST_USER_ID, true);
        
        // Then
        assertThat(result).isEqualTo(cachedProfile);
        verify(reviewLogMapper, never()).findRecentByUserId(anyLong(), anyInt());
    }
    
    @Test
    void getUserProfile_WithCacheMiss_ShouldComputeAndCache() {
        // Given
        when(cacheService.get(anyString(), eq(UserProfile.class))).thenReturn(null);
        
        List<ReviewLog> mockReviews = createMockReviewLogs();
        when(reviewLogMapper.findRecentByUserIdInWindow(eq(TEST_USER_ID), any(LocalDateTime.class), eq(2000)))
                .thenReturn(mockReviews);
        
        List<Problem> mockProblems = createMockProblems();
        when(problemMapper.selectBatchIds(any())).thenReturn(mockProblems);
        
        List<FSRSCard> mockCards = createMockFSRSCards();
        when(fsrsCardMapper.selectList(any(QueryWrapper.class))).thenReturn(mockCards);
        
        // When
        UserProfile result = userProfilingService.getUserProfile(TEST_USER_ID, true);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getWindow()).isEqualTo("recent90d");
        assertThat(result.getDomainSkills()).isNotEmpty();
        
        verify(cacheService).put(anyString(), eq(result), eq(Duration.ofHours(1)));
    }
    
    @Test
    void computeUserProfile_WithNoReviews_ShouldReturnDefaultProfile() {
        // Given
        when(reviewLogMapper.findRecentByUserIdInWindow(eq(TEST_USER_ID), any(LocalDateTime.class), eq(2000)))
                .thenReturn(List.of());
        
        // When
        UserProfile result = userProfilingService.computeUserProfile(TEST_USER_ID);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getOverallMastery()).isEqualTo(0.5);
        assertThat(result.getTotalProblemsReviewed()).isEqualTo(0);
        assertThat(result.getLearningPattern()).isEqualTo(UserProfile.LearningPattern.STEADY_PROGRESS);
        
        // Default difficulty preference should be balanced
        DifficultyPref difficultyPref = result.getDifficultyPref();
        assertThat(difficultyPref.getEasy()).isEqualTo(0.3);
        assertThat(difficultyPref.getMedium()).isEqualTo(0.5);
        assertThat(difficultyPref.getHard()).isEqualTo(0.2);
    }
    
    @Test
    void computeUserProfile_WithReviews_ShouldComputeAccurateProfile() {
        // Given
        List<ReviewLog> mockReviews = createMockReviewLogs();
        when(reviewLogMapper.findRecentByUserIdInWindow(eq(TEST_USER_ID), any(LocalDateTime.class), eq(2000)))
                .thenReturn(mockReviews);
        
        List<Problem> mockProblems = createMockProblems();
        when(problemMapper.selectBatchIds(any())).thenReturn(mockProblems);
        
        List<FSRSCard> mockCards = createMockFSRSCards();
        when(fsrsCardMapper.selectList(any(QueryWrapper.class))).thenReturn(mockCards);
        
        // When
        UserProfile result = userProfilingService.computeUserProfile(TEST_USER_ID);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
        
        // Check domain skills
        Map<String, DomainSkill> domainSkills = result.getDomainSkills();
        assertThat(domainSkills).isNotEmpty();
        assertThat(domainSkills).containsKeys("arrays", "dynamic_programming", "graph");
        
        // Check arrays domain (high success rate)
        DomainSkill arraysSkill = domainSkills.get("arrays");
        assertThat(arraysSkill).isNotNull();
        assertThat(arraysSkill.getAccuracy()).isGreaterThan(0.75); // High success rate (tolerant)
        assertThat(arraysSkill.getStrength()).isEqualTo(DomainSkill.StrengthLevel.NORMAL); // Small sample
        
        // Check difficulty preferences
        DifficultyPref difficultyPref = result.getDifficultyPref();
        assertThat(difficultyPref).isNotNull();
        assertThat(difficultyPref.isValidDistribution()).isTrue();
        
        // Check learning pattern determination
        assertThat(result.getLearningPattern()).isIn(
                UserProfile.LearningPattern.STRUGGLING,
                UserProfile.LearningPattern.STEADY_PROGRESS,
                UserProfile.LearningPattern.ADVANCED
        );
    }
    
    @Test
    void getDomainSkill_WithStrongPerformance_ShouldMarkAsStrong() {
        // Given
        List<ReviewLog> strongReviews = createStrongPerformanceReviews();
        when(reviewLogMapper.findRecentByUserIdInWindow(eq(TEST_USER_ID), any(LocalDateTime.class), eq(2000)))
                .thenReturn(strongReviews);
        
        List<Problem> mockProblems = createMockProblems();
        when(problemMapper.selectBatchIds(any())).thenReturn(mockProblems);
        
        List<FSRSCard> highStabilityCards = createHighStabilityCards();
        when(fsrsCardMapper.selectList(any(QueryWrapper.class))).thenReturn(highStabilityCards);
        
        // Adjust reliability threshold for this test to treat single-problem domain as sufficient
        com.codetop.recommendation.config.UserProfilingProperties.Thresholds tStrong = new com.codetop.recommendation.config.UserProfilingProperties.Thresholds();
        tStrong.setMinSamplesForReliability(1);
        org.mockito.Mockito.lenient().when(config.getThresholds()).thenReturn(tStrong);
        // When
        UserProfile result = userProfilingService.computeUserProfile(TEST_USER_ID);
        
        // Then
        DomainSkill arraysSkill = result.getDomainSkills().get("arrays");
        assertThat(arraysSkill).isNotNull();
        assertThat(arraysSkill.getSkillScore()).isGreaterThan(0.75);
        assertThat(arraysSkill.getStrength()).isEqualTo(DomainSkill.StrengthLevel.STRONG);
        // With threshold overridden to 1, single unique problem is sufficient
        assertThat(arraysSkill.hasSufficientSamples()).isTrue();
    }
    
    @Test
    void getDomainSkill_WithWeakPerformance_ShouldMarkAsWeak() {
        // Given
        List<ReviewLog> weakReviews = createWeakPerformanceReviews();
        when(reviewLogMapper.findRecentByUserIdInWindow(eq(TEST_USER_ID), any(LocalDateTime.class), eq(2000)))
                .thenReturn(weakReviews);
        
        List<Problem> mockProblems = createMockProblems();
        when(problemMapper.selectBatchIds(any())).thenReturn(mockProblems);
        
        List<FSRSCard> lowStabilityCards = createLowStabilityCards();
        when(fsrsCardMapper.selectList(any(QueryWrapper.class))).thenReturn(lowStabilityCards);
        
        // Adjust weak threshold to make weak classification more sensitive for this test
        com.codetop.recommendation.config.UserProfilingProperties.Thresholds tWeak = new com.codetop.recommendation.config.UserProfilingProperties.Thresholds();
        tWeak.setWeakSkillThreshold(0.5);
        tWeak.setMinSamplesForReliability(1);
        org.mockito.Mockito.lenient().when(config.getThresholds()).thenReturn(tWeak);
        // When
        UserProfile result = userProfilingService.computeUserProfile(TEST_USER_ID);
        
        // Then
        DomainSkill arraysSkill = result.getDomainSkills().get("arrays");
        assertThat(arraysSkill).isNotNull();
        assertThat(arraysSkill.getSkillScore()).isLessThan(0.5);
        assertThat(arraysSkill.getStrength()).isEqualTo(DomainSkill.StrengthLevel.WEAK);
    }
    
    @Test
    void invalidateUserProfileCache_ShouldCallCacheDelete() {
        // When
        userProfilingService.invalidateUserProfileCache(TEST_USER_ID);
        
        // Then
        verify(cacheService).delete(anyString());
    }
    
    // Helper methods for creating test data
    
    private List<ReviewLog> createMockReviewLogs() {
        LocalDateTime now = LocalDateTime.now();
        return Arrays.asList(
                // Arrays domain - good performance
                createReviewLog(PROBLEM_ID_ARRAY, 4, 30000, now.minusDays(1)),
                createReviewLog(PROBLEM_ID_ARRAY, 3, 45000, now.minusDays(2)),
                createReviewLog(PROBLEM_ID_ARRAY, 4, 25000, now.minusDays(5)),
                
                // DP domain - mixed performance
                createReviewLog(PROBLEM_ID_DP, 2, 90000, now.minusDays(3)),
                createReviewLog(PROBLEM_ID_DP, 3, 75000, now.minusDays(7)),
                createReviewLog(PROBLEM_ID_DP, 1, 120000, now.minusDays(10)),
                
                // Graph domain - new/learning
                createReviewLog(PROBLEM_ID_GRAPH, 2, 100000, now.minusDays(4)),
                createReviewLog(PROBLEM_ID_GRAPH, 1, 150000, now.minusDays(15))
        );
    }
    
    private List<ReviewLog> createStrongPerformanceReviews() {
        LocalDateTime now = LocalDateTime.now();
        // Create 15 reviews with high success rate for arrays domain
        return Arrays.asList(
                createReviewLog(PROBLEM_ID_ARRAY, 4, 20000, now.minusDays(1)),
                createReviewLog(PROBLEM_ID_ARRAY, 4, 25000, now.minusDays(2)),
                createReviewLog(PROBLEM_ID_ARRAY, 3, 30000, now.minusDays(3)),
                createReviewLog(PROBLEM_ID_ARRAY, 4, 22000, now.minusDays(4)),
                createReviewLog(PROBLEM_ID_ARRAY, 4, 18000, now.minusDays(5)),
                createReviewLog(PROBLEM_ID_ARRAY, 3, 28000, now.minusDays(6)),
                createReviewLog(PROBLEM_ID_ARRAY, 4, 20000, now.minusDays(7)),
                createReviewLog(PROBLEM_ID_ARRAY, 4, 24000, now.minusDays(8)),
                createReviewLog(PROBLEM_ID_ARRAY, 3, 26000, now.minusDays(9)),
                createReviewLog(PROBLEM_ID_ARRAY, 4, 19000, now.minusDays(10)),
                createReviewLog(PROBLEM_ID_ARRAY, 4, 23000, now.minusDays(11)),
                createReviewLog(PROBLEM_ID_ARRAY, 3, 27000, now.minusDays(12)),
                createReviewLog(PROBLEM_ID_ARRAY, 4, 21000, now.minusDays(13)),
                createReviewLog(PROBLEM_ID_ARRAY, 4, 25000, now.minusDays(14)),
                createReviewLog(PROBLEM_ID_ARRAY, 3, 29000, now.minusDays(15))
        );
    }
    
    private List<ReviewLog> createWeakPerformanceReviews() {
        LocalDateTime now = LocalDateTime.now();
        // Create 15 reviews with low success rate for arrays domain
        return Arrays.asList(
                createReviewLog(PROBLEM_ID_ARRAY, 1, 120000, now.minusDays(1)),
                createReviewLog(PROBLEM_ID_ARRAY, 2, 100000, now.minusDays(2)),
                createReviewLog(PROBLEM_ID_ARRAY, 1, 150000, now.minusDays(3)),
                createReviewLog(PROBLEM_ID_ARRAY, 2, 110000, now.minusDays(4)),
                createReviewLog(PROBLEM_ID_ARRAY, 1, 140000, now.minusDays(5)),
                createReviewLog(PROBLEM_ID_ARRAY, 2, 95000, now.minusDays(6)),
                createReviewLog(PROBLEM_ID_ARRAY, 1, 130000, now.minusDays(7)),
                createReviewLog(PROBLEM_ID_ARRAY, 2, 105000, now.minusDays(8)),
                createReviewLog(PROBLEM_ID_ARRAY, 1, 160000, now.minusDays(9)),
                createReviewLog(PROBLEM_ID_ARRAY, 2, 115000, now.minusDays(10)),
                createReviewLog(PROBLEM_ID_ARRAY, 1, 135000, now.minusDays(11)),
                createReviewLog(PROBLEM_ID_ARRAY, 3, 80000, now.minusDays(12)), // Occasional success
                createReviewLog(PROBLEM_ID_ARRAY, 1, 145000, now.minusDays(13)),
                createReviewLog(PROBLEM_ID_ARRAY, 2, 120000, now.minusDays(14)),
                createReviewLog(PROBLEM_ID_ARRAY, 1, 155000, now.minusDays(15))
        );
    }
    
    private ReviewLog createReviewLog(Long problemId, Integer rating, Integer responseTime, LocalDateTime reviewedAt) {
        return ReviewLog.builder()
                .userId(TEST_USER_ID)
                .problemId(problemId)
                .rating(rating)
                .responseTimeMs(responseTime)
                .reviewedAt(reviewedAt)
                .oldState(FSRSState.REVIEW)
                .newState(FSRSState.REVIEW)
                .build();
    }
    
    private List<Problem> createMockProblems() {
        Problem p1 = Problem.builder()
                .title("Two Sum")
                .difficulty(Difficulty.EASY)
                .tags("[\"array\"]")
                .build();
        p1.setId(PROBLEM_ID_ARRAY);

        Problem p2 = Problem.builder()
                .title("Climbing Stairs")
                .difficulty(Difficulty.MEDIUM)
                .tags("[\"dynamic-programming\"]")
                .build();
        p2.setId(PROBLEM_ID_DP);

        Problem p3 = Problem.builder()
                .title("Number of Islands")
                .difficulty(Difficulty.HARD)
                .tags("[\"graph\"]")
                .build();
        p3.setId(PROBLEM_ID_GRAPH);

        return Arrays.asList(p1, p2, p3);
    }
    
    private List<FSRSCard> createMockFSRSCards() {
        return Arrays.asList(
                createFSRSCard(PROBLEM_ID_ARRAY, new BigDecimal("5.0"), new BigDecimal("2.5")),
                createFSRSCard(PROBLEM_ID_DP, new BigDecimal("3.0"), new BigDecimal("6.0")),
                createFSRSCard(PROBLEM_ID_GRAPH, new BigDecimal("1.5"), new BigDecimal("8.0"))
        );
    }
    
    private List<FSRSCard> createHighStabilityCards() {
        return Arrays.asList(
                createFSRSCard(PROBLEM_ID_ARRAY, new BigDecimal("15.0"), new BigDecimal("3.2"))
        );
    }
    
    private List<FSRSCard> createLowStabilityCards() {
        return Arrays.asList(
                createFSRSCard(PROBLEM_ID_ARRAY, new BigDecimal("1.5"), new BigDecimal("7.5"))
        );
    }
    
    private FSRSCard createFSRSCard(Long problemId, BigDecimal stability, BigDecimal difficulty) {
        return FSRSCard.builder()
                .userId(TEST_USER_ID)
                .problemId(problemId)
                .stability(stability)
                .difficulty(difficulty)
                .state(FSRSState.REVIEW)
                .reviewCount(5)
                .lapses(1)
                .lastReview(LocalDateTime.now().minusDays(2))
                .build();
    }
}
