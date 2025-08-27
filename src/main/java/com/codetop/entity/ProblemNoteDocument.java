package com.codetop.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB document entity for storing detailed problem note content.
 * 
 * This entity stores rich content information for problem notes,
 * complementing the metadata stored in the MySQL ProblemNote entity.
 * 
 * Features:
 * - Markdown content storage
 * - Solution approaches and complexity analysis
 * - Code snippets in multiple languages
 * - Tips, pitfalls, and learning insights
 * - Tagging and categorization
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "problem_note_contents")
public class ProblemNoteDocument {
    
    @Id
    private String id;
    
    @NotNull
    @Indexed
    @Field("problem_note_id")
    private Long problemNoteId; // Links to MySQL ProblemNote.id
    
    @Field("content")
    @Size(max = 50000, message = "Content must not exceed 50000 characters")
    private String content; // Markdown content
    
    @Field("solution_approach")
    @Size(max = 5000, message = "Solution approach must not exceed 5000 characters")
    private String solutionApproach;
    
    @Field("time_complexity")
    @Size(max = 200, message = "Time complexity must not exceed 200 characters")
    private String timeComplexity;
    
    @Field("space_complexity")
    @Size(max = 200, message = "Space complexity must not exceed 200 characters")
    private String spaceComplexity;
    
    @Field("pitfalls")
    @Size(max = 3000, message = "Pitfalls must not exceed 3000 characters")
    private String pitfalls;
    
    @Field("tips")
    @Size(max = 3000, message = "Tips must not exceed 3000 characters")
    private String tips;
    
    @Field("tags")
    private List<String> tags;
    
    @Field("code_snippets")
    private List<CodeSnippet> codeSnippets;
    
    @Field("last_modified")
    private LocalDateTime lastModified;
    
    @Field("version")
    @Builder.Default
    private Integer version = 1;
    
    @Field("word_count")
    private Integer wordCount;
    
    /**
     * Embedded class for code snippets
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeSnippet {
        
        @Field("language")
        private String language; // java, python, cpp, javascript, etc.
        
        @Field("code")
        @Size(max = 10000, message = "Code must not exceed 10000 characters")
        private String code;
        
        @Field("explanation")
        @Size(max = 2000, message = "Explanation must not exceed 2000 characters")
        private String explanation;
        
        @Field("type")
        private String type; // SOLUTION, OPTIMIZED, BRUTE_FORCE, ALTERNATIVE, etc.
        
        @Field("complexity_note")
        private String complexityNote;
    }
    
    // Utility methods
    public boolean hasContent() {
        return content != null && !content.trim().isEmpty();
    }
    
    public boolean hasSolutionApproach() {
        return solutionApproach != null && !solutionApproach.trim().isEmpty();
    }
    
    public boolean hasComplexityAnalysis() {
        return (timeComplexity != null && !timeComplexity.trim().isEmpty()) ||
               (spaceComplexity != null && !spaceComplexity.trim().isEmpty());
    }
    
    public boolean hasPitfalls() {
        return pitfalls != null && !pitfalls.trim().isEmpty();
    }
    
    public boolean hasTips() {
        return tips != null && !tips.trim().isEmpty();
    }
    
    public boolean hasCodeSnippets() {
        return codeSnippets != null && !codeSnippets.isEmpty();
    }
    
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
    
    public int getContentLength() {
        return content != null ? content.length() : 0;
    }
    
    public void updateWordCount() {
        if (content != null && !content.trim().isEmpty()) {
            this.wordCount = content.trim().split("\\s+").length;
        } else {
            this.wordCount = 0;
        }
    }
    
    public void incrementVersion() {
        this.version++;
        this.lastModified = LocalDateTime.now();
    }
    
    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new java.util.ArrayList<>();
        }
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }
    
    public void removeTag(String tag) {
        if (this.tags != null) {
            this.tags.remove(tag);
        }
    }
    
    public void addCodeSnippet(CodeSnippet snippet) {
        if (this.codeSnippets == null) {
            this.codeSnippets = new java.util.ArrayList<>();
        }
        this.codeSnippets.add(snippet);
    }
    
    @Override
    public String toString() {
        return "ProblemNoteDocument{" +
                "id='" + id + '\'' +
                ", problemNoteId=" + problemNoteId +
                ", version=" + version +
                ", wordCount=" + wordCount +
                ", hasContent=" + hasContent() +
                ", codeSnippetCount=" + (codeSnippets != null ? codeSnippets.size() : 0) +
                ", tagCount=" + (tags != null ? tags.size() : 0) +
                ", lastModified=" + lastModified +
                '}';
    }
}