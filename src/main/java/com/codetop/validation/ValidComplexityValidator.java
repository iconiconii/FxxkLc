package com.codetop.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator for ValidComplexity annotation.
 * 
 * Validates Big O notation format for time and space complexity.
 * 
 * @author CodeTop Team
 */
public class ValidComplexityValidator implements ConstraintValidator<ValidComplexity, String> {
    
    // Pattern to match common Big O notation formats
    private static final Pattern COMPLEXITY_PATTERN = Pattern.compile(
        "^O\\(\\s*" +                           // Start with O(
        "(" +
            "1|" +                              // Constant: O(1)
            "log\\s+n|" +                       // Logarithmic: O(log n)
            "[a-zA-Z]|" +                       // Single variables: O(n), O(m), O(k)
            "[a-zA-Z]\\s+log\\s+[a-zA-Z]|" +    // Variable log: O(n log n), O(k log k)
            "[a-zA-Z]\\^?[2-9]|" +              // Polynomial: O(n^2), O(n^3), etc.
            "[a-zA-Z]²|[a-zA-Z]³|[a-zA-Z]⁴|" + // Unicode superscripts
            "2\\^[a-zA-Z]|" +                   // Exponential: O(2^n)
            "[a-zA-Z]!|" +                      // Factorial: O(n!)
            "[a-zA-Z]\\s*\\+\\s*[a-zA-Z]|" +    // Addition: O(m+n)
            "[a-zA-Z]\\s*\\*\\s*[a-zA-Z]|" +    // Multiplication: O(m*n)
            "sqrt\\([a-zA-Z]\\)" +               // Square root: O(sqrt(n))
        ")" +
        "\\s*\\)$",                             // End with )
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public boolean isValid(String complexity, ConstraintValidatorContext context) {
        if (complexity == null || complexity.trim().isEmpty()) {
            return true; // Allow empty complexity
        }
        
        String trimmed = complexity.trim();
        return COMPLEXITY_PATTERN.matcher(trimmed).matches();
    }
}