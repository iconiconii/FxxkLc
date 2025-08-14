package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codetop.enums.ProblemFrequency;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Junction entity representing the many-to-many relationship between Problems and Companies.
 * 
 * Features:
 * - Tracks frequency of problems asked by companies
 * - Records when problems were last asked
 * - Supports difficulty level specific to company-problem combinations
 * - Enables trending analysis for interview preparation
 * 
 * @author CodeTop Team
 */
@TableName("problem_companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemCompany extends BaseEntity {

    @NotNull
    @TableField("problem_id")
    private Long problemId;

    @NotNull
    @TableField("company_id")
    private Long companyId;

    @TableField(exist = false)
    private Problem problem;

    @TableField(exist = false)
    private Company company;

    @Builder.Default
    @TableField("frequency")
    private ProblemFrequency frequency = ProblemFrequency.LOW;

    @TableField("last_asked")
    private LocalDate lastAsked;

    @Builder.Default
    @TableField("ask_count")
    private Integer askCount = 0;

    @TableField("year_range")
    private String yearRange; // e.g., "2023-2024"

    @TableField("interview_round")
    private String interviewRound; // e.g., "Phone Screen", "Onsite", "Final"

    @Builder.Default
    @TableField("is_active")
    private Boolean isActive = true;

    @TableField("notes")
    private String notes;

    @TableField("difficulty_rating")
    private Double difficultyRating; // Company-specific difficulty rating

    // Enhanced frequency statistics fields
    @Builder.Default
    @TableField("frequency_score")
    private BigDecimal frequencyScore = BigDecimal.ZERO;

    @Builder.Default
    @TableField("recent_6m_count")
    private Integer recent6mCount = 0;

    @Builder.Default
    @TableField("recent_1y_count")
    private Integer recent1yCount = 0;

    @Builder.Default
    @TableField("trend_score")
    private BigDecimal trendScore = BigDecimal.ZERO;

    @TableField("department_id")
    private Long departmentId;

    @TableField("position_id")
    private Long positionId;

    @Builder.Default
    @TableField("source_type")
    private String sourceType = "USER_REPORT";

    @Builder.Default
    @TableField("credibility_score")
    private BigDecimal credibilityScore = new BigDecimal("50.00");

    @TableField("last_updated_by")
    private Long lastUpdatedBy;

    // Derived methods
    public boolean isHighFrequency() {
        return frequency == ProblemFrequency.HIGH;
    }

    public boolean isRecentlyAsked() {
        return lastAsked != null && lastAsked.isAfter(LocalDate.now().minusMonths(6));
    }

    public boolean isTrending() {
        return isHighFrequency() && isRecentlyAsked();
    }

    /**
     * Update frequency based on ask count.
     */
    public void updateFrequency() {
        if (askCount >= 20) {
            frequency = ProblemFrequency.HIGH;
        } else if (askCount >= 10) {
            frequency = ProblemFrequency.MEDIUM;
        } else {
            frequency = ProblemFrequency.LOW;
        }
    }

    /**
     * Record a new ask instance.
     */
    public void recordAsk(LocalDate askedDate) {
        this.askCount++;
        this.lastAsked = askedDate;
        updateFrequency();
        updateRecentCounts(askedDate);
        calculateTrendScore();
    }

    /**
     * Update recent ask counts based on date.
     */
    private void updateRecentCounts(LocalDate askedDate) {
        LocalDate now = LocalDate.now();
        if (askedDate.isAfter(now.minusMonths(6))) {
            this.recent6mCount++;
        }
        if (askedDate.isAfter(now.minusYears(1))) {
            this.recent1yCount++;
        }
    }

    /**
     * Calculate trend score based on recent activity.
     */
    private void calculateTrendScore() {
        // Simple trend calculation: weight recent activity more
        double score = (recent6mCount * 2.0) + recent1yCount;
        this.trendScore = BigDecimal.valueOf(score);
    }

    /**
     * Calculate frequency score based on multiple factors.
     */
    public void calculateFrequencyScore() {
        double baseScore = askCount * 10.0;
        double recentBonus = recent6mCount * 20.0;
        double trendBonus = trendScore.doubleValue() * 5.0;
        double credibilityMultiplier = credibilityScore.doubleValue() / 100.0;
        
        double finalScore = (baseScore + recentBonus + trendBonus) * credibilityMultiplier;
        this.frequencyScore = BigDecimal.valueOf(Math.min(finalScore, 1000.0));
    }

    /**
     * Mark as inactive (no longer asked by company).
     */
    public void deactivate() {
        this.isActive = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProblemCompany that)) return false;
        return problemId != null && problemId.equals(that.problemId) &&
               companyId != null && companyId.equals(that.companyId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ProblemCompany{" +
                "problemId=" + problemId +
                ", companyId=" + companyId +
                ", frequency=" + frequency +
                ", lastAsked=" + lastAsked +
                ", askCount=" + askCount +
                ", frequencyScore=" + frequencyScore +
                ", recent6mCount=" + recent6mCount +
                ", isActive=" + isActive +
                '}';
    }
}