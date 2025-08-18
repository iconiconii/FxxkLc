package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * User statistics entity for aggregated learning data.
 * 
 * Maps to user_statistics table with comprehensive learning metrics:
 * - Problem completion statistics by state
 * - Review performance and accuracy tracking
 * - Learning streak and consistency metrics
 * - Study time and response time analytics
 * - User preferences and targets
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_statistics")
public class UserStatistics {

    /**
     * Primary key.
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * User ID (logical reference to users.id).
     */
    @TableField("user_id")
    private Long userId;

    // Problem Statistics

    /**
     * Total problems attempted.
     */
    @TableField("total_problems")
    @Builder.Default
    private Integer totalProblems = 0;

    /**
     * Problems in REVIEW state with high stability.
     */
    @TableField("problems_mastered")
    @Builder.Default
    private Integer problemsMastered = 0;

    /**
     * Problems in LEARNING state.
     */
    @TableField("problems_learning")
    @Builder.Default
    private Integer problemsLearning = 0;

    /**
     * Problems in NEW state.
     */
    @TableField("problems_new")
    @Builder.Default
    private Integer problemsNew = 0;

    /**
     * Problems in RELEARNING state.
     */
    @TableField("problems_relearning")
    @Builder.Default
    private Integer problemsRelearning = 0;

    // Review Statistics

    /**
     * Total number of reviews completed.
     */
    @TableField("total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    /**
     * Number of correct reviews (rating >= 3).
     */
    @TableField("correct_reviews")
    @Builder.Default
    private Integer correctReviews = 0;

    /**
     * Total study time in milliseconds.
     */
    @TableField("total_study_time_ms")
    @Builder.Default
    private Long totalStudyTimeMs = 0L;

    // Streak Information

    /**
     * Current consecutive review days.
     */
    @TableField("current_streak_days")
    @Builder.Default
    private Integer currentStreakDays = 0;

    /**
     * Longest consecutive review streak.
     */
    @TableField("longest_streak_days")
    @Builder.Default
    private Integer longestStreakDays = 0;

    /**
     * Last review date for streak calculation.
     */
    @TableField("last_review_date")
    private LocalDate lastReviewDate;

    // Performance Metrics

    /**
     * Overall accuracy rate percentage.
     */
    @TableField("overall_accuracy_rate")
    @Builder.Default
    private BigDecimal overallAccuracyRate = BigDecimal.ZERO;

    /**
     * Average response time across all reviews.
     */
    @TableField("average_response_time_ms")
    @Builder.Default
    private Integer averageResponseTimeMs = 0;

    /**
     * Retention rate based on FSRS predictions.
     */
    @TableField("retention_rate")
    @Builder.Default
    private BigDecimal retentionRate = BigDecimal.ZERO;

    // Preferences

    /**
     * Daily review target count.
     */
    @TableField("daily_review_target")
    @Builder.Default
    private Integer dailyReviewTarget = 50;

    /**
     * Preferred review time.
     */
    @TableField("preferred_review_time")
    private LocalTime preferredReviewTime;

    // Timestamps

    /**
     * Record creation time.
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Last update time.
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // Calculated Properties

    /**
     * Calculate accuracy rate as percentage.
     */
    public double getAccuracyPercentage() {
        if (totalReviews == null || totalReviews == 0) {
            return 0.0;
        }
        return (correctReviews != null ? correctReviews : 0) * 100.0 / totalReviews;
    }

    /**
     * Calculate total study time in hours.
     */
    public double getStudyTimeHours() {
        if (totalStudyTimeMs == null) {
            return 0.0;
        }
        return totalStudyTimeMs / (1000.0 * 60.0 * 60.0);
    }

    /**
     * Calculate average response time in seconds.
     */
    public double getAverageResponseTimeSeconds() {
        if (averageResponseTimeMs == null) {
            return 0.0;
        }
        return averageResponseTimeMs / 1000.0;
    }

    /**
     * Check if user is on an active streak.
     */
    public boolean hasActiveStreak() {
        if (currentStreakDays == null || currentStreakDays <= 0) {
            return false;
        }
        if (lastReviewDate == null) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        return lastReviewDate.equals(today) || lastReviewDate.equals(yesterday);
    }

    /**
     * Get streak status description.
     */
    public String getStreakStatus() {
        if (currentStreakDays == null || currentStreakDays <= 0) {
            return "无连续记录";
        }
        
        if (hasActiveStreak()) {
            return String.format("连续学习 %d 天", currentStreakDays);
        } else {
            return String.format("上次连续 %d 天", currentStreakDays);
        }
    }

    /**
     * Calculate completion rate across all problems.
     */
    public double getCompletionRate() {
        if (totalProblems == null || totalProblems == 0) {
            return 0.0;
        }
        
        int completed = (problemsMastered != null ? problemsMastered : 0) +
                       (problemsLearning != null ? problemsLearning : 0);
        
        return completed * 100.0 / totalProblems;
    }

    /**
     * Get performance level based on multiple metrics.
     */
    public String getPerformanceLevel() {
        double accuracy = getAccuracyPercentage();
        int reviews = totalReviews != null ? totalReviews : 0;
        int streak = currentStreakDays != null ? currentStreakDays : 0;
        
        if (accuracy >= 95 && reviews >= 1000 && streak >= 30) {
            return "大师级";
        } else if (accuracy >= 90 && reviews >= 500 && streak >= 15) {
            return "专家级";
        } else if (accuracy >= 85 && reviews >= 200 && streak >= 7) {
            return "熟练级";
        } else if (accuracy >= 75 && reviews >= 50) {
            return "进步级";
        } else {
            return "新手级";
        }
    }

    /**
     * Check if user meets minimum requirements for leaderboard inclusion.
     */
    public boolean isEligibleForLeaderboard() {
        return totalReviews != null && totalReviews >= 10;
    }
}