package com.codetop.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for note types.
 * 
 * Validates that the note type is one of the allowed values:
 * SOLUTION, EXPLANATION, TIPS, PATTERN, OTHER
 * 
 * @author CodeTop Team
 */
@Documented
@Constraint(validatedBy = ValidNoteTypeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNoteType {
    String message() default "Invalid note type. Must be one of: SOLUTION, EXPLANATION, TIPS, PATTERN, OTHER";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}