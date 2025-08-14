package com.codetop.exception;

/**
 * Centralized error codes for the FSRS application.
 * 
 * Error code ranges:
 * - 1000-1099: Authentication errors
 * - 1100-1199: Authorization errors  
 * - 1200-1299: Validation errors
 * - 1300-1399: Business logic errors
 * - 1400-1499: System errors
 * - 1500-1599: Security errors
 * - 1600-1699: FSRS-specific errors
 * - 1700-1799: Data integrity errors
 * 
 * @author CodeTop Team
 */
public enum ErrorCodes {
    
    // Authentication Errors (1000-1099)
    INVALID_CREDENTIALS(1001, "Invalid username or password"),
    TOKEN_EXPIRED(1002, "Authentication token has expired"),
    TOKEN_INVALID(1003, "Authentication token is invalid"),
    RATE_LIMIT_EXCEEDED(1004, "Too many requests, please try again later"),
    ACCOUNT_LOCKED(1005, "Account has been temporarily locked"),
    MFA_REQUIRED(1006, "Multi-factor authentication required"),
    MFA_INVALID(1007, "Invalid multi-factor authentication code"),
    PASSWORD_EXPIRED(1008, "Password has expired and must be changed"),
    
    // Authorization Errors (1100-1199)
    INSUFFICIENT_PERMISSIONS(1101, "Insufficient permissions for this operation"),
    RESOURCE_ACCESS_DENIED(1102, "Access denied to the requested resource"),
    QUOTA_EXCEEDED(1103, "User quota exceeded for this resource"),
    OPERATION_NOT_ALLOWED(1104, "Operation not allowed in current state"),
    ADMIN_REQUIRED(1105, "Administrator privileges required"),
    
    // Validation Errors (1200-1299)
    INVALID_INPUT(1201, "Input data is invalid"),
    MISSING_REQUIRED_FIELD(1202, "Required field is missing"),
    CONSTRAINT_VIOLATION(1203, "Data constraint violation"),
    DUPLICATE_RESOURCE(1204, "Resource already exists"),
    INVALID_FORMAT(1205, "Data format is invalid"),
    VALUE_OUT_OF_RANGE(1206, "Value is outside acceptable range"),
    INVALID_EMAIL_FORMAT(1207, "Email address format is invalid"),
    INVALID_PASSWORD_FORMAT(1208, "Password does not meet security requirements"),
    
    // Business Logic Errors (1300-1399)
    FSRS_CALCULATION_FAILED(1301, "FSRS calculation failed"),
    INSUFFICIENT_REVIEW_DATA(1302, "Insufficient review data for operation"),
    PARAMETER_OPTIMIZATION_FAILED(1303, "Parameter optimization failed"),
    REVIEW_SESSION_EXPIRED(1304, "Review session has expired"),
    INVALID_RATING(1305, "Invalid rating value provided"),
    CARD_NOT_FOUND(1306, "FSRS card not found"),
    USER_NOT_FOUND(1307, "User not found"),
    PROBLEM_NOT_FOUND(1308, "Problem not found"),
    OPTIMIZATION_NOT_READY(1309, "User not ready for parameter optimization"),
    
    // System Errors (1400-1499)
    DATABASE_ERROR(1401, "Database operation failed"),
    EXTERNAL_SERVICE_ERROR(1402, "External service unavailable"),
    CACHE_ERROR(1403, "Cache operation failed"),
    FILE_SYSTEM_ERROR(1404, "File system operation failed"),
    CONFIGURATION_ERROR(1405, "System configuration error"),
    SERVICE_UNAVAILABLE(1406, "Service temporarily unavailable"),
    RESOURCE_EXHAUSTED(1407, "System resources exhausted"),
    TIMEOUT_ERROR(1408, "Operation timed out"),
    
    // Security Errors (1500-1599)
    SECURITY_VIOLATION(1501, "Security policy violation detected"),
    SUSPICIOUS_ACTIVITY(1502, "Suspicious activity detected"),
    DATA_INTEGRITY_ERROR(1503, "Data integrity violation"),
    ENCRYPTION_ERROR(1504, "Encryption/decryption failed"),
    INJECTION_ATTEMPT(1505, "Potential injection attack detected"),
    XSS_ATTEMPT(1506, "Potential XSS attack detected"),
    CSRF_TOKEN_INVALID(1507, "CSRF token is invalid or missing"),
    IP_BLOCKED(1508, "IP address has been blocked"),
    
    // FSRS-specific Errors (1600-1699)
    INVALID_FSRS_STATE(1601, "Invalid FSRS card state"),
    INVALID_STABILITY_VALUE(1602, "Invalid stability value"),
    INVALID_DIFFICULTY_VALUE(1603, "Invalid difficulty value"),
    INVALID_INTERVAL(1604, "Invalid review interval"),
    PARAMETER_VALIDATION_FAILED(1605, "FSRS parameter validation failed"),
    OPTIMIZATION_CONVERGENCE_FAILED(1606, "Parameter optimization did not converge"),
    INSUFFICIENT_TRAINING_DATA(1607, "Insufficient training data for optimization"),
    ALGORITHM_VERSION_MISMATCH(1608, "FSRS algorithm version mismatch"),
    
    // Data Integrity Errors (1700-1799)
    CONCURRENT_MODIFICATION(1701, "Resource was modified by another process"),
    REFERENTIAL_INTEGRITY_VIOLATION(1702, "Database referential integrity violation"),
    UNIQUE_CONSTRAINT_VIOLATION(1703, "Unique constraint violation"),
    FOREIGN_KEY_VIOLATION(1704, "Foreign key constraint violation"),
    DATA_CORRUPTION_DETECTED(1705, "Data corruption detected"),
    BACKUP_RESTORE_FAILED(1706, "Backup or restore operation failed"),
    MIGRATION_FAILED(1707, "Database migration failed");
    
    private final int code;
    private final String defaultMessage;
    
    ErrorCodes(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDefaultMessage() {
        return defaultMessage;
    }
    
    /**
     * Get error code by numeric value.
     */
    public static ErrorCodes fromCode(int code) {
        for (ErrorCodes errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException("Unknown error code: " + code);
    }
    
    /**
     * Check if error code represents a security-related error.
     */
    public boolean isSecurityError() {
        return code >= 1500 && code < 1600;
    }
    
    /**
     * Check if error code represents a system error.
     */
    public boolean isSystemError() {
        return code >= 1400 && code < 1500;
    }
    
    /**
     * Check if error code represents a validation error.
     */
    public boolean isValidationError() {
        return code >= 1200 && code < 1300;
    }
    
    /**
     * Get severity level for error code.
     */
    public ErrorSeverity getSeverity() {
        return switch (code / 100) {
            case 10, 11 -> ErrorSeverity.MEDIUM;  // Auth/Authorization
            case 12 -> ErrorSeverity.LOW;         // Validation
            case 13 -> ErrorSeverity.MEDIUM;      // Business Logic
            case 14 -> ErrorSeverity.HIGH;        // System
            case 15 -> ErrorSeverity.CRITICAL;    // Security
            case 16 -> ErrorSeverity.MEDIUM;      // FSRS
            case 17 -> ErrorSeverity.HIGH;        // Data Integrity
            default -> ErrorSeverity.MEDIUM;
        };
    }
    
    public enum ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}