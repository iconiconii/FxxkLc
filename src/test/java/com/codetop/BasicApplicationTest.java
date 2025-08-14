package com.codetop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Simple test to verify the application starts correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
public class BasicApplicationTest {

    @Test
    void contextLoads() {
        // Test that the Spring application context loads successfully
        // This verifies all beans are properly configured
    }
}