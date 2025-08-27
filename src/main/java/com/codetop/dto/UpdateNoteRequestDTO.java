package com.codetop.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * DTO for updating an existing problem note.
 * 
 * Contains optional fields that can be updated in both the MySQL metadata
 * record and the MongoDB content document.
 * 
 * @author CodeTop Team
 */
@Data
public class UpdateNoteRequestDTO {
    
    // Metadata fields (MySQL)
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;
    
    private String noteType; // SOLUTION, EXPLANATION, TIPS, PATTERN
    
    private Boolean isPublic;
    
    // Content fields (MongoDB)
    @Size(max = 50000, message = "Content must not exceed 50000 characters")
    private String content;
    
    @Size(max = 5000, message = "Solution approach must not exceed 5000 characters")
    private String solutionApproach;
    
    @Size(max = 200, message = "Time complexity must not exceed 200 characters")
    private String timeComplexity;
    
    @Size(max = 200, message = "Space complexity must not exceed 200 characters")
    private String spaceComplexity;
    
    @Size(max = 3000, message = "Pitfalls must not exceed 3000 characters")
    private String pitfalls;
    
    @Size(max = 3000, message = "Tips must not exceed 3000 characters")
    private String tips;
    
    private List<String> tags;
    
    @Valid
    private List<CodeSnippetUpdateDTO> codeSnippets;
    
    @Data
    public static class CodeSnippetUpdateDTO {
        
        private String language;
        
        @Size(min = 1, max = 10000, message = "Code must be between 1 and 10000 characters")
        private String code;
        
        @Size(max = 2000, message = "Explanation must not exceed 2000 characters")
        private String explanation;
        
        private String type; // SOLUTION, OPTIMIZED, BRUTE_FORCE, ALTERNATIVE
        
        private String complexityNote;
    }
    
    // Helper methods to check what fields are being updated
    public boolean hasMetadataUpdates() {
        return title != null || noteType != null || isPublic != null;
    }
    
    public boolean hasContentUpdates() {
        return content != null || 
               solutionApproach != null || 
               timeComplexity != null || 
               spaceComplexity != null || 
               pitfalls != null || 
               tips != null || 
               tags != null || 
               codeSnippets != null;
    }
    
    public boolean hasCodeSnippets() {
        return codeSnippets != null && !codeSnippets.isEmpty();
    }
    
    public boolean hasTags() {
        return tags != null;
    }
}