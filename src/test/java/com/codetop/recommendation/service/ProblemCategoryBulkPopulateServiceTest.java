package com.codetop.recommendation.service;

import com.codetop.mapper.ProblemCategoryMapper;
import com.codetop.mapper.ProblemMapper;
import com.codetop.recommendation.alg.TagsParser;
import com.codetop.recommendation.config.SimilarityProperties;
import com.codetop.recommendation.config.UserProfilingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProblemCategoryBulkPopulateServiceTest {

    @Mock
    private ProblemMapper problemMapper;
    @Mock
    private ProblemCategoryMapper problemCategoryMapper;

    private TagsParser tagsParser;
    private UserProfilingProperties userProfilingProperties;
    private SimilarityProperties similarityProperties;

    private ProblemCategoryBulkPopulateService service;

    @BeforeEach
    void setUp() {
        tagsParser = new TagsParser();
        userProfilingProperties = new UserProfilingProperties();
        // Configure tag→domain mapping
        userProfilingProperties.setTagDomainMapping(Map.of(
                "array", "arrays",
                "hash-table", "hash_tables"
        ));
        similarityProperties = new SimilarityProperties();
        similarityProperties.getBatch().setInsertBatchSize(100);

        service = new ProblemCategoryBulkPopulateService(
                problemMapper,
                problemCategoryMapper,
                tagsParser,
                userProfilingProperties,
                similarityProperties
        );
    }

    @Test
    void bulkPopulateFromTags_dryRun_countsAssignmentsAndSkipsDuplicates() {
        // Problems with tags
        ProblemMapper.ProblemTagsMinimal p1 = new ProblemMapper.ProblemTagsMinimal(); p1.setId(1L); p1.setTags("[\"array\",\"hash-table\"]");
        ProblemMapper.ProblemTagsMinimal p2 = new ProblemMapper.ProblemTagsMinimal(); p2.setId(2L); p2.setTags("[\"array\"]");
        when(problemMapper.findAllProblemsWithTags()).thenReturn(List.of(p1, p2));

        // Categories by name
        ProblemCategoryMapper.CategoryNameId cArrays = new ProblemCategoryMapper.CategoryNameId(); cArrays.setId(10L); cArrays.setName("arrays");
        ProblemCategoryMapper.CategoryNameId cHash = new ProblemCategoryMapper.CategoryNameId(); cHash.setId(11L); cHash.setName("hash_tables");
        when(problemCategoryMapper.findAllCategoryNameIds()).thenReturn(List.of(cArrays, cHash));

        // Existing association: (1, arrays) already present
        ProblemCategoryMapper.ProblemCategoryAssociation assoc = new ProblemCategoryMapper.ProblemCategoryAssociation();
        assoc.setProblemId(1L); assoc.setCategoryId(10L);
        when(problemCategoryMapper.findExistingAssociationsByProblemIds(any())).thenReturn(List.of(assoc));

        ProblemCategoryBulkPopulateService.BulkPopulateResult result = service.bulkPopulateFromTags(true);

        assertTrue(result.success);
        assertEquals(2, result.problemsProcessed);
        // p1 has two tags -> 2 assignments; p2 has one tag -> 1 assignment => 3 proposed
        assertEquals(3, result.categoriesAssigned);
        // One duplicate (1, arrays) skipped => 2 new assignments expected
        assertEquals(2, result.newAssignments);
        assertEquals(1, result.duplicatesSkipped);

        // Dry run: no inserts
        verify(problemCategoryMapper, never()).insert(any());
    }
}
