package com.codetop.exception;

/**
 * Exception thrown when a requested resource is not found.
 * 
 * @author CodeTop Team
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}