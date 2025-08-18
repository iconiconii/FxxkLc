package com.codetop.dto;

import com.codetop.service.CodeTopFilterService;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for CodeTop-style filtering results.
 * 
 * Contains filtered problems with ranking information and filter options.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
public class CodeTopFilterResponse {

    // Results
    private List<ProblemRankingDTO> problems;
    
    // Pagination info
    private Long totalElements;
    private Long totalPages;
    private Long currentPage;
    private Long pageSize;
    
    // Aggregation info
    private FilterSummary summary;
    
    @JsonCreator
    public CodeTopFilterResponse(
            @JsonProperty("problems") List<ProblemRankingDTO> problems,
            @JsonProperty("totalElements") Long totalElements,
            @JsonProperty("totalPages") Long totalPages,
            @JsonProperty("currentPage") Long currentPage,
            @JsonProperty("pageSize") Long pageSize,
            @JsonProperty("summary") FilterSummary summary) {
        this.problems = problems;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.summary = summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    public static class FilterSummary {
        private Integer totalProblems;
        private Integer hotProblems;
        private Integer trendingProblems;
        private Double avgFrequencyScore;
        private String mostCommonDifficulty;
        private String mostActiveCompany;
        
        @JsonCreator
        public FilterSummary(
                @JsonProperty("totalProblems") Integer totalProblems,
                @JsonProperty("hotProblems") Integer hotProblems,
                @JsonProperty("trendingProblems") Integer trendingProblems,
                @JsonProperty("avgFrequencyScore") Double avgFrequencyScore,
                @JsonProperty("mostCommonDifficulty") String mostCommonDifficulty,
                @JsonProperty("mostActiveCompany") String mostActiveCompany) {
            this.totalProblems = totalProblems;
            this.hotProblems = hotProblems;
            this.trendingProblems = trendingProblems;
            this.avgFrequencyScore = avgFrequencyScore;
            this.mostCommonDifficulty = mostCommonDifficulty;
            this.mostActiveCompany = mostActiveCompany;
        }
    }
}