package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * User Parameters entity for storing personalized FSRS algorithm parameters.
 * 
 * Features:
 * - Personalized FSRS parameters optimized for individual users
 * - Parameter history tracking for A/B testing
 * - Optimization metadata (training count, optimization timestamp)
 * - JSON storage for flexible parameter structure
 * - Support for multiple parameter sets per user
 * 
 * @author CodeTop Team
 */
@TableName("user_parameters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserParameters extends BaseEntity {

    @NotNull
    @TableField("user_id")
    private Long userId;

    @TableField("parameters")
    private Map<String, Double> parameters;

    @Builder.Default
    @TableField("optimized_at")
    private LocalDateTime optimizedAt = LocalDateTime.now();

    @Builder.Default
    @TableField("training_count")
    private Integer trainingCount = 0;

    @Builder.Default
    @TableField("is_active")
    private Boolean isActive = true;

    @TableField("version")
    private String version; // e.g., "FSRS-4.5"

    @TableField("optimization_method")
    private String optimizationMethod; // e.g., "GRADIENT_DESCENT", "LBFGS"

    @TableField("loss_value")
    private Double lossValue; // Final loss value from optimization

    @TableField("iterations")
    private Integer iterations; // Number of optimization iterations

    @TableField("convergence_threshold")
    private Double convergenceThreshold;

    @TableField("learning_rate")
    private Double learningRate;

    @TableField("regularization")
    private Double regularization;

    @TableField("optimization_metadata")
    private Map<String, Object> optimizationMetadata;

    @TableField("performance_improvement")
    private Double performanceImprovement; // Percentage improvement over default

    @TableField("notes")
    private String notes;

    // Association (not mapped to database in MyBatis-Plus)
    @TableField(exist = false)
    @JsonIgnore
    private User user;

    // FSRS parameter getters with defaults
    public double getW0() {
        return parameters != null ? parameters.getOrDefault("w0", 0.4) : 0.4;
    }

    public double getW1() {
        return parameters != null ? parameters.getOrDefault("w1", 0.6) : 0.6;
    }

    public double getW2() {
        return parameters != null ? parameters.getOrDefault("w2", 2.4) : 2.4;
    }

    public double getW3() {
        return parameters != null ? parameters.getOrDefault("w3", 5.8) : 5.8;
    }

    public double getW4() {
        return parameters != null ? parameters.getOrDefault("w4", 4.93) : 4.93;
    }

    public double getW5() {
        return parameters != null ? parameters.getOrDefault("w5", 0.94) : 0.94;
    }

    public double getW6() {
        return parameters != null ? parameters.getOrDefault("w6", 0.86) : 0.86;
    }

    public double getW7() {
        return parameters != null ? parameters.getOrDefault("w7", 0.01) : 0.01;
    }

    public double getW8() {
        return parameters != null ? parameters.getOrDefault("w8", 1.49) : 1.49;
    }

    public double getW9() {
        return parameters != null ? parameters.getOrDefault("w9", 0.14) : 0.14;
    }

    public double getW10() {
        return parameters != null ? parameters.getOrDefault("w10", 0.94) : 0.94;
    }

    public double getW11() {
        return parameters != null ? parameters.getOrDefault("w11", 2.18) : 2.18;
    }

    public double getW12() {
        return parameters != null ? parameters.getOrDefault("w12", 0.05) : 0.05;
    }

    public double getW13() {
        return parameters != null ? parameters.getOrDefault("w13", 0.34) : 0.34;
    }

    public double getW14() {
        return parameters != null ? parameters.getOrDefault("w14", 1.26) : 1.26;
    }

    public double getW15() {
        return parameters != null ? parameters.getOrDefault("w15", 0.29) : 0.29;
    }

    public double getW16() {
        return parameters != null ? parameters.getOrDefault("w16", 2.61) : 2.61;
    }

    public double getRequestRetention() {
        return parameters != null ? parameters.getOrDefault("requestRetention", 0.9) : 0.9;
    }

    // Derived methods
    public boolean isOptimized() {
        return trainingCount >= 30 && parameters != null && !parameters.isEmpty();
    }

    public boolean needsReoptimization() {
        return optimizedAt.isBefore(LocalDateTime.now().minusDays(30)) && trainingCount > 0;
    }

    public boolean isPerformingWell() {
        return performanceImprovement != null && performanceImprovement > 5.0; // 5% improvement
    }

    public int getDaysSinceOptimization() {
        return (int) java.time.Duration.between(optimizedAt, LocalDateTime.now()).toDays();
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
     * Update training count after new reviews.
     */
    public void incrementTrainingCount(int newReviews) {
        this.trainingCount += newReviews;
    }

    /**
     * Check if parameters are significantly different from defaults.
     */
    public boolean isDifferentFromDefaults() {
        if (parameters == null || parameters.isEmpty()) return false;
        
        double[] defaultParams = {0.4, 0.6, 2.4, 5.8, 4.93, 0.94, 0.86, 0.01, 1.49, 0.14, 0.94, 2.18, 0.05, 0.34, 1.26, 0.29, 2.61};
        
        for (int i = 0; i < defaultParams.length; i++) {
            String key = "w" + i;
            Double value = parameters.get(key);
            if (value != null && Math.abs(value - defaultParams[i]) > 0.1) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Get parameter array for FSRS calculations.
     */
    public double[] getParameterArray() {
        if (parameters == null) return getDefaultParameters();
        
        double[] params = new double[17];
        for (int i = 0; i < 17; i++) {
            String key = "w" + i;
            params[i] = parameters.getOrDefault(key, getDefaultParameters()[i]);
        }
        
        return params;
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
        double[] defaultParams = getDefaultParameters();
        Map<String, Double> paramMap = new java.util.HashMap<>();
        
        for (int i = 0; i < defaultParams.length; i++) {
            paramMap.put("w" + i, defaultParams[i]);
        }
        paramMap.put("requestRetention", 0.9);
        
        return UserParameters.builder()
                .userId(userId)
                .parameters(paramMap)
                .version("FSRS-4.5")
                .optimizationMethod("DEFAULT")
                .trainingCount(0)
                .isActive(true)
                .notes("Default FSRS parameters")
                .build();
    }

    @Override
    public String toString() {
        return "UserParameters{" +
                "id=" + getId() +
                ", userId=" + userId +
                ", version='" + version + '\'' +
                ", trainingCount=" + trainingCount +
                ", isActive=" + isActive +
                ", optimizedAt=" + optimizedAt +
                ", performanceImprovement=" + performanceImprovement +
                '}';
    }
}