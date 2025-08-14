package com.codetop.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Comprehensive input sanitization to prevent XSS and injection attacks.
 * 
 * Features:
 * - XSS prevention through HTML/script tag removal
 * - SQL injection prevention markers
 * - Path traversal attack prevention
 * - Whitelist-based validation approach
 * - Content length validation
 * - Special character handling
 * 
 * @author CodeTop Team
 */
@Component
public class InputSanitizer {

    // Patterns for detecting malicious input
    private static final Pattern HTML_PATTERN = Pattern.compile("<[^>]+>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        ".*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|declare|cast|convert)" +
        "\\s+(.*)(from|into|table|database|schema|index|procedure|function).*", 
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(".*(\\.\\./|\\.\\\\).*");
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(".*(;|\\||&|\\$\\(|`).*");

    // Safe character patterns
    private static final Pattern SAFE_ALPHANUMERIC = Pattern.compile("^[a-zA-Z0-9\\s._-]+$");
    private static final Pattern SAFE_EMAIL = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern SAFE_USERNAME = Pattern.compile("^[a-zA-Z0-9._\\-\\u4e00-\\u9fa5\\u3040-\\u309f\\u30a0-\\u30ff]{3,50}$");

    // Maximum lengths for different input types
    private static final int MAX_TEXT_LENGTH = 1000;
    private static final int MAX_DESCRIPTION_LENGTH = 5000;
    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_EMAIL_LENGTH = 320;

    /**
     * Sanitize general text input by removing potentially dangerous content.
     */
    public String sanitizeText(String input) {
        if (StringUtils.isBlank(input)) {
            return "";
        }

        // Trim and check length
        String sanitized = input.trim();
        if (sanitized.length() > MAX_TEXT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_TEXT_LENGTH);
        }

        // Remove HTML tags and scripts
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = HTML_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Encode special characters
        sanitized = sanitized.replace("<", "&lt;")
                            .replace(">", "&gt;")
                            .replace("\"", "&quot;")
                            .replace("'", "&#x27;")
                            .replace("&", "&amp;");

        return sanitized.trim();
    }

    /**
     * Validate and sanitize email input.
     */
    public String sanitizeEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return "";
        }

        String sanitized = email.trim().toLowerCase();
        
        if (sanitized.length() > MAX_EMAIL_LENGTH) {
            throw new ValidationException("Email address is too long");
        }

        if (!SAFE_EMAIL.matcher(sanitized).matches()) {
            throw new ValidationException("Invalid email format");
        }

        return sanitized;
    }

    /**
     * Validate and sanitize username input.
     */
    public String sanitizeUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return "";
        }

        String sanitized = username.trim();

        if (!SAFE_USERNAME.matcher(sanitized).matches()) {
            throw new ValidationException("Username contains invalid characters or is invalid length");
        }

        return sanitized;
    }

    /**
     * Sanitize description/content that may contain rich text.
     */
    public String sanitizeDescription(String description) {
        if (StringUtils.isBlank(description)) {
            return "";
        }

        String sanitized = description.trim();
        
        if (sanitized.length() > MAX_DESCRIPTION_LENGTH) {
            sanitized = sanitized.substring(0, MAX_DESCRIPTION_LENGTH);
        }

        // Check for dangerous patterns
        validateNotMalicious(sanitized);

        // Allow some basic formatting but escape dangerous content
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        return sanitized;
    }

    /**
     * Validate that input doesn't contain SQL injection patterns.
     */
    public void validateNotSqlInjection(String input) {
        if (StringUtils.isBlank(input)) {
            return;
        }

        if (SQL_INJECTION_PATTERN.matcher(input).matches()) {
            throw new SecurityException("Input contains potential SQL injection patterns");
        }
    }

    /**
     * Validate that input doesn't contain path traversal patterns.
     */
    public void validateNotPathTraversal(String input) {
        if (StringUtils.isBlank(input)) {
            return;
        }

        if (PATH_TRAVERSAL_PATTERN.matcher(input).matches()) {
            throw new SecurityException("Input contains potential path traversal patterns");
        }
    }

    /**
     * Validate that input doesn't contain command injection patterns.
     */
    public void validateNotCommandInjection(String input) {
        if (StringUtils.isBlank(input)) {
            return;
        }

        if (COMMAND_INJECTION_PATTERN.matcher(input).matches()) {
            throw new SecurityException("Input contains potential command injection patterns");
        }
    }

    /**
     * Comprehensive validation to detect various malicious patterns.
     */
    public void validateNotMalicious(String input) {
        validateNotSqlInjection(input);
        validateNotPathTraversal(input);
        validateNotCommandInjection(input);

        // Check for script content
        if (SCRIPT_PATTERN.matcher(input).find()) {
            throw new SecurityException("Input contains script content");
        }

        // Check for javascript protocol
        if (JAVASCRIPT_PATTERN.matcher(input).find()) {
            throw new SecurityException("Input contains javascript protocol");
        }
    }

    /**
     * Validate alphanumeric input only.
     */
    public String sanitizeAlphanumeric(String input) {
        if (StringUtils.isBlank(input)) {
            return "";
        }

        String sanitized = input.trim();
        
        if (!SAFE_ALPHANUMERIC.matcher(sanitized).matches()) {
            throw new ValidationException("Input contains invalid characters");
        }

        return sanitized;
    }

    /**
     * Sanitize ID values (positive integers only).
     */
    public Long sanitizeId(String idStr) {
        if (StringUtils.isBlank(idStr)) {
            throw new ValidationException("ID cannot be empty");
        }

        try {
            long id = Long.parseLong(idStr.trim());
            if (id <= 0) {
                throw new ValidationException("ID must be positive");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid ID format");
        }
    }

    /**
     * Sanitize and validate pagination parameters.
     */
    public int sanitizePaginationParameter(String paramStr, int defaultValue, int maxValue) {
        if (StringUtils.isBlank(paramStr)) {
            return defaultValue;
        }

        try {
            int value = Integer.parseInt(paramStr.trim());
            if (value < 1) {
                return defaultValue;
            }
            if (value > maxValue) {
                return maxValue;
            }
            return value;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Custom validation exception.
     */
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}