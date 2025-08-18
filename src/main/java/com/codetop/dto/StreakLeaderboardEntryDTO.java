package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Streak leaderboard entry DTO for API responses and caching.
 * 
 * @author CodeTop Team
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreakLeaderboardEntryDTO {
    private Long rank;
    private Long userId;
    private String username;
    private String avatarUrl;
    private Integer currentStreak;
    private Integer longestStreak;
    private Long totalActiveDays;
    private String badge;
}