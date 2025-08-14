package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codetop.enums.FSRSState;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FSRS Card entity representing spaced repetition learning cards for problems.
 * 
 * Features:
 * - FSRS algorithm state management (New, Learning, Review, Relearning)
 * - Difficulty and stability tracking for intelligent scheduling
 * - Review count and lapse tracking for analytics
 * - Optimized indexing for queue generation performance
 * - Support for interval-based scheduling with retention targeting
 * 
 * Core FSRS Algorithm Fields:
 * - difficulty: How hard the card is for the user (0-10 scale)
 * - stability: How long the card can be retained in memory (days)
 * - state: Current learning state in the FSRS workflow
 * - reviewCount: Total number of reviews completed
 * - lapses: Number of times the card was forgotten
 * 
 * @author CodeTop Team
 */
@TableName("fsrs_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FSRSCard extends BaseEntity {

    @NotNull
    @TableField("user_id")
    private Long userId;

    @NotNull
    @TableField("problem_id")
    private Long problemId;

    @JsonIgnore
    private User user;

    private Problem problem;

    @Builder.Default
    @TableField("state")
    private FSRSState state = FSRSState.NEW;

    @Builder.Default
    @TableField("difficulty")
    private BigDecimal difficulty = BigDecimal.ZERO;

    @Builder.Default
    @TableField("stability")
    private BigDecimal stability = BigDecimal.ZERO;

    @Builder.Default
    @TableField("review_count")
    private Integer reviewCount = 0;

    @Builder.Default
    @TableField("lapses")
    private Integer lapses = 0;

    @TableField("last_review_at")
    private LocalDateTime lastReview;

    @TableField("next_review_at")
    private LocalDateTime nextReview;

    // Generated column for date-based queries
    @TableField(value = "due_date", select = false)
    private LocalDate dueDate;

    @Builder.Default
    @TableField("interval_days")
    private Integer intervalDays = 0;

    @Builder.Default
    @TableField("ease_factor")
    private BigDecimal easeFactor = new BigDecimal("2.5000");

    @TableField("elapsed_days")
    private Integer elapsedDays;

    @TableField("scheduled_days")
    private Integer scheduledDays;

    @TableField("reps")
    private Integer reps;

    @TableField("grade")
    private Integer grade;

    // Computed fields for queue prioritization
    private Double priorityScore;

    private Integer daysOverdue;

    private Double retentionProbability;

    // Derived methods
    public boolean isNew() {
        return state == FSRSState.NEW;
    }

    public boolean isLearning() {
        return state == FSRSState.LEARNING;
    }

    public boolean isReview() {
        return state == FSRSState.REVIEW;
    }

    public boolean isRelearning() {
        return state == FSRSState.RELEARNING;
    }

    public boolean isDue() {
        return nextReview != null && nextReview.isBefore(LocalDateTime.now());
    }

    public boolean isOverdue() {
        return getDaysOverdue() > 0;
    }

    public int getDaysOverdue() {
        if (nextReview == null) return 0;
        
        LocalDate today = LocalDate.now();
        LocalDate reviewDate = nextReview.toLocalDate();
        
        if (reviewDate.isBefore(today)) {
            return (int) reviewDate.until(today).getDays();
        }
        return 0;
    }

    public double getDifficultyAsDouble() {
        return difficulty != null ? difficulty.doubleValue() : 0.0;
    }

    public double getStabilityAsDouble() {
        return stability != null ? stability.doubleValue() : 0.0;
    }

    public double getEaseFactorAsDouble() {
        return easeFactor != null ? easeFactor.doubleValue() : 2.5;
    }

    /**
     * Calculate retention probability based on current stability and elapsed time.
     */
    public double calculateRetentionProbability() {
        if (stability == null || stability.compareTo(BigDecimal.ZERO) <= 0) {
            return 1.0;
        }
        
        int daysSinceReview = lastReview != null ? 
            (int) lastReview.toLocalDate().until(LocalDate.now()).getDays() : 0;
            
        double stabilityDays = stability.doubleValue();
        return Math.pow(0.9, daysSinceReview / stabilityDays);
    }

    /**
     * Calculate priority score for queue ordering.
     * Higher score = higher priority for review.
     */
    public double calculatePriorityScore() {
        double baseScore = 0.0;
        
        // Overdue bonus - exponentially increase priority
        int overdue = getDaysOverdue();
        if (overdue > 0) {
            baseScore += Math.min(overdue * 0.1, 2.0); // Cap at 2.0
        }
        
        // State-based priority
        baseScore += switch (state) {
            case NEW -> 0.8;
            case LEARNING -> 0.9;
            case RELEARNING -> 1.0;
            case REVIEW -> 0.5 + (1.0 - calculateRetentionProbability()) * 0.5;
        };
        
        // Difficulty factor - harder cards get slight priority
        if (difficulty != null) {
            baseScore += (difficulty.doubleValue() / 20.0); // Max 0.5 bonus
        }
        
        // Lapse penalty/bonus - cards with more lapses need more attention
        baseScore += Math.min(lapses * 0.05, 0.3); // Max 0.3 bonus
        
        return Math.max(0.0, Math.min(baseScore, 5.0)); // Clamp between 0-5
    }

    /**
     * Update card state after review.
     */
    public void updateAfterReview(FSRSState newState, BigDecimal newDifficulty, 
                                  BigDecimal newStability, LocalDateTime nextReviewTime,
                                  Integer rating) {
        this.state = newState;
        this.difficulty = newDifficulty;
        this.stability = newStability;
        this.lastReview = LocalDateTime.now();
        this.nextReview = nextReviewTime;
        this.reviewCount++;
        
        if (nextReviewTime != null && lastReview != null) {
            this.intervalDays = (int) lastReview.toLocalDate().until(nextReviewTime.toLocalDate()).getDays();
        }
        
        // Count lapse if rating was "Again" (rating = 1)
        if (rating != null && rating == 1) {
            this.lapses++;
        }
        
        // Update grade for analytics
        this.grade = rating;
    }

    /**
     * Initialize card for new user-problem combination.
     */
    public void initializeForUser(Long userId, Long problemId) {
        this.userId = userId;
        this.problemId = problemId;
        this.state = FSRSState.NEW;
        this.difficulty = BigDecimal.ZERO;
        this.stability = BigDecimal.ZERO;
        this.reviewCount = 0;
        this.lapses = 0;
        this.intervalDays = 0;
        this.easeFactor = new BigDecimal("2.5000");
        this.reps = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FSRSCard fsrsCard)) return false;
        return userId != null && userId.equals(fsrsCard.userId) &&
               problemId != null && problemId.equals(fsrsCard.problemId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "FSRSCard{" +
                "id=" + getId() +
                ", userId=" + userId +
                ", problemId=" + problemId +
                ", state=" + state +
                ", difficulty=" + difficulty +
                ", stability=" + stability +
                ", reviewCount=" + reviewCount +
                ", lapses=" + lapses +
                ", nextReview=" + nextReview +
                '}';
    }
}