package com.codetop.enums;

/**
 * Review type enumeration for different kinds of review sessions.
 * 
 * Review Types:
 * - SCHEDULED: Regular FSRS-scheduled review
 * - EXTRA: Additional practice outside of schedule
 * - CRAM: Intensive review session before interviews
 * - MANUAL: User-initiated review
 * - BULK: Batch review session
 * 
 * @author CodeTop Team
 */
public enum ReviewType {
    SCHEDULED,
    EXTRA,
    CRAM,
    MANUAL,
    BULK;

    /**
     * Get display name for UI.
     */
    public String getDisplayName() {
        return switch (this) {
            case SCHEDULED -> "Scheduled Review";
            case EXTRA -> "Extra Practice";
            case CRAM -> "Cram Session";
            case MANUAL -> "Manual Review";
            case BULK -> "Bulk Review";
        };
    }

    /**
     * Get description for tooltips.
     */
    public String getDescription() {
        return switch (this) {
            case SCHEDULED -> "Regular spaced repetition review";
            case EXTRA -> "Additional practice outside of schedule";
            case CRAM -> "Intensive review before interviews";
            case MANUAL -> "User-initiated review";
            case BULK -> "Batch review session";
        };
    }

    /**
     * Check if review type affects FSRS scheduling.
     */
    public boolean affectsScheduling() {
        return this == SCHEDULED || this == MANUAL;
    }

    /**
     * Check if review type is for intensive practice.
     */
    public boolean isIntensivePractice() {
        return this == CRAM || this == BULK;
    }

    /**
     * Get weight factor for analytics (how much this review type counts).
     */
    public double getAnalyticsWeight() {
        return switch (this) {
            case SCHEDULED -> 1.0;
            case MANUAL -> 1.0;
            case EXTRA -> 0.8;
            case CRAM -> 0.6;
            case BULK -> 0.5;
        };
    }
}