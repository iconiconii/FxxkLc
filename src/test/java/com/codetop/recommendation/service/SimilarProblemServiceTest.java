package com.codetop.recommendation.service;

import com.codetop.mapper.ProblemCategoryMapper;
import com.codetop.mapper.ProblemMapper;
import com.codetop.recommendation.alg.SimilarityScorer;
import com.codetop.recommendation.alg.TagsParser;
import com.codetop.recommendation.config.SimilarityProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SimilarProblemServiceTest {

    @Mock
    private ProblemCategoryMapper problemCategoryMapper;

    @Mock
    private ProblemMapper problemMapper;

    private SimilarityScorer similarityScorer;
    private TagsParser tagsParser;
    @Mock
    private CategoryMetadataService categoryMetadataService;

    private SimilarProblemService service;

    @BeforeEach
    void setUp() {
        // Configurable properties
        SimilarityProperties props = new SimilarityProperties();
        props.getWeights().setTagsWeight(0.4);
        props.getWeights().setCategoriesWeight(0.3);
        props.getWeights().setDifficultyWeight(0.3);
        props.getThresholds().setEmptyFeatureSimilarity(0.0);

        similarityScorer = new SimilarityScorer(props, new SimpleMeterRegistry());
        tagsParser = new TagsParser();
        service = new SimilarProblemService(problemCategoryMapper, problemMapper, similarityScorer, tagsParser, new SimpleMeterRegistry(), categoryMetadataService);
    }

    @Test
    void findEnhancedSimilarProblems_returnsSortedAndFiltered() {
        Long targetId = 1L;

        // Category-based similar candidates: 2,3,4
        List<ProblemCategoryMapper.SimilarProblem> catSimilar = new ArrayList<>();
        catSimilar.add(makeSimilar(2L, "MEDIUM", 2, "arrays,hash_tables"));
        catSimilar.add(makeSimilar(3L, "MEDIUM", 1, "arrays"));
        catSimilar.add(makeSimilar(4L, "HARD", 1, "graphs"));
        when(problemCategoryMapper.findSimilarProblems(eq(targetId), any(Integer.class), any(Integer.class)))
                .thenReturn(catSimilar);

        // Target tags and categories
        when(problemMapper.findTagsByProblemIds(eq(List.of(targetId))))
                .thenReturn(List.of(minimalTags(targetId, "[\"array\",\"hash-table\"]")));
        when(problemCategoryMapper.findCategoriesByProblemId(eq(targetId)))
                .thenReturn(List.of(withDetails(10L), withDetails(11L))); // category IDs 10,11
        when(problemMapper.findDifficultiesByIds(eq(List.of(targetId))))
                .thenReturn(List.of(minimalDifficulty(targetId, "MEDIUM")));

        // Candidates tags
        when(problemMapper.findTagsByProblemIds(eq(List.of(2L, 3L, 4L))))
                .thenReturn(List.of(
                        minimalTags(2L, "[\"array\",\"hash-table\"]"), // identical
                        minimalTags(3L, "[\"array\"]"),
                        minimalTags(4L, "[\"tree\"]")
                ));
        // Candidate difficulties
        when(problemMapper.findDifficultiesByIds(eq(List.of(2L, 3L, 4L))))
                .thenReturn(List.of(
                        minimalDifficulty(2L, "MEDIUM"),
                        minimalDifficulty(3L, "MEDIUM"),
                        minimalDifficulty(4L, "HARD")
                ));
        // Candidate category associations
        when(problemCategoryMapper.findExistingAssociationsByProblemIds(eq(List.of(2L, 3L, 4L))))
                .thenReturn(List.of(
                        assoc(2L, 10L), assoc(2L, 11L), // matches target categories
                        assoc(3L, 10L),
                        assoc(4L, 12L)
                ));

        List<SimilarProblemService.EnhancedSimilarProblem> results =
                service.findEnhancedSimilarProblems(targetId, 3, 1, 0.1);

        assertNotNull(results);
        assertEquals(3, results.size());
        // Expect problem 2 (identical tags, categories, same difficulty) to be ranked first
        assertEquals(2L, results.get(0).getProblemId());
        // Expect non-zero similarity for candidate 3 and 4 filtered by threshold
        assertTrue(results.get(1).getSimilarityScore() >= results.get(2).getSimilarityScore());
    }

    private ProblemCategoryMapper.SimilarProblem makeSimilar(Long id, String diff, int shared, String names) {
        ProblemCategoryMapper.SimilarProblem p = new ProblemCategoryMapper.SimilarProblem();
        p.setProblemId(id);
        p.setDifficulty(diff);
        p.setSharedCategories(shared);
        p.setSharedCategoryNames(names);
        p.setTitle("p" + id);
        return p;
    }

    private ProblemMapper.ProblemTagsMinimal minimalTags(Long id, String json) {
        ProblemMapper.ProblemTagsMinimal pt = new ProblemMapper.ProblemTagsMinimal();
        pt.setId(id);
        pt.setTags(json);
        return pt;
    }

    private ProblemMapper.ProblemDifficultyMinimal minimalDifficulty(Long id, String diff) {
        ProblemMapper.ProblemDifficultyMinimal pd = new ProblemMapper.ProblemDifficultyMinimal();
        pd.setId(id);
        pd.setDifficulty(diff);
        return pd;
    }

    private ProblemCategoryMapper.ProblemCategoryAssociation assoc(Long pid, Long cid) {
        ProblemCategoryMapper.ProblemCategoryAssociation a = new ProblemCategoryMapper.ProblemCategoryAssociation();
        a.setProblemId(pid);
        a.setCategoryId(cid);
        return a;
    }

    private ProblemCategoryMapper.ProblemCategoryWithDetails withDetails(Long cid) {
        ProblemCategoryMapper.ProblemCategoryWithDetails d = new ProblemCategoryMapper.ProblemCategoryWithDetails();
        d.setCategoryId(cid);
        return d;
    }
}
