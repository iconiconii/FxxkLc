package com.codetop.dto;

import com.codetop.enums.FSRSState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing the result of an FSRS calculation.
 * 
 * Contains all the computed values needed to update a card after review:
 * - New FSRS state
 * - Updated difficulty and stability values
 * - Next review schedule
 * - Interval information
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FSRSCalculationResult {

    private FSRSState newState;
    private BigDecimal newDifficulty;
    private BigDecimal newStability;
    private LocalDateTime nextReviewTime;
    private Integer intervalDays;
    private Integer elapsedDays;
    private Double retrievability;
    private Double stabilityChange;
    private Double difficultyChange;

    /**
     * Get difficulty as double value.
     */
    public double getDifficultyAsDouble() {
        return newDifficulty != null ? newDifficulty.doubleValue() : 0.0;
    }

    /**
     * Get stability as double value.
     */
    public double getStabilityAsDouble() {
        return newStability != null ? newStability.doubleValue() : 0.0;
    }

    /**
     * Check if the card state changed.
     */
    public boolean hasStateChange(FSRSState oldState) {
        return oldState != newState;
    }

    /**
     * Check if the card graduated to review state.
     */
    public boolean hasGraduated(FSRSState oldState) {
        return oldState != FSRSState.REVIEW && newState == FSRSState.REVIEW;
    }

    /**
     * Check if the card lapsed (went to relearning).
     */
    public boolean hasLapsed() {
        return newState == FSRSState.RELEARNING;
    }

    /**
     * Get a summary of the calculation result.
     */
    public String getSummary() {
        return String.format("State: %s, Difficulty: %.2f, Stability: %.2f, Next review in %d days",
                newState, getDifficultyAsDouble(), getStabilityAsDouble(), intervalDays);
    }
}