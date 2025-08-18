package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codetop.enums.FSRSState;
import com.codetop.enums.ReviewType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Review Log entity for tracking all review activities and FSRS state changes.
 * 
 * Features:
 * - Immutable log of all review sessions
 * - FSRS state transition tracking
 * - Response time analytics
 * - Support for different review types (scheduled, extra, cram)
 * - Comprehensive data for algorithm optimization
 * - Partitioned by month for performance
 * 
 * Rating System:
 * - 1 (Again): Completely forgot, need to restart
 * - 2 (Hard): Remembered with significant difficulty
 * - 3 (Good): Remembered with normal effort
 * - 4 (Easy): Remembered easily
 * 
 * @author CodeTop Team
 */
@TableName("review_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull
    @TableField("user_id")
    private Long userId;

    @NotNull
    @TableField("problem_id")
    private Long problemId;

    @NotNull
    @TableField("card_id")
    private Long cardId;

    @TableField("session_id")
    private Long sessionId;

    @Min(1)
    @Max(4)
    @NotNull
    @TableField("rating")
    private Integer rating;

    @TableField("response_time_ms")
    private Integer responseTimeMs;

    @TableField("old_state")
    private FSRSState oldState;

    @TableField("new_state")
    private FSRSState newState;

    @TableField(exist = false)
    private BigDecimal stabilityChange;

    @TableField(exist = false)
    private BigDecimal difficultyChange;

    @Builder.Default
    @TableField("reviewed_at")
    private LocalDateTime reviewedAt = LocalDateTime.now();

    @Builder.Default
    @TableField("review_type")
    private ReviewType reviewType = ReviewType.SCHEDULED;

    // Previous FSRS values for analytics  
    @TableField("difficulty_before")
    private BigDecimal oldDifficulty;

    @TableField("difficulty_after")
    private BigDecimal newDifficulty;

    @TableField("stability_before")
    private BigDecimal oldStability;

    @TableField("stability_after")
    private BigDecimal newStability;

    @TableField("interval_before_days")
    private Integer oldIntervalDays;

    @TableField("interval_after_days")
    private Integer newIntervalDays;

    @TableField(exist = false)
    private Integer elapsedDays;

    @TableField(exist = false)
    private Integer scheduledDays;

    @TableField(exist = false)
    private Integer reviewDurationMs;

    // Associations (not mapped to database)
    @TableField(exist = false)
    private User user;

    @TableField(exist = false)
    private Problem problem;

    @TableField(exist = false)
    private FSRSCard fsrsCard;

    @TableField(exist = false)
    private Object reviewSession;

    // Derived methods
    public boolean isAgain() {
        return rating == 1;
    }

    public boolean isHard() {
        return rating == 2;
    }

    public boolean isGood() {
        return rating == 3;
    }

    public boolean isEasy() {
        return rating == 4;
    }

    public boolean isSuccessful() {
        return rating >= 3;
    }

    public boolean isFailed() {
        return rating == 1;
    }

    public String getRatingText() {
        return switch (rating) {
            case 1 -> "Again";
            case 2 -> "Hard";
            case 3 -> "Good";
            case 4 -> "Easy";
            default -> "Unknown";
        };
    }

    
    public boolean hasStateChange() {
        return oldState != newState;
    }

    
    public boolean hasGraduated() {
        return oldState != FSRSState.REVIEW && newState == FSRSState.REVIEW;
    }

    
    public boolean hasLapsed() {
        return newState == FSRSState.RELEARNING;
    }

    
    public double getResponseTimeSeconds() {
        return responseTimeMs != null ? responseTimeMs / 1000.0 : 0.0;
    }

    
    public double getStabilityChangeAsDouble() {
        return stabilityChange != null ? stabilityChange.doubleValue() : 0.0;
    }

    
    public double getDifficultyChangeAsDouble() {
        return difficultyChange != null ? difficultyChange.doubleValue() : 0.0;
    }

    /**
     * Calculate accuracy based on rating (1=0%, 2=33%, 3=66%, 4=100%).
     */
    
    public double getAccuracyScore() {
        return switch (rating) {
            case 1 -> 0.0;
            case 2 -> 0.33;
            case 3 -> 0.66;
            case 4 -> 1.0;
            default -> 0.0;
        };
    }

    /**
     * Check if response time was fast (under 2 minutes).
     */
    
    public boolean isFastResponse() {
        return responseTimeMs != null && responseTimeMs < 120000; // 2 minutes
    }

    /**
     * Check if response time was slow (over 5 minutes).
     */
    
    public boolean isSlowResponse() {
        return responseTimeMs != null && responseTimeMs > 300000; // 5 minutes
    }

    /**
     * Get performance category based on rating and response time.
     */
    
    public String getPerformanceCategory() {
        if (rating == 4 && isFastResponse()) {
            return "Excellent";
        } else if (rating >= 3 && !isSlowResponse()) {
            return "Good";
        } else if (rating >= 2) {
            return "Struggling";
        } else {
            return "Needs Practice";
        }
    }

    /**
     * Get old stability (for backward compatibility with algorithm).
     */
    public BigDecimal getOldStability() {
        return oldStability;
    }

    /**
     * Get new stability (for backward compatibility with algorithm).
     */
    public BigDecimal getNewStability() {
        return newStability;
    }

    /**
     * Get old difficulty (for backward compatibility with algorithm).
     */
    public BigDecimal getOldDifficulty() {
        return oldDifficulty;
    }

    /**
     * Get new difficulty (for backward compatibility with algorithm).
     */
    public BigDecimal getNewDifficulty() {
        return newDifficulty;
    }

    /**
     * Get elapsed days (for backward compatibility with algorithm).
     */
    public Integer getElapsedDays() {
        return elapsedDays;
    }

    /**
     * Get ID (for backward compatibility).
     */
    public Long getId() {
        return id;
    }

    /**
     * Set ID.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Create review log from FSRS card update.
     */
    public static ReviewLog fromCardUpdate(FSRSCard oldCard, FSRSCard newCard, Integer rating, 
                                          Integer responseTime, Long sessionId, ReviewType reviewType) {
        return ReviewLog.builder()
                .userId(oldCard.getUserId())
                .problemId(oldCard.getProblemId())
                .cardId(oldCard.getId())
                .sessionId(sessionId)
                .rating(rating)
                .responseTimeMs(responseTime)
                .oldState(oldCard.getState())
                .newState(newCard.getState())
                .oldDifficulty(oldCard.getDifficulty())
                .newDifficulty(newCard.getDifficulty())
                .oldStability(oldCard.getStability())
                .newStability(newCard.getStability())
                .stabilityChange(newCard.getStability().subtract(oldCard.getStability()))
                .difficultyChange(newCard.getDifficulty().subtract(oldCard.getDifficulty()))
                .oldIntervalDays(oldCard.getIntervalDays())
                .newIntervalDays(newCard.getIntervalDays())
                .reviewedAt(LocalDateTime.now())
                .reviewType(reviewType != null ? reviewType : ReviewType.SCHEDULED)
                .build();
    }

    @Override
    public String toString() {
        return "ReviewLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", problemId=" + problemId +
                ", cardId=" + cardId +
                ", rating=" + rating +
                ", oldState=" + oldState +
                ", newState=" + newState +
                ", reviewedAt=" + reviewedAt +
                ", reviewType=" + reviewType +
                '}';
    }
}