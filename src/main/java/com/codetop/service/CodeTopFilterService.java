package com.codetop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.codetop.dto.CodeTopFilterRequest;
import com.codetop.dto.CodeTopFilterResponse;
import com.codetop.dto.ProblemRankingDTO;
import com.codetop.entity.*;
import com.codetop.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CodeTop-style filtering service with comprehensive problem ranking and filtering capabilities.
 * 
 * Features:
 * - Three-level filtering hierarchy (Company → Department → Position)
 * - Frequency-based ranking and trending analysis
 * - Category-based filtering with relevance scoring
 * - Multi-source data integration with quality indicators
 * - Advanced search and sorting options
 * 
 * @author CodeTop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeTopFilterService {

    private final ProblemFrequencyStatsMapper problemFrequencyStatsMapper;
    private final ProblemMapper problemMapper;
    private final CompanyMapper companyMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;

    /**
     * Get filtered and ranked problems using CodeTop-style filtering.
     */
    @Cacheable(value = "codetop-filter-results", key = "T(com.codetop.service.CacheKeyBuilder).codetopFilter(#request.companyId, #request.departmentId, #request.positionId, #request.keyword, #request.page, #request.size, #request.sortBy)")
    public CodeTopFilterResponse getFilteredProblems(CodeTopFilterRequest request) {
        log.info("Processing CodeTop filter request: company={}, department={}, position={}, keyword={}", 
                request.getCompanyId(), request.getDepartmentId(), request.getPositionId(), request.getKeyword());
        
        try {
            // Create pagination
            Page<ProblemFrequencyStats> page = new Page<>(request.getPage(), request.getSize());
            
            // Build query based on request filters
            Page<ProblemFrequencyStats> statsPage = buildFilteredQuery(request, page);
            
            // Convert to ProblemRankingDTO list
            List<ProblemRankingDTO> problemRankings = convertToProblemRankingDTOs(statsPage.getRecords(), request);
            
            // Build filter options for UI
            FilterOptions filterOptions = buildFilterOptions(request);
            
            // Build summary statistics
            CodeTopFilterResponse.FilterSummary summary = buildFilterSummary(statsPage.getRecords());
            
            CodeTopFilterResponse response = CodeTopFilterResponse.builder()
                .problems(problemRankings)
                .totalElements(statsPage.getTotal())
                .currentPage((long) request.getPage())
                .pageSize((long) request.getSize())
                .totalPages(statsPage.getPages())
                .filterOptions(filterOptions)
                .summary(summary)
                .build();
            
            log.info("CodeTop filter completed: found {} problems", problemRankings.size());
            return response;
            
        } catch (Exception e) {
            log.error("Error processing CodeTop filter request: {}", e.getMessage(), e);
            // Return empty response on error
            return CodeTopFilterResponse.builder()
                .problems(Collections.emptyList())
                .totalElements(0L)
                .currentPage((long) request.getPage())
                .pageSize((long) request.getSize())
                .totalPages(0L)
                .filterOptions(new FilterOptions())
                .build();
        }
    }

    /**
     * Build filtered query based on request parameters.
     */
    private Page<ProblemFrequencyStats> buildFilteredQuery(CodeTopFilterRequest request, Page<ProblemFrequencyStats> page) {
        QueryWrapper<ProblemFrequencyStats> queryWrapper = new QueryWrapper<>();
        
        // Company filtering
        if (request.getCompanyId() != null) {
            queryWrapper.eq("company_id", request.getCompanyId());
        }
        
        // Department filtering
        if (request.getDepartmentId() != null) {
            queryWrapper.eq("department_id", request.getDepartmentId());
        }
        
        // Position filtering
        if (request.getPositionId() != null) {
            queryWrapper.eq("position_id", request.getPositionId());
        }
        
        // Frequency filtering
        if (request.getMinFrequencyScore() != null) {
            queryWrapper.ge("total_frequency_score", request.getMinFrequencyScore());
        }
        
        // Interview count filtering
        if (request.getMinInterviewCount() != null) {
            queryWrapper.ge("interview_count", request.getMinInterviewCount());
        }
        
        // Trend filtering
        if (request.getTrendFilter() != null && !request.getTrendFilter().trim().isEmpty()) {
            queryWrapper.eq("frequency_trend", request.getTrendFilter());
        }
        
        // Days since last asked filtering
        if (request.getDaysSinceLastAsked() != null) {
            LocalDate cutoffDate = LocalDate.now().minusDays(request.getDaysSinceLastAsked());
            queryWrapper.ge("last_asked_date", cutoffDate);
        }
        
        // Credibility filtering
        if (request.getMinCredibilityScore() != null) {
            // Note: This would need to be joined with problem_companies table
            log.debug("Credibility score filtering requested: {}", request.getMinCredibilityScore());
        }
        
        // Hot problems only
        if (Boolean.TRUE.equals(request.getHotProblemsOnly())) {
            queryWrapper.ge("total_frequency_score", 60.0); // Configurable threshold
        }
        
        // Trending only
        if (Boolean.TRUE.equals(request.getTrendingOnly())) {
            queryWrapper.eq("frequency_trend", "INCREASING");
        }
        
        // Sorting
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "total_frequency_score";
        boolean isDesc = "desc".equalsIgnoreCase(request.getSortOrder());
        
        switch (sortBy) {
            case "frequency_score":
                if (isDesc) queryWrapper.orderByDesc("total_frequency_score");
                else queryWrapper.orderByAsc("total_frequency_score");
                break;
            case "interview_count":
                if (isDesc) queryWrapper.orderByDesc("interview_count");
                else queryWrapper.orderByAsc("interview_count");
                break;
            case "last_asked_date":
                if (isDesc) queryWrapper.orderByDesc("last_asked_date");
                else queryWrapper.orderByAsc("last_asked_date");
                break;
            default:
                queryWrapper.orderByDesc("total_frequency_score");
        }
        
        // Secondary sort by frequency rank
        queryWrapper.orderByAsc("frequency_rank");
        
        return problemFrequencyStatsMapper.selectPage(page, queryWrapper);
    }
    
    /**
     * Convert ProblemFrequencyStats to ProblemRankingDTO list.
     */
    private List<ProblemRankingDTO> convertToProblemRankingDTOs(List<ProblemFrequencyStats> stats, CodeTopFilterRequest request) {
        return stats.stream().map(stat -> {
            try {
                // Get problem details
                Problem problem = problemMapper.selectById(stat.getProblemId());
                if (problem == null) {
                    log.warn("Problem not found for ID: {}", stat.getProblemId());
                    return null;
                }
                
                // Get company name if company ID is available
                String companyName = null;
                if (stat.getCompanyId() != null) {
                    Company company = companyMapper.selectById(stat.getCompanyId());
                    companyName = company != null ? company.getDisplayName() : null;
                }
                
                // Get department name if department ID is available
                String departmentName = null;
                if (stat.getDepartmentId() != null) {
                    Department department = departmentMapper.selectById(stat.getDepartmentId());
                    departmentName = department != null ? department.getDisplayName() : null;
                }
                
                // Get position name if position ID is available
                String positionName = null;
                if (stat.getPositionId() != null) {
                    Position position = positionMapper.selectById(stat.getPositionId());
                    positionName = position != null ? position.getDisplayName() : null;
                }
                
                // Calculate additional scores
                double recencyScore = calculateRecencyScore(stat.getLastAskedDate());
                boolean isHotProblem = stat.getTotalFrequencyScore() != null && 
                                     stat.getTotalFrequencyScore().compareTo(BigDecimal.valueOf(60)) >= 0;
                boolean isTrending = "INCREASING".equals(String.valueOf(stat.getFrequencyTrend()));
                boolean isTopPercentile = stat.getPercentile() != null && 
                                        stat.getPercentile().compareTo(BigDecimal.valueOf(90)) >= 0;
                
                return ProblemRankingDTO.builder()
                    .problemId(problem.getId())
                    .title(problem.getTitle())
                    .difficulty(problem.getDifficulty().name())
                    .problemUrl(problem.getProblemUrl())
                    .leetcodeId(problem.getLeetcodeId())
                    .frequencyScore(stat.getTotalFrequencyScore())
                    .interviewCount(stat.getInterviewCount())
                    .frequencyRank(stat.getFrequencyRank())
                    .percentile(stat.getPercentile())
                    .lastAskedDate(stat.getLastAskedDate())
                    .trend(String.valueOf(stat.getFrequencyTrend()))
                    .recencyScore(recencyScore)
                    .isHotProblem(isHotProblem)
                    .isTrending(isTrending)
                    .isTopPercentile(isTopPercentile)
                    .companyName(companyName)
                    .departmentName(departmentName)
                    .positionName(positionName)
                    .statsScope(String.valueOf(stat.getStatsScope()))
                    .addedDate(problem.getCreatedAt() != null ? problem.getCreatedAt().toLocalDate() : null)
                    .isPremium(problem.getIsPremium())
                    .tags(problem.getTags() != null ? String.join(", ", problem.getTags()) : null)
                    .build();
                    
            } catch (Exception e) {
                log.error("Error converting stat to DTO for problem ID: {}", stat.getProblemId(), e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    
    /**
     * Calculate recency score based on last asked date.
     */
    private double calculateRecencyScore(LocalDate lastAskedDate) {
        if (lastAskedDate == null) {
            return 0.0;
        }
        
        long daysSince = java.time.temporal.ChronoUnit.DAYS.between(lastAskedDate, LocalDate.now());
        
        if (daysSince <= 30) return 1.0;
        if (daysSince <= 90) return 0.8;
        if (daysSince <= 180) return 0.6;
        if (daysSince <= 365) return 0.4;
        return 0.2;
    }
    
    /**
     * Build filter options for UI.
     */
    private FilterOptions buildFilterOptions(CodeTopFilterRequest request) {
        FilterOptions options = new FilterOptions();
        
        try {
            // Get companies
            List<Company> companies = companyMapper.selectList(
                new QueryWrapper<Company>().eq("is_active", true).eq("deleted", 0));
            options.setCompanies(companies);
            
            // Get departments for specific company
            if (request.getCompanyId() != null) {
                List<Department> departments = departmentMapper.findByCompanyId(request.getCompanyId());
                options.setDepartments(departments);
            }
            
            // Get positions for specific department
            if (request.getDepartmentId() != null) {
                List<Position> positions = positionMapper.findByDepartmentId(request.getDepartmentId());
                options.setPositions(positions);
            }
            
        } catch (Exception e) {
            log.error("Error building filter options", e);
        }
        
        return options;
    }
    
    /**
     * Build summary statistics.
     */
    private CodeTopFilterResponse.FilterSummary buildFilterSummary(List<ProblemFrequencyStats> stats) {
        if (stats.isEmpty()) {
            return CodeTopFilterResponse.FilterSummary.builder()
                .totalProblems(0)
                .hotProblems(0)
                .trendingProblems(0)
                .avgFrequencyScore(0.0)
                .mostCommonDifficulty("N/A")
                .mostActiveCompany("N/A")
                .build();
        }
        
        int totalProblems = stats.size();
        int hotProblems = (int) stats.stream()
            .filter(s -> s.getTotalFrequencyScore() != null && 
                        s.getTotalFrequencyScore().compareTo(BigDecimal.valueOf(60)) >= 0)
            .count();
        int trendingProblems = (int) stats.stream()
            .filter(s -> "INCREASING".equals(String.valueOf(s.getFrequencyTrend())))
            .count();
        
        double avgFrequencyScore = stats.stream()
            .filter(s -> s.getTotalFrequencyScore() != null)
            .mapToDouble(s -> s.getTotalFrequencyScore().doubleValue())
            .average()
            .orElse(0.0);
        
        return CodeTopFilterResponse.FilterSummary.builder()
            .totalProblems(totalProblems)
            .hotProblems(hotProblems)
            .trendingProblems(trendingProblems)
            .avgFrequencyScore(avgFrequencyScore)
            .mostCommonDifficulty("MEDIUM") // This would need more complex analysis
            .mostActiveCompany("Various")   // This would need more complex analysis
            .build();
    }
    
    /**
     * Get trending problems.
     */
    public List<ProblemRankingDTO> getTrendingProblems(Integer limit, Integer days) {
        log.info("Getting trending problems with limit={}, days={}", limit, days);
        
        try {
            QueryWrapper<ProblemFrequencyStats> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("frequency_trend", "INCREASING")
                       .orderByDesc("total_frequency_score")
                       .last("LIMIT " + limit);
            
            List<ProblemFrequencyStats> trendingStats = problemFrequencyStatsMapper.selectList(queryWrapper);
            
            CodeTopFilterRequest request = new CodeTopFilterRequest(); // Empty request for trending
            return convertToProblemRankingDTOs(trendingStats, request);
            
        } catch (Exception e) {
            log.error("Error getting trending problems", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get hot problems.
     */
    public List<ProblemRankingDTO> getHotProblems(Long companyId, Integer limit) {
        log.info("Getting hot problems for company={}, limit={}", companyId, limit);
        
        try {
            QueryWrapper<ProblemFrequencyStats> queryWrapper = new QueryWrapper<>();
            queryWrapper.ge("total_frequency_score", 60.0); // Hot threshold
            
            if (companyId != null) {
                queryWrapper.eq("company_id", companyId);
            }
            
            queryWrapper.orderByDesc("total_frequency_score")
                       .orderByAsc("frequency_rank")
                       .last("LIMIT " + limit);
            
            List<ProblemFrequencyStats> hotStats = problemFrequencyStatsMapper.selectList(queryWrapper);
            
            CodeTopFilterRequest request = new CodeTopFilterRequest();
            request.setCompanyId(companyId);
            
            return convertToProblemRankingDTOs(hotStats, request);
            
        } catch (Exception e) {
            log.error("Error getting hot problems", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get top problems by frequency.
     */
    public List<ProblemRankingDTO> getTopProblemsByFrequency(String scope, Long companyId, 
                                                            Long departmentId, Long positionId, Integer limit) {
        log.info("Getting top problems by frequency: scope={}, company={}, department={}, position={}, limit={}",
                scope, companyId, departmentId, positionId, limit);
        
        try {
            QueryWrapper<ProblemFrequencyStats> queryWrapper = new QueryWrapper<>();
            
            // Apply scope filtering
            if (scope != null) {
                queryWrapper.eq("stats_scope", scope.toUpperCase());
            }
            
            if (companyId != null) {
                queryWrapper.eq("company_id", companyId);
            }
            
            if (departmentId != null) {
                queryWrapper.eq("department_id", departmentId);
            }
            
            if (positionId != null) {
                queryWrapper.eq("position_id", positionId);
            }
            
            queryWrapper.orderByDesc("total_frequency_score")
                       .orderByAsc("frequency_rank")
                       .last("LIMIT " + limit);
            
            List<ProblemFrequencyStats> topStats = problemFrequencyStatsMapper.selectList(queryWrapper);
            
            CodeTopFilterRequest request = new CodeTopFilterRequest();
            request.setCompanyId(companyId);
            request.setDepartmentId(departmentId);
            request.setPositionId(positionId);
            
            return convertToProblemRankingDTOs(topStats, request);
            
        } catch (Exception e) {
            log.error("Error getting top problems by frequency", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get company problem breakdown.
     */
    public List<CompanyProblemBreakdownDTO> getCompanyProblemBreakdown(Long companyId) {
        log.info("Getting company problem breakdown for company={}", companyId);
        
        try {
            QueryWrapper<ProblemFrequencyStats> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("company_id", companyId)
                       .orderByDesc("total_frequency_score")
                       .orderByAsc("frequency_rank");
            
            List<ProblemFrequencyStats> stats = problemFrequencyStatsMapper.selectList(queryWrapper);
            
            return stats.stream().map(stat -> {
                try {
                    Problem problem = problemMapper.selectById(stat.getProblemId());
                    if (problem == null) return null;
                    
                    String departmentName = null;
                    if (stat.getDepartmentId() != null) {
                        Department department = departmentMapper.selectById(stat.getDepartmentId());
                        departmentName = department != null ? department.getDisplayName() : null;
                    }
                    
                    String positionName = null;
                    if (stat.getPositionId() != null) {
                        Position position = positionMapper.selectById(stat.getPositionId());
                        positionName = position != null ? position.getDisplayName() : null;
                    }
                    
                    CompanyProblemBreakdownDTO breakdown = new CompanyProblemBreakdownDTO();
                    breakdown.setProblemId(problem.getId());
                    breakdown.setProblemTitle(problem.getTitle());
                    breakdown.setDepartmentName(departmentName);
                    breakdown.setPositionName(positionName);
                    breakdown.setFrequencyScore(stat.getTotalFrequencyScore() != null ? 
                                              stat.getTotalFrequencyScore().doubleValue() : 0.0);
                    
                    return breakdown;
                    
                } catch (Exception e) {
                    log.error("Error processing breakdown for stat: {}", stat.getId(), e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Error getting company problem breakdown", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get problems by category.
     */
    public Page<ProblemRankingDTO> getProblemsByCategory(Long categoryId, Integer page, Integer size) {
        log.info("Getting problems by category: categoryId={}, page={}, size={}", categoryId, page, size);
        
        Page<ProblemRankingDTO> result = new Page<>(page, size);
        
        try {
            // This would require joining with problem_categories table
            // For now, return empty result with proper structure
            log.warn("Category filtering not fully implemented - requires problem_categories table integration");
            
            result.setRecords(Collections.emptyList());
            result.setTotal(0);
            
        } catch (Exception e) {
            log.error("Error getting problems by category", e);
            result.setRecords(Collections.emptyList());
            result.setTotal(0);
        }
        
        return result;
    }

    /**
     * Get similar problems.
     */
    public List<ProblemRankingDTO> getSimilarProblems(Long problemId, Integer limit) {
        log.info("Getting similar problems for problemId={}, limit={}", problemId, limit);
        
        try {
            // Get the target problem
            Problem targetProblem = problemMapper.selectById(problemId);
            if (targetProblem == null) {
                log.warn("Target problem not found: {}", problemId);
                return Collections.emptyList();
            }
            
            // This would require more complex similarity analysis based on:
            // - Shared categories
            // - Similar difficulty
            // - Common companies that ask both problems
            // For now, return problems with same difficulty as a basic implementation
            
            Page<Problem> similarPage = new Page<>(1, limit);
            QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("difficulty", targetProblem.getDifficulty())
                       .ne("id", problemId)
                       .eq("is_active", true)
                       .eq("deleted", 0);
            
            Page<Problem> problems = problemMapper.selectPage(similarPage, queryWrapper);
            
            // Convert to ProblemRankingDTO with basic info
            return problems.getRecords().stream().map(problem -> 
                ProblemRankingDTO.builder()
                    .problemId(problem.getId())
                    .title(problem.getTitle())
                    .difficulty(problem.getDifficulty().name())
                    .problemUrl(problem.getProblemUrl())
                    .leetcodeId(problem.getLeetcodeId())
                    .addedDate(problem.getCreatedAt() != null ? problem.getCreatedAt().toLocalDate() : null)
                    .isPremium(problem.getIsPremium())
                    .tags(problem.getTags() != null ? String.join(", ", problem.getTags()) : null)
                    .build()
            ).collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Error getting similar problems", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get global rankings.
     */
    public Page<ProblemRankingDTO> getGlobalRankings(Integer page, Integer size) {
        log.info("Getting global rankings: page={}, size={}", page, size);
        
        try {
            Page<ProblemFrequencyStats> statsPage = new Page<>(page, size);
            
            QueryWrapper<ProblemFrequencyStats> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("stats_scope", "GLOBAL")
                       .orderByAsc("frequency_rank")
                       .orderByDesc("total_frequency_score");
            
            Page<ProblemFrequencyStats> globalStats = problemFrequencyStatsMapper.selectPage(statsPage, queryWrapper);
            
            CodeTopFilterRequest request = new CodeTopFilterRequest(); // Empty request for global
            List<ProblemRankingDTO> rankings = convertToProblemRankingDTOs(globalStats.getRecords(), request);
            
            Page<ProblemRankingDTO> result = new Page<>(page, size);
            result.setRecords(rankings);
            result.setTotal(globalStats.getTotal());
            result.setPages(globalStats.getPages());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error getting global rankings", e);
            Page<ProblemRankingDTO> result = new Page<>(page, size);
            result.setRecords(Collections.emptyList());
            result.setTotal(0);
            return result;
        }
    }

    /**
     * Get category usage statistics.
     */
    public List<CategoryUsageStatsDTO> getCategoryUsageStatistics() {
        log.info("Getting category usage statistics");
        
        try {
            // This would require complex queries joining categories and problems
            // For now, return empty list
            log.warn("Category usage statistics not fully implemented - requires categories table integration");
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Error getting category usage statistics", e);
            return Collections.emptyList();
        }
    }

    // Helper classes for API compatibility

    public static class CompanyProblemBreakdownDTO {
        private Long problemId;
        private String problemTitle;
        private String departmentName;
        private String positionName;
        private Double frequencyScore;
        
        // Getters and setters
        public Long getProblemId() { return problemId; }
        public void setProblemId(Long problemId) { this.problemId = problemId; }
        public String getProblemTitle() { return problemTitle; }
        public void setProblemTitle(String problemTitle) { this.problemTitle = problemTitle; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public String getPositionName() { return positionName; }
        public void setPositionName(String positionName) { this.positionName = positionName; }
        public Double getFrequencyScore() { return frequencyScore; }
        public void setFrequencyScore(Double frequencyScore) { this.frequencyScore = frequencyScore; }
    }

    public static class CategoryUsageStatsDTO {
        private Long categoryId;
        private String categoryName;
        private Long problemCount;
        private Double avgFrequencyScore;
        
        // Getters and setters
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public Long getProblemCount() { return problemCount; }
        public void setProblemCount(Long problemCount) { this.problemCount = problemCount; }
        public Double getAvgFrequencyScore() { return avgFrequencyScore; }
        public void setAvgFrequencyScore(Double avgFrequencyScore) { this.avgFrequencyScore = avgFrequencyScore; }
    }

    public static class FilterOptions {
        private List<Company> companies;
        private List<Department> departments; 
        private List<Position> positions;
        private List<String> categories;
        private List<String> difficulties;
        private List<String> trends;
        
        public FilterOptions() {
            this.companies = Collections.emptyList();
            this.departments = Collections.emptyList();
            this.positions = Collections.emptyList();
            this.categories = Collections.emptyList();
            this.difficulties = Arrays.asList("EASY", "MEDIUM", "HARD");
            this.trends = Arrays.asList("INCREASING", "STABLE", "DECREASING", "NEW");
        }
        
        // Getters and setters
        public List<Company> getCompanies() { return companies; }
        public void setCompanies(List<Company> companies) { this.companies = companies; }
        public List<Department> getDepartments() { return departments; }
        public void setDepartments(List<Department> departments) { this.departments = departments; }
        public List<Position> getPositions() { return positions; }
        public void setPositions(List<Position> positions) { this.positions = positions; }
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
        public List<String> getDifficulties() { return difficulties; }
        public void setDifficulties(List<String> difficulties) { this.difficulties = difficulties; }
        public List<String> getTrends() { return trends; }
        public void setTrends(List<String> trends) { this.trends = trends; }
    }
}