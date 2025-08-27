package com.codetop.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for programming languages in code snippets.
 * 
 * Validates that the programming language is one of the supported values.
 * 
 * @author CodeTop Team
 */
@Documented
@Constraint(validatedBy = ValidProgrammingLanguageValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidProgrammingLanguage {
    String message() default "Invalid programming language. Must be one of the supported languages.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}