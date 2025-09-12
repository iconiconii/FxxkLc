package com.codetop.recommendation.alg;

import com.codetop.recommendation.config.SimilarityProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class SimilarityScorerTest {
    
    @Mock
    private SimilarityProperties mockConfig;
    
    @Mock 
    private SimilarityProperties.Weights mockWeights;
    
    @Mock
    private SimilarityProperties.Thresholds mockThresholds;
    
    private SimilarityScorer similarityScorer;
    
    @BeforeEach
    void setUp() {
        // Use lenient mode to avoid unnecessary stubbing errors
        lenient().when(mockConfig.getWeights()).thenReturn(mockWeights);
        lenient().when(mockConfig.getThresholds()).thenReturn(mockThresholds);
        
        // Setup default weights
        lenient().when(mockWeights.getTagsWeight()).thenReturn(0.4);
        lenient().when(mockWeights.getCategoriesWeight()).thenReturn(0.3);
        lenient().when(mockWeights.getDifficultyWeight()).thenReturn(0.3);
        
        // Setup default thresholds
        lenient().when(mockThresholds.getEmptyFeatureSimilarity()).thenReturn(0.0);
        
        similarityScorer = new SimilarityScorer(mockConfig, new SimpleMeterRegistry());
    }
    
    @Test
    void calculateSimilarity_identicalProblems_returnsOne() {
        // Given
        List<String> tags = Arrays.asList("array", "two-pointers");
        List<Long> categories = Arrays.asList(1L, 2L);
        String difficulty = "MEDIUM";
        
        // When
        double similarity = similarityScorer.calculateSimilarity(
                tags, tags, categories, categories, difficulty, difficulty);
        
        // Then
        assertEquals(1.0, similarity, 0.001, "Identical problems should have similarity of 1.0");
    }
    
    @Test
    void calculateSimilarity_emptyTagsAndCategories_returnsConfiguredValue() {
        // Given
        List<String> emptyTags = Collections.emptyList();
        List<Long> emptyCategories = Collections.emptyList();
        String difficulty = "EASY";
        
        // When
        double similarity = similarityScorer.calculateSimilarity(
                emptyTags, emptyTags, emptyCategories, emptyCategories, difficulty, difficulty);
        
        // Then - should use configured empty feature similarity (0.0) for both tags and categories
        // Formula: 0.4*0.0 + 0.3*0.0 + 0.3*1.0 = 0.3 (only difficulty matches)
        assertEquals(0.3, similarity, 0.001, "Empty features should use configured empty similarity");
    }
    
    @Test
    void calculateSimilarity_noCommonElements_returnsLowSimilarity() {
        // Given
        List<String> tags1 = Arrays.asList("array", "hash-table");
        List<String> tags2 = Arrays.asList("tree", "dfs");
        List<Long> categories1 = Arrays.asList(1L, 2L);
        List<Long> categories2 = Arrays.asList(3L, 4L);
        String difficulty1 = "EASY";
        String difficulty2 = "HARD";
        
        // When
        double similarity = similarityScorer.calculateSimilarity(
                tags1, tags2, categories1, categories2, difficulty1, difficulty2);
        
        // Then - no jaccard overlap (0.0), no category overlap (0.0), difficulty 2 levels apart (0.0)
        // Formula: 0.4*0.0 + 0.3*0.0 + 0.3*0.0 = 0.0
        assertEquals(0.0, similarity, 0.001, "Problems with no common elements should have low similarity");
    }
    
    @Test
    void calculateSimilarity_partialOverlap_returnsPartialSimilarity() {
        // Given
        List<String> tags1 = Arrays.asList("array", "two-pointers", "hash-table");
        List<String> tags2 = Arrays.asList("array", "sliding-window"); // 1 common out of 4 unique = 0.25
        List<Long> categories1 = Arrays.asList(1L, 2L, 3L);
        List<Long> categories2 = Arrays.asList(2L, 4L); // 1 common, max(3,2)=3, so 1/3 = 0.333
        String difficulty1 = "MEDIUM";
        String difficulty2 = "MEDIUM"; // Same = 1.0
        
        // When
        double similarity = similarityScorer.calculateSimilarity(
                tags1, tags2, categories1, categories2, difficulty1, difficulty2);
        
        // Then
        // Formula: 0.4*0.25 + 0.3*0.333 + 0.3*1.0 = 0.1 + 0.1 + 0.3 = 0.5
        assertEquals(0.5, similarity, 0.001, "Partial overlap should return expected similarity");
    }
    
    @Test
    void calculateSimilarity_adjacentDifficulty_returnsHalfDifficultyScore() {
        // Given
        List<String> sameTags = Arrays.asList("array");
        List<Long> sameCategories = Arrays.asList(1L);
        String difficulty1 = "EASY";
        String difficulty2 = "MEDIUM"; // Adjacent = 0.5
        
        // When
        double similarity = similarityScorer.calculateSimilarity(
                sameTags, sameTags, sameCategories, sameCategories, difficulty1, difficulty2);
        
        // Then
        // Formula: 0.4*1.0 + 0.3*1.0 + 0.3*0.5 = 0.4 + 0.3 + 0.15 = 0.85
        assertEquals(0.85, similarity, 0.001, "Adjacent difficulty should have 0.5 difficulty score");
    }
    
    @Test
    void calculateSimilarity_caseInsensitiveTags_treatedAsSame() {
        // Given
        List<String> tags1 = Arrays.asList("Array", "HASH-TABLE");
        List<String> tags2 = Arrays.asList("array", "hash-table");
        List<Long> categories = Arrays.asList(1L);
        String difficulty = "EASY";
        
        // When
        double similarity = similarityScorer.calculateSimilarity(
                tags1, tags2, categories, categories, difficulty, difficulty);
        
        // Then - tags should be normalized and treated as identical
        assertEquals(1.0, similarity, 0.001, "Case-insensitive tags should be treated as identical");
    }
    
    @Test
    void calculateSimilarity_nullInputs_handlesGracefully() {
        // Given
        List<Long> categories = Arrays.asList(1L);
        String difficulty = "MEDIUM";
        
        // When
        double similarity = similarityScorer.calculateSimilarity(
                null, null, categories, categories, difficulty, difficulty);
        
        // Then - null tags should be treated as empty, using configured empty similarity
        // Formula: 0.4*0.0 + 0.3*1.0 + 0.3*1.0 = 0.0 + 0.3 + 0.3 = 0.6
        assertEquals(0.6, similarity, 0.001, "Null inputs should be handled gracefully");
    }
    
    @Test
    void calculateSimilarity_customWeights_appliesCorrectly() {
        // Given
        List<String> tags1 = Arrays.asList("array");
        List<String> tags2 = Arrays.asList("array");
        List<Long> categories1 = Arrays.asList(1L);
        List<Long> categories2 = Arrays.asList(1L);
        String difficulty = "EASY";
        
        double customTagsWeight = 0.8;
        double customCategoriesWeight = 0.1;
        double customDifficultyWeight = 0.1;
        
        // When
        double similarity = similarityScorer.calculateSimilarity(
                tags1, tags2, categories1, categories2, difficulty, difficulty,
                customTagsWeight, customCategoriesWeight, customDifficultyWeight);
        
        // Then - identical problems with custom weights
        // Formula: 0.8*1.0 + 0.1*1.0 + 0.1*1.0 = 0.8 + 0.1 + 0.1 = 1.0
        assertEquals(1.0, similarity, 0.001, "Custom weights should apply correctly");
    }
    
    @Test
    void calculateSimilarities_batchCalculation_returnsCorrectResults() {
        // Given
        List<String> targetTags = Arrays.asList("array", "two-pointers");
        List<Long> targetCategories = Arrays.asList(1L, 2L);
        String targetDifficulty = "MEDIUM";
        
        List<List<String>> candidateTags = Arrays.asList(
                Arrays.asList("array", "two-pointers"), // Identical = 1.0
                Arrays.asList("array"), // Partial = less than 1.0
                Collections.emptyList() // Empty = low similarity
        );
        
        List<List<Long>> candidateCategories = Arrays.asList(
                Arrays.asList(1L, 2L), // Identical
                Arrays.asList(1L), // Partial
                Collections.emptyList() // Empty
        );
        
        List<String> candidateDifficulties = Arrays.asList("MEDIUM", "MEDIUM", "MEDIUM");
        
        // When
        List<Double> similarities = similarityScorer.calculateSimilarities(
                targetTags, targetCategories, targetDifficulty,
                candidateTags, candidateCategories, candidateDifficulties);
        
        // Then
        assertEquals(3, similarities.size(), "Should return same number of similarities as candidates");
        assertEquals(1.0, similarities.get(0), 0.001, "First candidate should be identical");
        assertTrue(similarities.get(1) > similarities.get(2), "Second candidate should have higher similarity than third");
        assertTrue(similarities.get(2) >= 0.0, "All similarities should be non-negative");
    }
    
    @Test
    void calculateSimilarities_mismatchedInputSizes_throwsException() {
        // Given
        List<String> targetTags = Arrays.asList("array");
        List<Long> targetCategories = Arrays.asList(1L);
        String targetDifficulty = "EASY";
        
        List<List<String>> candidateTags = Arrays.asList(Arrays.asList("array"));
        List<List<Long>> candidateCategories = Arrays.asList(Arrays.asList(1L), Arrays.asList(2L)); // Different size
        List<String> candidateDifficulties = Arrays.asList("EASY");
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
                similarityScorer.calculateSimilarities(
                        targetTags, targetCategories, targetDifficulty,
                        candidateTags, candidateCategories, candidateDifficulties));
    }
}
