package com.codetop.dto;

import com.codetop.entity.InterviewReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting an interview report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitReportRequest {
    private String company;
    private String department;
    private String position;
    private String problemSearch;
    private String date;
    private String additionalNotes;
    private Integer difficultyRating;
    private InterviewReport.InterviewRound interviewRound;
}