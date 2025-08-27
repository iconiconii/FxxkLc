package com.codetop.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "problem_note_details")
public class ProblemNoteDetail {
    
    @Id
    private String id;
    
    @Field("problem_note_id")
    private Long problemNoteId;
    
    @Field("problem_id")
    private Long problemId;
    
    @Field("user_id")
    private Long userId;
    
    @Field("code_snippets")
    private List<CodeSnippet> codeSnippets;
    
    @Field("diagrams")
    private List<String> diagrams;
    
    @Field("examples")
    private List<Example> examples;
    
    @Field("related_problems")
    private List<Long> relatedProblems;
    
    @Field("patterns")
    private List<String> patterns;
    
    @Field("metadata")
    private Map<String, Object> metadata;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeSnippet {
        private String language;
        private String code;
        private String explanation;
        private String type; // solution, optimized, brute-force, etc.
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Example {
        private String input;
        private String output;
        private String explanation;
        private String walkthrough;
    }
}