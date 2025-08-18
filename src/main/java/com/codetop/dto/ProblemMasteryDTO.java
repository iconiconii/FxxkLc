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
    private Double accuracy;
    private String lastAttemptDate;
    private String nextReviewDate;
    private String difficulty;
    private String notes;
}