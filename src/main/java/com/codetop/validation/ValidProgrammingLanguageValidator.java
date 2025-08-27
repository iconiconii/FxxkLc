package com.codetop.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

/**
 * Validator for ValidProgrammingLanguage annotation.
 * 
 * @author CodeTop Team
 */
public class ValidProgrammingLanguageValidator implements ConstraintValidator<ValidProgrammingLanguage, String> {
    
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
        "java", "python", "cpp", "c", "javascript", "typescript", 
        "go", "rust", "swift", "kotlin", "scala", "php", "ruby", 
        "csharp", "sql", "bash", "python3", "c++", "js", "ts"
    );
    
    @Override
    public boolean isValid(String language, ConstraintValidatorContext context) {
        if (language == null || language.trim().isEmpty()) {
            return true; // Let @NotNull handle null validation
        }
        
        return SUPPORTED_LANGUAGES.contains(language.trim().toLowerCase());
    }
}