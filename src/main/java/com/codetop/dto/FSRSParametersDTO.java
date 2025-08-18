package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for FSRS algorithm parameters.
 * 
 * Contains all 17 FSRS parameters (w0-w16) plus request retention rate.
 * These parameters control how the algorithm behaves for different users.
 * 
 * Parameters can be:
 * - Default values suitable for most users
 * - Personalized values optimized from user's review history
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FSRSParametersDTO {

    // Initial stability parameters (w0-w3)
    @Builder.Default
    private Double w0 = 0.4;   // Initial stability for "Again"
    @Builder.Default
    private Double w1 = 0.6;   // Initial stability for "Hard"
    @Builder.Default
    private Double w2 = 2.4;   // Initial stability for "Good"
    @Builder.Default
    private Double w3 = 5.8;   // Initial stability for "Easy"

    // Difficulty and stability calculation parameters (w4-w16)
    @Builder.Default
    private Double w4 = 4.93;  // Initial difficulty calculation
    @Builder.Default
    private Double w5 = 0.94;  // Difficulty decay rate
    @Builder.Default
    private Double w6 = 0.86;  // Learning stability growth
    @Builder.Default
    private Double w7 = 0.01;  // Learning stability bonus
    @Builder.Default
    private Double w8 = 1.49;  // Review stability for "Hard"
    @Builder.Default
    private Double w9 = 0.14;  // Review stability for "Good"
    @Builder.Default
    private Double w10 = 0.94; // Review stability for "Easy"
    @Builder.Default
    private Double w11 = 2.18; // Lapse stability decay
    @Builder.Default
    private Double w12 = 0.05; // Lapse stability multiplier
    @Builder.Default
    private Double w13 = 0.34; // Difficulty factor in stability
    @Builder.Default
    private Double w14 = 1.26; // Retrievability factor in stability
    @Builder.Default
    private Double w15 = 0.29; // Lapse count factor
    @Builder.Default
    private Double w16 = 2.61; // Interval factor in stability

    // Target retention rate (typically 0.9 = 90%)
    @Builder.Default
    private Double requestRetention = 0.9;
    
    // Additional FSRS Configuration Parameters
    @Builder.Default
    private Integer maximumInterval = 36500;    // Maximum interval in days
    
    @Builder.Default
    private Double easyBonus = 1.3;             // Bonus multiplier for "Easy" button
    
    @Builder.Default
    private Double hardInterval = 1.2;          // Interval multiplier for "Hard" button

    /**
     * Create parameters from array.
     */
    public static FSRSParametersDTO fromArray(double[] params) {
        if (params.length < 17) {
            throw new IllegalArgumentException("Parameters array must have at least 17 elements");
        }

        return FSRSParametersDTO.builder()
                .w0(params[0])
                .w1(params[1])
                .w2(params[2])
                .w3(params[3])
                .w4(params[4])
                .w5(params[5])
                .w6(params[6])
                .w7(params[7])
                .w8(params[8])
                .w9(params[9])
                .w10(params[10])
                .w11(params[11])
                .w12(params[12])
                .w13(params[13])
                .w14(params[14])
                .w15(params[15])
                .w16(params[16])
                .build();
    }

    /**
     * Convert to array format.
     */
    public double[] getParameterArray() {
        return new double[] {
                w0, w1, w2, w3, w4, w5, w6, w7, w8, w9, w10, w11, w12, w13, w14, w15, w16
        };
    }

    /**
     * Get default parameters.
     */
    public static FSRSParametersDTO getDefault() {
        return FSRSParametersDTO.builder().build();
    }

    /**
     * Check if parameters are default values.
     */
    public boolean isDefault() {
        FSRSParametersDTO defaultParams = getDefault();
        double[] current = getParameterArray();
        double[] defaults = defaultParams.getParameterArray();
        
        for (int i = 0; i < current.length; i++) {
            if (Math.abs(current[i] - defaults[i]) > 0.01) {
                return false;
            }
        }
        
        return Math.abs(requestRetention - defaultParams.getRequestRetention()) < 0.01;
    }

    /**
     * Calculate similarity to another parameter set (0-1 scale).
     */
    public double calculateSimilarity(FSRSParametersDTO other) {
        if (other == null) return 0.0;
        
        double[] current = getParameterArray();
        double[] otherParams = other.getParameterArray();
        
        double sumSquaredDiff = 0.0;
        for (int i = 0; i < current.length; i++) {
            double diff = current[i] - otherParams[i];
            sumSquaredDiff += diff * diff;
        }
        
        // Add retention difference
        double retentionDiff = requestRetention - other.getRequestRetention();
        sumSquaredDiff += retentionDiff * retentionDiff * 100; // Weight retention more heavily
        
        // Convert to similarity score (higher = more similar)
        return Math.exp(-sumSquaredDiff / 10.0);
    }

    /**
     * Validate parameters are within acceptable ranges.
     */
    public boolean isValid() {
        double[] params = getParameterArray();
        
        // Check for NaN or infinite values
        for (double param : params) {
            if (Double.isNaN(param) || Double.isInfinite(param)) {
                return false;
            }
        }
        
        // Check specific parameter ranges
        if (w0 < 0.01 || w0 > 100) return false; // Initial stability for "Again"
        if (w1 < 0.01 || w1 > 100) return false; // Initial stability for "Hard"
        if (w2 < 0.01 || w2 > 100) return false; // Initial stability for "Good"
        if (w3 < 0.01 || w3 > 100) return false; // Initial stability for "Easy"
        
        // Other parameters should be within reasonable bounds
        for (int i = 4; i < 17; i++) {
            if (params[i] < -10.0 || params[i] > 10.0) return false;
        }
        
        // Request retention should be between 70% and 99%
        return requestRetention >= 0.7 && requestRetention < 0.99;
    }

    /**
     * Get a description of the parameter set.
     */
    public String getDescription() {
        if (isDefault()) {
            return "Default FSRS parameters suitable for most users";
        } else {
            return String.format("Personalized FSRS parameters (%.0f%% retention target)", 
                               requestRetention * 100);
        }
    }

    // Individual parameter getters (for backward compatibility)
    public double getW0() { return w0; }
    public double getW1() { return w1; }
    public double getW2() { return w2; }
    public double getW3() { return w3; }
    public double getW4() { return w4; }
    public double getW5() { return w5; }
    public double getW6() { return w6; }
    public double getW7() { return w7; }
    public double getW8() { return w8; }
    public double getW9() { return w9; }
    public double getW10() { return w10; }
    public double getW11() { return w11; }
    public double getW12() { return w12; }
    public double getW13() { return w13; }
    public double getW14() { return w14; }
    public double getW15() { return w15; }
    public double getW16() { return w16; }
    
    public double getRequestRetention() { return requestRetention; }
    public int getMaximumInterval() { return maximumInterval; }
    public double getEasyBonus() { return easyBonus; }
    public double getHardInterval() { return hardInterval; }

    @Override
    public String toString() {
        return String.format("FSRSParameters{retention=%.1f%%, w0=%.3f, w1=%.3f, w2=%.3f, w3=%.3f, ...}",
                           requestRetention * 100, w0, w1, w2, w3);
    }
}