package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified learning stats VO for frontend.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningStatsVO {
    private Long totalCards;
    private Long newCards;
    private Long learningCards;
    private Long reviewCards;
    private Long relearningCards;
    private Long dueCards;
    private Double avgDifficulty;
    private Double avgStability;
}