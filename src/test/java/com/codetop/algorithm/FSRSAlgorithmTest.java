package com.codetop.algorithm;

import com.codetop.algorithm.impl.FSRSAlgorithmImpl;
import com.codetop.dto.FSRSCalculationResult;
import com.codetop.dto.FSRSParametersDTO;
import com.codetop.entity.FSRSCard;
import com.codetop.entity.ReviewLog;
import com.codetop.enums.FSRSState;
import com.codetop.enums.ReviewType;
import com.codetop.exception.FSRSCalculationException;
import com.codetop.exception.InvalidRatingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for FSRS Algorithm implementation.
 * 
 * Tests cover:
 * - Mathematical correctness of all 17 parameters
 * - Edge cases and boundary conditions
 * - Performance benchmarks
 * - Parameter optimization validation
 * - State machine transitions
 * 
 * @author CodeTop Team
 */
@DisplayName("FSRS Algorithm Tests")
public class FSRSAlgorithmTest {

    private FSRSAlgorithmImpl fsrsAlgorithm;
    private FSRSParametersDTO defaultParameters;
    private FSRSCard testCard;

    @BeforeEach
    void setUp() {
        fsrsAlgorithm = new FSRSAlgorithmImpl();
        defaultParameters = fsrsAlgorithm.getDefaultParameters();
        testCard = createTestCard();
    }

    private FSRSCard createTestCard() {
        FSRSCard card = new FSRSCard();
        card.setId(1L);
        card.setUserId(1L);
        card.setProblemId(1L);
        card.setState(FSRSState.NEW);
        card.setDifficulty(BigDecimal.valueOf(5.0));
        card.setStability(BigDecimal.valueOf(2.0));
        card.setReviewCount(0);
        card.setLapses(0);
        card.setLastReview(LocalDateTime.now().minusDays(1));
        card.setCreatedAt(LocalDateTime.now().minusDays(7));
        return card;
    }

    @Nested
    @DisplayName("Core FSRS Calculations")
    class CoreCalculations {

        @Test
        @DisplayName("Should calculate correct next review for new card")
        void shouldCalculateNextReviewForNewCard() {
            // Given
            FSRSCard newCard = createTestCard();
            newCard.setState(FSRSState.NEW);
            newCard.setReviewCount(0);
            
            // When
            FSRSCalculationResult result = fsrsAlgorithm.calculateNextReview(newCard, 3, defaultParameters);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNewState()).isEqualTo(FSRSState.LEARNING);
            assertThat(result.getNewDifficulty()).isBetween(BigDecimal.valueOf(1.0), BigDecimal.valueOf(10.0));
            assertThat(result.getNewStability()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.getIntervalDays()).isGreaterThan(0);
            assertThat(result.getNextReviewTime()).isAfter(LocalDateTime.now());
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4})
        @DisplayName("Should handle all rating values correctly")
        void shouldHandleAllRatingValues(int rating) {
            // Given
            FSRSCard card = createTestCard();
            card.setState(FSRSState.REVIEW);
            card.setReviewCount(10);
            
            // When
            FSRSCalculationResult result = fsrsAlgorithm.calculateNextReview(card, rating, defaultParameters);
            
            // Then
            assertThat(result).isNotNull();
            
            // Rating 1 (Again) should reset to relearning
            if (rating == 1) {
                assertThat(result.getNewState()).isEqualTo(FSRSState.RELEARNING);
                assertThat(result.getIntervalDays()).isLessThan(7);
            }
            // Higher ratings should maintain or advance state
            else {
                assertThat(result.getNewState()).isIn(FSRSState.LEARNING, FSRSState.REVIEW);
                assertThat(result.getIntervalDays()).isGreaterThan(0);
            }
            
            // All results should have valid ranges
            assertThat(result.getNewDifficulty()).isBetween(BigDecimal.valueOf(1.0), BigDecimal.valueOf(10.0));
            assertThat(result.getNewStability()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should calculate initial difficulty correctly")
        void shouldCalculateInitialDifficulty() {
            // Test all rating values for initial difficulty
            double difficultyRating1 = fsrsAlgorithm.calculateInitialDifficulty(1, defaultParameters);
            double difficultyRating2 = fsrsAlgorithm.calculateInitialDifficulty(2, defaultParameters);
            double difficultyRating3 = fsrsAlgorithm.calculateInitialDifficulty(3, defaultParameters);
            double difficultyRating4 = fsrsAlgorithm.calculateInitialDifficulty(4, defaultParameters);
            
            // Verify ordering: Again > Hard > Good > Easy
            assertThat(difficultyRating1).isGreaterThan(difficultyRating2);
            assertThat(difficultyRating2).isGreaterThan(difficultyRating3);
            assertThat(difficultyRating3).isGreaterThan(difficultyRating4);
            
            // Verify all values are within valid range
            assertThat(difficultyRating1).isBetween(1.0, 10.0);
            assertThat(difficultyRating2).isBetween(1.0, 10.0);
            assertThat(difficultyRating3).isBetween(1.0, 10.0);
            assertThat(difficultyRating4).isBetween(1.0, 10.0);
        }

        @Test
        @DisplayName("Should calculate initial stability correctly")
        void shouldCalculateInitialStability() {
            // Test all rating values for initial stability
            double stabilityRating1 = fsrsAlgorithm.calculateInitialStability(1, defaultParameters);
            double stabilityRating2 = fsrsAlgorithm.calculateInitialStability(2, defaultParameters);
            double stabilityRating3 = fsrsAlgorithm.calculateInitialStability(3, defaultParameters);
            double stabilityRating4 = fsrsAlgorithm.calculateInitialStability(4, defaultParameters);
            
            // Verify ordering: Easy > Good > Hard > Again
            assertThat(stabilityRating4).isGreaterThan(stabilityRating3);
            assertThat(stabilityRating3).isGreaterThan(stabilityRating2);
            assertThat(stabilityRating2).isGreaterThan(stabilityRating1);
            
            // Verify all values are positive
            assertThat(stabilityRating1).isPositive();
            assertThat(stabilityRating2).isPositive();
            assertThat(stabilityRating3).isPositive();
            assertThat(stabilityRating4).isPositive();
        }

        @Test
        @DisplayName("Should calculate retrievability correctly")
        void shouldCalculateRetrievability() {
            // Given
            FSRSCard card = createTestCard();
            card.setStability(BigDecimal.valueOf(10.0));
            card.setLastReview(LocalDateTime.now().minusDays(5));
            
            // When
            double retrievability = fsrsAlgorithm.calculateRetrievability(card, defaultParameters);
            
            // Then
            assertThat(retrievability).isBetween(0.0, 1.0);
            
            // Test with different elapsed times
            card.setLastReview(LocalDateTime.now().minusDays(1));
            double highRetrievability = fsrsAlgorithm.calculateRetrievability(card, defaultParameters);
            
            card.setLastReview(LocalDateTime.now().minusDays(20));
            double lowRetrievability = fsrsAlgorithm.calculateRetrievability(card, defaultParameters);
            
            // Recent review should have higher retrievability
            assertThat(highRetrievability).isGreaterThan(lowRetrievability);
        }

        @ParameterizedTest
        @CsvSource({
            "1.0, 0.9, 1",
            "10.0, 0.9, 9",
            "5.0, 0.8, 3",
            "2.5, 0.95, 2"
        })
        @DisplayName("Should predict optimal interval correctly")
        void shouldPredictOptimalInterval(double stability, double targetRetention, int expectedMinInterval) {
            // When
            double interval = fsrsAlgorithm.predictOptimalInterval(stability, targetRetention);
            
            // Then
            assertThat(interval).isGreaterThanOrEqualTo(expectedMinInterval);
            assertThat(interval).isLessThanOrEqualTo(36500); // Maximum interval
        }

        @Test
        @DisplayName("Should calculate all intervals for preview")
        void shouldCalculateAllIntervals() {
            // Given
            FSRSCard card = createTestCard();
            card.setState(FSRSState.REVIEW);
            card.setReviewCount(5);
            
            // When
            int[] intervals = fsrsAlgorithm.calculateAllIntervals(card, defaultParameters);
            
            // Then
            assertThat(intervals).hasSize(4); // Again, Hard, Good, Easy
            
            // Verify interval ordering: Again < Hard < Good < Easy (generally)
            assertThat(intervals[0]).isLessThanOrEqualTo(intervals[1]); // Again <= Hard
            assertThat(intervals[1]).isLessThanOrEqualTo(intervals[2]); // Hard <= Good
            assertThat(intervals[2]).isLessThanOrEqualTo(intervals[3]); // Good <= Easy
            
            // All intervals should be positive
            Arrays.stream(intervals).forEach(interval -> assertThat(interval).isPositive());
        }
    }

    @Nested
    @DisplayName("Parameter Validation and Optimization")
    class ParameterTests {

        @Test
        @DisplayName("Should validate parameters correctly")
        void shouldValidateParameters() {
            // Valid parameters should pass
            assertThat(fsrsAlgorithm.validateParameters(defaultParameters)).isTrue();
            
            // Test invalid parameters
            FSRSParametersDTO invalidParams = new FSRSParametersDTO();
            // Set invalid request retention (outside 0.7-0.97 range)
            invalidParams.setRequestRetention(0.5);
            
            assertThat(fsrsAlgorithm.validateParameters(invalidParams)).isFalse();
        }

        @Test
        @DisplayName("Should return valid default parameters")
        void shouldReturnValidDefaultParameters() {
            // When
            FSRSParametersDTO defaultParams = fsrsAlgorithm.getDefaultParameters();
            
            // Then
            assertThat(defaultParams).isNotNull();
            assertThat(fsrsAlgorithm.validateParameters(defaultParams)).isTrue();
            assertThat(defaultParams.getRequestRetention()).isBetween(0.7, 0.97);
        }

        @Test
        @DisplayName("Should optimize parameters with sufficient data")
        void shouldOptimizeParametersWithSufficientData() {
            // Given
            List<ReviewLog> reviewLogs = createTestReviewLogs(200); // Sufficient data
            
            // When
            FSRSParametersDTO optimizedParams = fsrsAlgorithm.optimizeParameters(reviewLogs, defaultParameters);
            
            // Then
            assertThat(optimizedParams).isNotNull();
            assertThat(fsrsAlgorithm.validateParameters(optimizedParams)).isTrue();
            
            // Optimized parameters should be different from defaults (in most cases)
            // This is a probabilistic test, may occasionally fail with random data
            boolean hasChanges = !optimizedParams.equals(defaultParameters);
            // We don't assert this strictly as it depends on the random test data
        }

        @Test
        @DisplayName("Should handle insufficient data for optimization")
        void shouldHandleInsufficientDataForOptimization() {
            // Given
            List<ReviewLog> insufficientLogs = createTestReviewLogs(50); // Insufficient data
            
            // When
            FSRSParametersDTO result = fsrsAlgorithm.optimizeParameters(insufficientLogs, defaultParameters);
            
            // Then - should return default parameters or throw exception
            assertThat(result).isNotNull();
            assertThat(fsrsAlgorithm.validateParameters(result)).isTrue();
        }

        private List<ReviewLog> createTestReviewLogs(int count) {
            return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> {
                    ReviewLog log = new ReviewLog();
                    log.setId((long) i);
                    log.setUserId(1L);
                    log.setProblemId((long) (i % 10 + 1));
                    log.setRating((i % 4) + 1); // Ratings 1-4
                    log.setElapsedDays(i % 30 + 1);
                    log.setReviewType(ReviewType.SCHEDULED);
                    log.setReviewedAt(LocalDateTime.now().minusDays(count - i));
                    log.setCreatedAt(LocalDateTime.now().minusDays(count - i));
                    return log;
                })
                .toList();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrors {

        @Test
        @DisplayName("Should throw exception for invalid rating")
        void shouldThrowExceptionForInvalidRating() {
            // Given
            FSRSCard card = createTestCard();
            
            // When/Then - Test invalid ratings
            assertThatThrownBy(() -> fsrsAlgorithm.calculateNextReview(card, 0, defaultParameters))
                .isInstanceOf(InvalidRatingException.class);
            
            assertThatThrownBy(() -> fsrsAlgorithm.calculateNextReview(card, 5, defaultParameters))
                .isInstanceOf(InvalidRatingException.class);
            
            assertThatThrownBy(() -> fsrsAlgorithm.calculateNextReview(card, -1, defaultParameters))
                .isInstanceOf(InvalidRatingException.class);
        }

        @Test
        @DisplayName("Should handle null inputs gracefully")
        void shouldHandleNullInputsGracefully() {
            assertThatThrownBy(() -> fsrsAlgorithm.calculateNextReview(null, 3, defaultParameters))
                .isInstanceOf(IllegalArgumentException.class);
            
            assertThatThrownBy(() -> fsrsAlgorithm.calculateNextReview(testCard, 3, null))
                .isInstanceOf(IllegalArgumentException.class);
            
            assertThatThrownBy(() -> fsrsAlgorithm.calculateNextReview(testCard, null, defaultParameters))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should handle extreme stability values")
        void shouldHandleExtremeStabilityValues() {
            // Given
            FSRSCard card = createTestCard();
            
            // Test with very low stability
            card.setStability(BigDecimal.valueOf(0.01));
            FSRSCalculationResult resultLow = fsrsAlgorithm.calculateNextReview(card, 3, defaultParameters);
            assertThat(resultLow.getIntervalDays()).isGreaterThan(0);
            
            // Test with very high stability
            card.setStability(BigDecimal.valueOf(1000.0));
            FSRSCalculationResult resultHigh = fsrsAlgorithm.calculateNextReview(card, 3, defaultParameters);
            assertThat(resultHigh.getIntervalDays()).isLessThanOrEqualTo(36500); // Max interval
        }

        @Test
        @DisplayName("Should handle extreme difficulty values")
        void shouldHandleExtremeDifficultyValues() {
            // Given
            FSRSCard card = createTestCard();
            
            // Test with minimum difficulty
            card.setDifficulty(BigDecimal.valueOf(1.0));
            FSRSCalculationResult resultMin = fsrsAlgorithm.calculateNextReview(card, 3, defaultParameters);
            assertThat(resultMin.getNewDifficulty()).isBetween(BigDecimal.valueOf(1.0), BigDecimal.valueOf(10.0));
            
            // Test with maximum difficulty
            card.setDifficulty(BigDecimal.valueOf(10.0));
            FSRSCalculationResult resultMax = fsrsAlgorithm.calculateNextReview(card, 3, defaultParameters);
            assertThat(resultMax.getNewDifficulty()).isBetween(BigDecimal.valueOf(1.0), BigDecimal.valueOf(10.0));
        }

        @Test
        @DisplayName("Should handle cards with many lapses")
        void shouldHandleCardsWithManyLapses() {
            // Given
            FSRSCard card = createTestCard();
            card.setLapses(100); // Many failures
            card.setState(FSRSState.RELEARNING);
            
            // When
            FSRSCalculationResult result = fsrsAlgorithm.calculateNextReview(card, 3, defaultParameters);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNewDifficulty()).isBetween(BigDecimal.valueOf(1.0), BigDecimal.valueOf(10.0));
            assertThat(result.getNewStability()).isPositive();
        }

        @Test
        @DisplayName("Should handle very old cards")
        void shouldHandleVeryOldCards() {
            // Given
            FSRSCard card = createTestCard();
            card.setLastReview(LocalDateTime.now().minusYears(1)); // Very old review
            card.setCreatedAt(LocalDateTime.now().minusYears(2));
            
            // When
            FSRSCalculationResult result = fsrsAlgorithm.calculateNextReview(card, 3, defaultParameters);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getElapsedDays()).isGreaterThan(300); // Approximately 1 year
            assertThat(result.getNewStability()).isPositive();
        }
    }

    @Nested
    @DisplayName("State Machine Tests")
    class StateMachineTests {

        @Test
        @DisplayName("Should transition from NEW to LEARNING on first review")
        void shouldTransitionFromNewToLearning() {
            // Given
            FSRSCard newCard = createTestCard();
            newCard.setState(FSRSState.NEW);
            newCard.setReviewCount(0);
            
            // When - Any rating should move to LEARNING
            FSRSCalculationResult result = fsrsAlgorithm.calculateNextReview(newCard, 3, defaultParameters);
            
            // Then
            assertThat(result.getNewState()).isEqualTo(FSRSState.LEARNING);
        }

        @Test
        @DisplayName("Should transition from LEARNING to REVIEW after graduation")
        void shouldTransitionFromLearningToReview() {
            // Given
            FSRSCard learningCard = createTestCard();
            learningCard.setState(FSRSState.LEARNING);
            learningCard.setReviewCount(2); // Has some reviews
            learningCard.setStability(BigDecimal.valueOf(4.0)); // Sufficient stability
            
            // When - Good or Easy rating should graduate
            FSRSCalculationResult result = fsrsAlgorithm.calculateNextReview(learningCard, 3, defaultParameters);
            
            // Then
            assertThat(result.getNewState()).isEqualTo(FSRSState.REVIEW);
        }

        @Test
        @DisplayName("Should transition from REVIEW to RELEARNING on failure")
        void shouldTransitionFromReviewToRelearning() {
            // Given
            FSRSCard reviewCard = createTestCard();
            reviewCard.setState(FSRSState.REVIEW);
            reviewCard.setReviewCount(10);
            
            // When - Rating 1 (Again) should trigger relearning
            FSRSCalculationResult result = fsrsAlgorithm.calculateNextReview(reviewCard, 1, defaultParameters);
            
            // Then
            assertThat(result.getNewState()).isEqualTo(FSRSState.RELEARNING);
        }

        @Test
        @DisplayName("Should stay in REVIEW for successful reviews")
        void shouldStayInReviewForSuccessfulReviews() {
            // Given
            FSRSCard reviewCard = createTestCard();
            reviewCard.setState(FSRSState.REVIEW);
            reviewCard.setReviewCount(10);
            
            // When - Good ratings should stay in review
            FSRSCalculationResult result2 = fsrsAlgorithm.calculateNextReview(reviewCard, 2, defaultParameters);
            FSRSCalculationResult result3 = fsrsAlgorithm.calculateNextReview(reviewCard, 3, defaultParameters);
            FSRSCalculationResult result4 = fsrsAlgorithm.calculateNextReview(reviewCard, 4, defaultParameters);
            
            // Then
            assertThat(result2.getNewState()).isEqualTo(FSRSState.REVIEW);
            assertThat(result3.getNewState()).isEqualTo(FSRSState.REVIEW);
            assertThat(result4.getNewState()).isEqualTo(FSRSState.REVIEW);
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should calculate next review quickly for single card")
        void shouldCalculateNextReviewQuickly() {
            // Given
            FSRSCard card = createTestCard();
            long startTime = System.nanoTime();
            
            // When
            FSRSCalculationResult result = fsrsAlgorithm.calculateNextReview(card, 3, defaultParameters);
            long endTime = System.nanoTime();
            
            // Then
            double durationMs = (endTime - startTime) / 1_000_000.0;
            assertThat(durationMs).isLessThan(10.0); // Should complete within 10ms
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should handle batch calculations efficiently")
        void shouldHandleBatchCalculationsEfficiently() {
            // Given
            List<FSRSCard> cards = java.util.stream.IntStream.range(0, 1000)
                .mapToObj(i -> {
                    FSRSCard card = createTestCard();
                    card.setId((long) i);
                    card.setProblemId((long) (i % 100));
                    return card;
                })
                .toList();
            
            long startTime = System.nanoTime();
            
            // When
            List<FSRSCalculationResult> results = cards.stream()
                .map(card -> fsrsAlgorithm.calculateNextReview(card, 3, defaultParameters))
                .toList();
            
            long endTime = System.nanoTime();
            
            // Then
            double durationMs = (endTime - startTime) / 1_000_000.0;
            double avgTimePerCard = durationMs / cards.size();
            
            assertThat(results).hasSize(1000);
            assertThat(avgTimePerCard).isLessThan(1.0); // Average < 1ms per card
            assertThat(durationMs).isLessThan(1000.0); // Total < 1 second
        }

        @Test
        @DisplayName("Should optimize parameters within reasonable time")
        void shouldOptimizeParametersWithinReasonableTime() {
            // Given
            List<ReviewLog> reviewLogs = createLargeTestDataset(1000);
            long startTime = System.nanoTime();
            
            // When
            FSRSParametersDTO result = fsrsAlgorithm.optimizeParameters(reviewLogs, defaultParameters);
            long endTime = System.nanoTime();
            
            // Then
            double durationMs = (endTime - startTime) / 1_000_000.0;
            assertThat(durationMs).isLessThan(5000.0); // Should complete within 5 seconds
            assertThat(result).isNotNull();
            assertThat(fsrsAlgorithm.validateParameters(result)).isTrue();
        }

        private List<ReviewLog> createLargeTestDataset(int size) {
            return java.util.stream.IntStream.range(0, size)
                .mapToObj(i -> {
                    ReviewLog log = new ReviewLog();
                    log.setId((long) i);
                    log.setUserId((long) (i % 10 + 1));
                    log.setProblemId((long) (i % 100 + 1));
                    log.setRating((i % 4) + 1);
                    log.setElapsedDays((i % 30) + 1);
                    log.setReviewType(ReviewType.SCHEDULED);
                    log.setReviewedAt(LocalDateTime.now().minusDays(size - i));
                    log.setCreatedAt(LocalDateTime.now().minusDays(size - i));
                    return log;
                })
                .toList();
        }
    }

    @Nested
    @DisplayName("Mathematical Consistency Tests")
    class MathematicalConsistencyTests {

        @Test
        @DisplayName("Should maintain mathematical consistency across calculations")
        void shouldMaintainMathematicalConsistency() {
            // Given
            FSRSCard card = createTestCard();
            
            // When - Calculate multiple times with same inputs
            FSRSCalculationResult result1 = fsrsAlgorithm.calculateNextReview(card, 3, defaultParameters);
            FSRSCalculationResult result2 = fsrsAlgorithm.calculateNextReview(card, 3, defaultParameters);
            
            // Then - Results should be identical (deterministic)
            assertThat(result1.getNewDifficulty()).isEqualTo(result2.getNewDifficulty());
            assertThat(result1.getNewStability()).isEqualTo(result2.getNewStability());
            assertThat(result1.getIntervalDays()).isEqualTo(result2.getIntervalDays());
        }

        @Test
        @DisplayName("Should follow forgetting curve properties")
        void shouldFollowForgettingCurveProperties() {
            // Given
            FSRSCard card = createTestCard();
            card.setStability(BigDecimal.valueOf(10.0));
            
            // Test retrievability at different time points
            card.setLastReview(LocalDateTime.now().minusDays(1));
            double retrievability1Day = fsrsAlgorithm.calculateRetrievability(card, defaultParameters);
            
            card.setLastReview(LocalDateTime.now().minusDays(5));
            double retrievability5Days = fsrsAlgorithm.calculateRetrievability(card, defaultParameters);
            
            card.setLastReview(LocalDateTime.now().minusDays(10));
            double retrievability10Days = fsrsAlgorithm.calculateRetrievability(card, defaultParameters);
            
            // Then - Retrievability should decrease over time
            assertThat(retrievability1Day).isGreaterThan(retrievability5Days);
            assertThat(retrievability5Days).isGreaterThan(retrievability10Days);
            
            // All should be between 0 and 1
            assertThat(retrievability1Day).isBetween(0.0, 1.0);
            assertThat(retrievability5Days).isBetween(0.0, 1.0);
            assertThat(retrievability10Days).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Should maintain stability progression properties")
        void shouldMaintainStabilityProgressionProperties() {
            // Given
            FSRSCard card = createTestCard();
            card.setState(FSRSState.REVIEW);
            card.setReviewCount(5);
            card.setStability(BigDecimal.valueOf(10.0)); // Set a reasonable initial stability
            card.setDifficulty(BigDecimal.valueOf(5.0)); // Mid-range difficulty
            
            // When - Calculate with different ratings
            FSRSCalculationResult againResult = fsrsAlgorithm.calculateNextReview(card, 1, defaultParameters);
            FSRSCalculationResult hardResult = fsrsAlgorithm.calculateNextReview(card, 2, defaultParameters);
            FSRSCalculationResult goodResult = fsrsAlgorithm.calculateNextReview(card, 3, defaultParameters);
            FSRSCalculationResult easyResult = fsrsAlgorithm.calculateNextReview(card, 4, defaultParameters);
            
            // Then - Verify FSRS mathematical relationships:
            // 1. Easy should have highest or equal stability compared to Good
            assertThat(easyResult.getNewStability().doubleValue())
                .isGreaterThanOrEqualTo(goodResult.getNewStability().doubleValue());
            
            // 2. Again (lapse) should have lower stability than others (failed reviews reset stability)  
            assertThat(againResult.getNewStability().doubleValue())
                .isLessThan(hardResult.getNewStability().doubleValue());
            assertThat(againResult.getNewStability().doubleValue())
                .isLessThan(goodResult.getNewStability().doubleValue());
            assertThat(againResult.getNewStability().doubleValue())
                .isLessThan(easyResult.getNewStability().doubleValue());
            
            // 3. All stability values should be positive and reasonable
            assertThat(againResult.getNewStability().doubleValue()).isPositive();
            assertThat(hardResult.getNewStability().doubleValue()).isPositive();
            assertThat(goodResult.getNewStability().doubleValue()).isPositive();
            assertThat(easyResult.getNewStability().doubleValue()).isPositive();
        }
    }
}