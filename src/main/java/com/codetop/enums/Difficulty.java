package com.codetop.enums;

/**
 * Problem difficulty enumeration.
 * 
 * Difficulty levels:
 * - EASY: Basic problems suitable for beginners
 * - MEDIUM: Intermediate problems requiring solid understanding
 * - HARD: Advanced problems requiring expert knowledge
 * 
 * @author CodeTop Team
 */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD;

    /**
     * Get difficulty level as integer (1=Easy, 2=Medium, 3=Hard).
     */
    public int getLevel() {
        return ordinal() + 1;
    }

    /**
     * Get difficulty from level integer.
     */
    public static Difficulty fromLevel(int level) {
        return switch (level) {
            case 1 -> EASY;
            case 2 -> MEDIUM;
            case 3 -> HARD;
            default -> throw new IllegalArgumentException("Invalid difficulty level: " + level);
        };
    }

    /**
     * Check if this difficulty is easier than another.
     */
    public boolean isEasierThan(Difficulty other) {
        return this.getLevel() < other.getLevel();
    }

    /**
     * Check if this difficulty is harder than another.
     */
    public boolean isHarderThan(Difficulty other) {
        return this.getLevel() > other.getLevel();
    }
}