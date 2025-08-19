package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Problem statistics VO for API responses.
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
public class ProblemStatisticsVO {
    private Long totalProblems;
    private Long easyCount;
    private Long mediumCount;
    private Long hardCount;
    private Long premiumCount;
}