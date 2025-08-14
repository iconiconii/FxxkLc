package com.codetop.enums;

/**
 * Problem frequency enumeration for company-problem associations.
 * 
 * Frequency levels:
 * - LOW: Rarely asked (1-9 times)
 * - MEDIUM: Moderately asked (10-19 times)
 * - HIGH: Frequently asked (20+ times)
 * 
 * @author CodeTop Team
 */
public enum ProblemFrequency {
    LOW,
    MEDIUM,
    HIGH;

    /**
     * Get frequency level as integer (1=Low, 2=Medium, 3=High).
     */
    public int getLevel() {
        return ordinal() + 1;
    }

    /**
     * Get frequency from ask count.
     */
    public static ProblemFrequency fromAskCount(int askCount) {
        if (askCount >= 20) {
            return HIGH;
        } else if (askCount >= 10) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }

    /**
     * Get display name for UI.
     */
    public String getDisplayName() {
        return switch (this) {
            case LOW -> "Low";
            case MEDIUM -> "Medium";
            case HIGH -> "High";
        };
    }

    /**
     * Get CSS class for styling.
     */
    public String getCssClass() {
        return switch (this) {
            case LOW -> "frequency-low";
            case MEDIUM -> "frequency-medium";
            case HIGH -> "frequency-high";
        };
    }
}