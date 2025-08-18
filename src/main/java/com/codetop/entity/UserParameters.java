package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * User Parameters entity for storing personalized FSRS algorithm parameters.
 * 
 * Features:
 * - Personalized FSRS parameters optimized for individual users
 * - 17 weight parameters (w0-w16) for FSRS algorithm
 * - Additional FSRS configuration parameters
 * - Optimization metadata and performance tracking
 * 
 * @author CodeTop Team
 */
@TableName("user_fsrs_parameters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserParameters extends BaseEntity {

    @NotNull
    @TableField("user_id")
    private Long userId;

    // FSRS Weight Parameters (w0-w16)
    @TableField("w0")
    @Builder.Default
    private BigDecimal w0 = BigDecimal.valueOf(0.4);

    @TableField("w1")
    @Builder.Default
    private BigDecimal w1 = BigDecimal.valueOf(0.6);

    @TableField("w2")
    @Builder.Default
    private BigDecimal w2 = BigDecimal.valueOf(2.4);

    @TableField("w3")
    @Builder.Default
    private BigDecimal w3 = BigDecimal.valueOf(5.8);

    @TableField("w4")
    @Builder.Default
    private BigDecimal w4 = BigDecimal.valueOf(4.93);

    @TableField("w5")
    @Builder.Default
    private BigDecimal w5 = BigDecimal.valueOf(0.94);

    @TableField("w6")
    @Builder.Default
    private BigDecimal w6 = BigDecimal.valueOf(0.86);

    @TableField("w7")
    @Builder.Default
    private BigDecimal w7 = BigDecimal.valueOf(0.01);

    @TableField("w8")
    @Builder.Default
    private BigDecimal w8 = BigDecimal.valueOf(1.49);

    @TableField("w9")
    @Builder.Default
    private BigDecimal w9 = BigDecimal.valueOf(0.14);

    @TableField("w10")
    @Builder.Default
    private BigDecimal w10 = BigDecimal.valueOf(0.94);

    @TableField("w11")
    @Builder.Default
    private BigDecimal w11 = BigDecimal.valueOf(2.18);

    @TableField("w12")
    @Builder.Default
    private BigDecimal w12 = BigDecimal.valueOf(0.05);

    @TableField("w13")
    @Builder.Default
    private BigDecimal w13 = BigDecimal.valueOf(0.34);

    @TableField("w14")
    @Builder.Default
    private BigDecimal w14 = BigDecimal.valueOf(1.26);

    @TableField("w15")
    @Builder.Default
    private BigDecimal w15 = BigDecimal.valueOf(0.29);

    @TableField("w16")
    @Builder.Default
    private BigDecimal w16 = BigDecimal.valueOf(2.61);

    // FSRS Configuration Parameters
    @TableField("request_retention")
    @Builder.Default
    private BigDecimal requestRetention = BigDecimal.valueOf(0.9);

    @TableField("maximum_interval")
    @Builder.Default
    private Integer maximumInterval = 36500;

    @TableField("easy_bonus")
    @Builder.Default
    private BigDecimal easyBonus = BigDecimal.valueOf(1.3);

    @TableField("hard_interval")
    @Builder.Default
    private BigDecimal hardInterval = BigDecimal.valueOf(1.2);

    @TableField("new_interval")
    @Builder.Default
    private BigDecimal newInterval = BigDecimal.valueOf(0.0);

    @TableField("graduating_interval")
    @Builder.Default
    private Integer graduatingInterval = 1;

    // Optimization Tracking
    @TableField("review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @TableField("optimization_accuracy")
    private BigDecimal optimizationAccuracy;

    @TableField("optimization_method")
    @Builder.Default
    private String optimizationMethod = "DEFAULT";

    @TableField("optimization_iterations")
    private Integer optimizationIterations;

    @TableField("optimization_loss")
    private BigDecimal optimizationLoss;

    @TableField("learning_rate")
    private BigDecimal learningRate;

    @TableField("regularization")
    private BigDecimal regularization;

    @TableField("convergence_threshold")
    private BigDecimal convergenceThreshold;

    @TableField("performance_improvement")
    private BigDecimal performanceImprovement;

    @TableField("confidence_score")
    private BigDecimal confidenceScore;

    // Status Fields
    @TableField("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @TableField("is_optimized")
    @Builder.Default
    private Boolean isOptimized = false;

    @TableField("version")
    @Builder.Default
    private String version = "FSRS-4.5";

    @TableField("last_optimized")
    private LocalDateTime lastOptimized;

    // Association (not mapped to database in MyBatis-Plus)
    @TableField(exist = false)
    @JsonIgnore
    private User user;

    // Utility Methods
    
    /**
     * Get parameter array for FSRS calculations.
     */
    public double[] getParameterArray() {
        return new double[]{
            w0.doubleValue(), w1.doubleValue(), w2.doubleValue(), w3.doubleValue(),
            w4.doubleValue(), w5.doubleValue(), w6.doubleValue(), w7.doubleValue(),
            w8.doubleValue(), w9.doubleValue(), w10.doubleValue(), w11.doubleValue(),
            w12.doubleValue(), w13.doubleValue(), w14.doubleValue(), w15.doubleValue(),
            w16.doubleValue()
        };
    }

    /**
     * Set parameter array from FSRS calculations.
     */
    public void setParameterArray(double[] params) {
        if (params == null || params.length != 17) {
            throw new IllegalArgumentException("Parameter array must contain exactly 17 values");
        }
        
        w0 = BigDecimal.valueOf(params[0]);
        w1 = BigDecimal.valueOf(params[1]);
        w2 = BigDecimal.valueOf(params[2]);
        w3 = BigDecimal.valueOf(params[3]);
        w4 = BigDecimal.valueOf(params[4]);
        w5 = BigDecimal.valueOf(params[5]);
        w6 = BigDecimal.valueOf(params[6]);
        w7 = BigDecimal.valueOf(params[7]);
        w8 = BigDecimal.valueOf(params[8]);
        w9 = BigDecimal.valueOf(params[9]);
        w10 = BigDecimal.valueOf(params[10]);
        w11 = BigDecimal.valueOf(params[11]);
        w12 = BigDecimal.valueOf(params[12]);
        w13 = BigDecimal.valueOf(params[13]);
        w14 = BigDecimal.valueOf(params[14]);
        w15 = BigDecimal.valueOf(params[15]);
        w16 = BigDecimal.valueOf(params[16]);
    }

    /**
     * Get default FSRS parameters.
     */
    public static double[] getDefaultParameters() {
        return new double[]{
            0.4, 0.6, 2.4, 5.8, 4.93, 0.94, 0.86, 0.01, 1.49, 0.14, 0.94, 2.18, 0.05, 0.34, 1.26, 0.29, 2.61
        };
    }

    /**
     * Create default parameters for a user.
     */
    public static UserParameters createDefault(Long userId) {
        return UserParameters.builder()
                .userId(userId)
                .version("FSRS-4.5")
                .optimizationMethod("DEFAULT")
                .reviewCount(0)
                .isActive(true)
                .isOptimized(false)
                .build();
    }

    // Derived methods
    public boolean needsReoptimization() {
        return lastOptimized != null && 
               lastOptimized.isBefore(LocalDateTime.now().minusDays(30)) && 
               reviewCount > 0;
    }

    public boolean isPerformingWell() {
        return performanceImprovement != null && 
               performanceImprovement.compareTo(BigDecimal.valueOf(5.0)) > 0; // 5% improvement
    }

    public int getDaysSinceOptimization() {
        if (lastOptimized == null) return Integer.MAX_VALUE;
        return (int) java.time.Duration.between(lastOptimized, LocalDateTime.now()).toDays();
    }

    /**
     * Deactivate current parameters (when new ones are created).
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Activate these parameters.
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Update review count after new reviews.
     */
    public void incrementReviewCount(int newReviews) {
        this.reviewCount += newReviews;
    }

    /**
     * Check if parameters are significantly different from defaults.
     */
    public boolean isDifferentFromDefaults() {
        double[] defaultParams = getDefaultParameters();
        double[] currentParams = getParameterArray();
        
        for (int i = 0; i < defaultParams.length; i++) {
            if (Math.abs(currentParams[i] - defaultParams[i]) > 0.1) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Mark as optimized with optimization metadata.
     */
    public void markAsOptimized(BigDecimal accuracy, Integer iterations, BigDecimal loss) {
        this.isOptimized = true;
        this.optimizationAccuracy = accuracy;
        this.optimizationIterations = iterations;
        this.optimizationLoss = loss;
        this.lastOptimized = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "UserParameters{" +
                "id=" + getId() +
                ", userId=" + userId +
                ", version='" + version + '\'' +
                ", reviewCount=" + reviewCount +
                ", isActive=" + isActive +
                ", isOptimized=" + isOptimized +
                ", lastOptimized=" + lastOptimized +
                ", performanceImprovement=" + performanceImprovement +
                '}';
    }
}