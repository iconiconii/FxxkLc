package com.codetop.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

/**
 * Validator for ValidNoteType annotation.
 * 
 * @author CodeTop Team
 */
public class ValidNoteTypeValidator implements ConstraintValidator<ValidNoteType, String> {
    
    private static final Set<String> VALID_NOTE_TYPES = Set.of(
        "SOLUTION", "EXPLANATION", "TIPS", "PATTERN", "OTHER"
    );
    
    @Override
    public boolean isValid(String noteType, ConstraintValidatorContext context) {
        if (noteType == null || noteType.trim().isEmpty()) {
            return true; // Let @NotNull handle null validation
        }
        
        return VALID_NOTE_TYPES.contains(noteType.trim().toUpperCase());
    }
}