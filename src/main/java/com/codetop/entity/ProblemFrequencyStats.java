package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Problem frequency statistics entity for CodeTop-style analytics.
 * 
 * Aggregated statistics table for efficient querying and ranking.
 * Supports multiple scopes: global, company, department, position.
 * 
 * @author CodeTop Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("problem_frequency_stats")
public class ProblemFrequencyStats {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Problem ID (logical reference to problems.id)
     */
    @TableField("problem_id")
    private Long problemId;

    /**
     * Company ID (null for global stats)
     */
    @TableField("company_id")
    private Long companyId;

    /**
     * Department ID (null for company-wide stats)
     */
    @TableField("department_id")
    private Long departmentId;

    /**
     * Position ID (null for department-wide stats)
     */
    @TableField("position_id")
    private Long positionId;

    /**
     * Weighted frequency score
     */
    @TableField("total_frequency_score")
    private BigDecimal totalFrequencyScore;

    /**
     * Total number of times asked
     */
    @TableField("interview_count")
    private Integer interviewCount;

    /**
     * Number of unique interviewers
     */
    @TableField("unique_interviewers")
    private Integer uniqueInterviewers;

    /**
     * Most recent interview date
     */
    @TableField("last_asked_date")
    private LocalDate lastAskedDate;

    /**
     * Earliest recorded interview date
     */
    @TableField("first_asked_date")
    private LocalDate firstAskedDate;

    /**
     * Recent trend analysis
     */
    @TableField("frequency_trend")
    private FrequencyTrend frequencyTrend;

    /**
     * Average difficulty rating from interviews
     */
    @TableField("avg_difficulty_rating")
    private BigDecimal avgDifficultyRating;

    /**
     * Success rate percentage
     */
    @TableField("success_rate")
    private BigDecimal successRate;

    /**
     * Average time to solve in minutes
     */
    @TableField("avg_solve_time_minutes")
    private Integer avgSolveTimeMinutes;

    /**
     * Rank by frequency within scope
     */
    @TableField("frequency_rank")
    private Integer frequencyRank;

    /**
     * Frequency percentile (0-100)
     */
    @TableField("percentile")
    private BigDecimal percentile;

    /**
     * Statistics scope level
     */
    @TableField("stats_scope")
    private StatsScope statsScope;

    /**
     * Date when statistics were calculated
     */
    @TableField("calculation_date")
    private LocalDate calculationDate;

    /**
     * Record creation time
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Last update time
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * Frequency trend enumeration
     */
    public enum FrequencyTrend {
        INCREASING,
        STABLE,
        DECREASING,
        NEW
    }

    /**
     * Statistics scope enumeration
     */
    public enum StatsScope {
        GLOBAL,
        COMPANY,
        DEPARTMENT,
        POSITION
    }

    // Business logic methods

    /**
     * Check if this problem is trending (high frequency + recent activity)
     */
    public boolean isTrending() {
        return frequencyTrend == FrequencyTrend.INCREASING && 
               lastAskedDate != null && 
               lastAskedDate.isAfter(LocalDate.now().minusMonths(3));
    }

    /**
     * Check if this problem is in top percentile
     */
    public boolean isTopPercentile() {
        return percentile != null && percentile.compareTo(new BigDecimal("90")) >= 0;
    }

    /**
     * Check if this problem is highly asked recently
     */
    public boolean isHotProblem() {
        return isTrending() && isTopPercentile();
    }

    /**
     * Get difficulty level based on average rating
     */
    public String getDifficultyLevel() {
        if (avgDifficultyRating == null) return "UNKNOWN";
        
        double rating = avgDifficultyRating.doubleValue();
        if (rating <= 2.0) return "EASY";
        if (rating <= 3.5) return "MEDIUM";
        return "HARD";
    }

    /**
     * Calculate recency score (higher for more recent problems)
     */
    public double getRecencyScore() {
        if (lastAskedDate == null) return 0.0;
        
        long daysSinceLastAsked = LocalDate.now().toEpochDay() - lastAskedDate.toEpochDay();
        
        // Exponential decay: score decreases as days increase
        return Math.exp(-daysSinceLastAsked / 90.0) * 100.0; // 90-day half-life
    }

    /**
     * Get comprehensive ranking score combining frequency, recency, and trend
     */
    public double getComprehensiveScore() {
        double frequencyWeight = totalFrequencyScore != null ? totalFrequencyScore.doubleValue() : 0.0;
        double recencyWeight = getRecencyScore() * 0.3;
        double trendWeight = frequencyTrend == FrequencyTrend.INCREASING ? 20.0 : 0.0;
        
        return frequencyWeight + recencyWeight + trendWeight;
    }
}