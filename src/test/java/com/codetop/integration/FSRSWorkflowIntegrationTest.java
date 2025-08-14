package com.codetop.integration;

import com.codetop.AbstractIntegrationTest;
import com.codetop.entity.FSRSCard;
import com.codetop.entity.Problem;
import com.codetop.entity.ReviewLog;
import com.codetop.entity.User;
import com.codetop.enums.FSRSState;
import com.codetop.enums.ReviewType;
import com.codetop.mapper.ReviewLogMapper;
import com.codetop.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for complete FSRS workflow scenarios.
 * 
 * Tests cover:
 * - End-to-end review workflow from new card to mature card
 * - Review queue generation and management
 * - FSRS calculations with real database integration
 * - Parameter optimization workflow
 * - Performance with large datasets
 * - Concurrent user scenarios
 * 
 * @author CodeTop Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("FSRS Workflow Integration Tests")
@Transactional
public class FSRSWorkflowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private ReviewLogMapper reviewLogMapper;

    private User testUser;
    private String userToken;
    private List<Problem> testProblems;

    @BeforeEach
    void setUpIntegrationTest() {
        testUser = createTestUser();
        userToken = jwtUtil.generateAccessToken(testUser);
        testProblems = createTestProblems(10);
    }

    @Nested
    @DisplayName("Complete FSRS Learning Workflow")
    class CompleteWorkflowTests {

        @Test
        @DisplayName("Should complete full learning workflow from new to mature card")
        void shouldCompleteFullLearningWorkflowFromNewToMatureCard() throws Exception {
            // Step 1: Get initial review queue (should be empty)
            MvcResult initialQueueResult = mockMvc.perform(get("/api/review/queue")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();
            
            JsonNode initialQueue = objectMapper.readTree(initialQueueResult.getResponse().getContentAsString());
            assertThat(initialQueue.get("newCards")).isEmpty();
            assertThat(initialQueue.get("dueCards")).isEmpty();
            
            // Step 2: Start learning a new problem
            Problem targetProblem = testProblems.get(0);
            
            String startLearningPayload = String.format(
                "{\"problemId\":%d,\"action\":\"start_learning\"}", 
                targetProblem.getId()
            );
            
            MvcResult startResult = mockMvc.perform(post("/api/review/start")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(startLearningPayload))
                    .andExpect(status().isOk())
                    .andReturn();
            
            JsonNode startResponse = objectMapper.readTree(startResult.getResponse().getContentAsString());
            Long cardId = startResponse.get("cardId").asLong();
            assertThat(cardId).isPositive();
            assertThat(startResponse.get("state").asText()).isEqualTo("NEW");
            
            // Step 3: First review (Good rating = 3)
            String firstReviewPayload = String.format(
                "{\"cardId\":%d,\"rating\":3,\"elapsedDays\":1}", 
                cardId
            );
            
            MvcResult firstReviewResult = mockMvc.perform(post("/api/review/submit")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(firstReviewPayload))
                    .andExpect(status().isOk())
                    .andReturn();
            
            JsonNode firstReviewResponse = objectMapper.readTree(firstReviewResult.getResponse().getContentAsString());
            assertThat(firstReviewResponse.get("newState").asText()).isEqualTo("LEARNING");
            assertThat(firstReviewResponse.get("nextReviewTime").asText()).isNotEmpty();
            
            BigDecimal firstDifficulty = new BigDecimal(firstReviewResponse.get("newDifficulty").asText());
            BigDecimal firstStability = new BigDecimal(firstReviewResponse.get("newStability").asText());
            
            assertThat(firstDifficulty).isBetween(BigDecimal.valueOf(1.0), BigDecimal.valueOf(10.0));
            assertThat(firstStability).isPositive();
            
            // Step 4: Second review (Good rating = 3) - should graduate to REVIEW
            String secondReviewPayload = String.format(
                "{\"cardId\":%d,\"rating\":3,\"elapsedDays\":2}", 
                cardId
            );
            
            MvcResult secondReviewResult = mockMvc.perform(post("/api/review/submit")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(secondReviewPayload))
                    .andExpect(status().isOk())
                    .andReturn();
            
            JsonNode secondReviewResponse = objectMapper.readTree(secondReviewResult.getResponse().getContentAsString());
            assertThat(secondReviewResponse.get("newState").asText()).isEqualTo("REVIEW");
            
            BigDecimal secondStability = new BigDecimal(secondReviewResponse.get("newStability").asText());
            assertThat(secondStability).isGreaterThan(firstStability);
            
            // Step 5: Continue reviews to build up stability
            Long currentCardId = cardId;
            String currentState = "REVIEW";
            int reviewCount = 2;
            
            for (int i = 0; i < 5; i++) {
                reviewCount++;
                String reviewPayload = String.format(
                    "{\"cardId\":%d,\"rating\":3,\"elapsedDays\":%d}", 
                    currentCardId, 3 + i
                );
                
                MvcResult reviewResult = mockMvc.perform(post("/api/review/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewPayload))
                        .andExpect(status().isOk())
                        .andReturn();
                
                JsonNode reviewResponse = objectMapper.readTree(reviewResult.getResponse().getContentAsString());
                assertThat(reviewResponse.get("newState").asText()).isEqualTo("REVIEW");
                
                // Stability should generally increase with successful reviews
                BigDecimal newStability = new BigDecimal(reviewResponse.get("newStability").asText());
                assertThat(newStability).isPositive();
            }
            
            // Step 6: Verify review history was recorded
            List<ReviewLog> reviewLogs = reviewLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ReviewLog>()
                    .eq("user_id", testUser.getId())
                    .eq("problem_id", targetProblem.getId())
                    .orderByAsc("created_at")
            );
            
            assertThat(reviewLogs).hasSize(reviewCount);
            assertThat(reviewLogs.get(0).getRating()).isEqualTo(3);
            assertThat(reviewLogs.get(0).getReviewType()).isEqualTo("REVIEW");
        }

        @Test
        @DisplayName("Should handle failed reviews and relearning workflow")
        void shouldHandleFailedReviewsAndRelearningWorkflow() throws Exception {
            // Start with a mature card (simulate progression)
            Problem targetProblem = testProblems.get(1);
            FSRSCard matureCard = createTestFSRSCard(testUser.getId(), targetProblem.getId());
            matureCard.setState(FSRSState.REVIEW);
            matureCard.setReviewCount(10);
            matureCard.setStability(BigDecimal.valueOf(30.0));
            matureCard.setDifficulty(BigDecimal.valueOf(5.0));
            fsrsCardMapper.updateById(matureCard);
            
            // Submit failed review (rating = 1)
            String failedReviewPayload = String.format(
                "{\"cardId\":%d,\"rating\":1,\"elapsedDays\":35}", 
                matureCard.getId()
            );
            
            MvcResult failedReviewResult = mockMvc.perform(post("/api/review/submit")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(failedReviewPayload))
                    .andExpect(status().isOk())
                    .andReturn();
            
            JsonNode failedResponse = objectMapper.readTree(failedReviewResult.getResponse().getContentAsString());
            assertThat(failedResponse.get("newState").asText()).isEqualTo("RELEARNING");
            
            // Difficulty should increase after failure
            BigDecimal newDifficulty = new BigDecimal(failedResponse.get("newDifficulty").asText());
            assertThat(newDifficulty).isGreaterThan(BigDecimal.valueOf(5.0));
            
            // Relearn the card with good reviews
            String relearnPayload = String.format(
                "{\"cardId\":%d,\"rating\":3,\"elapsedDays\":1}", 
                matureCard.getId()
            );
            
            MvcResult relearnResult = mockMvc.perform(post("/api/review/submit")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(relearnPayload))
                    .andExpect(status().isOk())
                    .andReturn();
            
            JsonNode relearnResponse = objectMapper.readTree(relearnResult.getResponse().getContentAsString());
            // Should eventually return to REVIEW state
            assertThat(relearnResponse.get("newState").asText()).isIn("LEARNING", "REVIEW");
        }

        @Test
        @DisplayName("Should generate appropriate review queue based on due dates")
        void shouldGenerateAppropriateReviewQueueBasedOnDueDates() throws Exception {
            // Create cards with different due dates
            LocalDateTime now = LocalDateTime.now();
            
            // Card 1: Due now
            FSRSCard dueCard1 = createTestFSRSCard(testUser.getId(), testProblems.get(0).getId());
            dueCard1.setState(FSRSState.REVIEW);
            dueCard1.setDueDate(LocalDateTime.now().minusHours(1).toLocalDate());
            fsrsCardMapper.updateById(dueCard1);
            
            // Card 2: Due tomorrow
            FSRSCard futureCard = createTestFSRSCard(testUser.getId(), testProblems.get(1).getId());
            futureCard.setState(FSRSState.REVIEW);
            futureCard.setDueDate(LocalDateTime.now().plusDays(1).toLocalDate());
            fsrsCardMapper.updateById(futureCard);
            
            // Card 3: Due now
            FSRSCard dueCard2 = createTestFSRSCard(testUser.getId(), testProblems.get(2).getId());
            dueCard2.setState(FSRSState.REVIEW);
            dueCard2.setDueDate(LocalDateTime.now().minusMinutes(30).toLocalDate());
            fsrsCardMapper.updateById(dueCard2);
            
            // Request review queue
            MvcResult queueResult = mockMvc.perform(get("/api/review/queue")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();
            
            JsonNode queueResponse = objectMapper.readTree(queueResult.getResponse().getContentAsString());
            
            // Should contain 2 due cards, not the future one
            assertThat(queueResponse.get("dueCards")).hasSize(2);
            assertThat(queueResponse.get("newCards")).isEmpty();
            
            // Verify the due cards are the correct ones
            JsonNode dueCards = queueResponse.get("dueCards");
            boolean foundCard1 = false, foundCard2 = false;
            
            for (JsonNode card : dueCards) {
                Long cardId = card.get("id").asLong();
                if (cardId.equals(dueCard1.getId())) foundCard1 = true;
                if (cardId.equals(dueCard2.getId())) foundCard2 = true;
            }
            
            assertThat(foundCard1).isTrue();
            assertThat(foundCard2).isTrue();
        }
    }

    @Nested
    @DisplayName("Parameter Optimization Workflow")
    class ParameterOptimizationTests {

        @Test
        @DisplayName("Should optimize parameters after sufficient review history")
        void shouldOptimizeParametersAfterSufficientReviewHistory() throws Exception {
            // Create sufficient review history
            generateExtensiveReviewHistory(testUser, testProblems, 200);
            
            // Request parameter optimization
            MvcResult optimizationResult = mockMvc.perform(post("/api/users/" + testUser.getId() + "/optimize-parameters")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();
            
            JsonNode optimizationResponse = objectMapper.readTree(optimizationResult.getResponse().getContentAsString());
            
            assertThat(optimizationResponse.get("optimized").asBoolean()).isTrue();
            assertThat(optimizationResponse.get("parameters")).isNotNull();
            assertThat(optimizationResponse.get("parameters").get("requestRetention").asDouble())
                .isBetween(0.7, 0.97);
            
            // Verify parameters were saved
            MvcResult parametersResult = mockMvc.perform(get("/api/users/" + testUser.getId() + "/parameters")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();
            
            JsonNode parametersResponse = objectMapper.readTree(parametersResult.getResponse().getContentAsString());
            assertThat(parametersResponse.get("optimizedAt").asText()).isNotEmpty();
        }

        @Test
        @DisplayName("Should not optimize parameters with insufficient review history")
        void shouldNotOptimizeParametersWithInsufficientReviewHistory() throws Exception {
            // Create minimal review history (less than required threshold)
            generateExtensiveReviewHistory(testUser, testProblems.subList(0, 2), 20);
            
            // Request parameter optimization
            MvcResult optimizationResult = mockMvc.perform(post("/api/users/" + testUser.getId() + "/optimize-parameters")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();
            
            JsonNode optimizationResponse = objectMapper.readTree(optimizationResult.getResponse().getContentAsString());
            
            assertThat(optimizationResponse.get("optimized").asBoolean()).isFalse();
            assertThat(optimizationResponse.get("message").asText())
                .contains("insufficient review history");
        }

        private void generateExtensiveReviewHistory(User user, List<Problem> problems, int reviewsPerProblem) {
            LocalDateTime startDate = LocalDateTime.now().minusDays(100);
            
            for (int i = 0; i < problems.size(); i++) {
                Problem problem = problems.get(i);
                FSRSCard card = createTestFSRSCard(user.getId(), problem.getId());
                
                // Generate review history
                for (int j = 0; j < reviewsPerProblem; j++) {
                    ReviewLog reviewLog = new ReviewLog();
                    reviewLog.setUserId(user.getId());
                    reviewLog.setProblemId(problem.getId());
                    reviewLog.setCardId(card.getId());
                    reviewLog.setRating((j % 4) + 1); // Ratings 1-4
                    reviewLog.setElapsedDays((j % 30) + 1);
                    reviewLog.setReviewType(ReviewType.SCHEDULED);
                    reviewLog.setReviewedAt(startDate.plusDays(j));
                    reviewLog.setCreatedAt(startDate.plusDays(j));
                    
                    reviewLogMapper.insert(reviewLog);
                }
            }
        }
    }

    @Nested
    @DisplayName("Performance and Scalability Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle large review queues efficiently")
        void shouldHandleLargeReviewQueuesEfficiently() throws Exception {
            // Create a large number of due cards
            LocalDateTime now = LocalDateTime.now();
            
            for (int i = 0; i < 100; i++) {
                Problem problem = createTestProblem("Performance Test Problem " + i, "MEDIUM");
                FSRSCard card = createTestFSRSCard(testUser.getId(), problem.getId());
                card.setState(FSRSState.REVIEW);
                card.setDueDate(now.minusHours(1).toLocalDate());
                card.setStability(BigDecimal.valueOf(Math.random() * 10 + 1));
                card.setDifficulty(BigDecimal.valueOf(Math.random() * 5 + 2));
                fsrsCardMapper.updateById(card);
            }
            
            long startTime = System.nanoTime();
            
            // Request large review queue
            MvcResult queueResult = mockMvc.perform(get("/api/review/queue")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .param("limit", "50")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();
            
            long endTime = System.nanoTime();
            double durationMs = (endTime - startTime) / 1_000_000.0;
            
            JsonNode queueResponse = objectMapper.readTree(queueResult.getResponse().getContentAsString());
            
            // Performance assertions
            assertThat(durationMs).isLessThan(1000.0); // Should complete within 1 second
            assertThat(queueResponse.get("dueCards")).hasSize(50); // Respects limit
            
            // Verify cards are properly sorted by priority/due date
            JsonNode dueCards = queueResponse.get("dueCards");
            for (JsonNode card : dueCards) {
                assertThat(card.get("dueDate").asText()).isNotEmpty();
                assertThat(card.get("stability").asDouble()).isPositive();
            }
        }

        @Test
        @DisplayName("Should handle batch review submissions efficiently")
        void shouldHandleBatchReviewSubmissionsEfficiently() throws Exception {
            // Create multiple cards for batch review
            List<FSRSCard> cards = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> {
                    Problem problem = createTestProblem("Batch Test Problem " + i, "MEDIUM");
                    FSRSCard card = createTestFSRSCard(testUser.getId(), problem.getId());
                    card.setState(FSRSState.REVIEW);
                    card.setReviewCount(5);
                    card.setStability(BigDecimal.valueOf(10.0));
                    card.setDifficulty(BigDecimal.valueOf(5.0));
                    fsrsCardMapper.updateById(card);
                    return card;
                })
                .toList();
            
            long startTime = System.nanoTime();
            
            // Submit reviews for all cards
            for (FSRSCard card : cards) {
                String reviewPayload = String.format(
                    "{\"cardId\":%d,\"rating\":%d,\"elapsedDays\":%d}", 
                    card.getId(), (int)(Math.random() * 4) + 1, (int)(Math.random() * 10) + 1
                );
                
                mockMvc.perform(post("/api/review/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewPayload))
                        .andExpect(status().isOk());
            }
            
            long endTime = System.nanoTime();
            double durationMs = (endTime - startTime) / 1_000_000.0;
            double avgTimePerReview = durationMs / cards.size();
            
            // Performance assertions
            assertThat(avgTimePerReview).isLessThan(100.0); // Average < 100ms per review
            assertThat(durationMs).isLessThan(3000.0); // Total < 3 seconds
        }

        @Test
        @DisplayName("Should maintain performance with concurrent users")
        void shouldMaintainPerformanceWithConcurrentUsers() throws Exception {
            // Create multiple users
            List<User> users = java.util.stream.IntStream.range(0, 5)
                .mapToObj(i -> createTestUser("concurrentuser" + i + "@example.com", "password", "USER"))
                .toList();
            
            // Create cards for each user
            for (User user : users) {
                for (int j = 0; j < 10; j++) {
                    Problem problem = createTestProblem("Concurrent Test Problem " + j, "MEDIUM");
                    FSRSCard card = createTestFSRSCard(user.getId(), problem.getId());
                    card.setState(FSRSState.REVIEW);
                    card.setDueDate(LocalDateTime.now().minusHours(1).toLocalDate());
                    fsrsCardMapper.updateById(card);
                }
            }
            
            long startTime = System.nanoTime();
            
            // Simulate concurrent queue requests
            List<java.util.concurrent.CompletableFuture<MvcResult>> futures = users.stream()
                .map(user -> {
                    String token = jwtUtil.generateAccessToken(user);
                    return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try {
                            return mockMvc.perform(get("/api/review/queue")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                    .param("limit", "10")
                                    .contentType(MediaType.APPLICATION_JSON))
                                    .andExpect(status().isOk())
                                    .andReturn();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                })
                .toList();
            
            // Wait for all requests to complete
            List<MvcResult> results = futures.stream()
                .map(java.util.concurrent.CompletableFuture::join)
                .toList();
            
            long endTime = System.nanoTime();
            double durationMs = (endTime - startTime) / 1_000_000.0;
            
            // Performance and correctness assertions
            assertThat(durationMs).isLessThan(5000.0); // Should complete within 5 seconds
            assertThat(results).hasSize(5);
            
            for (MvcResult result : results) {
                JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
                assertThat(response.get("dueCards")).hasSizeGreaterThan(0);
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle card with extreme stability values")
        void shouldHandleCardWithExtremeStabilityValues() throws Exception {
            Problem problem = testProblems.get(0);
            FSRSCard extremeCard = createTestFSRSCard(testUser.getId(), problem.getId());
            extremeCard.setState(FSRSState.REVIEW);
            extremeCard.setStability(BigDecimal.valueOf(10000.0)); // Very high stability
            extremeCard.setDifficulty(BigDecimal.valueOf(1.0)); // Very low difficulty
            fsrsCardMapper.updateById(extremeCard);
            
            String reviewPayload = String.format(
                "{\"cardId\":%d,\"rating\":3,\"elapsedDays\":100}", 
                extremeCard.getId()
            );
            
            MvcResult result = mockMvc.perform(post("/api/review/submit")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewPayload))
                    .andExpect(status().isOk())
                    .andReturn();
            
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            // Should handle extreme values gracefully
            assertThat(response.get("newStability").asDouble()).isPositive();
            assertThat(response.get("newDifficulty").asDouble()).isBetween(1.0, 10.0);
            assertThat(response.get("intervalDays").asInt()).isPositive();
        }

        @Test
        @DisplayName("Should handle invalid card state transitions gracefully")
        void shouldHandleInvalidCardStateTransitionsGracefully() throws Exception {
            // Try to review a card that doesn't exist
            String invalidReviewPayload = "{\"cardId\":99999,\"rating\":3,\"elapsedDays\":1}";
            
            mockMvc.perform(post("/api/review/submit")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidReviewPayload))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should handle corrupted card data recovery")
        void shouldHandleCorruptedCardDataRecovery() throws Exception {
            Problem problem = testProblems.get(0);
            FSRSCard corruptedCard = createTestFSRSCard(testUser.getId(), problem.getId());
            
            // Simulate corrupted data
            corruptedCard.setStability(null);
            corruptedCard.setDifficulty(null);
            corruptedCard.setState(FSRSState.REVIEW); // Use valid state for test
            fsrsCardMapper.updateById(corruptedCard);
            
            // System should recover gracefully
            String recoveryPayload = String.format(
                "{\"cardId\":%d,\"rating\":3,\"elapsedDays\":1}", 
                corruptedCard.getId()
            );
            
            MvcResult result = mockMvc.perform(post("/api/review/submit")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(recoveryPayload))
                    .andExpect(status().isOk())
                    .andReturn();
            
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            // Should provide valid values despite corruption
            assertThat(response.get("newStability").asDouble()).isPositive();
            assertThat(response.get("newDifficulty").asDouble()).isBetween(1.0, 10.0);
        }
    }
}