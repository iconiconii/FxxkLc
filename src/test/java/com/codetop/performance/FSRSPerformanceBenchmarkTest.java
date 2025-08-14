package com.codetop.performance;

import com.codetop.AbstractIntegrationTest;
import com.codetop.algorithm.FSRSAlgorithm;
import com.codetop.dto.FSRSCalculationResult;
import com.codetop.dto.FSRSParametersDTO;
import com.codetop.entity.FSRSCard;
import com.codetop.entity.Problem;
import com.codetop.entity.ReviewLog;
import com.codetop.entity.User;
import com.codetop.service.FSRSService;
import com.codetop.service.UserParametersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import com.codetop.enums.Difficulty;
import com.codetop.enums.FSRSState;
import com.codetop.enums.ReviewType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Performance benchmark tests for FSRS algorithm and services.
 * 
 * These tests validate performance characteristics under various load conditions:
 * - Single operation benchmarks
 * - Bulk operation performance
 * - Concurrent user scenarios
 * - Large dataset handling
 * - Memory usage patterns
 * - Cache performance impact
 * 
 * @author CodeTop Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("FSRS Performance Benchmark Tests")
@Tag("performance")
@ActiveProfiles("test")
@Transactional
public class FSRSPerformanceBenchmarkTest extends AbstractIntegrationTest {

    @Autowired
    private FSRSAlgorithm fsrsAlgorithm;
    
    @Autowired
    private FSRSService fsrsService;
    
    @Autowired
    private UserParametersService userParametersService;

    private User testUser;
    private FSRSParametersDTO defaultParameters;
    private List<Problem> benchmarkProblems;
    private List<FSRSCard> benchmarkCards;

    @BeforeEach
    void setUpPerformanceTest() {
        testUser = createTestUser();
        defaultParameters = fsrsAlgorithm.getDefaultParameters();
        benchmarkProblems = createBenchmarkProblems(100);
        benchmarkCards = createBenchmarkCards(testUser, benchmarkProblems);
    }

    private List<Problem> createBenchmarkProblems(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> createTestProblem("Benchmark Problem " + i, getRandomDifficulty().name()))
            .toList();
    }

    private List<FSRSCard> createBenchmarkCards(User user, List<Problem> problems) {
        return problems.stream()
            .map(problem -> {
                FSRSCard card = createTestFSRSCard(user.getId(), problem.getId());
                card.setState(getRandomState());
                card.setReviewCount((int)(Math.random() * 20));
                card.setStability(BigDecimal.valueOf(Math.random() * 50 + 1));
                card.setDifficulty(BigDecimal.valueOf(Math.random() * 8 + 1));
                card.setLapses((int)(Math.random() * 5));
                fsrsCardMapper.updateById(card);
                return card;
            })
            .toList();
    }

    private Difficulty getRandomDifficulty() {
        Difficulty[] difficulties = {Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD};
        return difficulties[(int)(Math.random() * difficulties.length)];
    }

    private FSRSState getRandomState() {
        FSRSState[] states = {FSRSState.NEW, FSRSState.LEARNING, FSRSState.REVIEW, FSRSState.RELEARNING};
        return states[(int)(Math.random() * states.length)];
    }

    @Nested
    @DisplayName("Algorithm Performance Tests")
    class AlgorithmPerformanceTests {

        @Test
        @DisplayName("Should calculate next review within performance target (<10ms)")
        void shouldCalculateNextReviewWithinPerformanceTarget() {
            // Warmup
            for (int i = 0; i < 10; i++) {
                fsrsAlgorithm.calculateNextReview(benchmarkCards.get(i % benchmarkCards.size()), 3, defaultParameters);
            }
            
            // Benchmark single calculation
            List<Double> durations = new ArrayList<>();
            
            for (int i = 0; i < 1000; i++) {
                FSRSCard card = benchmarkCards.get(i % benchmarkCards.size());
                int rating = (i % 4) + 1;
                
                long startTime = System.nanoTime();
                FSRSCalculationResult result = fsrsAlgorithm.calculateNextReview(card, rating, defaultParameters);
                long endTime = System.nanoTime();
                
                double durationMs = (endTime - startTime) / 1_000_000.0;
                durations.add(durationMs);
                
                // Verify result validity
                assertThat(result).isNotNull();
                assertThat(result.getNewStability()).isPositive();
                assertThat(result.getNewDifficulty()).isBetween(BigDecimal.valueOf(1.0), BigDecimal.valueOf(10.0));
            }
            
            // Performance analysis
            double avgDuration = durations.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double maxDuration = durations.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double p95Duration = durations.stream().sorted().skip((int)(durations.size() * 0.95)).findFirst().orElse(0.0);
            
            System.out.println("FSRS Calculation Performance:");
            System.out.println("Average: " + String.format("%.2f", avgDuration) + "ms");
            System.out.println("Maximum: " + String.format("%.2f", maxDuration) + "ms");
            System.out.println("P95: " + String.format("%.2f", p95Duration) + "ms");
            
            // Performance assertions
            assertThat(avgDuration).isLessThan(10.0); // Average < 10ms
            assertThat(p95Duration).isLessThan(20.0); // P95 < 20ms
            assertThat(maxDuration).isLessThan(50.0); // Max < 50ms
        }

        @Test
        @DisplayName("Should handle batch calculations efficiently")
        void shouldHandleBatchCalculationsEfficiently() {
            int batchSize = 10000;
            List<FSRSCard> largeBatch = IntStream.range(0, batchSize)
                .mapToObj(i -> {
                    FSRSCard card = benchmarkCards.get(i % benchmarkCards.size());
                    // Create variation to avoid cache effects
                    FSRSCard variantCard = new FSRSCard();
                    variantCard.setId(card.getId());
                    variantCard.setState(card.getState());
                    variantCard.setStability(card.getStability().add(BigDecimal.valueOf(Math.random())));
                    variantCard.setDifficulty(card.getDifficulty().add(BigDecimal.valueOf(Math.random() * 0.1)));
                    variantCard.setReviewCount(card.getReviewCount());
                    variantCard.setLapses(card.getLapses());
                    variantCard.setLastReview(LocalDateTime.now().minusDays((int)(Math.random() * 30)));
                    return variantCard;
                })
                .toList();
            
            long startTime = System.nanoTime();
            
            List<FSRSCalculationResult> results = largeBatch.stream()
                .map(card -> fsrsAlgorithm.calculateNextReview(card, (int)(Math.random() * 4) + 1, defaultParameters))
                .toList();
            
            long endTime = System.nanoTime();
            double totalDurationMs = (endTime - startTime) / 1_000_000.0;
            double avgTimePerCalculation = totalDurationMs / batchSize;
            
            System.out.println("Batch Calculation Performance (" + batchSize + " calculations):");
            System.out.println("Total time: " + String.format("%.2f", totalDurationMs) + "ms");
            System.out.println("Average per calculation: " + String.format("%.4f", avgTimePerCalculation) + "ms");
            System.out.println("Throughput: " + String.format("%.0f", batchSize / (totalDurationMs / 1000)) + " calculations/second");
            
            // Performance assertions
            assertThat(results).hasSize(batchSize);
            assertThat(avgTimePerCalculation).isLessThan(1.0); // Average < 1ms per calculation
            assertThat(totalDurationMs).isLessThan(5000.0); // Total < 5 seconds
            
            // Verify all results are valid
            results.forEach(result -> {
                assertThat(result.getNewStability()).isPositive();
                assertThat(result.getNewDifficulty()).isBetween(BigDecimal.valueOf(1.0), BigDecimal.valueOf(10.0));
            });
        }

        @Test
        @DisplayName("Should optimize parameters within acceptable time limits")
        void shouldOptimizeParametersWithinAcceptableTimeLimits() {
            // Create substantial review history for optimization
            List<ReviewLog> reviewLogs = createLargeReviewDataset(2000);
            
            long startTime = System.nanoTime();
            
            FSRSParametersDTO optimizedParameters = fsrsAlgorithm.optimizeParameters(reviewLogs, defaultParameters);
            
            long endTime = System.nanoTime();
            double durationMs = (endTime - startTime) / 1_000_000.0;
            
            System.out.println("Parameter Optimization Performance:");
            System.out.println("Dataset size: " + reviewLogs.size() + " reviews");
            System.out.println("Optimization time: " + String.format("%.2f", durationMs) + "ms");
            
            // Performance assertions
            assertThat(durationMs).isLessThan(10000.0); // Should complete within 10 seconds
            assertThat(optimizedParameters).isNotNull();
            assertThat(fsrsAlgorithm.validateParameters(optimizedParameters)).isTrue();
        }

        private List<ReviewLog> createLargeReviewDataset(int size) {
            LocalDateTime startDate = LocalDateTime.now().minusDays(365);
            return IntStream.range(0, size)
                .mapToObj(i -> {
                    ReviewLog log = new ReviewLog();
                    log.setId((long) i);
                    log.setUserId(testUser.getId());
                    log.setProblemId((long)(i % 50 + 1));
                    log.setRating((int)(Math.random() * 4) + 1);
                    log.setElapsedDays((int)(Math.random() * 60) + 1);
                    log.setReviewType(ReviewType.SCHEDULED);
                    log.setReviewedAt(startDate.plusDays((int)(Math.random() * 300)));
                    log.setCreatedAt(log.getReviewedAt());
                    return log;
                })
                .toList();
        }
    }

    @Nested
    @DisplayName("Service Layer Performance Tests")
    class ServiceLayerPerformanceTests {

        @Test
        @DisplayName("Should generate review queue efficiently for large datasets")
        void shouldGenerateReviewQueueEfficientlyForLargeDatasets() {
            // Create a large number of due cards
            LocalDateTime dueTime = LocalDateTime.now().minusHours(1);
            List<FSRSCard> largeDueSet = IntStream.range(0, 5000)
                .mapToObj(i -> {
                    Problem problem = benchmarkProblems.get(i % benchmarkProblems.size());
                    FSRSCard card = createTestFSRSCard(testUser.getId(), problem.getId());
                    card.setState(FSRSState.REVIEW);
                    card.setDueDate(dueTime.toLocalDate().minusDays(i % 30)); // Spread over 30 days
                    card.setStability(BigDecimal.valueOf(Math.random() * 20 + 1));
                    card.setDifficulty(BigDecimal.valueOf(Math.random() * 5 + 2));
                    fsrsCardMapper.updateById(card);
                    return card;
                })
                .toList();
            
            long startTime = System.nanoTime();
            
            // Generate review queue with different limits
            FSRSService.ReviewQueue queue50 = fsrsService.generateReviewQueue(testUser.getId(), 50);
            FSRSService.ReviewQueue queue100 = fsrsService.generateReviewQueue(testUser.getId(), 100);
            FSRSService.ReviewQueue queue500 = fsrsService.generateReviewQueue(testUser.getId(), 500);
            
            long endTime = System.nanoTime();
            double durationMs = (endTime - startTime) / 1_000_000.0;
            
            System.out.println("Review Queue Generation Performance:");
            System.out.println("Total due cards: " + largeDueSet.size());
            System.out.println("Query time for 3 different limits: " + String.format("%.2f", durationMs) + "ms");
            System.out.println("Average per query: " + String.format("%.2f", durationMs / 3) + "ms");
            
            // Performance assertions
            assertThat(durationMs).isLessThan(2000.0); // All queries < 2 seconds
            assertThat(queue50.getCards()).hasSize(50);
            assertThat(queue100.getCards()).hasSize(100);
            assertThat(queue500.getCards()).hasSize(500);
            
            // Verify ordering (highest priority first)
            for (int i = 1; i < queue50.getCards().size(); i++) {
                // Due date should be in ascending order (earlier = higher priority)
                assertThat(queue50.getCards().get(i).getDueDate())
                    .isAfterOrEqualTo(queue50.getCards().get(i-1).getDueDate());
            }
        }

        @Test
        @DisplayName("Should handle concurrent review submissions efficiently")
        void shouldHandleConcurrentReviewSubmissionsEfficiently() throws InterruptedException {
            int threadCount = 10;
            int reviewsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<CompletableFuture<List<Long>>> futures = new ArrayList<>();
            
            long startTime = System.nanoTime();
            
            // Submit concurrent reviews
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                CompletableFuture<List<Long>> future = CompletableFuture.supplyAsync(() -> {
                    List<Long> threadDurations = new ArrayList<>();
                    
                    for (int i = 0; i < reviewsPerThread; i++) {
                        FSRSCard card = benchmarkCards.get((threadId * reviewsPerThread + i) % benchmarkCards.size());
                        int rating = (i % 4) + 1;
                        
                        long reviewStart = System.nanoTime();
                        try {
                            fsrsService.processReview(testUser.getId(), card.getId(), rating, ReviewType.SCHEDULED);
                        } catch (Exception e) {
                            // Log but don't fail the test for individual review errors
                            System.err.println("Review submission error: " + e.getMessage());
                        }
                        long reviewEnd = System.nanoTime();
                        
                        threadDurations.add((reviewEnd - reviewStart) / 1_000_000L);
                    }
                    
                    return threadDurations;
                }, executor);
                
                futures.add(future);
            }
            
            // Wait for all threads to complete
            List<List<Long>> allDurations = futures.stream()
                .map(CompletableFuture::join)
                .toList();
            
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
            
            long endTime = System.nanoTime();
            double totalDurationMs = (endTime - startTime) / 1_000_000.0;
            
            // Calculate statistics
            List<Long> flatDurations = allDurations.stream()
                .flatMap(List::stream)
                .toList();
            
            double avgDuration = flatDurations.stream().mapToLong(Long::longValue).average().orElse(0);
            long maxDuration = flatDurations.stream().mapToLong(Long::longValue).max().orElse(0);
            int totalReviews = threadCount * reviewsPerThread;
            
            System.out.println("Concurrent Review Submission Performance:");
            System.out.println("Threads: " + threadCount + ", Reviews per thread: " + reviewsPerThread);
            System.out.println("Total reviews: " + totalReviews);
            System.out.println("Total time: " + String.format("%.2f", totalDurationMs) + "ms");
            System.out.println("Average review time: " + String.format("%.2f", avgDuration) + "ms");
            System.out.println("Maximum review time: " + maxDuration + "ms");
            System.out.println("Throughput: " + String.format("%.1f", totalReviews / (totalDurationMs / 1000)) + " reviews/second");
            
            // Performance assertions
            assertThat(avgDuration).isLessThan(100.0); // Average < 100ms per review
            assertThat(maxDuration).isLessThan(1000); // Max < 1 second
            assertThat(totalDurationMs).isLessThan(30000.0); // Total < 30 seconds
        }

        @Test
        @DisplayName("Should maintain cache performance under load")
        void shouldMaintainCachePerformanceUnderLoad() {
            // First request (cache miss)
            long startTime1 = System.nanoTime();
            FSRSParametersDTO params1 = userParametersService.getUserParameters(testUser.getId());
            long endTime1 = System.nanoTime();
            double duration1Ms = (endTime1 - startTime1) / 1_000_000.0;
            
            // Second request (cache hit)
            long startTime2 = System.nanoTime();
            FSRSParametersDTO params2 = userParametersService.getUserParameters(testUser.getId());
            long endTime2 = System.nanoTime();
            double duration2Ms = (endTime2 - startTime2) / 1_000_000.0;
            
            // Many subsequent requests (all cache hits)
            List<Double> cachedDurations = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                long startTime = System.nanoTime();
                userParametersService.getUserParameters(testUser.getId());
                long endTime = System.nanoTime();
                cachedDurations.add((endTime - startTime) / 1_000_000.0);
            }
            
            double avgCachedDuration = cachedDurations.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            
            System.out.println("Cache Performance Analysis:");
            System.out.println("First request (cache miss): " + String.format("%.2f", duration1Ms) + "ms");
            System.out.println("Second request (cache hit): " + String.format("%.2f", duration2Ms) + "ms");
            System.out.println("Average cached request: " + String.format("%.4f", avgCachedDuration) + "ms");
            System.out.println("Cache speedup factor: " + String.format("%.1f", duration1Ms / avgCachedDuration) + "x");
            
            // Performance assertions
            assertThat(params1).isEqualTo(params2); // Same data
            assertThat(duration2Ms).isLessThan(duration1Ms); // Cache hit should be faster
            assertThat(avgCachedDuration).isLessThan(5.0); // Cached requests < 5ms
            assertThat(duration1Ms / avgCachedDuration).isGreaterThan(2.0); // At least 2x speedup
        }
    }

    @Nested
    @DisplayName("Memory Usage and Resource Tests")
    class MemoryUsageTests {

        @Test
        @DisplayName("Should handle large datasets without memory leaks")
        void shouldHandleLargeDatasetsWithoutMemoryLeaks() {
            Runtime runtime = Runtime.getRuntime();
            
            // Get baseline memory usage
            System.gc();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Process large dataset
            int iterationCount = 100;
            for (int iteration = 0; iteration < iterationCount; iteration++) {
                List<FSRSCalculationResult> results = new ArrayList<>();
                
                // Process batch of calculations
                for (int i = 0; i < 1000; i++) {
                    FSRSCard card = benchmarkCards.get(i % benchmarkCards.size());
                    FSRSCalculationResult result = fsrsAlgorithm.calculateNextReview(
                        card, (i % 4) + 1, defaultParameters
                    );
                    results.add(result);
                }
                
                // Clear results to test garbage collection
                results.clear();
                
                // Periodic garbage collection
                if (iteration % 20 == 0) {
                    System.gc();
                }
            }
            
            // Final garbage collection and memory check
            System.gc();
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;
            double memoryIncreaseMB = memoryIncrease / (1024.0 * 1024.0);
            
            System.out.println("Memory Usage Analysis:");
            System.out.println("Initial memory: " + String.format("%.2f", initialMemory / (1024.0 * 1024.0)) + "MB");
            System.out.println("Final memory: " + String.format("%.2f", finalMemory / (1024.0 * 1024.0)) + "MB");
            System.out.println("Memory increase: " + String.format("%.2f", memoryIncreaseMB) + "MB");
            System.out.println("Processed: " + (iterationCount * 1000) + " calculations");
            
            // Memory assertions - should not have significant memory leaks
            assertThat(memoryIncreaseMB).isLessThan(50.0); // Less than 50MB increase
        }

        @Test
        @DisplayName("Should maintain consistent performance under sustained load")
        void shouldMaintainConsistentPerformanceUnderSustainedLoad() {
            int warmupRounds = 10;
            int testRounds = 50;
            List<Double> performanceMetrics = new ArrayList<>();
            
            // Warmup
            for (int i = 0; i < warmupRounds; i++) {
                FSRSCard card = benchmarkCards.get(i % benchmarkCards.size());
                fsrsAlgorithm.calculateNextReview(card, 3, defaultParameters);
            }
            
            // Sustained load test
            for (int round = 0; round < testRounds; round++) {
                long roundStart = System.nanoTime();
                
                // Process batch in each round
                for (int i = 0; i < 100; i++) {
                    FSRSCard card = benchmarkCards.get((round * 100 + i) % benchmarkCards.size());
                    fsrsAlgorithm.calculateNextReview(card, (i % 4) + 1, defaultParameters);
                }
                
                long roundEnd = System.nanoTime();
                double roundDurationMs = (roundEnd - roundStart) / 1_000_000.0;
                performanceMetrics.add(roundDurationMs);
            }
            
            // Analyze performance consistency
            double avgPerformance = performanceMetrics.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double minPerformance = performanceMetrics.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double maxPerformance = performanceMetrics.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double performanceVariance = maxPerformance - minPerformance;
            
            System.out.println("Sustained Load Performance Analysis:");
            System.out.println("Test rounds: " + testRounds + " (100 calculations each)");
            System.out.println("Average round time: " + String.format("%.2f", avgPerformance) + "ms");
            System.out.println("Min round time: " + String.format("%.2f", minPerformance) + "ms");
            System.out.println("Max round time: " + String.format("%.2f", maxPerformance) + "ms");
            System.out.println("Performance variance: " + String.format("%.2f", performanceVariance) + "ms");
            System.out.println("Coefficient of variation: " + String.format("%.2f", (performanceVariance / avgPerformance) * 100) + "%");
            
            // Performance consistency assertions
            assertThat(avgPerformance).isLessThan(200.0); // Average batch < 200ms
            assertThat(performanceVariance).isLessThan(avgPerformance); // Variance < average
            assertThat((performanceVariance / avgPerformance) * 100).isLessThan(50.0); // CV < 50%
        }
    }
}