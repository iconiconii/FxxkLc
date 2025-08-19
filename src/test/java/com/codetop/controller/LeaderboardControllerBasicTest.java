package com.codetop.controller;

import com.codetop.service.LeaderboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Basic tests for LeaderboardController to verify functionality.
 * 
 * @author CodeTop Team
 */
@WebMvcTest(LeaderboardController.class)
@DisplayName("Leaderboard Controller Basic Tests")
class LeaderboardControllerBasicTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaderboardService leaderboardService;

    @Test
    @DisplayName("Should return 200 OK for global leaderboard endpoint")
    void shouldReturnOkForGlobalLeaderboard() throws Exception {
        // Arrange
        when(leaderboardService.getGlobalLeaderboard(50)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/leaderboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should return 200 OK for weekly leaderboard endpoint")
    void shouldReturnOkForWeeklyLeaderboard() throws Exception {
        // Arrange
        when(leaderboardService.getWeeklyLeaderboard(50)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/leaderboard/weekly")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should return 200 OK for accuracy leaderboard endpoint")
    void shouldReturnOkForAccuracyLeaderboard() throws Exception {
        // Arrange
        when(leaderboardService.getAccuracyLeaderboard(50, 30)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/leaderboard/accuracy")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should return 200 OK for streak leaderboard endpoint")
    void shouldReturnOkForStreakLeaderboard() throws Exception {
        // Arrange
        when(leaderboardService.getStreakLeaderboard(50)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/leaderboard/streak")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should return 200 OK for stats endpoint")
    void shouldReturnOkForStatsEndpoint() throws Exception {
        // Arrange
        LeaderboardService.TopPerformersSummary mockSummary = 
                LeaderboardService.TopPerformersSummary.builder()
                        .topByVolume(Collections.emptyList())
                        .topByAccuracy(Collections.emptyList())
                        .topByStreak(Collections.emptyList())
                        .build();
        when(leaderboardService.getTopPerformersSummary()).thenReturn(mockSummary);

        // Act & Assert
        mockMvc.perform(get("/leaderboard/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}