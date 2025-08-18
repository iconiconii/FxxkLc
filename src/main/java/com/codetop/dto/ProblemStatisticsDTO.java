package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Problem statistics DTO for API responses and caching.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemStatisticsDTO {
    private Long totalProblems;
    private Long easyCount;
    private Long mediumCount;
    private Long hardCount;
    private Long premiumCount;
}