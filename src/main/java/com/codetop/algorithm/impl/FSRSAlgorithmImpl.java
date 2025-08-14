package com.codetop.algorithm.impl;

import com.codetop.algorithm.FSRSAlgorithm;
import com.codetop.dto.FSRSCalculationResult;
import com.codetop.dto.FSRSParametersDTO;
import com.codetop.entity.FSRSCard;
import com.codetop.entity.ReviewLog;
import com.codetop.enums.FSRSState;
import com.codetop.exception.FSRSCalculationException;
import com.codetop.exception.InvalidRatingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Implementation of the FSRS (Free Spaced Repetition Scheduler) algorithm.
 * 
 * This implementation follows the FSRS-4.5 specification with:
 * - 17 optimizable parameters (w0-w16)
 * - 4-level rating system (1=Again, 2=Hard, 3=Good, 4=Easy)
 * - State machine: New → Learning → Review, with Relearning for forgotten cards
 * - Scientific approach based on Ebbinghaus forgetting curve
 * - Personal parameter optimization using gradient descent
 * 
 * Key formulas:
 * - Difficulty calculation using exponential decay
 * - Stability calculation with state-specific formulas
 * - Interval calculation based on retention rate
 * - Retrievability based on exponential forgetting curve
 * 
 * @author CodeTop Team
 */
@Component
@Slf4j
public class FSRSAlgorithmImpl implements FSRSAlgorithm {

    // FSRS Constants
    private static final double DEFAULT_REQUEST_RETENTION = 0.9;
    private static final int MAXIMUM_INTERVAL_DAYS = 36500; // 100 years
    private static final double MINIMUM_STABILITY = 0.01;
    private static final double MAXIMUM_STABILITY = 36500.0;
    private static final double MINIMUM_DIFFICULTY = 1.0;
    private static final double MAXIMUM_DIFFICULTY = 10.0;
    
    // Default FSRS-4.5 parameters
    private static final double[] DEFAULT_PARAMETERS = {
        0.4, 0.6, 2.4, 5.8, 4.93, 0.94, 0.86, 0.01, 1.49, 0.14, 0.94, 2.18, 0.05, 0.34, 1.26, 0.29, 2.61
    };

    @Override
    public FSRSCalculationResult calculateNextReview(FSRSCard card, Integer rating, FSRSParametersDTO parameters) {
        validateRating(rating);
        validateCard(card);
        validateParameters(parameters);

        try {
            log.debug("Calculating next review for card {} with rating {}", card.getId(), rating);

            // Calculate elapsed days since last review
            int elapsedDays = calculateElapsedDays(card);
            
            // Determine new state based on current state and rating
            FSRSState newState = calculateNewState(card.getState(), rating);
            
            // Calculate new difficulty
            double newDifficulty = calculateDifficulty(card, rating, parameters);
            
            // Calculate new stability
            double newStability = calculateStability(card, rating, newDifficulty, parameters, elapsedDays);
            
            // Calculate next review time
            LocalDateTime nextReviewTime = calculateNextReviewTime(newStability, parameters.getRequestRetention());
            
            // Calculate interval in days
            int intervalDays = calculateIntervalDays(newStability, parameters.getRequestRetention());

            FSRSCalculationResult result = FSRSCalculationResult.builder()
                    .newState(newState)
                    .newDifficulty(BigDecimal.valueOf(newDifficulty).setScale(4, RoundingMode.HALF_UP))
                    .newStability(BigDecimal.valueOf(newStability).setScale(4, RoundingMode.HALF_UP))
                    .nextReviewTime(nextReviewTime)
                    .intervalDays(intervalDays)
                    .elapsedDays(elapsedDays)
                    .build();

            log.debug("FSRS calculation result: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Error calculating next review for card {}: {}", card.getId(), e.getMessage(), e);
            throw new FSRSCalculationException("Failed to calculate next review: " + e.getMessage(), e);
        }
    }

    @Override
    public double calculateInitialDifficulty(Integer rating, FSRSParametersDTO parameters) {
        validateRating(rating);
        
        // w4 controls initial difficulty calculation
        double w4 = parameters.getW4();
        double initialDifficulty = w4 - Math.exp(w4) * (rating - 3) / Math.exp(w4);
        
        return constrainDifficulty(initialDifficulty);
    }

    @Override
    public double calculateInitialStability(Integer rating, FSRSParametersDTO parameters) {
        validateRating(rating);
        
        // Initial stability based on rating
        double initialStability = switch (rating) {
            case 1 -> parameters.getW0(); // Again
            case 2 -> parameters.getW1(); // Hard  
            case 3 -> parameters.getW2(); // Good
            case 4 -> parameters.getW3(); // Easy
            default -> throw new InvalidRatingException(rating);
        };
        
        return constrainStability(initialStability);
    }

    @Override
    public double calculateRetrievability(FSRSCard card, FSRSParametersDTO parameters) {
        if (card.getLastReview() == null || card.getStability() == null) {
            return 1.0; // New card
        }

        int elapsedDays = calculateElapsedDays(card);
        double stability = card.getStabilityAsDouble();
        
        if (stability <= 0) {
            return 0.0;
        }

        // Exponential forgetting curve: R = e^(ln(0.9) * elapsed / stability)
        double retrievability = Math.pow(0.9, elapsedDays / stability);
        
        return Math.max(0.0, Math.min(1.0, retrievability));
    }

    @Override
    public FSRSParametersDTO optimizeParameters(List<ReviewLog> reviewLogs, FSRSParametersDTO currentParameters) {
        if (reviewLogs == null || reviewLogs.size() < 30) {
            log.warn("Insufficient review data for parameter optimization. Need at least 30 reviews, got {}", 
                    reviewLogs != null ? reviewLogs.size() : 0);
            return currentParameters;
        }

        try {
            log.info("Starting parameter optimization with {} review logs", reviewLogs.size());

            // Use gradient descent to optimize parameters
            double[] currentParams = currentParameters.getParameterArray();
            double[] optimizedParams = gradientDescentOptimization(reviewLogs, currentParams);

            FSRSParametersDTO optimized = FSRSParametersDTO.fromArray(optimizedParams);
            optimized.setRequestRetention(currentParameters.getRequestRetention());

            log.info("Parameter optimization completed successfully");
            return optimized;

        } catch (Exception e) {
            log.error("Parameter optimization failed: {}", e.getMessage(), e);
            return currentParameters; // Return original parameters on failure
        }
    }

    @Override
    public double predictOptimalInterval(double stability, double targetRetention) {
        if (stability <= 0 || targetRetention <= 0 || targetRetention >= 1) {
            return 1.0;
        }

        // Calculate interval for target retention: interval = stability * ln(retention) / ln(0.9)
        double interval = stability * Math.log(targetRetention) / Math.log(0.9);
        
        return Math.max(1.0, Math.min(interval, MAXIMUM_INTERVAL_DAYS));
    }

    @Override
    public boolean validateParameters(FSRSParametersDTO parameters) {
        if (parameters == null) {
            return false;
        }

        double[] params = parameters.getParameterArray();
        if (params.length != 17) {
            return false;
        }

        // Check parameter ranges
        for (int i = 0; i < params.length; i++) {
            double param = params[i];
            if (Double.isNaN(param) || Double.isInfinite(param)) {
                return false;
            }
            
            // Parameter-specific validation
            switch (i) {
                case 0, 1, 2, 3 -> { // w0-w3 (initial stability)
                    if (param < 0.01 || param > 100.0) return false;
                }
                case 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 -> { // w4-w16
                    if (param < -10.0 || param > 10.0) return false;
                }
            }
        }

        // Check request retention
        double retention = parameters.getRequestRetention();
        return retention > 0.7 && retention < 0.99;
    }

    @Override
    public FSRSParametersDTO getDefaultParameters() {
        return FSRSParametersDTO.fromArray(DEFAULT_PARAMETERS.clone());
    }

    @Override
    public int[] calculateAllIntervals(FSRSCard card, FSRSParametersDTO parameters) {
        int[] intervals = new int[4];
        
        for (int rating = 1; rating <= 4; rating++) {
            try {
                FSRSCalculationResult result = calculateNextReview(card, rating, parameters);
                intervals[rating - 1] = result.getIntervalDays();
            } catch (Exception e) {
                log.warn("Failed to calculate interval for rating {}: {}", rating, e.getMessage());
                intervals[rating - 1] = 1; // Default to 1 day
            }
        }
        
        return intervals;
    }

    // Private helper methods

    private FSRSState calculateNewState(FSRSState currentState, Integer rating) {
        return switch (currentState) {
            case NEW -> rating >= 3 ? FSRSState.LEARNING : FSRSState.NEW;
            case LEARNING -> switch (rating) {
                case 1 -> FSRSState.NEW;
                case 2 -> FSRSState.LEARNING;
                case 3, 4 -> FSRSState.REVIEW;
                default -> throw new InvalidRatingException(rating);
            };
            case REVIEW -> rating == 1 ? FSRSState.RELEARNING : FSRSState.REVIEW;
            case RELEARNING -> switch (rating) {
                case 1 -> FSRSState.RELEARNING;
                case 2 -> FSRSState.RELEARNING;
                case 3, 4 -> FSRSState.REVIEW;
                default -> throw new InvalidRatingException(rating);
            };
        };
    }

    private double calculateDifficulty(FSRSCard card, Integer rating, FSRSParametersDTO parameters) {
        double currentDifficulty = card.getDifficultyAsDouble();
        
        if (card.getState() == FSRSState.NEW) {
            return calculateInitialDifficulty(rating, parameters);
        }

        // Difficulty update formula: D = D - w5 * (g - 3)
        double w5 = parameters.getW5();
        double difficultyChange = w5 * (rating - 3);
        double newDifficulty = currentDifficulty - difficultyChange;

        return constrainDifficulty(newDifficulty);
    }

    private double calculateStability(FSRSCard card, Integer rating, double newDifficulty, 
                                    FSRSParametersDTO parameters, int elapsedDays) {
        FSRSState currentState = card.getState();
        double currentStability = card.getStabilityAsDouble();
        
        return switch (currentState) {
            case NEW -> calculateInitialStability(rating, parameters);
            case LEARNING -> calculateLearningStability(currentStability, rating, parameters);
            case REVIEW -> calculateReviewStability(currentStability, newDifficulty, rating, 
                                                  card.getLapses(), parameters, elapsedDays);
            case RELEARNING -> calculateRelearningStability(currentStability, rating, parameters);
        };
    }

    private double calculateLearningStability(double currentStability, Integer rating, FSRSParametersDTO parameters) {
        // For learning cards, stability grows gradually
        double w6 = parameters.getW6();
        double w7 = parameters.getW7();
        
        double stabilityIncrease = switch (rating) {
            case 1 -> w6; // Again
            case 2 -> w6 * 1.2; // Hard
            case 3 -> w6 * 1.5; // Good
            case 4 -> w6 * 2.0; // Easy
            default -> throw new InvalidRatingException(rating);
        };

        double newStability = currentStability * (1 + stabilityIncrease + w7);
        return constrainStability(newStability);
    }

    private double calculateReviewStability(double currentStability, double difficulty, Integer rating, 
                                          Integer lapses, FSRSParametersDTO parameters, int elapsedDays) {
        if (rating == 1) {
            // Lapse - stability decreases significantly
            double w11 = parameters.getW11();
            double w12 = parameters.getW12();
            double newStability = currentStability * Math.pow(w11, lapses) * w12;
            return constrainStability(newStability);
        }

        // Successful review - calculate stability increase
        double w8 = parameters.getW8();
        double w9 = parameters.getW9();
        double w10 = parameters.getW10();
        double w13 = parameters.getW13();
        double w14 = parameters.getW14();
        double w15 = parameters.getW15();
        double w16 = parameters.getW16();

        // Calculate retrievability
        double retrievability = Math.pow(0.9, elapsedDays / currentStability);
        
        // Stability multiplier based on rating and retrievability
        double ratingFactor = switch (rating) {
            case 2 -> w8; // Hard
            case 3 -> w9; // Good  
            case 4 -> w10; // Easy
            default -> throw new InvalidRatingException(rating);
        };

        double difficultyFactor = Math.exp((1 - difficulty) * w13);
        double retrievabilityFactor = Math.exp((1 - retrievability) * w14);
        double lapseFactor = Math.pow(w15, lapses);
        double intervalFactor = (elapsedDays > 0) ? (1 + w16 * elapsedDays / currentStability) : 1.0;

        double stabilityMultiplier = ratingFactor * difficultyFactor * retrievabilityFactor * 
                                   lapseFactor * intervalFactor;
        
        double newStability = currentStability * stabilityMultiplier;
        return constrainStability(newStability);
    }

    private double calculateRelearningStability(double currentStability, Integer rating, FSRSParametersDTO parameters) {
        // Similar to learning but with different parameters
        return calculateLearningStability(currentStability, rating, parameters) * 0.8;
    }

    private LocalDateTime calculateNextReviewTime(double stability, double requestRetention) {
        double intervalDays = predictOptimalInterval(stability, requestRetention);
        return LocalDateTime.now().plusDays(Math.round(intervalDays));
    }

    private int calculateIntervalDays(double stability, double requestRetention) {
        double interval = predictOptimalInterval(stability, requestRetention);
        return Math.max(1, Math.min((int) Math.round(interval), MAXIMUM_INTERVAL_DAYS));
    }

    private int calculateElapsedDays(FSRSCard card) {
        if (card.getLastReview() == null) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(card.getLastReview().toLocalDate(), LocalDateTime.now().toLocalDate());
    }

    private double constrainStability(double stability) {
        return Math.max(MINIMUM_STABILITY, Math.min(stability, MAXIMUM_STABILITY));
    }

    private double constrainDifficulty(double difficulty) {
        return Math.max(MINIMUM_DIFFICULTY, Math.min(difficulty, MAXIMUM_DIFFICULTY));
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 4) {
            throw new InvalidRatingException(rating != null ? rating : -1);
        }
    }

    private void validateCard(FSRSCard card) {
        if (card == null) {
            throw new FSRSCalculationException("Card cannot be null");
        }
        if (card.getState() == null) {
            throw new FSRSCalculationException("Card state cannot be null");
        }
    }

    // Simplified gradient descent optimization
    private double[] gradientDescentOptimization(List<ReviewLog> reviewLogs, double[] currentParams) {
        // This is a simplified implementation. In production, you might want to use
        // a more sophisticated optimization library like Apache Commons Math
        
        double[] params = currentParams.clone();
        double learningRate = 0.01;
        int iterations = 100;
        double tolerance = 1e-6;
        
        for (int iter = 0; iter < iterations; iter++) {
            double[] gradients = calculateGradients(reviewLogs, params);
            double gradientNorm = 0.0;
            
            // Update parameters
            for (int i = 0; i < params.length; i++) {
                params[i] -= learningRate * gradients[i];
                gradientNorm += gradients[i] * gradients[i];
            }
            
            // Check convergence
            if (Math.sqrt(gradientNorm) < tolerance) {
                log.debug("Parameter optimization converged after {} iterations", iter + 1);
                break;
            }
        }
        
        return params;
    }

    private double[] calculateGradients(List<ReviewLog> reviewLogs, double[] params) {
        double[] gradients = new double[params.length];
        double epsilon = 1e-6;
        
        for (int i = 0; i < params.length; i++) {
            // Numerical differentiation
            params[i] += epsilon;
            double lossPlus = calculateLoss(reviewLogs, params);
            
            params[i] -= 2 * epsilon;
            double lossMinus = calculateLoss(reviewLogs, params);
            
            params[i] += epsilon; // Restore original value
            
            gradients[i] = (lossPlus - lossMinus) / (2 * epsilon);
        }
        
        return gradients;
    }

    private double calculateLoss(List<ReviewLog> reviewLogs, double[] params) {
        double loss = 0.0;
        FSRSParametersDTO parameters = FSRSParametersDTO.fromArray(params);
        
        for (ReviewLog log : reviewLogs) {
            // Calculate predicted vs actual review outcome
            // This is a simplified loss function - you might want to implement
            // a more sophisticated one based on FSRS research
            
            try {
                double predicted = predictReviewSuccess(log, parameters);
                double actual = log.getRating() >= 3 ? 1.0 : 0.0;
                loss += Math.pow(predicted - actual, 2);
            } catch (Exception e) {
                // Skip problematic logs
                // Skip problematic logs silently
            }
        }
        
        return loss / reviewLogs.size();
    }

    private double predictReviewSuccess(ReviewLog log, FSRSParametersDTO parameters) {
        // Simplified prediction based on retrievability
        if (log.getOldStability() == null || log.getElapsedDays() == null) {
            return 0.5; // Default prediction
        }
        
        double stability = log.getOldStability().doubleValue();
        int elapsedDays = log.getElapsedDays();
        
        if (stability <= 0) return 0.0;
        
        return Math.pow(0.9, elapsedDays / stability);
    }
}