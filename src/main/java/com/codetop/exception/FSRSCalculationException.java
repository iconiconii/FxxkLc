package com.codetop.exception;

/**
 * Exception thrown when FSRS calculations fail.
 * 
 * @author CodeTop Team
 */
public class FSRSCalculationException extends FSRSException {
    
    public FSRSCalculationException(String message) {
        super(message);
    }
    
    public FSRSCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}