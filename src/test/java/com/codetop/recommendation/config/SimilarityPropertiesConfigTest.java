package com.codetop.recommendation.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Configuration binding test for SimilarityProperties.
 * Validates that properties are correctly bound from application.yml.
 */
@SpringBootTest(classes = {SimilarityProperties.class})
@EnableConfigurationProperties(SimilarityProperties.class)
@TestPropertySource(properties = {
    "rec.similarity.weights.tags-weight=0.4",
    "rec.similarity.weights.categories-weight=0.3", 
    "rec.similarity.weights.difficulty-weight=0.3",
    "rec.similarity.weights.diversity-weight=0.2",
    "rec.similarity.thresholds.min-similarity-score=0.3",
    "rec.similarity.thresholds.empty-feature-similarity=0.0",
    "rec.similarity.thresholds.max-similar-problems=5",
    "rec.similarity.thresholds.min-shared-categories=1",
    "rec.similarity.cache.enabled=true",
    "rec.similarity.cache.ttl-minutes=15",
    "rec.similarity.cache.max-entries=1000",
    "rec.similarity.batch.insert-batch-size=100",
    "rec.similarity.batch.batch-timeout-ms=5000",
    "rec.similarity.integration.pre-filter-enabled=true",
    "rec.similarity.integration.hybrid-scoring-enabled=false",
    "rec.similarity.integration.pre-filter-limit=30",
    "rec.similarity.integration.llm-weight=0.5",
    "rec.similarity.integration.fsrs-weight=0.3",
    "rec.similarity.integration.similarity-weight=0.2"
})
class SimilarityPropertiesConfigTest {
    
    @Autowired
    private SimilarityProperties properties;
    
    @Test
    void weights_shouldBindCorrectly() {
        SimilarityProperties.Weights weights = properties.getWeights();
        
        assertNotNull(weights, "Weights should not be null");
        assertEquals(0.4, weights.getTagsWeight(), 0.001, "Tags weight should be 0.4");
        assertEquals(0.3, weights.getCategoriesWeight(), 0.001, "Categories weight should be 0.3");
        assertEquals(0.3, weights.getDifficultyWeight(), 0.001, "Difficulty weight should be 0.3");
        assertEquals(0.2, weights.getDiversityWeight(), 0.001, "Diversity weight should be 0.2");
    }
    
    @Test
    void thresholds_shouldBindCorrectly() {
        SimilarityProperties.Thresholds thresholds = properties.getThresholds();
        
        assertNotNull(thresholds, "Thresholds should not be null");
        assertEquals(0.3, thresholds.getMinSimilarityScore(), 0.001, "Min similarity score should be 0.3");
        assertEquals(0.0, thresholds.getEmptyFeatureSimilarity(), 0.001, "Empty feature similarity should be 0.0");
        assertEquals(5, thresholds.getMaxSimilarProblems(), "Max similar problems should be 5");
        assertEquals(1, thresholds.getMinSharedCategories(), "Min shared categories should be 1");
    }
    
    @Test
    void cache_shouldBindCorrectly() {
        SimilarityProperties.Cache cache = properties.getCache();
        
        assertNotNull(cache, "Cache should not be null");
        assertTrue(cache.isEnabled(), "Cache should be enabled");
        assertEquals(15, cache.getTtlMinutes(), "Cache TTL should be 15 minutes");
        assertEquals(1000, cache.getMaxEntries(), "Cache max entries should be 1000");
    }
    
    @Test
    void batch_shouldBindCorrectly() {
        SimilarityProperties.Batch batch = properties.getBatch();
        
        assertNotNull(batch, "Batch should not be null");
        assertEquals(100, batch.getInsertBatchSize(), "Insert batch size should be 100");
        assertEquals(5000, batch.getBatchTimeoutMs(), "Batch timeout should be 5000ms");
    }
    
    @Test
    void integration_shouldBindCorrectly() {
        SimilarityProperties.Integration integration = properties.getIntegration();
        
        assertNotNull(integration, "Integration should not be null");
        assertTrue(integration.isPreFilterEnabled(), "Pre-filter should be enabled");
        assertFalse(integration.isHybridScoringEnabled(), "Hybrid scoring should be disabled");
        assertEquals(30, integration.getPreFilterLimit(), "Pre-filter limit should be 30");
        assertEquals(0.5, integration.getLlmWeight(), 0.001, "LLM weight should be 0.5");
        assertEquals(0.3, integration.getFsrsWeight(), 0.001, "FSRS weight should be 0.3");
        assertEquals(0.2, integration.getSimilarityWeight(), 0.001, "Similarity weight should be 0.2");
    }
    
    @Test
    void validation_shouldWork() {
        // Test that validation annotations work
        SimilarityProperties.Weights weights = properties.getWeights();
        
        // All weights should be in valid range [0.0, 1.0]
        assertTrue(weights.getTagsWeight() >= 0.0 && weights.getTagsWeight() <= 1.0, 
                "Tags weight should be in [0.0, 1.0] range");
        assertTrue(weights.getCategoriesWeight() >= 0.0 && weights.getCategoriesWeight() <= 1.0,
                "Categories weight should be in [0.0, 1.0] range");
        assertTrue(weights.getDifficultyWeight() >= 0.0 && weights.getDifficultyWeight() <= 1.0,
                "Difficulty weight should be in [0.0, 1.0] range");
        
        // Cache TTL should be positive
        assertTrue(properties.getCache().getTtlMinutes() > 0, "Cache TTL should be positive");
        
        // Batch size should be reasonable
        assertTrue(properties.getBatch().getInsertBatchSize() >= 1 && properties.getBatch().getInsertBatchSize() <= 1000,
                "Batch size should be in reasonable range");
    }
    
    @Test
    void defaultValues_shouldBeReasonable() {
        // Create a new instance to test default values
        SimilarityProperties defaultProps = new SimilarityProperties();
        
        assertNotNull(defaultProps.getWeights(), "Default weights should not be null");
        assertNotNull(defaultProps.getThresholds(), "Default thresholds should not be null");
        assertNotNull(defaultProps.getCache(), "Default cache should not be null");
        assertNotNull(defaultProps.getBatch(), "Default batch should not be null");
        assertNotNull(defaultProps.getIntegration(), "Default integration should not be null");
        
        // Verify some key defaults
        assertEquals(0.4, defaultProps.getWeights().getTagsWeight(), 0.001, "Default tags weight");
        assertEquals(15, defaultProps.getCache().getTtlMinutes(), "Default cache TTL");
        assertEquals(100, defaultProps.getBatch().getInsertBatchSize(), "Default batch size");
    }
}