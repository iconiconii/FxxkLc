package com.codetop.debug;

import com.codetop.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Debug test for authentication issues with problem status endpoint
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Slf4j
public class ProblemStatusAuthDebugTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testProblemStatusUpdateWithRegularUser() throws Exception {
        // Create a regular user token (without ADMIN role)
        String regularUserToken = createTestToken(1L, "USER");
        
        log.info("Testing with regular user token: {}", regularUserToken);
        
        // Try to update problem status with regular user
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("status", "done");
        requestBody.put("mastery", 2);
        requestBody.put("notes", "Test update");
        
        mockMvc.perform(put("/api/v1/problems/6/status")
                .header("Authorization", "Bearer " + regularUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
    }
    
    @Test
    public void testProblemStatusUpdateWithAdminUser() throws Exception {
        // Create an admin user token
        String adminUserToken = createTestToken(2L, "ADMIN");
        
        log.info("Testing with admin user token: {}", adminUserToken);
        
        // Try to update problem status with admin user
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("status", "done");
        requestBody.put("mastery", 2);
        requestBody.put("notes", "Test update");
        
        mockMvc.perform(put("/api/v1/problems/6/status")
                .header("Authorization", "Bearer " + adminUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
    }
    
    private String createTestToken(Long userId, String role) {
        // Create a mock user with the specified role
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", "testuser" + userId);
        claims.put("email", "test" + userId + "@example.com");
        claims.put("roles", "[" + role + "]");
        claims.put("type", "access");
        
        // This simulates creating a token without using the full User entity
        return createTokenWithClaims(claims, "testuser" + userId, jwtUtil.getAccessTokenExpiration());
    }
    
    private String createTokenWithClaims(Map<String, Object> claims, String subject, Long expiration) {
        // This is a simplified token creation for testing
        // In a real scenario, you'd use the full JwtUtil.generateAccessToken method
        return "mock_token_for_user_" + subject;
    }
}