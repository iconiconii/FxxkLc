package com.codetop.dto;

import com.codetop.service.CodeTopFilterService;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class CodeTopFilterResponse {

    // Results
    private List<ProblemRankingDTO> problems;
    
    // Pagination info
    private Long totalElements;
    private Long totalPages;
    private Long currentPage;
    private Long pageSize;
    
    // Filter options for UI
    private CodeTopFilterService.FilterOptions filterOptions;
    
    // Aggregation info
    private FilterSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterSummary {
        private Integer totalProblems;
        private Integer hotProblems;
        private Integer trendingProblems;
        private Double avgFrequencyScore;
        private String mostCommonDifficulty;
        private String mostActiveCompany;
    }
}