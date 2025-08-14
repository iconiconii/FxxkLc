package com.codetop.controller;

import com.codetop.dto.CodeTopFilterRequest;
import com.codetop.dto.CodeTopFilterResponse;
import com.codetop.dto.ProblemRankingDTO;
import com.codetop.service.CodeTopFilterService;
import com.codetop.service.CodeTopFilterService.CompanyProblemBreakdownDTO;
import com.codetop.service.CodeTopFilterService.CategoryUsageStatsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * CodeTop-style filtering API controller.
 * 
 * Provides advanced problem filtering and ranking capabilities:
 * - Three-level filtering hierarchy (Company → Department → Position)
 * - Frequency-based ranking and trending analysis
 * - Category-based filtering with relevance scoring
 * - Multi-source data integration with quality indicators
 * 
 * @author CodeTop Team
 */
@RestController
@RequestMapping("/codetop")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CodeTop Filtering", description = "Advanced problem filtering and ranking APIs")
public class CodeTopFilterController {

    private final CodeTopFilterService codeTopFilterService;

    @Operation(
        summary = "Get filtered problems with CodeTop-style ranking",
        description = "Advanced filtering with company/department/position hierarchy, frequency ranking, and trend analysis"
    )
    @PostMapping("/filter")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CodeTopFilterResponse> getFilteredProblems(
            @Valid @RequestBody CodeTopFilterRequest request) {
        
        log.info("Processing CodeTop filter request from user, filters: company={}, department={}, position={}", 
                request.getCompanyId(), request.getDepartmentId(), request.getPositionId());
        
        CodeTopFilterResponse response = codeTopFilterService.getFilteredProblems(request);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get trending problems",
        description = "Get problems with increasing frequency trend across all companies"
    )
    @GetMapping("/trending")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ProblemRankingDTO>> getTrendingProblems(
            @Parameter(description = "Maximum number of problems to return") 
            @RequestParam(defaultValue = "20") Integer limit,
            @Parameter(description = "Number of days to analyze for trend") 
            @RequestParam(defaultValue = "30") Integer days) {
        
        List<ProblemRankingDTO> trendingProblems = codeTopFilterService.getTrendingProblems(limit, days);
        
        return ResponseEntity.ok(trendingProblems);
    }

    @Operation(
        summary = "Get hot problems",
        description = "Get problems in top percentile with recent interview activity"
    )
    @GetMapping("/hot")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ProblemRankingDTO>> getHotProblems(
            @Parameter(description = "Company ID for company-specific hot problems") 
            @RequestParam(required = false) Long companyId,
            @Parameter(description = "Maximum number of problems to return") 
            @RequestParam(defaultValue = "15") Integer limit) {
        
        List<ProblemRankingDTO> hotProblems = codeTopFilterService.getHotProblems(companyId, limit);
        
        return ResponseEntity.ok(hotProblems);
    }

    @Operation(
        summary = "Get top problems by frequency",
        description = "Get highest frequency problems for specific scope (global, company, department, position)"
    )
    @GetMapping("/top-frequency")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ProblemRankingDTO>> getTopProblemsByFrequency(
            @Parameter(description = "Scope level: GLOBAL, COMPANY, DEPARTMENT, POSITION") 
            @RequestParam String scope,
            @Parameter(description = "Company ID (required for COMPANY+ scopes)") 
            @RequestParam(required = false) Long companyId,
            @Parameter(description = "Department ID (required for DEPARTMENT+ scopes)") 
            @RequestParam(required = false) Long departmentId,
            @Parameter(description = "Position ID (required for POSITION scope)") 
            @RequestParam(required = false) Long positionId,
            @Parameter(description = "Maximum number of problems to return") 
            @RequestParam(defaultValue = "20") Integer limit) {
        
        List<ProblemRankingDTO> topProblems = codeTopFilterService.getTopProblemsByFrequency(
                scope, companyId, departmentId, positionId, limit);
        
        return ResponseEntity.ok(topProblems);
    }

    @Operation(
        summary = "Get company problem breakdown",
        description = "Get detailed breakdown of problems by department and position for a company"
    )
    @GetMapping("/company/{companyId}/breakdown")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<CodeTopFilterService.CompanyProblemBreakdownDTO>> getCompanyProblemBreakdown(
            @Parameter(description = "Company ID") 
            @PathVariable Long companyId) {
        
        List<CodeTopFilterService.CompanyProblemBreakdownDTO> breakdown = 
            codeTopFilterService.getCompanyProblemBreakdown(companyId);
        
        return ResponseEntity.ok(breakdown);
    }

    @Operation(
        summary = "Get problems by category",
        description = "Get problems for a specific category with frequency information"
    )
    @GetMapping("/category/{categoryId}/problems")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ProblemRankingDTO>> getProblemsByCategory(
            @Parameter(description = "Category ID") 
            @PathVariable Long categoryId,
            @Parameter(description = "Page number (1-based)") 
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "20") Integer size) {
        
        var problemPage = codeTopFilterService.getProblemsByCategory(categoryId, page, size);
        
        return ResponseEntity.ok(problemPage.getRecords());
    }

    @Operation(
        summary = "Get similar problems",
        description = "Get problems similar to the given problem based on shared categories"
    )
    @GetMapping("/problems/{problemId}/similar")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ProblemRankingDTO>> getSimilarProblems(
            @Parameter(description = "Problem ID") 
            @PathVariable Long problemId,
            @Parameter(description = "Maximum number of similar problems to return") 
            @RequestParam(defaultValue = "10") Integer limit) {
        
        List<ProblemRankingDTO> similarProblems = codeTopFilterService.getSimilarProblems(problemId, limit);
        
        return ResponseEntity.ok(similarProblems);
    }

    @Operation(
        summary = "Get global problem rankings",
        description = "Get global frequency-based problem rankings"
    )
    @GetMapping("/rankings/global")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ProblemRankingDTO>> getGlobalRankings(
            @Parameter(description = "Page number (1-based)") 
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "50") Integer size) {
        
        var rankingPage = codeTopFilterService.getGlobalRankings(page, size);
        
        return ResponseEntity.ok(rankingPage.getRecords());
    }

    @Operation(
        summary = "Get category usage statistics",
        description = "Get statistics about category usage across all problems"
    )
    @GetMapping("/categories/stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<CodeTopFilterService.CategoryUsageStatsDTO>> getCategoryUsageStatistics() {
        
        List<CodeTopFilterService.CategoryUsageStatsDTO> stats = 
            codeTopFilterService.getCategoryUsageStatistics();
        
        return ResponseEntity.ok(stats);
    }

    @Operation(
        summary = "Get filter options",
        description = "Get available filter options for building dynamic UI filters"
    )
    @GetMapping("/filter-options")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CodeTopFilterService.FilterOptions> getFilterOptions(
            @Parameter(description = "Company ID to get departments for") 
            @RequestParam(required = false) Long companyId,
            @Parameter(description = "Department ID to get positions for") 
            @RequestParam(required = false) Long departmentId) {
        
        // Create a basic request to get filter options
        CodeTopFilterRequest request = new CodeTopFilterRequest();
        request.setCompanyId(companyId);
        request.setDepartmentId(departmentId);
        request.setSize(1); // We only need the filter options, not actual results
        
        CodeTopFilterResponse response = codeTopFilterService.getFilteredProblems(request);
        
        return ResponseEntity.ok(response.getFilterOptions());
    }

    // Health check endpoint for monitoring
    @Operation(
        summary = "Health check for CodeTop filtering system",
        description = "Check if the filtering system is working properly"
    )
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        try {
            // Test basic functionality
            codeTopFilterService.getTrendingProblems(1, 7);
            return ResponseEntity.ok("CodeTop filtering system is healthy");
        } catch (Exception e) {
            log.error("CodeTop filtering system health check failed", e);
            return ResponseEntity.status(500).body("CodeTop filtering system is experiencing issues");
        }
    }
}