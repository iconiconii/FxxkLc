package com.codetop.dto;

import com.codetop.validation.ValidComplexity;
import com.codetop.validation.ValidNoteType;
import com.codetop.validation.ValidProgrammingLanguage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * DTO for creating a new problem note.
 * 
 * Contains all necessary information to create both the MySQL metadata
 * record and the MongoDB content document.
 * 
 * @author CodeTop Team
 */
@Data
public class CreateNoteRequestDTO {
    
    @NotNull(message = "Problem ID is required")
    private Long problemId;
    
    @NotNull(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;
    
    @ValidNoteType
    private String noteType = "SOLUTION"; // SOLUTION, EXPLANATION, TIPS, PATTERN, OTHER
    
    private Boolean isPublic = false;
    
    // Content fields
    @Size(max = 50000, message = "Content must not exceed 50000 characters")
    private String content;
    
    @Size(max = 5000, message = "Solution approach must not exceed 5000 characters")
    private String solutionApproach;
    
    @ValidComplexity
    @Size(max = 200, message = "Time complexity must not exceed 200 characters")
    private String timeComplexity;
    
    @ValidComplexity
    @Size(max = 200, message = "Space complexity must not exceed 200 characters")
    private String spaceComplexity;
    
    @Size(max = 3000, message = "Pitfalls must not exceed 3000 characters")
    private String pitfalls;
    
    @Size(max = 3000, message = "Tips must not exceed 3000 characters")
    private String tips;
    
    private List<String> tags;
    
    @Valid
    private List<CodeSnippetRequestDTO> codeSnippets;
    
    @Data
    public static class CodeSnippetRequestDTO {
        
        @NotNull(message = "Programming language is required")
        @ValidProgrammingLanguage
        private String language;
        
        @NotNull(message = "Code content is required")
        @Size(min = 1, max = 10000, message = "Code must be between 1 and 10000 characters")
        private String code;
        
        @Size(max = 2000, message = "Explanation must not exceed 2000 characters")
        private String explanation;
        
        private String type = "SOLUTION"; // SOLUTION, OPTIMIZED, BRUTE_FORCE, ALTERNATIVE
        
        private String complexityNote;
    }
    
    // Validation methods
    public boolean hasContent() {
        return (content != null && !content.trim().isEmpty()) ||
               (solutionApproach != null && !solutionApproach.trim().isEmpty()) ||
               (timeComplexity != null && !timeComplexity.trim().isEmpty()) ||
               (spaceComplexity != null && !spaceComplexity.trim().isEmpty()) ||
               (pitfalls != null && !pitfalls.trim().isEmpty()) ||
               (tips != null && !tips.trim().isEmpty()) ||
               (codeSnippets != null && !codeSnippets.isEmpty());
    }
    
    public boolean hasCodeSnippets() {
        return codeSnippets != null && !codeSnippets.isEmpty();
    }
    
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
}