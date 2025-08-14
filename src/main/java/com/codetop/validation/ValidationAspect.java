package com.codetop.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * AOP aspect for automatic input validation and sanitization.
 * 
 * Features:
 * - Automatic validation on controller methods
 * - Deep object traversal for nested validation
 * - Performance-optimized validation
 * - Security event logging for suspicious input
 * - Integration with existing validation annotations
 * 
 * @author CodeTop Team
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationAspect {

    private final InputSanitizer inputSanitizer;

    /**
     * Validate input parameters for all controller methods.
     */
    @Before("execution(* com.codetop.controller..*(..))")
    public void validateControllerInput(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        
        if (args == null || args.length == 0) {
            return;
        }

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        try {
            for (Object arg : args) {
                if (arg != null) {
                    validateObject(arg, className + "." + methodName);
                }
            }
        } catch (Exception e) {
            log.warn("Input validation failed for {}.{}: {}", className, methodName, e.getMessage());
            throw new InputSanitizer.ValidationException("Input validation failed: " + e.getMessage());
        }
    }

    /**
     * Recursively validate object properties.
     */
    private void validateObject(Object obj, String context) {
        if (obj == null) {
            return;
        }

        Class<?> clazz = obj.getClass();

        // Skip primitive types and common framework objects
        if (shouldSkipValidation(clazz)) {
            return;
        }

        // Skip authentication and security-related objects
        if (isSecurityRelatedObject(obj)) {
            return;
        }

        // Validate string objects directly
        if (obj instanceof String) {
            validateString((String) obj, context);
            return;
        }

        // Handle collections
        if (obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) obj;
            int index = 0;
            for (Object item : collection) {
                validateObject(item, context + "[" + index + "]");
                index++;
            }
            return;
        }

        // Validate object fields
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object fieldValue = field.get(obj);
                
                if (fieldValue instanceof String) {
                    String stringValue = (String) fieldValue;
                    
                    // Apply field-specific validation based on field name
                    String fieldName = field.getName().toLowerCase();
                    
                    if (fieldName.contains("email")) {
                        inputSanitizer.sanitizeEmail(stringValue);
                    } else if (fieldName.contains("username") || fieldName.equals("name")) {
                        // Use proper username validation that supports unicode
                        inputSanitizer.sanitizeUsername(stringValue);
                    } else if (fieldName.contains("description") || fieldName.contains("content")) {
                        inputSanitizer.validateNotMalicious(stringValue);
                    } else {
                        // General text validation
                        inputSanitizer.validateNotMalicious(stringValue);
                    }
                } else if (fieldValue != null) {
                    validateObject(fieldValue, context + "." + field.getName());
                }
                
            } catch (IllegalAccessException e) {
                log.debug("Could not access field {} for validation", field.getName());
            }
        }
    }

    /**
     * Validate string content based on context.
     */
    private void validateString(String value, String context) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        // Apply different validation based on context
        String contextLower = context.toLowerCase();
        
        try {
            if (contextLower.contains("email")) {
                inputSanitizer.sanitizeEmail(value);
            } else if (contextLower.contains("username")) {
                inputSanitizer.sanitizeUsername(value);
            } else {
                inputSanitizer.validateNotMalicious(value);
            }
        } catch (Exception e) {
            log.warn("SECURITY_EVENT: Suspicious input detected in {}: {}", context, e.getMessage());
            throw e;
        }
    }

    /**
     * Determine if a class should be skipped for validation.
     */
    private boolean shouldSkipValidation(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return true;
        }

        String className = clazz.getName();
        
        // Skip JDK classes
        if (className.startsWith("java.") || 
            className.startsWith("javax.") || 
            className.startsWith("sun.")) {
            return true;
        }

        // Skip Spring classes
        if (className.startsWith("org.springframework.")) {
            return true;
        }

        // Skip common framework classes
        if (className.startsWith("com.fasterxml.") ||
            className.startsWith("org.apache.") ||
            className.startsWith("jakarta.servlet.")) {
            return true;
        }

        // Skip Spring Security classes and authentication objects
        if (className.startsWith("org.springframework.security.") ||
            className.contains("UserPrincipal") ||
            className.contains("UserDetails") ||
            className.equals("com.codetop.security.UserPrincipal") ||
            clazz.getSimpleName().equals("UserPrincipal")) {
            return true;
        }

        // Skip wrapper classes
        return clazz == Long.class || 
               clazz == Integer.class || 
               clazz == Boolean.class || 
               clazz == Double.class ||
               clazz == Float.class;
    }

    /**
     * Check if object is security-related and should be skipped from validation.
     */
    private boolean isSecurityRelatedObject(Object obj) {
        if (obj == null) {
            return false;
        }

        Class<?> clazz = obj.getClass();
        String className = clazz.getName();

        // Skip UserPrincipal and other authentication objects
        if (className.equals("com.codetop.security.UserPrincipal") ||
            className.contains("UserPrincipal") ||
            className.contains("UserDetails") ||
            className.contains("Authentication") ||
            obj instanceof org.springframework.security.core.userdetails.UserDetails) {
            return true;
        }

        // Check if implementing Spring Security interfaces
        try {
            return org.springframework.security.core.userdetails.UserDetails.class.isAssignableFrom(clazz) ||
                   org.springframework.security.core.Authentication.class.isAssignableFrom(clazz);
        } catch (Exception e) {
            // If Spring Security classes not available, just return false
            return false;
        }
    }
}