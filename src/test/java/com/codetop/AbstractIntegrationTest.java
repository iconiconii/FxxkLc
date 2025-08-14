package com.codetop;

import com.codetop.config.TestContainerConfiguration;
import com.codetop.entity.User;
import com.codetop.entity.Problem;
import com.codetop.entity.FSRSCard;
import com.codetop.enums.Difficulty;
import com.codetop.enums.FSRSState;
import com.codetop.mapper.UserMapper;
import com.codetop.mapper.ProblemMapper;
import com.codetop.mapper.FSRSCardMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Base class for integration tests with TestContainers support.
 * 
 * Provides:
 * - MySQL and Redis containers via TestContainers
 * - Test data generation and cleanup utilities
 * - Authentication helpers for testing secured endpoints
 * - Common test fixtures and utilities
 * - Proper transaction management for test isolation
 * 
 * @author CodeTop Team
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.profiles.active=test",
        "spring.flyway.enabled=true",
        "logging.level.com.codetop=DEBUG"
    }
)
@ExtendWith(SpringExtension.class)
@Import(TestContainerConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureWebMvc
@Testcontainers
@Transactional
@Slf4j
public abstract class AbstractIntegrationTest {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    @Autowired
    protected UserMapper userMapper;

    @Autowired
    protected ProblemMapper problemMapper;

    @Autowired
    protected FSRSCardMapper fsrsCardMapper;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected MockMvc mockMvc;

    // Test data constants
    protected static final String TEST_USER_EMAIL = "test@example.com";
    protected static final String TEST_USER_PASSWORD = "TestPassword123!";
    protected static final String TEST_ADMIN_EMAIL = "admin@example.com";
    protected static final String TEST_ADMIN_PASSWORD = "AdminPassword123!";

    @BeforeEach
    void setUp() {
        // Set up MockMvc
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        // Clean up test data and Redis
        cleanTestData();
        TestContainerConfiguration.TestContainerUtils.cleanRedisData();

        // Wait for containers to be ready
        TestContainerConfiguration.TestContainerUtils.waitForContainers();

        log.debug("Integration test setup completed");
    }

    /**
     * Clean up test data for isolation between tests.
     */
    protected void cleanTestData() {
        try {
            // Clean up in reverse dependency order
            fsrsCardMapper.delete(null); // Delete all FSRS cards
            // Note: We should also clean user_fsrs_parameters, security_events, etc.
            // But for test isolation with @Transactional, this should be sufficient
            
            log.debug("Test data cleaned up");
        } catch (Exception e) {
            log.warn("Failed to clean test data", e);
        }
    }

    /**
     * Create a test user with basic profile.
     */
    protected User createTestUser() {
        return createTestUser(TEST_USER_EMAIL, TEST_USER_PASSWORD, "USER");
    }

    /**
     * Create a test user with specified details.
     */
    protected User createTestUser(String email, String password, String role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUsername(email.substring(0, email.indexOf('@')));
        user.addRole(role);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        userMapper.insert(user);
        
        log.debug("Created test user: {}", user.getEmail());
        return user;
    }

    /**
     * Create a test admin user.
     */
    protected User createTestAdmin() {
        return createTestUser(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD, "ADMIN");
    }

    /**
     * Create a test problem.
     */
    protected Problem createTestProblem(String title, String difficulty) {
        Problem problem = new Problem();
        problem.setTitle(title);
        problem.setDifficulty(Difficulty.valueOf(difficulty));
        problem.setIsDeleted(false);
        problem.setCreatedAt(LocalDateTime.now());
        problem.setUpdatedAt(LocalDateTime.now());
        
        problemMapper.insert(problem);
        
        log.debug("Created test problem: {}", problem.getTitle());
        return problem;
    }

    /**
     * Create a test FSRS card for a user and problem.
     */
    protected FSRSCard createTestFSRSCard(Long userId, Long problemId) {
        FSRSCard card = new FSRSCard();
        card.setUserId(userId);
        card.setProblemId(problemId);
        card.setState(FSRSState.NEW);
        card.setDifficulty(BigDecimal.ZERO);
        card.setStability(BigDecimal.ZERO);
        card.setReviewCount(0);
        card.setLapses(0);
        card.setCreatedAt(LocalDateTime.now());
        card.setUpdatedAt(LocalDateTime.now());
        
        fsrsCardMapper.insert(card);
        
        log.debug("Created test FSRS card for user {} and problem {}", userId, problemId);
        return card;
    }

    /**
     * Create multiple test problems with different difficulties.
     */
    protected List<Problem> createTestProblems(int count) {
        String[] difficulties = {"EASY", "MEDIUM", "HARD"};
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createTestProblem(
                        "Test Problem " + (i + 1),
                        difficulties[i % difficulties.length]
                ))
                .toList();
    }

    /**
     * Create test FSRS cards for a user with multiple problems.
     */
    protected List<FSRSCard> createTestFSRSCards(User user, List<Problem> problems) {
        return problems.stream()
                .map(problem -> createTestFSRSCard(user.getId(), problem.getId()))
                .toList();
    }

    /**
     * Authenticate a user for testing secured endpoints.
     */
    protected void authenticateUser(User user) {
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRoles().iterator().next()))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        log.debug("Authenticated user: {}", user.getEmail());
    }

    /**
     * Authenticate an admin user for testing admin endpoints.
     */
    protected void authenticateAdmin() {
        User admin = createTestAdmin();
        authenticateUser(admin);
    }

    /**
     * Clear authentication context.
     */
    protected void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Generate test review data for FSRS testing.
     */
    protected void generateTestReviewData(User user, FSRSCard card, int reviewCount) {
        // Simulate multiple reviews with different grades
        LocalDateTime lastReview = LocalDateTime.now().minusDays(reviewCount);
        
        for (int i = 0; i < reviewCount; i++) {
            // Simulate review with random grade (1-4)
            int grade = (i % 4) + 1;
            
            // Update card state based on review
            if (grade == 1) { // Failed
                card.setLapses(card.getLapses() + 1);
                card.setState(FSRSState.RELEARNING);
            } else if (card.getReviewCount() < 2) {
                card.setState(FSRSState.LEARNING);
            } else {
                card.setState(FSRSState.REVIEW);
            }
            
            card.setReviewCount(card.getReviewCount() + 1);
            card.setLastReview(lastReview.plusDays(i));
            card.setGrade(grade);
            
            // Simulate FSRS calculations (simplified)
            card.setDifficulty(card.getDifficulty().add(BigDecimal.valueOf(0.1)));
            card.setStability(card.getStability().add(BigDecimal.valueOf(1.0)));
            
            fsrsCardMapper.updateById(card);
        }
        
        log.debug("Generated {} review records for card {}", reviewCount, card.getId());
    }

    /**
     * Wait for async operations to complete (useful for testing async methods).
     */
    protected void waitForAsyncOperations() throws InterruptedException {
        Thread.sleep(100); // Short wait for async operations
    }

    /**
     * Utility method to convert objects to JSON strings for request bodies.
     */
    protected String asJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    /**
     * Utility method to parse JSON responses to objects.
     */
    protected <T> T parseJsonResponse(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON response", e);
        }
    }
}