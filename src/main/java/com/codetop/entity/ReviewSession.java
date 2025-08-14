package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codetop.enums.ReviewType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Review Session entity for tracking review sessions and batch operations.
 * 
 * Features:
 * - Session-based review tracking for analytics
 * - Support for different session types
 * - Progress tracking within sessions
 * - Session timeout handling
 * - Performance metrics aggregation
 * 
 * @author CodeTop Team
 */
@TableName("review_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSession extends BaseEntity {

    @NotNull
    @TableField("user_id")
    private Long userId;

    @Builder.Default
    @TableField("started_at")
    private LocalDateTime startedAt = LocalDateTime.now();

    @TableField("ended_at")
    private LocalDateTime endedAt;

    @Builder.Default
    @TableField("is_active")
    private Boolean isActive = true;

    @Builder.Default
    @TableField("session_type")
    private ReviewType sessionType = ReviewType.SCHEDULED;

    @Builder.Default
    @TableField("problems_planned")
    private Integer problemsPlanned = 0;

    @Builder.Default
    @TableField("problems_completed")
    private Integer problemsCompleted = 0;

    @TableField("target_duration_ms")
    private Integer targetDurationMs;

    @TableField("actual_duration_ms")
    private Integer actualDurationMs;

    @Builder.Default
    @TableField("total_response_time_ms")
    private Integer totalResponseTimeMs = 0;

    @Builder.Default
    @TableField("correct_answers")
    private Integer correctAnswers = 0;

    @Builder.Default
    @TableField("total_reviews")
    private Integer totalReviews = 0;

    @TableField("average_rating")
    private Double averageRating;

    @TableField("notes")
    private String notes;

    @Builder.Default
    @TableField("interrupted")
    private Boolean interrupted = false;

    @TableField("last_activity_at")
    private LocalDateTime lastActivityAt;

    // Associations (not mapped to database in MyBatis-Plus)
    @TableField(exist = false)
    @JsonIgnore
    private User user;

    @TableField(exist = false)
    @JsonIgnore
    private Set<ReviewLog> reviewLogs;

    // Derived methods
    
    public boolean isCompleted() {
        return endedAt != null;
    }

    
    public boolean isInProgress() {
        return isActive && endedAt == null;
    }

    
    public boolean isTimedOut() {
        if (!isActive || lastActivityAt == null) return false;
        return lastActivityAt.isBefore(LocalDateTime.now().minusMinutes(30));
    }

    
    public int getDurationMinutes() {
        if (startedAt == null) return 0;
        
        LocalDateTime end = endedAt != null ? endedAt : LocalDateTime.now();
        return (int) java.time.Duration.between(startedAt, end).toMinutes();
    }

    
    public double getAccuracyRate() {
        return totalReviews > 0 ? (double) correctAnswers / totalReviews * 100.0 : 0.0;
    }

    
    public double getCompletionRate() {
        return problemsPlanned > 0 ? (double) problemsCompleted / problemsPlanned * 100.0 : 0.0;
    }

    
    public double getAverageResponseTimeSeconds() {
        return totalReviews > 0 ? totalResponseTimeMs / 1000.0 / totalReviews : 0.0;
    }

    
    public boolean isEfficient() {
        return getAverageResponseTimeSeconds() < 180; // Under 3 minutes average
    }

    
    public String getPerformanceLevel() {
        double accuracy = getAccuracyRate();
        boolean efficient = isEfficient();
        
        if (accuracy >= 90 && efficient) return "Excellent";
        if (accuracy >= 80 && efficient) return "Very Good";
        if (accuracy >= 70) return "Good";
        if (accuracy >= 60) return "Fair";
        return "Needs Improvement";
    }

    /**
     * Start the session.
     */
    public void start() {
        this.startedAt = LocalDateTime.now();
        this.isActive = true;
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * End the session.
     */
    public void end() {
        this.endedAt = LocalDateTime.now();
        this.isActive = false;
        this.lastActivityAt = LocalDateTime.now();
        
        if (startedAt != null && endedAt != null) {
            this.actualDurationMs = (int) java.time.Duration.between(startedAt, endedAt).toMillis();
        }
        
        calculateFinalStats();
    }

    /**
     * Mark session as interrupted.
     */
    public void interrupt() {
        this.interrupted = true;
        this.isActive = false;
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * Resume interrupted session.
     */
    public void resume() {
        this.interrupted = false;
        this.isActive = true;
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * Record a review completion.
     */
    public void recordReview(int rating, int responseTimeMs) {
        this.totalReviews++;
        this.totalResponseTimeMs += responseTimeMs;
        this.lastActivityAt = LocalDateTime.now();
        
        if (rating >= 3) { // Good or Easy
            this.correctAnswers++;
        }
        
        this.problemsCompleted++;
        
        // Update average rating
        if (averageRating == null) {
            averageRating = (double) rating;
        } else {
            averageRating = ((averageRating * (totalReviews - 1)) + rating) / totalReviews;
        }
    }

    /**
     * Check if session should timeout.
     */
    public boolean shouldTimeout() {
        return isInProgress() && isTimedOut();
    }

    /**
     * Timeout the session.
     */
    public void timeout() {
        this.isActive = false;
        this.interrupted = true;
        this.endedAt = LocalDateTime.now();
        this.notes = (notes != null ? notes + " " : "") + "Session timed out due to inactivity.";
    }

    /**
     * Calculate final session statistics.
     */
    private void calculateFinalStats() {
        if (averageRating == null && totalReviews > 0) {
            // This shouldn't happen, but just in case
            averageRating = (double) correctAnswers / totalReviews * 4.0; // Rough estimate
        }
    }

    /**
     * Get session summary for display.
     */
    
    public String getSummary() {
        return String.format("Session: %d/%d problems, %.1f%% accuracy, %d min duration", 
                problemsCompleted, problemsPlanned, getAccuracyRate(), getDurationMinutes());
    }

    @Override
    public String toString() {
        return "ReviewSession{" +
                "id=" + getId() +
                ", userId=" + userId +
                ", sessionType=" + sessionType +
                ", problemsCompleted=" + problemsCompleted +
                ", problemsPlanned=" + problemsPlanned +
                ", isActive=" + isActive +
                ", startedAt=" + startedAt +
                ", endedAt=" + endedAt +
                '}';
    }
}