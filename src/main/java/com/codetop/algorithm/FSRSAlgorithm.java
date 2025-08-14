package com.codetop.algorithm;

import com.codetop.dto.FSRSCalculationResult;
import com.codetop.dto.FSRSParametersDTO;
import com.codetop.entity.FSRSCard;
import com.codetop.entity.ReviewLog;

import java.util.List;

/**
 * Interface for the FSRS (Free Spaced Repetition Scheduler) algorithm.
 * 
 * The FSRS algorithm is an advanced spaced repetition system that uses:
 * - Difficulty: How hard the card is for the user (0-10 scale)
 * - Stability: How long the card can be retained in memory (in days)
 * - Retrievability: Current probability of successful recall
 * - 4-level rating system: Again(1), Hard(2), Good(3), Easy(4)
 * 
 * Key improvements over traditional algorithms:
 * - Personalized parameters based on user review history
 * - Scientific approach based on memory research
 * - Optimization for 90% retention rate by default
 * - Support for different learning patterns
 * 
 * @author CodeTop Team
 */
public interface FSRSAlgorithm {

    /**
     * Calculate the next review schedule for a card based on rating.
     * 
     * This is the core FSRS calculation that:
     * 1. Updates card difficulty based on performance
     * 2. Calculates new stability based on current state and rating
     * 3. Determines next review time for target retention rate
     * 4. Updates card state according to FSRS state machine
     * 
     * @param card The current FSRS card state
     * @param rating The user's rating (1=Again, 2=Hard, 3=Good, 4=Easy)
     * @param parameters User's personalized FSRS parameters
     * @return Calculation result with new card state and schedule
     */
    FSRSCalculationResult calculateNextReview(FSRSCard card, Integer rating, FSRSParametersDTO parameters);

    /**
     * Calculate initial difficulty for a new card based on first rating.
     * 
     * @param rating First rating given to the card
     * @param parameters User's FSRS parameters
     * @return Initial difficulty value
     */
    double calculateInitialDifficulty(Integer rating, FSRSParametersDTO parameters);

    /**
     * Calculate initial stability for a new card based on first rating.
     * 
     * @param rating First rating given to the card
     * @param parameters User's FSRS parameters
     * @return Initial stability value in days
     */
    double calculateInitialStability(Integer rating, FSRSParametersDTO parameters);

    /**
     * Calculate retrievability (probability of successful recall) for a card.
     * 
     * @param card The FSRS card
     * @param parameters User's FSRS parameters
     * @return Retrievability between 0.0 and 1.0
     */
    double calculateRetrievability(FSRSCard card, FSRSParametersDTO parameters);

    /**
     * Optimize FSRS parameters based on user's review history.
     * 
     * Uses gradient descent or other optimization algorithms to find
     * parameters that minimize prediction error for the user's data.
     * 
     * @param reviewLogs User's historical review data
     * @param currentParameters Current parameter values (can be defaults)
     * @return Optimized parameters
     */
    FSRSParametersDTO optimizeParameters(List<ReviewLog> reviewLogs, FSRSParametersDTO currentParameters);

    /**
     * Predict the optimal review time for target retention rate.
     * 
     * @param stability Current stability of the card
     * @param targetRetention Desired retention probability (e.g., 0.9)
     * @return Number of days until review
     */
    double predictOptimalInterval(double stability, double targetRetention);

    /**
     * Validate FSRS parameters to ensure they're within acceptable ranges.
     * 
     * @param parameters Parameters to validate
     * @return true if parameters are valid
     */
    boolean validateParameters(FSRSParametersDTO parameters);

    /**
     * Get default FSRS parameters suitable for most users.
     * 
     * @return Default parameter set
     */
    FSRSParametersDTO getDefaultParameters();

    /**
     * Calculate the next four possible review intervals for a card.
     * This helps users understand the consequences of different ratings.
     * 
     * @param card Current card state
     * @param parameters User's FSRS parameters
     * @return Array of intervals for [Again, Hard, Good, Easy] ratings
     */
    int[] calculateAllIntervals(FSRSCard card, FSRSParametersDTO parameters);
}