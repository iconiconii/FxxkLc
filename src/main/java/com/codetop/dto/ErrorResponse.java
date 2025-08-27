package com.codetop.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response DTO for API endpoints.
 * 
 * Provides consistent error format across all API responses.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
public class ErrorResponse {
    
    /**
     * Error timestamp.
     */
    private LocalDateTime timestamp;
    
    /**
     * HTTP status code.
     */
    private Integer status;
    
    /**
     * Error code for programmatic handling.
     */
    private String error;
    
    /**
     * Human-readable error message.
     */
    private String message;
    
    /**
     * Request path that caused the error.
     */
    private String path;
    
    /**
     * Additional error details.
     */
    private String details;
    
    /**
     * Field-specific validation errors.
     */
    private Map<String, String> fieldErrors;
    
    /**
     * Request ID for tracing (optional).
     */
    private String requestId;
}