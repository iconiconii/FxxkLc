package com.codetop.exception;

/**
 * Exception thrown when an invalid rating is provided to FSRS calculations.
 * 
 * Valid ratings are:
 * - 1 (Again)
 * - 2 (Hard)  
 * - 3 (Good)
 * - 4 (Easy)
 * 
 * @author CodeTop Team
 */
public class InvalidRatingException extends FSRSException {
    
    public InvalidRatingException(int rating) {
        super(String.format("Invalid rating: %d. Must be between 1 and 4 (1=Again, 2=Hard, 3=Good, 4=Easy)", rating));
    }
}