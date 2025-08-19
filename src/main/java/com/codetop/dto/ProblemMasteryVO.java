package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Problem mastery VO for API responses.
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
public class ProblemMasteryVO {
    private Long problemId;
    private Integer masteryLevel;
    private Integer attemptCount;
    private Double accuracy;
    private String lastAttemptDate;
    private String nextReviewDate;
    private String difficulty;
    private String notes;
}