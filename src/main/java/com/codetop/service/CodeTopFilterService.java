package com.codetop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.codetop.dto.CodeTopFilterRequest;
import com.codetop.dto.CodeTopFilterResponse;
import com.codetop.dto.ProblemRankingDTO;
import com.codetop.entity.*;
import com.codetop.mapper.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import com.codetop.util.CacheHelper;

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
    private final FSRSCardMapper fsrsCardMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheHelper cacheHelper;

    /**
     * Get filtered and ranked problems using CodeTop-style filtering.
     */
    public CodeTopFilterResponse getFilteredProblems(CodeTopFilterRequest request) {
        log.info("Processing CodeTop filter request: company={}, department={}, position={}, keyword={}", 
                request.getCompanyId(), request.getDepartmentId(), request.getPositionId(), request.getKeyword());

        // Safer cache key: hash of full request + paging
        String cacheKey = com.codetop.service.CacheKeyBuilder.buildHashKey(
                "codetop",
                request,
                "filter",
                "page_" + request.getPage(),
                "size_" + request.getSize()
        );

        return cacheHelper.cacheOrCompute(cacheKey, CodeTopFilterResponse.class, Duration.ofMinutes(15), () -> {
            try {
                Page<ProblemFrequencyStats> page = new Page<>(request.getPage(), request.getSize());
                Page<ProblemFrequencyStats> statsPage = buildFilteredQuery(request, page);
                List<ProblemRankingDTO> problemRankings = convertToProblemRankingDTOs(statsPage.getRecords(), request);
                CodeTopFilterResponse.FilterSummary summary = buildFilterSummary(statsPage.getRecords());

                CodeTopFilterResponse response = CodeTopFilterResponse.builder()
                        .problems(problemRankings)
                        .totalElements(statsPage.getTotal())
                        .currentPage((long) request.getPage())
                        .pageSize((long) request.getSize())
                        .totalPages(statsPage.getPages())
                        .summary(summary)
                        .build();
                log.info("CodeTop filter completed: found {} problems", problemRankings.size());
                return response;
            } catch (Exception e) {
                log.error("Error processing CodeTop filter request: {}", e.getMessage(), e);
                return CodeTopFilterResponse.builder()
                        .problems(Collections.emptyList())
                        .totalElements(0L)
                        .currentPage((long) request.getPage())
                        .pageSize((long) request.getSize())
                        .totalPages(0L)
                        .build();
            }
        });
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
                    .tags(problem.getTags())
                    .build();
                    
            } catch (Exception e) {
                log.error("Error converting stat to DTO for problem ID: {}", stat.getProblemId(), e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    
    /**
     * Convert ProblemFrequencyStats to ProblemRankingDTO with integrated user status.
     * This version includes user-specific progress data from FSRS cards.
     */
    private List<ProblemRankingDTO> convertToProblemRankingDTOsWithUserStatus(
            List<ProblemFrequencyStats> stats, Map<Long, FSRSCard> userCardsMap) {
        return stats.stream().map(stat -> {
            try {
                // Get problem details
                Problem problem = problemMapper.selectById(stat.getProblemId());
                if (problem == null) {
                    log.warn("Problem not found for ID: {}", stat.getProblemId());
                    return null;
                }
                
                // Get user's FSRS card for this problem
                FSRSCard userCard = userCardsMap.get(stat.getProblemId());
                
                // Extract user status from FSRS card
                Integer mastery = 0;
                String status = "not_done";
                String notes = "";
                java.time.LocalDateTime lastAttemptDate = null;
                java.time.LocalDateTime lastConsideredDate = null;
                Integer attemptCount = 0;
                Double accuracy = 0.0;
                
                if (userCard != null) {
                    // Determine status based on FSRS card state
                    status = determineStatusFromFSRSCard(userCard);
                    
                    // Calculate mastery level based on FSRS stability and difficulty
                    mastery = calculateMasteryFromFSRS(userCard);
                    
                    // Extract other user-specific data
                    notes = ""; // FSRSCard doesn't have notes field
                    lastAttemptDate = userCard.getLastReview();
                    lastConsideredDate = userCard.getNextReview();
                    attemptCount = userCard.getReviewCount() != null ? userCard.getReviewCount() : 0;
                    accuracy = calculateAccuracyFromFSRS(userCard);
                }
                
                // Calculate additional scores (same as before)
                double recencyScore = calculateRecencyScore(stat.getLastAskedDate());
                boolean isHotProblem = stat.getTotalFrequencyScore() != null && 
                                     stat.getTotalFrequencyScore().compareTo(BigDecimal.valueOf(60)) >= 0;
                boolean isTrending = "INCREASING".equals(String.valueOf(stat.getFrequencyTrend()));
                boolean isTopPercentile = stat.getPercentile() != null && 
                                        stat.getPercentile().compareTo(BigDecimal.valueOf(90)) >= 0;
                
                return ProblemRankingDTO.builder()
                    // Basic problem info
                    .problemId(problem.getId())
                    .title(problem.getTitle())
                    .difficulty(problem.getDifficulty().name())
                    .problemUrl(problem.getProblemUrl())
                    .leetcodeId(problem.getLeetcodeId())
                    
                    // User-specific status
                    .mastery(mastery)
                    .status(status)
                    .notes(notes)
                    .lastAttemptDate(lastAttemptDate)
                    .lastConsideredDate(lastConsideredDate)
                    .attemptCount(attemptCount)
                    .accuracy(accuracy)
                    
                    // Frequency statistics
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
                    
                    // Additional metadata
                    .statsScope(String.valueOf(stat.getStatsScope()))
                    .addedDate(problem.getCreatedAt() != null ? problem.getCreatedAt().toLocalDate() : null)
                    .isPremium(problem.getIsPremium())
                    .tags(problem.getTags())
                    .build();
                    
            } catch (Exception e) {
                log.error("Error converting stat to DTO with user status for problem ID: {}", stat.getProblemId(), e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    
    /**
     * Determine user status from FSRS card state with enhanced logic for attempted status.
     */
    private String determineStatusFromFSRSCard(FSRSCard card) {
        if (card == null) {
            log.debug("FSRS card is null, returning not_done");
            return "not_done";
        }
        
        log.debug("Determining status for card: problemId={}, state={}, reviewCount={}", 
                card.getProblemId(), card.getState(), card.getReviewCount());
        
        switch (card.getState()) {
            case NEW:
                // If the card has been reviewed (review count > 0) but still in NEW state,
                // it means the user attempted but struggled (got Hard ratings)
                if (card.getReviewCount() != null && card.getReviewCount() > 0) {
                    log.debug("Card problemId={} has NEW state with reviewCount={}, returning attempted", 
                            card.getProblemId(), card.getReviewCount());
                    return "attempted";
                }
                log.debug("Card problemId={} has NEW state with reviewCount={}, returning not_done", 
                        card.getProblemId(), card.getReviewCount());
                return "not_done";
            case LEARNING:
            case REVIEW:
                log.debug("Card problemId={} has state={}, returning done", 
                        card.getProblemId(), card.getState());
                return "done";
            case RELEARNING:
                log.debug("Card problemId={} has RELEARNING state, returning reviewed", 
                        card.getProblemId());
                return "reviewed";
            default:
                log.debug("Card problemId={} has unknown state={}, returning not_done", 
                        card.getProblemId(), card.getState());
                return "not_done";
        }
    }
    
    /**
     * Calculate mastery level from FSRS card data.
     */
    private Integer calculateMasteryFromFSRS(FSRSCard card) {
        if (card == null || card.getStability() == null) {
            return 0;
        }
        
        // Convert FSRS stability to mastery stars (0-3)
        double stability = card.getStability().doubleValue();
        if (stability >= 30) {
            return 3; // 3 stars for high stability (>30 days)
        } else if (stability >= 10) {
            return 2; // 2 stars for medium stability (10-30 days)
        } else if (stability >= 1) {
            return 1; // 1 star for low stability (1-10 days)
        } else {
            return 0; // 0 stars for very low stability (<1 day)
        }
    }
    
    /**
     * Calculate accuracy from FSRS card data.
     */
    private Double calculateAccuracyFromFSRS(FSRSCard card) {
        if (card == null || card.getStability() == null || card.getDifficulty() == null) {
            return 0.0;
        }
        
        // Simple accuracy calculation based on stability and difficulty
        // Higher stability and lower difficulty indicate better mastery
        double stability = card.getStability().doubleValue();
        double difficulty = card.getDifficulty().doubleValue();
        
        // Normalize to 0-100 scale
        double accuracy = Math.max(0, Math.min(100, (stability * 10) - (difficulty * 20)));
        return accuracy;
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
        String cacheKey = CacheKeyBuilder.buildKey("codetop", "trending",
                "limit_" + limit,
                "days_" + days);

        return cacheHelper.cacheOrComputeList(cacheKey, ProblemRankingDTO.class, Duration.ofMinutes(15), () -> {
            try {
                QueryWrapper<ProblemFrequencyStats> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("frequency_trend", "INCREASING")
                           .orderByDesc("total_frequency_score")
                           .last("LIMIT " + limit);

                List<ProblemFrequencyStats> trendingStats = problemFrequencyStatsMapper.selectList(queryWrapper);
                CodeTopFilterRequest request = new CodeTopFilterRequest();
                return convertToProblemRankingDTOs(trendingStats, request);
            } catch (Exception e) {
                log.error("Error getting trending problems", e);
                return Collections.emptyList();
            }
        });
    }

    /**
     * Get hot problems.
     */
    public List<ProblemRankingDTO> getHotProblems(Long companyId, Integer limit) {
        log.info("Getting hot problems for company={}, limit={}", companyId, limit);
        String cacheKey = CacheKeyBuilder.buildKey("codetop", "hot",
                companyId != null ? "companyId_" + companyId : null,
                "limit_" + limit);

        return cacheHelper.cacheOrComputeList(cacheKey, ProblemRankingDTO.class, Duration.ofMinutes(15), () -> {
            try {
                QueryWrapper<ProblemFrequencyStats> queryWrapper = new QueryWrapper<>();
                queryWrapper.ge("total_frequency_score", 60.0);
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
        });
    }

    /**
     * Get top problems by frequency.
     */
    public List<ProblemRankingDTO> getTopProblemsByFrequency(String scope, Long companyId, 
                                                            Long departmentId, Long positionId, Integer limit) {
        log.info("Getting top problems by frequency: scope={}, company={}, department={}, position={}, limit={}",
                scope, companyId, departmentId, positionId, limit);
        String cacheKey = CacheKeyBuilder.buildKey("codetop", "topfreq",
                scope != null ? "scope_" + scope.toUpperCase() : null,
                companyId != null ? "companyId_" + companyId : null,
                departmentId != null ? "departmentId_" + departmentId : null,
                positionId != null ? "positionId_" + positionId : null,
                "limit_" + limit);

        return cacheHelper.cacheOrComputeList(cacheKey, ProblemRankingDTO.class, Duration.ofMinutes(15), () -> {
            try {
                QueryWrapper<ProblemFrequencyStats> queryWrapper = new QueryWrapper<>();
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
        });
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
        String cacheKey = CacheKeyBuilder.buildKey("codetop", "similar",
                "problemId_" + problemId,
                "limit_" + limit);

        return cacheHelper.cacheOrComputeList(cacheKey, ProblemRankingDTO.class, Duration.ofMinutes(30), () -> {
            try {
                Problem targetProblem = problemMapper.selectById(problemId);
                if (targetProblem == null) {
                    log.warn("Target problem not found: {}", problemId);
                    return Collections.emptyList();
                }
                Page<Problem> similarPage = new Page<>(1, limit);
                QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("difficulty", targetProblem.getDifficulty())
                           .ne("id", problemId)
                           .eq("is_active", true)
                           .eq("deleted", 0);
                Page<Problem> problems = problemMapper.selectPage(similarPage, queryWrapper);
                return problems.getRecords().stream().map(problem ->
                    ProblemRankingDTO.builder()
                        .problemId(problem.getId())
                        .title(problem.getTitle())
                        .difficulty(problem.getDifficulty().name())
                        .problemUrl(problem.getProblemUrl())
                        .leetcodeId(problem.getLeetcodeId())
                        .addedDate(problem.getCreatedAt() != null ? problem.getCreatedAt().toLocalDate() : null)
                        .isPremium(problem.getIsPremium())
                        .tags(problem.getTags())
                        .build()
                ).collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Error getting similar problems", e);
                return Collections.emptyList();
            }
        });
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
     * Get global problems with full pagination info.
     * Only returns GLOBAL scope statistics to avoid duplicates.
     */
    public CodeTopFilterResponse getGlobalProblems(Integer page, Integer size, String sortBy, String sortOrder) {
        log.info("Getting global problems: page={}, size={}, sortBy={}, sortOrder={}", page, size, sortBy, sortOrder);
        String cacheKey = CacheKeyBuilder.codetopGlobalProblems(page, size, sortBy, sortOrder);
        return cacheHelper.cacheOrCompute(cacheKey, CodeTopFilterResponse.class, Duration.ofHours(1), () -> {
            try {
                Page<ProblemFrequencyStats> statsPage = new Page<>(page, size);
                QueryWrapper<ProblemFrequencyStats> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("stats_scope", "GLOBAL");
                boolean isDesc = "desc".equalsIgnoreCase(sortOrder);
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
                queryWrapper.orderByAsc("frequency_rank");
                Page<ProblemFrequencyStats> globalStats = problemFrequencyStatsMapper.selectPage(statsPage, queryWrapper);
                CodeTopFilterRequest emptyRequest = new CodeTopFilterRequest();
                List<ProblemRankingDTO> problemRankings = convertToProblemRankingDTOs(globalStats.getRecords(), emptyRequest);
                CodeTopFilterResponse.FilterSummary summary = buildFilterSummary(globalStats.getRecords());
                CodeTopFilterResponse response = CodeTopFilterResponse.builder()
                        .problems(problemRankings)
                        .totalElements(globalStats.getTotal())
                        .currentPage((long) page)
                        .pageSize((long) size)
                        .totalPages(globalStats.getPages())
                        .summary(summary)
                        .build();
                log.info("Global problems query completed: found {} problems", problemRankings.size());
                return response;
            } catch (Exception e) {
                log.error("Error getting global problems: {}", e.getMessage(), e);
                return CodeTopFilterResponse.builder()
                        .problems(Collections.emptyList())
                        .totalElements(0L)
                        .currentPage((long) page)
                        .pageSize((long) size)
                        .totalPages(0L)
                        .build();
            }
        });
    }

    /**
     * Get global problems with integrated user status.
     * This method combines problem rankings with user-specific progress data.
     */
    public CodeTopFilterResponse getGlobalProblemsWithUserStatus(Long userId, Integer page, Integer size, String sortBy, String sortOrder) {
        log.info("Getting global problems with user status: userId={}, page={}, size={}, sortBy={}, sortOrder={}", 
                 userId, page, size, sortBy, sortOrder);
        
        String cacheKey = CacheKeyBuilder.codetopGlobalProblemsWithUserStatus(userId, page, size, sortBy, sortOrder);
        return cacheHelper.cacheOrCompute(cacheKey, CodeTopFilterResponse.class, Duration.ofMinutes(15), () -> {
            try {
            // Get base global problems data
            Page<ProblemFrequencyStats> statsPage = new Page<>(page, size);
            
            // Build query for GLOBAL scope only
            QueryWrapper<ProblemFrequencyStats> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("stats_scope", "GLOBAL");
            
            // Sorting
            boolean isDesc = "desc".equalsIgnoreCase(sortOrder);
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
            
            Page<ProblemFrequencyStats> globalStats = problemFrequencyStatsMapper.selectPage(statsPage, queryWrapper);
            
            // Get all problem IDs from the current page
            List<Long> problemIds = globalStats.getRecords().stream()
                    .map(ProblemFrequencyStats::getProblemId)
                    .collect(Collectors.toList());
            
            // Get user's FSRS cards for these problems in one query
            Map<Long, FSRSCard> userCardsMap = Collections.emptyMap();
            if (!problemIds.isEmpty()) {
                QueryWrapper<FSRSCard> cardQuery = new QueryWrapper<>();
                cardQuery.eq("user_id", userId)
                        .in("problem_id", problemIds);
                List<FSRSCard> userCards = fsrsCardMapper.selectList(cardQuery);
                userCardsMap = userCards.stream()
                        .collect(Collectors.toMap(FSRSCard::getProblemId, card -> card));
            }
            
            // Convert to ProblemRankingDTO list with user status integrated
            List<ProblemRankingDTO> problemRankings = convertToProblemRankingDTOsWithUserStatus(
                    globalStats.getRecords(), userCardsMap);
            
            // Build summary statistics
            CodeTopFilterResponse.FilterSummary summary = buildFilterSummary(globalStats.getRecords());
            
            CodeTopFilterResponse response = CodeTopFilterResponse.builder()
                .problems(problemRankings)
                .totalElements(globalStats.getTotal())
                .currentPage((long) page)
                .pageSize((long) size)
                .totalPages(globalStats.getPages())
                .summary(summary)
                .build();
            
                log.info("Global problems with user status query completed: found {} problems for user {}", 
                         problemRankings.size(), userId);
                return response;
            } catch (Exception e) {
                log.error("Error getting global problems with user status: {}", e.getMessage(), e);
                return CodeTopFilterResponse.builder()
                    .problems(Collections.emptyList())
                    .totalElements(0L)
                    .currentPage((long) page)
                    .pageSize((long) size)
                    .totalPages(0L)
                    .build();
            }
        });
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

    @Setter
    @Getter
    public static class CompanyProblemBreakdownDTO {
        // Getters and setters
        private Long problemId;
        private String problemTitle;
        private String departmentName;
        private String positionName;
        private Double frequencyScore;

    }

    @Setter
    @Getter
    public static class CategoryUsageStatsDTO {
        // Getters and setters
        private Long categoryId;
        private String categoryName;
        private Long problemCount;
        private Double avgFrequencyScore;

    }

    @Setter
    @Getter
    public static class FilterOptions {
        // Getters and setters
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

    }
    
    /**
     * Clear user-specific problem status cache after status updates.
     * This ensures the unified API returns the latest user progress data.
     */
    public void clearUserStatusCache(Long userId) {
        if (userId == null) {
            return;
        }
        
        try {
            String pattern = CacheKeyBuilder.codetopUserStatusDomain(userId);
            java.util.Set<String> keys = new java.util.HashSet<>();
            org.springframework.data.redis.connection.RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            org.springframework.data.redis.core.ScanOptions options = org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match(pattern)
                    .count(1000)
                    .build();
            try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} user status cache entries for user {} (SCAN)", keys.size(), userId);
            } else {
                log.debug("No user status cache entries found for user {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to clear user status cache for user {}: {}", userId, e.getMessage(), e);
        }
    }
}
