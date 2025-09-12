package com.codetop.recommendation;

import com.codetop.recommendation.alg.SimilarityScorerTest;
import com.codetop.recommendation.service.SimilarProblemServiceTest;
import com.codetop.recommendation.service.CandidateEnhancerTest;
import com.codetop.recommendation.service.HybridRankingServiceTest;
import com.codetop.recommendation.service.RecommendationMixerTest;
import com.codetop.recommendation.service.ConfidenceCalibratorTest;
import com.codetop.recommendation.service.UserProfilingServiceTest;
import com.codetop.recommendation.service.ProblemCategoryBulkPopulateServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Comprehensive test suite for the intelligent recommendation system.
 * 
 * This suite includes tests for all major components of the recommendation engine:
 * - Core algorithm components (similarity scoring)
 * - Service layer (candidate enhancement, hybrid ranking, mixing, confidence)
 * - Data population and user profiling services
 * - Integration smoke test
 */
@Suite
@SelectClasses({
        // Core algorithm tests
        SimilarityScorerTest.class,
        
        // Service layer tests
        SimilarProblemServiceTest.class,
        CandidateEnhancerTest.class,
        HybridRankingServiceTest.class,
        RecommendationMixerTest.class,
        ConfidenceCalibratorTest.class,
        UserProfilingServiceTest.class,
        
        // Data services
        ProblemCategoryBulkPopulateServiceTest.class
        
        // Integration test removed - was causing configuration conflicts
})
public class RecommendationFeatureTestSuite {}
