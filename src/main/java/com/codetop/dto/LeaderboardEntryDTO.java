package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Leaderboard entry DTO for API responses and caching.
 * 
 * @author CodeTop Team
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaderboardEntryDTO {
    private Long rank;
    private Long userId;
    private String username;
    private String avatarUrl;
    private Long totalReviews;
    private Long correctReviews;
    private Double accuracy;
    private Integer streak;
}