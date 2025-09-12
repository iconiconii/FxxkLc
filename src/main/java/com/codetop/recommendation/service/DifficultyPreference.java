package com.codetop.recommendation.service;

/**
 * Difficulty preferences for problem recommendations.
 * Allows users to specify their preferred difficulty level.
 */
public enum DifficultyPreference {
    /**
     * Easy problems to build confidence.
     */
    EASY("EASY"),
    
    /**
     * Medium difficulty problems for balanced learning.
     */
    MEDIUM("MEDIUM"),
    
    /**
     * Hard problems for advanced challenge.
     */
    HARD("HARD");
    
    private final String value;
    
    DifficultyPreference(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static DifficultyPreference fromString(String value) {
        if (value == null) return null;
        
        for (DifficultyPreference difficulty : values()) {
            if (difficulty.value.equalsIgnoreCase(value)) {
                return difficulty;
            }
        }
        return null;
    }
}