package com.codetop.dto;

import com.codetop.entity.ProblemNote;
import com.codetop.entity.ProblemNoteDocument;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Complete Problem Note DTO combining metadata and content for API responses.
 * 
 * This DTO merges data from MySQL ProblemNote (metadata) and 
 * MongoDB ProblemNoteDocument (content) entities.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
public class ProblemNoteDTO {
    // Metadata from MySQL
    private Long id;
    private Long userId;
    private Long problemId;
    private String title;
    private Boolean isPublic;
    private String noteType;
    private Integer helpfulVotes;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Content from MongoDB
    private String content;
    private String solutionApproach;
    private String timeComplexity;
    private String spaceComplexity;
    private String pitfalls;
    private String tips;
    private List<String> tags;
    private List<CodeSnippetDTO> codeSnippets;
    private Integer wordCount;
    private Integer version;
    private LocalDateTime lastModified;
    
    // User information (for public notes)
    private String authorUsername;
    private String authorAvatarUrl;
    
    // Problem information
    private String problemTitle;
    private String problemDifficulty;
    
    @Data
    @Builder
    public static class CodeSnippetDTO {
        private String language;
        private String code;
        private String explanation;
        private String type;
        private String complexityNote;
    }
    
    /**
     * Create DTO from MySQL entity only (metadata only).
     */
    public static ProblemNoteDTO fromEntity(ProblemNote entity) {
        return ProblemNoteDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .problemId(entity.getProblemId())
                .title(entity.getTitle())
                .isPublic(entity.getIsPublic())
                .noteType(entity.getNoteType())
                .helpfulVotes(entity.getHelpfulVotes())
                .viewCount(entity.getViewCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    /**
     * Create DTO from both MySQL entity and MongoDB document.
     */
    public static ProblemNoteDTO fromEntityAndDocument(ProblemNote entity, ProblemNoteDocument document) {
        ProblemNoteDTOBuilder builder = ProblemNoteDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .problemId(entity.getProblemId())
                .title(entity.getTitle())
                .isPublic(entity.getIsPublic())
                .noteType(entity.getNoteType())
                .helpfulVotes(entity.getHelpfulVotes())
                .viewCount(entity.getViewCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());
        
        if (document != null) {
            builder.content(document.getContent())
                   .solutionApproach(document.getSolutionApproach())
                   .timeComplexity(document.getTimeComplexity())
                   .spaceComplexity(document.getSpaceComplexity())
                   .pitfalls(document.getPitfalls())
                   .tips(document.getTips())
                   .tags(document.getTags())
                   .wordCount(document.getWordCount())
                   .version(document.getVersion())
                   .lastModified(document.getLastModified());
            
            // Convert code snippets
            if (document.getCodeSnippets() != null) {
                List<CodeSnippetDTO> codeSnippetDTOs = document.getCodeSnippets().stream()
                        .map(snippet -> CodeSnippetDTO.builder()
                                .language(snippet.getLanguage())
                                .code(snippet.getCode())
                                .explanation(snippet.getExplanation())
                                .type(snippet.getType())
                                .complexityNote(snippet.getComplexityNote())
                                .build())
                        .toList();
                builder.codeSnippets(codeSnippetDTOs);
            }
        }
        
        return builder.build();
    }
    
    // Helper methods
    public boolean hasContent() {
        return content != null && !content.trim().isEmpty();
    }
    
    public boolean hasCodeSnippets() {
        return codeSnippets != null && !codeSnippets.isEmpty();
    }
    
    public boolean hasComplexityAnalysis() {
        return (timeComplexity != null && !timeComplexity.trim().isEmpty()) ||
               (spaceComplexity != null && !spaceComplexity.trim().isEmpty());
    }
    
    public boolean isHelpful() {
        return helpfulVotes != null && helpfulVotes > 0;
    }
    
    public boolean isPopular() {
        return (viewCount != null && viewCount > 100) || 
               (helpfulVotes != null && helpfulVotes > 10);
    }
}