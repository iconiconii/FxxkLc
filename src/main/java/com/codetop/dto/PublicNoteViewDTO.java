package com.codetop.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for public note view with limited information.
 * 
 * This DTO is used for public API endpoints to show note information
 * without exposing sensitive user data or private content.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
public class PublicNoteViewDTO {
    
    // Basic note information
    private Long id;
    private Long problemId;
    private String title;
    private String noteType;
    
    // Public metrics
    private Integer helpfulVotes;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Content preview (limited)
    private String contentPreview; // First 500 characters
    private String solutionApproach;
    private String timeComplexity;
    private String spaceComplexity;
    
    // Learning insights
    private String pitfalls;
    private String tips;
    private List<String> tags;
    
    // Code snippets (language names only for preview)
    private List<String> programmingLanguages;
    
    // Author information (anonymous or username only)
    private String authorUsername;
    private String authorAvatarUrl;
    
    // Problem information
    private String problemTitle;
    private String problemDifficulty;
    
    // Additional metrics for sorting/filtering
    private Integer wordCount;
    private boolean hasCodeSnippets;
    private boolean hasComplexityAnalysis;
    private LocalDateTime lastModified;
    
    /**
     * Create DTO from ProblemNoteDTO for public view.
     */
    public static PublicNoteViewDTO fromProblemNoteDTO(ProblemNoteDTO dto) {
        return PublicNoteViewDTO.builder()
                .id(dto.getId())
                .problemId(dto.getProblemId())
                .title(dto.getTitle())
                .noteType(dto.getNoteType())
                .helpfulVotes(dto.getHelpfulVotes())
                .viewCount(dto.getViewCount())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .contentPreview(truncateContent(dto.getContent(), 500))
                .solutionApproach(dto.getSolutionApproach())
                .timeComplexity(dto.getTimeComplexity())
                .spaceComplexity(dto.getSpaceComplexity())
                .pitfalls(dto.getPitfalls())
                .tips(dto.getTips())
                .tags(dto.getTags())
                .programmingLanguages(extractProgrammingLanguages(dto.getCodeSnippets()))
                .authorUsername(dto.getAuthorUsername())
                .authorAvatarUrl(dto.getAuthorAvatarUrl())
                .problemTitle(dto.getProblemTitle())
                .problemDifficulty(dto.getProblemDifficulty())
                .wordCount(dto.getWordCount())
                .hasCodeSnippets(dto.hasCodeSnippets())
                .hasComplexityAnalysis(dto.hasComplexityAnalysis())
                .lastModified(dto.getLastModified())
                .build();
    }
    
    /**
     * Truncate content for preview.
     */
    private static String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        
        String truncated = content.substring(0, maxLength);
        int lastSpace = truncated.lastIndexOf(' ');
        
        if (lastSpace > maxLength * 0.8) { // Don't break words if we're close to the end
            truncated = truncated.substring(0, lastSpace);
        }
        
        return truncated + "...";
    }
    
    /**
     * Extract programming language names from code snippets.
     */
    private static List<String> extractProgrammingLanguages(List<ProblemNoteDTO.CodeSnippetDTO> codeSnippets) {
        if (codeSnippets == null || codeSnippets.isEmpty()) {
            return List.of();
        }
        
        return codeSnippets.stream()
                .map(ProblemNoteDTO.CodeSnippetDTO::getLanguage)
                .distinct()
                .sorted()
                .toList();
    }
    
    // Helper methods for view logic
    public boolean isHelpful() {
        return helpfulVotes != null && helpfulVotes > 0;
    }
    
    public boolean isPopular() {
        return (viewCount != null && viewCount > 100) || 
               (helpfulVotes != null && helpfulVotes > 10);
    }
    
    public boolean hasLearningInsights() {
        return (pitfalls != null && !pitfalls.trim().isEmpty()) ||
               (tips != null && !tips.trim().isEmpty());
    }
    
    public String getDisplayTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }
        return problemTitle != null ? "关于 " + problemTitle + " 的笔记" : "算法笔记";
    }
}