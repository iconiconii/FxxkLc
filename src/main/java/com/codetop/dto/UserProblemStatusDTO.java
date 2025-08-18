package com.codetop.dto;

import com.codetop.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User problem status DTO for API responses and caching.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProblemStatusDTO {
    private Long problemId;
    private String title;
    private Difficulty difficulty;
    private String status; // "not_done", "done", "reviewed"
    private Integer mastery; // 0-3 stars
    private String lastAttemptDate;
    private String lastConsideredDate;
    private Integer attemptCount;
    private Double accuracy;
    private String notes;
}