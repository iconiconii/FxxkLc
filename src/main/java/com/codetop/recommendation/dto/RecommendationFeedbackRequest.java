package com.codetop.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RecommendationFeedbackRequest {
    @NotNull
    private Long userId; // optional if using session; required in this simple impl

    @NotBlank
    private String feedback; // helpful | not_helpful | mastered

    private String note; // optional comment

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

