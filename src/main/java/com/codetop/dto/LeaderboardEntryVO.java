package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Leaderboard entry VO for API responses.
 * 
 * This VO is specifically designed for API responses to avoid
 * exposing internal type information from cached DTOs.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeaderboardEntryVO {
    private Long rank;
    private Long userId;
    private String username;
    private String avatarUrl;
    private Long totalReviews;
    private Long correctReviews;
    private Double accuracy;
    private Integer streak;
    private String badge;
}