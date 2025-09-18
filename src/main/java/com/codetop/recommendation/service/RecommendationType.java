package com.codetop.recommendation.service;

/**
 * Enumeration of recommendation types supported by the system.
 * 
 * This enum defines different strategies for generating problem recommendations:
 * - AI: Pure AI-powered recommendations using LLM
 * - FSRS: Traditional FSRS-based recommendations
 * - HYBRID: Combined AI + FSRS recommendations (default)
 * - AUTO: System automatically chooses the best strategy based on user profile and availability
 */
public enum RecommendationType {
    AI("ai", "Pure AI-powered recommendations using language models"),
    FSRS("fsrs", "Traditional FSRS spaced repetition recommendations"),
    HYBRID("hybrid", "Combined AI and FSRS recommendations"),
    AUTO("auto", "System automatically chooses optimal recommendation strategy");

    private final String value;
    private final String description;

    RecommendationType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Parse recommendation type from string value.
     * 
     * @param value String representation of the recommendation type
     * @return RecommendationType enum value
     * @throws IllegalArgumentException if value is not recognized
     */
    public static RecommendationType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return HYBRID; // Default to hybrid for null/empty values
        }
        
        for (RecommendationType type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("Unknown recommendation type: " + value);
    }

    /**
     * Get the default recommendation type for new users or fallback scenarios.
     * 
     * @return Default recommendation type (HYBRID)
     */
    public static RecommendationType getDefault() {
        return HYBRID;
    }

    /**
     * Check if this recommendation type requires AI services.
     * 
     * @return true if AI services are needed, false otherwise
     */
    public boolean requiresAI() {
        return this == AI || this == HYBRID || this == AUTO;
    }

    /**
     * Check if this recommendation type can fallback to FSRS.
     * 
     * @return true if FSRS fallback is supported, false otherwise
     */
    public boolean supportsFsrsFallback() {
        return this == HYBRID || this == AUTO || this == FSRS;
    }

    @Override
    public String toString() {
        return value;
    }
}