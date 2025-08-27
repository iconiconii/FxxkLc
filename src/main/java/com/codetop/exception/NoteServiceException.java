package com.codetop.exception;

/**
 * Exception thrown when note service operations fail.
 * 
 * This runtime exception is used for note-specific business logic failures,
 * such as data synchronization issues between MySQL and MongoDB.
 * 
 * @author CodeTop Team
 */
public class NoteServiceException extends RuntimeException {
    
    public NoteServiceException(String message) {
        super(message);
    }
    
    public NoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}