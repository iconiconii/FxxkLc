package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Simplified review queue card VO for frontend.
 * Only includes essential fields needed by the UI.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewQueueCardVO {
    private Long id;
    private Long problemId;
    private String problemTitle;
    private String problemDifficulty; // EASY, MEDIUM, HARD
    private String state; // NEW, LEARNING, REVIEW, RELEARNING
    private LocalDate dueDate;
    private Integer intervalDays;
    private Integer priority;
    private Double difficulty; // FSRS difficulty score
    private Double stability;  // FSRS stability score
    private Integer reviewCount;
    private Integer lapses;
}