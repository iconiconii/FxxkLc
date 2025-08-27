package com.codetop.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for time and space complexity strings.
 * 
 * Validates that complexity follows standard Big O notation format.
 * Examples: O(1), O(n), O(log n), O(n²), O(n log n), etc.
 * 
 * @author CodeTop Team
 */
@Documented
@Constraint(validatedBy = ValidComplexityValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidComplexity {
    String message() default "Invalid complexity format. Must follow Big O notation (e.g., O(n), O(log n), O(1))";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}