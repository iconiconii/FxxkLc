package com.codetop.enums;

/**
 * FSRS State enumeration representing the learning states in the spaced repetition system.
 * 
 * States in the FSRS workflow:
 * - NEW: Card has never been reviewed
 * - LEARNING: Card is in initial learning phase (short intervals)
 * - REVIEW: Card has graduated to review phase (longer intervals)
 * - RELEARNING: Card was forgotten and needs to be relearned
 * 
 * State Transitions:
 * NEW → LEARNING (first review with Good/Easy)
 * NEW → NEW (first review with Again/Hard)
 * LEARNING → REVIEW (successful learning completion)
 * LEARNING → LEARNING (continue learning with short intervals)
 * REVIEW → REVIEW (successful review)
 * REVIEW → RELEARNING (forgotten, needs relearning)
 * RELEARNING → REVIEW (successful relearning)
 * 
 * @author CodeTop Team
 */
public enum FSRSState {
    NEW,
    LEARNING,
    REVIEW,
    RELEARNING;

    /**
     * Get display name for UI.
     */
    public String getDisplayName() {
        return switch (this) {
            case NEW -> "New";
            case LEARNING -> "Learning";
            case REVIEW -> "Review";
            case RELEARNING -> "Relearning";
        };
    }

    /**
     * Get CSS class for styling.
     */
    public String getCssClass() {
        return switch (this) {
            case NEW -> "state-new";
            case LEARNING -> "state-learning";
            case REVIEW -> "state-review";
            case RELEARNING -> "state-relearning";
        };
    }

    /**
     * Get color for UI representation.
     */
    public String getColor() {
        return switch (this) {
            case NEW -> "#6c757d"; // gray
            case LEARNING -> "#fd7e14"; // orange
            case REVIEW -> "#28a745"; // green
            case RELEARNING -> "#dc3545"; // red
        };
    }

    /**
     * Check if state is in active learning phase.
     */
    public boolean isActivelyLearning() {
        return this == LEARNING || this == RELEARNING;
    }

    /**
     * Check if state has graduated from initial learning.
     */
    public boolean hasGraduated() {
        return this == REVIEW;
    }

    /**
     * Check if card needs immediate attention.
     */
    public boolean needsImmediateAttention() {
        return this == NEW || this == RELEARNING;
    }

    /**
     * Get next possible states based on review rating.
     */
    public FSRSState getNextState(int rating) {
        return switch (this) {
            case NEW -> rating >= 3 ? LEARNING : NEW;
            case LEARNING -> rating >= 3 ? REVIEW : (rating == 1 ? NEW : LEARNING);
            case REVIEW -> rating == 1 ? RELEARNING : REVIEW;
            case RELEARNING -> rating >= 3 ? REVIEW : (rating == 1 ? RELEARNING : RELEARNING);
        };
    }

    /**
     * Get minimum interval days for this state.
     */
    public int getMinIntervalDays() {
        return switch (this) {
            case NEW -> 0;
            case LEARNING -> 1;
            case REVIEW -> 1;
            case RELEARNING -> 1;
        };
    }

    /**
     * Check if state allows long-term intervals.
     */
    public boolean allowsLongIntervals() {
        return this == REVIEW;
    }
}