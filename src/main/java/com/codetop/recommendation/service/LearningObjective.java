package com.codetop.recommendation.service;

/**
 * Learning objectives for AI recommendation strategies.
 * These guide the LLM on what type of recommendations to prioritize.
 */
public enum LearningObjective {
    /**
     * Focus on strengthening weak knowledge domains.
     * Prioritizes problems in areas where the user struggles.
     */
    WEAKNESS_FOCUS("weakness_focus"),
    
    /**
     * Progressive difficulty advancement.
     * Gradually increases problem difficulty to build confidence.
     */
    PROGRESSIVE_DIFFICULTY("progressive_difficulty"),
    
    /**
     * Comprehensive topic coverage.
     * Explores diverse domains to broaden knowledge.
     */
    TOPIC_COVERAGE("topic_coverage"),
    
    /**
     * Exam preparation mode.
     * Focuses on high-frequency and important problems.
     */
    EXAM_PREP("exam_prep"),
    
    /**
     * Refresh mastered concepts.
     * Reviews previously learned material to maintain retention.
     */
    REFRESH_MASTERED("refresh_mastered");
    
    private final String value;
    
    LearningObjective(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static LearningObjective fromString(String value) {
        if (value == null) return null;
        
        for (LearningObjective objective : values()) {
            if (objective.value.equals(value)) {
                return objective;
            }
        }
        return null;
    }
}