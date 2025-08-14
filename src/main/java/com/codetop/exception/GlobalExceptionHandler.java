package com.codetop.exception;

import com.codetop.validation.InputSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive global exception handler with security-first approach.
 * 
 * Features:
 * - Structured error responses with correlation IDs
 * - Security event logging for suspicious activities
 * - User-friendly messages without exposing sensitive details
 * - Performance monitoring and metrics collection
 * - Comprehensive error categorization and handling
 * 
 * @author CodeTop Team
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Security and validation exception handlers

    @ExceptionHandler(FSRSException.class)
    public ResponseEntity<ErrorResponse> handleFSRSException(
            FSRSException ex, HttpServletRequest request) {
        
        logSecurityEventIfNeeded(ex, request);
        
        HttpStatus status = getHttpStatusForFSRSException(ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(ex.getErrorCode().name())
                .errorCode(ex.getCode())
                .message(ex.getUserMessage()) // Use user-friendly message
                .correlationId(ex.getCorrelationId())
                .path(request.getRequestURI())
                .severity(ex.getSeverity().name())
                .build();

        // Log based on severity
        logErrorBySeverity(ex, errorResponse);

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(InputSanitizer.ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            InputSanitizer.ValidationException ex, HttpServletRequest request) {
        
        // Log security event for potential injection attempt
        log.warn("SECURITY_EVENT: INPUT_VALIDATION_FAILED ip={} path={} message={}", 
                getClientIp(request), request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("INVALID_INPUT")
                .errorCode(ErrorCodes.INVALID_INPUT.getCode())
                .message("The provided input contains invalid characters or format")
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .severity("MEDIUM")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(
            SecurityException ex, HttpServletRequest request) {
        
        // Log critical security event
        log.error("SECURITY_EVENT: CRITICAL_SECURITY_VIOLATION ip={} path={} message={}", 
                getClientIp(request), request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("SECURITY_VIOLATION")
                .errorCode(ErrorCodes.SECURITY_VIOLATION.getCode())
                .message("Security policy violation detected")
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .severity("CRITICAL")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // Authentication and authorization exceptions

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        
        log.warn("SECURITY_EVENT: AUTH_FAILED ip={} path={} reason={}", 
                getClientIp(request), request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("INVALID_CREDENTIALS")
                .errorCode(ErrorCodes.INVALID_CREDENTIALS.getCode())
                .message(ex.getMessage()) // Use the actual exception message
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .severity("MEDIUM")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        log.warn("SECURITY_EVENT: AUTH_EXCEPTION ip={} path={} type={}", 
                getClientIp(request), request.getRequestURI(), ex.getClass().getSimpleName());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("AUTHENTICATION_FAILED")
                .errorCode(ErrorCodes.TOKEN_INVALID.getCode())
                .message("Authentication failed")
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .severity("MEDIUM")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        log.warn("SECURITY_EVENT: ACCESS_DENIED ip={} path={}", 
                getClientIp(request), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("ACCESS_DENIED")
                .errorCode(ErrorCodes.INSUFFICIENT_PERMISSIONS.getCode())
                .message("Access denied to the requested resource")
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .severity("MEDIUM")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // Data validation exceptions

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_FAILED")
                .errorCode(ErrorCodes.INVALID_INPUT.getCode())
                .message("Input validation failed")
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .details(errors)
                .severity("LOW")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("MALFORMED_REQUEST")
                .errorCode(ErrorCodes.INVALID_FORMAT.getCode())
                .message("Malformed JSON request")
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .severity("LOW")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("INVALID_PARAMETER_TYPE")
                .errorCode(ErrorCodes.INVALID_FORMAT.getCode())
                .message(String.format("Invalid value for parameter '%s'", ex.getName()))
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .severity("LOW")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("MISSING_PARAMETER")
                .errorCode(ErrorCodes.MISSING_REQUIRED_FIELD.getCode())
                .message(String.format("Missing required parameter: %s", ex.getParameterName()))
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .severity("LOW")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Database exceptions

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        
        log.error("Database integrity violation: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("DATA_INTEGRITY_VIOLATION")
                .errorCode(ErrorCodes.CONSTRAINT_VIOLATION.getCode())
                .message("Operation violates data integrity constraints")
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .severity("MEDIUM")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKeyException(
            DuplicateKeyException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("DUPLICATE_RESOURCE")
                .errorCode(ErrorCodes.DUPLICATE_RESOURCE.getCode())
                .message("Resource already exists")
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .severity("LOW")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    // HTTP method and upload exceptions

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("METHOD_NOT_SUPPORTED")
                .errorCode(ErrorCodes.OPERATION_NOT_ALLOWED.getCode())
                .message(String.format("Method %s is not supported for this endpoint", ex.getMethod()))
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .severity("LOW")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .error("FILE_TOO_LARGE")
                .errorCode(ErrorCodes.VALUE_OUT_OF_RANGE.getCode())
                .message("File size exceeds maximum allowed limit")
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .severity("LOW")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    // Generic exception handlers

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        // Log if this is likely a security-related issue
        if ("Account does not exist".equals(ex.getMessage())) {
            log.warn("SECURITY_EVENT: ACCOUNT_NOT_FOUND ip={} path={}", 
                    getClientIp(request), request.getRequestURI());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("INVALID_ARGUMENT")
                .errorCode(ErrorCodes.INVALID_INPUT.getCode())
                .message(ex.getMessage()) // Use the actual exception message
                .correlationId(generateCorrelationId())
                .path(request.getRequestURI())
                .severity("LOW")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        
        // Log full error details for debugging
        log.error("SYSTEM_ERROR: Unexpected error occurred correlationId={} ip={} path={}", 
                correlationId, getClientIp(request), request.getRequestURI(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .errorCode(ErrorCodes.SERVICE_UNAVAILABLE.getCode())
                .message("An unexpected error occurred. Please try again later.")
                .correlationId(correlationId)
                .path(request.getRequestURI())
                .severity("HIGH")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Helper methods

    private HttpStatus getHttpStatusForFSRSException(FSRSException ex) {
        return switch (ex.getErrorCode().getCode() / 100) {
            case 10 -> HttpStatus.UNAUTHORIZED;        // Authentication
            case 11 -> HttpStatus.FORBIDDEN;           // Authorization
            case 12 -> HttpStatus.BAD_REQUEST;         // Validation
            case 13 -> HttpStatus.BAD_REQUEST;         // Business Logic
            case 14 -> HttpStatus.INTERNAL_SERVER_ERROR; // System
            case 15 -> HttpStatus.FORBIDDEN;           // Security
            case 16 -> HttpStatus.BAD_REQUEST;         // FSRS
            case 17 -> HttpStatus.CONFLICT;            // Data Integrity
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private void logSecurityEventIfNeeded(FSRSException ex, HttpServletRequest request) {
        if (ex.isSecurityError()) {
            log.error("SECURITY_EVENT: {} ip={} path={} correlationId={}", 
                    ex.getErrorCode(), getClientIp(request), request.getRequestURI(), ex.getCorrelationId());
        }
    }

    private void logErrorBySeverity(FSRSException ex, ErrorResponse response) {
        switch (ex.getSeverity()) {
            case CRITICAL -> log.error("CRITICAL: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);
            case HIGH -> log.error("HIGH: {} - {}", ex.getErrorCode(), ex.getMessage());
            case MEDIUM -> log.warn("MEDIUM: {} - {}", ex.getErrorCode(), ex.getMessage());
            case LOW -> log.info("LOW: {} - {}", ex.getErrorCode(), ex.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString();
    }

    @lombok.Data
    @lombok.Builder
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private Integer errorCode;
        private String message;
        private String correlationId;
        private String path;
        private String severity;
        private Map<String, String> details;
    }
}