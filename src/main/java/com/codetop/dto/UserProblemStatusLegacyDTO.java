package com.codetop.dto;

import com.codetop.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Legacy User problem status DTO for backward compatibility.
 * This replaces ProblemController.UserProblemStatus inner class.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProblemStatusLegacyDTO {
    private Long problemId;
    private String title;
    private Difficulty difficulty;
    private String status; // "not_done", "done", "reviewed"
    private Integer mastery; // 0-3 stars
    private String lastAttemptDate;
    private String lastConsideredDate;
    private Integer attemptCount;
    // FSRS 估算的掌握度（0-100）。为避免误解，推荐前端展示该字段为“掌握度”。
    private Double masteryScore;
    private String notes;
}
