package com.codetop.exception;

/**
 * Base exception class for all FSRS-related errors.
 * 
 * Features:
 * - Structured error codes and messages
 * - Correlation ID support for request tracing
 * - User-friendly messages separate from technical details
 * - Support for nested exceptions and error context
 * 
 * @author CodeTop Team
 */
public class FSRSException extends RuntimeException {
    
    private final ErrorCodes errorCode;
    private final String correlationId;
    private final String userMessage;
    private final Object context;
    
    public FSRSException(ErrorCodes errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), (Throwable) null, null, null);
    }
    
    public FSRSException(ErrorCodes errorCode, String message) {
        this(errorCode, message, (Throwable) null, null, null);
    }
    
    public FSRSException(ErrorCodes errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, null, null);
    }
    
    public FSRSException(ErrorCodes errorCode, String message, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage != null ? userMessage : errorCode.getDefaultMessage();
        this.context = null;
        this.correlationId = generateCorrelationId();
    }
    
    public FSRSException(ErrorCodes errorCode, String message, Throwable cause, String userMessage, Object context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage != null ? userMessage : errorCode.getDefaultMessage();
        this.context = context;
        this.correlationId = generateCorrelationId();
    }
    
    public FSRSException(ErrorCodes errorCode, String message, String correlationId, String userMessage, Object context) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage != null ? userMessage : errorCode.getDefaultMessage();
        this.context = context;
        this.correlationId = correlationId != null ? correlationId : generateCorrelationId();
    }
    
    // Legacy constructors for backward compatibility
    public FSRSException(String message) {
        this(ErrorCodes.FSRS_CALCULATION_FAILED, message);
    }
    
    public FSRSException(String message, Throwable cause) {
        this(ErrorCodes.FSRS_CALCULATION_FAILED, message, cause);
    }
    
    public ErrorCodes getErrorCode() {
        return errorCode;
    }
    
    public int getCode() {
        return errorCode.getCode();
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public Object getContext() {
        return context;
    }
    
    public ErrorCodes.ErrorSeverity getSeverity() {
        return errorCode.getSeverity();
    }
    
    public boolean isSecurityError() {
        return errorCode.isSecurityError();
    }
    
    public boolean isSystemError() {
        return errorCode.isSystemError();
    }
    
    private String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString();
    }
    
    @Override
    public String toString() {
        return String.format("FSRSException{code=%d, message='%s', correlationId='%s', severity=%s}", 
                getCode(), getMessage(), correlationId, getSeverity());
    }
}