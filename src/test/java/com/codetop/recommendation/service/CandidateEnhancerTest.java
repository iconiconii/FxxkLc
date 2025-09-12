package com.codetop.recommendation.service;

import com.codetop.recommendation.alg.SimilarityScorer;
import com.codetop.recommendation.config.SimilarityProperties;
import com.codetop.recommendation.config.UserProfilingProperties;
import com.codetop.recommendation.dto.DomainSkill;
import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.provider.LlmProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CandidateEnhancerTest {

    private CandidateEnhancer enhancer;

    @Mock
    private SimilarProblemService similarProblemService;

    private SimilarityScorer similarityScorer;
    private SimilarityProperties similarityProps;

    @BeforeEach
    void setUp() {
        UserProfilingProperties up = new UserProfilingProperties();
        up.getWindows().setRecentDays(90);
        up.getThresholds().setWeakSkillThreshold(0.45);
        up.getThresholds().setStrongSkillThreshold(0.75);

        similarityProps = new SimilarityProperties();
        similarityProps.getIntegration().setPreFilterEnabled(true);
        similarityProps.getIntegration().setPreFilterLimit(5);
        similarityProps.getWeights().setDiversityWeight(0.2);

        SimilarityProperties scorerProps = new SimilarityProperties();
        similarityScorer = new SimilarityScorer(scorerProps, new SimpleMeterRegistry());

        enhancer = new CandidateEnhancer(up, new com.fasterxml.jackson.databind.ObjectMapper(), similarityScorer, similarProblemService, similarityProps);
    }

    @Test
    void enhanceCandidates_appliesPreFilterAndIsDeterministic() {
        List<LlmProvider.ProblemCandidate> candidates = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            LlmProvider.ProblemCandidate c = new LlmProvider.ProblemCandidate();
            c.id = i;
            c.topic = "p" + i;
            c.difficulty = (i % 2 == 0) ? "MEDIUM" : "EASY";
            c.tags = List.of("array");
            candidates.add(c);
        }

        // Mock similar problems to control similarity scores: higher id -> higher score
        lenient().when(similarProblemService.findEnhancedSimilarProblems(anyLong(), anyInt(), any(), any()))
                .thenAnswer(inv -> {
                    Long pid = inv.getArgument(0);
                    SimilarProblemService.EnhancedSimilarProblem sp = new SimilarProblemService.EnhancedSimilarProblem();
                    sp.setProblemId(pid + 1000);
                    sp.setSimilarityScore(Math.min(1.0, pid / 10.0));
                    sp.setTags(List.of("array"));
                    return List.of(sp);
                });

        UserProfile profile = makeProfile(42L, Set.of("arrays"), Set.of("graphs"));

        List<LlmProvider.ProblemCandidate> out1 = enhancer.enhanceCandidates(candidates, profile, 5);
        List<LlmProvider.ProblemCandidate> out2 = enhancer.enhanceCandidates(candidates, profile, 5);

        // Pre-filter should reduce candidate pool before selection and not exceed limit
        assertTrue(out1.size() > 0 && out1.size() <= 5);
        // Deterministic behavior: same input -> same selection
        assertEquals(out1.stream().map(c -> c.id).toList(), out2.stream().map(c -> c.id).toList());
    }

    private UserProfile makeProfile(Long userId, Set<String> weakDomains, Set<String> strongDomains) {
        Map<String, DomainSkill> domainSkills = new HashMap<>();
        DomainSkill weak = DomainSkill.builder().domain("arrays").skillScore(0.3).build();
        DomainSkill strong = DomainSkill.builder().domain("graphs").skillScore(0.9).build();
        domainSkills.put("arrays", weak);
        domainSkills.put("graphs", strong);

        return UserProfile.builder()
                .userId(userId)
                .generatedAt(Instant.now())
                .domainSkills(domainSkills)
                .overallMastery(0.5)
                .tagAffinity(Map.of("array", 0.6))
                .learningPattern(UserProfile.LearningPattern.STEADY_PROGRESS)
                .build();
    }
}
