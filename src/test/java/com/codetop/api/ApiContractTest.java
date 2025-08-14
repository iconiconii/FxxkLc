package com.codetop.api;

import com.codetop.AbstractIntegrationTest;
import com.codetop.entity.FSRSCard;
import com.codetop.entity.Problem;
import com.codetop.entity.User;
import com.codetop.enums.FSRSState;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive API contract tests for all REST endpoints.
 * 
 * Tests cover:
 * - All REST endpoints with various scenarios
 * - Request/response format validation
 * - Error handling and status codes
 * - Authentication integration
 * - Data consistency and validation
 * - Edge cases and boundary conditions
 * 
 * @author CodeTop Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("API Contract Tests")
@Transactional
public class ApiContractTest extends AbstractIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private User adminUser;
    private String userToken;
    private String adminToken;
    private List<Problem> testProblems;

    @BeforeEach
    void setUpApiTest() {
        testUser = createTestUser();
        adminUser = createTestAdmin();
        userToken = jwtUtil.generateAccessToken(testUser);
        adminToken = jwtUtil.generateAccessToken(adminUser);
        testProblems = createTestProblems(5);
    }

    @Nested
    @DisplayName("Authentication API Tests")
    class AuthenticationApiTests {

        @Test
        @DisplayName("POST /api/auth/register - Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() throws Exception {
            String registrationPayload = """
                {
                    "email": "newuser@example.com",
                    "username": "newuser",
                    "password": "NewUserPassword123!",
                    "confirmPassword": "NewUserPassword123!"
                }
                """;

            MvcResult result = mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registrationPayload))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.has("user")).isTrue();
            assertThat(response.has("accessToken")).isTrue();
            assertThat(response.has("refreshToken")).isTrue();
            assertThat(response.get("user").get("email").asText()).isEqualTo("newuser@example.com");
            assertThat(response.get("user").get("username").asText()).isEqualTo("newuser");
            assertThat(response.get("user").has("password")).isFalse(); // Should not expose password
        }

        @Test
        @DisplayName("POST /api/auth/register - Should reject invalid registration data")
        void shouldRejectInvalidRegistrationData() throws Exception {
            String invalidPayloads[] = {
                // Invalid email
                """
                {
                    "email": "invalid-email",
                    "username": "user",
                    "password": "Password123!",
                    "confirmPassword": "Password123!"
                }
                """,
                // Weak password
                """
                {
                    "email": "user@example.com",
                    "username": "user",
                    "password": "weak",
                    "confirmPassword": "weak"
                }
                """,
                // Password mismatch
                """
                {
                    "email": "user@example.com",
                    "username": "user",
                    "password": "Password123!",
                    "confirmPassword": "DifferentPassword123!"
                }
                """,
                // Missing fields
                """
                {
                    "email": "user@example.com"
                }
                """
            };

            for (String payload : invalidPayloads) {
                mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
            }
        }

        @Test
        @DisplayName("POST /api/auth/login - Should login successfully with valid credentials")
        void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
            String loginPayload = String.format("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """, testUser.getEmail(), TEST_USER_PASSWORD);

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginPayload))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.has("user")).isTrue();
            assertThat(response.has("accessToken")).isTrue();
            assertThat(response.has("refreshToken")).isTrue();
            assertThat(response.get("user").get("id").asLong()).isEqualTo(testUser.getId());
            assertThat(response.get("accessToken").asText()).isNotEmpty();
        }

        @Test
        @DisplayName("POST /api/auth/login - Should reject invalid credentials")
        void shouldRejectInvalidCredentials() throws Exception {
            String invalidLoginPayload = String.format("""
                {
                    "email": "%s",
                    "password": "WrongPassword123!"
                }
                """, testUser.getEmail());

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidLoginPayload))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("POST /api/auth/refresh - Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            String refreshToken = jwtUtil.generateRefreshToken(testUser);

            MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.has("accessToken")).isTrue();
            assertThat(response.has("refreshToken")).isTrue();
            assertThat(response.get("accessToken").asText()).isNotEmpty();
        }

        @Test
        @DisplayName("POST /api/auth/logout - Should logout successfully")
        void shouldLogoutSuccessfully() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // Verify token is invalidated
            mockMvc.perform(get("/api/problems")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Problems API Tests")
    class ProblemsApiTests {

        @Test
        @DisplayName("GET /api/problems - Should return paginated problems list")
        void shouldReturnPaginatedProblemsList() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/problems")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", "title,asc")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.has("content")).isTrue();
            assertThat(response.has("totalElements")).isTrue();
            assertThat(response.has("totalPages")).isTrue();
            assertThat(response.has("size")).isTrue();
            assertThat(response.has("number")).isTrue();
            
            JsonNode content = response.get("content");
            assertThat(content.isArray()).isTrue();
            
            if (content.size() > 0) {
                JsonNode firstProblem = content.get(0);
                assertThat(firstProblem.has("id")).isTrue();
                assertThat(firstProblem.has("title")).isTrue();
                assertThat(firstProblem.has("difficulty")).isTrue();
                assertThat(firstProblem.has("content")).isTrue();
            }
        }

        @Test
        @DisplayName("GET /api/problems - Should filter problems by difficulty")
        void shouldFilterProblemsByDifficulty() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/problems")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .param("difficulty", "MEDIUM")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            JsonNode content = response.get("content");
            
            for (JsonNode problem : content) {
                assertThat(problem.get("difficulty").asText()).isEqualTo("MEDIUM");
            }
        }

        @Test
        @DisplayName("GET /api/problems - Should search problems by text")
        void shouldSearchProblemsByText() throws Exception {
            String searchTerm = "Test";
            
            MvcResult result = mockMvc.perform(get("/api/problems")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .param("search", searchTerm)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            JsonNode content = response.get("content");
            
            for (JsonNode problem : content) {
                String title = problem.get("title").asText();
                assertThat(title.toLowerCase().contains(searchTerm.toLowerCase())).isTrue();
            }
        }

        @Test
        @DisplayName("GET /api/problems/{id} - Should return specific problem")
        void shouldReturnSpecificProblem() throws Exception {
            Problem testProblem = testProblems.get(0);
            
            MvcResult result = mockMvc.perform(get("/api/problems/" + testProblem.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.get("id").asLong()).isEqualTo(testProblem.getId());
            assertThat(response.get("title").asText()).isEqualTo(testProblem.getTitle());
            assertThat(response.get("difficulty").asText()).isEqualTo(testProblem.getDifficulty());
        }

        @Test
        @DisplayName("GET /api/problems/{id} - Should return 404 for non-existent problem")
        void shouldReturn404ForNonExistentProblem() throws Exception {
            mockMvc.perform(get("/api/problems/99999")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("POST /api/problems - Should create new problem (admin only)")
        void shouldCreateNewProblemAdminOnly() throws Exception {
            String newProblemPayload = """
                {
                    "title": "New API Test Problem",
                    "content": "This is a test problem created via API",
                    "difficulty": "HARD"
                }
                """;

            MvcResult result = mockMvc.perform(post("/api/problems")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(newProblemPayload))
                    .andExpect(status().isCreated())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.get("title").asText()).isEqualTo("New API Test Problem");
            assertThat(response.get("difficulty").asText()).isEqualTo("HARD");
            assertThat(response.has("id")).isTrue();
            assertThat(response.has("createdAt")).isTrue();
        }

        @Test
        @DisplayName("POST /api/problems - Should reject non-admin problem creation")
        void shouldRejectNonAdminProblemCreation() throws Exception {
            String newProblemPayload = """
                {
                    "title": "Unauthorized Problem",
                    "content": "This should fail",
                    "difficulty": "EASY"
                }
                """;

            mockMvc.perform(post("/api/problems")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(newProblemPayload))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Review API Tests")
    class ReviewApiTests {

        @Test
        @DisplayName("GET /api/review/queue - Should return review queue")
        void shouldReturnReviewQueue() throws Exception {
            // Create some due cards
            LocalDateTime dueTime = LocalDateTime.now().minusHours(1);
            List<FSRSCard> dueCards = testProblems.stream()
                .limit(3)
                .map(problem -> {
                    FSRSCard card = createTestFSRSCard(testUser.getId(), problem.getId());
                    card.setState(FSRSState.REVIEW);
                    card.setDueDate(dueTime.toLocalDate());
                    fsrsCardMapper.updateById(card);
                    return card;
                })
                .toList();

            MvcResult result = mockMvc.perform(get("/api/review/queue")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.has("dueCards")).isTrue();
            assertThat(response.has("newCards")).isTrue();
            assertThat(response.has("totalDue")).isTrue();
            assertThat(response.has("totalNew")).isTrue();
            
            JsonNode dueCardsJson = response.get("dueCards");
            assertThat(dueCardsJson.size()).isEqualTo(3);
            
            for (JsonNode cardJson : dueCardsJson) {
                assertThat(cardJson.has("id")).isTrue();
                assertThat(cardJson.has("problem")).isTrue();
                assertThat(cardJson.has("state")).isTrue();
                assertThat(cardJson.has("dueDate")).isTrue();
                assertThat(cardJson.has("difficulty")).isTrue();
                assertThat(cardJson.has("stability")).isTrue();
            }
        }

        @Test
        @DisplayName("POST /api/review/start - Should start learning a new problem")
        void shouldStartLearningNewProblem() throws Exception {
            Problem targetProblem = testProblems.get(0);
            
            String startPayload = String.format("""
                {
                    "problemId": %d,
                    "action": "start_learning"
                }
                """, targetProblem.getId());

            MvcResult result = mockMvc.perform(post("/api/review/start")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(startPayload))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.has("cardId")).isTrue();
            assertThat(response.has("problem")).isTrue();
            assertThat(response.has("state")).isTrue();
            assertThat(response.has("intervals")).isTrue();
            
            assertThat(response.get("cardId").asLong()).isPositive();
            assertThat(response.get("state").asText()).isEqualTo("NEW");
            assertThat(response.get("problem").get("id").asLong()).isEqualTo(targetProblem.getId());
            
            JsonNode intervals = response.get("intervals");
            assertThat(intervals.has("again")).isTrue();
            assertThat(intervals.has("hard")).isTrue();
            assertThat(intervals.has("good")).isTrue();
            assertThat(intervals.has("easy")).isTrue();
        }

        @Test
        @DisplayName("POST /api/review/submit - Should submit review successfully")
        void shouldSubmitReviewSuccessfully() throws Exception {
            // Create a card to review
            Problem problem = testProblems.get(0);
            FSRSCard card = createTestFSRSCard(testUser.getId(), problem.getId());
            card.setState(FSRSState.REVIEW);
            card.setReviewCount(5);
            card.setStability(BigDecimal.valueOf(10.0));
            card.setDifficulty(BigDecimal.valueOf(5.0));
            fsrsCardMapper.updateById(card);

            String reviewPayload = String.format("""
                {
                    "cardId": %d,
                    "rating": 3,
                    "elapsedDays": 7
                }
                """, card.getId());

            MvcResult result = mockMvc.perform(post("/api/review/submit")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewPayload))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.has("success")).isTrue();
            assertThat(response.has("newState")).isTrue();
            assertThat(response.has("newDifficulty")).isTrue();
            assertThat(response.has("newStability")).isTrue();
            assertThat(response.has("nextReviewTime")).isTrue();
            assertThat(response.has("intervalDays")).isTrue();
            
            assertThat(response.get("success").asBoolean()).isTrue();
            assertThat(response.get("newState").asText()).isEqualTo("REVIEW");
            assertThat(response.get("intervalDays").asInt()).isPositive();
        }

        @Test
        @DisplayName("POST /api/review/submit - Should validate rating values")
        void shouldValidateRatingValues() throws Exception {
            FSRSCard card = createTestFSRSCard(testUser.getId(), testProblems.get(0).getId());
            card.setState(FSRSState.REVIEW);
            fsrsCardMapper.updateById(card);

            int[] invalidRatings = {0, 5, -1, 10};
            
            for (int rating : invalidRatings) {
                String invalidPayload = String.format("""
                    {
                        "cardId": %d,
                        "rating": %d,
                        "elapsedDays": 1
                    }
                    """, card.getId(), rating);

                mockMvc.perform(post("/api/review/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error").exists());
            }
        }

        @Test
        @DisplayName("POST /api/review/submit - Should prevent reviewing other user's cards")
        void shouldPreventReviewingOtherUsersCards() throws Exception {
            // Create card for another user
            User otherUser = createTestUser("other@example.com", "password", "USER");
            FSRSCard otherUserCard = createTestFSRSCard(otherUser.getId(), testProblems.get(0).getId());
            fsrsCardMapper.updateById(otherUserCard);

            String reviewPayload = String.format("""
                {
                    "cardId": %d,
                    "rating": 3,
                    "elapsedDays": 1
                }
                """, otherUserCard.getId());

            mockMvc.perform(post("/api/review/submit")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewPayload))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("User Parameters API Tests")
    class UserParametersApiTests {

        @Test
        @DisplayName("GET /api/users/{id}/parameters - Should return user parameters")
        void shouldReturnUserParameters() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/" + testUser.getId() + "/parameters")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            // Verify all FSRS parameters are present
            String[] expectedParams = {
                "requestRetention", "maximumInterval", "w1", "w2", "w3", "w4", "w5", "w6", "w7", "w8", "w9", 
                "w10", "w11", "w12", "w13", "w14", "w15", "w16", "w17"
            };
            
            for (String param : expectedParams) {
                assertThat(response.has(param)).isTrue();
            }
            
            assertThat(response.get("requestRetention").asDouble()).isBetween(0.7, 0.97);
            assertThat(response.get("maximumInterval").asInt()).isPositive();
        }

        @Test
        @DisplayName("PUT /api/users/{id}/parameters - Should update user parameters")
        void shouldUpdateUserParameters() throws Exception {
            String updatePayload = """
                {
                    "requestRetention": 0.85,
                    "maximumInterval": 10000,
                    "w1": 0.45,
                    "w2": 1.2,
                    "w3": 3.5
                }
                """;

            MvcResult result = mockMvc.perform(put("/api/users/" + testUser.getId() + "/parameters")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.get("requestRetention").asDouble()).isEqualTo(0.85);
            assertThat(response.get("maximumInterval").asInt()).isEqualTo(10000);
            assertThat(response.get("w1").asDouble()).isEqualTo(0.45);
        }

        @Test
        @DisplayName("PUT /api/users/{id}/parameters - Should validate parameter ranges")
        void shouldValidateParameterRanges() throws Exception {
            String invalidPayload = """
                {
                    "requestRetention": 1.5,
                    "maximumInterval": -1,
                    "w1": -10.0
                }
                """;

            mockMvc.perform(put("/api/users/" + testUser.getId() + "/parameters")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").exists());
        }

        @Test
        @DisplayName("POST /api/users/{id}/optimize-parameters - Should optimize parameters")
        void shouldOptimizeParameters() throws Exception {
            // Create review history first
            generateReviewHistoryForOptimization(testUser, testProblems, 150);

            MvcResult result = mockMvc.perform(post("/api/users/" + testUser.getId() + "/optimize-parameters")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.has("optimized")).isTrue();
            assertThat(response.has("parameters")).isTrue();
            assertThat(response.has("improvementScore")).isTrue();
            
            if (response.get("optimized").asBoolean()) {
                assertThat(response.get("parameters")).isNotNull();
                assertThat(response.get("improvementScore").asDouble()).isGreaterThanOrEqualTo(0.0);
            }
        }

        private void generateReviewHistoryForOptimization(User user, List<Problem> problems, int reviewCount) {
            LocalDateTime startDate = LocalDateTime.now().minusDays(100);
            
            for (int i = 0; i < reviewCount; i++) {
                Problem problem = problems.get(i % problems.size());
                FSRSCard card = createTestFSRSCard(user.getId(), problem.getId());
                
                // Generate diverse review history
                generateTestReviewData(user, card, 5 + (i % 10));
            }
        }
    }

    @Nested
    @DisplayName("Analytics API Tests")
    class AnalyticsApiTests {

        @Test
        @DisplayName("GET /api/analytics/dashboard - Should return user dashboard data")
        void shouldReturnUserDashboardData() throws Exception {
            // Create some learning data
            testProblems.stream()
                .limit(3)
                .forEach(problem -> {
                    FSRSCard card = createTestFSRSCard(testUser.getId(), problem.getId());
                    card.setState(FSRSState.REVIEW);
                    card.setReviewCount(5);
                    generateTestReviewData(testUser, card, 3);
                });

            MvcResult result = mockMvc.perform(get("/api/analytics/dashboard")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.has("totalProblems")).isTrue();
            assertThat(response.has("totalReviews")).isTrue();
            assertThat(response.has("streakDays")).isTrue();
            assertThat(response.has("todayReviews")).isTrue();
            assertThat(response.has("dueTomorrow")).isTrue();
            assertThat(response.has("accuracyRate")).isTrue();
            assertThat(response.has("learningProgress")).isTrue();
            
            assertThat(response.get("totalProblems").asInt()).isGreaterThanOrEqualTo(0);
            assertThat(response.get("totalReviews").asInt()).isGreaterThanOrEqualTo(0);
            assertThat(response.get("accuracyRate").asDouble()).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("GET /api/analytics/progress - Should return learning progress data")
        void shouldReturnLearningProgressData() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/analytics/progress")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .param("period", "30")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.has("reviewsPerDay")).isTrue();
            assertThat(response.has("accuracyPerDay")).isTrue();
            assertThat(response.has("newCardsPerDay")).isTrue();
            assertThat(response.has("period")).isTrue();
            
            assertThat(response.get("period").asInt()).isEqualTo(30);
            assertThat(response.get("reviewsPerDay").isArray()).isTrue();
        }

        @Test
        @DisplayName("GET /api/admin/analytics - Should return admin analytics (admin only)")
        void shouldReturnAdminAnalyticsAdminOnly() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/admin/analytics")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            assertThat(response.has("totalUsers")).isTrue();
            assertThat(response.has("activeUsers")).isTrue();
            assertThat(response.has("totalProblems")).isTrue();
            assertThat(response.has("totalReviews")).isTrue();
            assertThat(response.has("averageAccuracy")).isTrue();
            assertThat(response.has("systemPerformance")).isTrue();
        }

        @Test
        @DisplayName("GET /api/admin/analytics - Should deny non-admin access")
        void shouldDenyNonAdminAccess() throws Exception {
            mockMvc.perform(get("/api/admin/analytics")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return consistent error format")
        void shouldReturnConsistentErrorFormat() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/problems/99999")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            
            // Standard error format
            assertThat(response.has("error")).isTrue();
            assertThat(response.has("message")).isTrue();
            assertThat(response.has("timestamp")).isTrue();
            assertThat(response.has("path")).isTrue();
            
            assertThat(response.get("error").asText()).isNotEmpty();
            assertThat(response.get("message").asText()).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJsonGracefully() throws Exception {
            String malformedJson = "{\"title\": \"Test\", \"incomplete\": }";

            mockMvc.perform(post("/api/problems")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should handle content type validation")
        void shouldHandleContentTypeValidation() throws Exception {
            mockMvc.perform(post("/api/problems")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("plain text content"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Should handle method not allowed")
        void shouldHandleMethodNotAllowed() throws Exception {
            mockMvc.perform(patch("/api/problems")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.error").value("Method Not Allowed"));
        }
    }
}