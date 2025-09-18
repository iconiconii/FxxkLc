package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Problem mastery DTO for API responses and caching.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemMasteryDTO {
    private Long problemId;
    private Integer masteryLevel; // 0-3
    private Integer attemptCount;
    // FSRS 估算的掌握度（0-100）。为避免误解，推荐前端展示该字段为“掌握度”。
    private Double masteryScore;
    private String lastAttemptDate;
    private String nextReviewDate;
    private String difficulty;
    private String notes;
}
