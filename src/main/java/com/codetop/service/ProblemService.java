package com.codetop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.dto.*;
import com.codetop.entity.Problem;
import com.codetop.enums.Difficulty;
import com.codetop.event.Events;
import com.codetop.mapper.ProblemMapper;
import com.codetop.mapper.FSRSCardMapper;
import com.codetop.entity.FSRSCard;
import com.codetop.enums.ReviewType;
import com.codetop.enums.FSRSState;
import com.codetop.service.cache.CacheService;
import com.codetop.util.CacheHelper;
import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.service.LlmToggleService;
import com.codetop.recommendation.service.RecommendationStrategyResolver;
import com.codetop.recommendation.service.RecommendationType;
import com.codetop.recommendation.service.RecommendationStrategy;
import com.codetop.recommendation.service.RequestContext;
import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationItemDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codetop.logging.TraceContext;
import org.springframework.cache.annotation.Cacheable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * Problem service for managing algorithm problems.
 * 
 * Features:
 * - Simplified problem management for FSRS algorithm
 * - Problem search and filtering by title and tags
 * - Company-based problem queries
 * - Basic statistics
 * - Tag management
 * - Caching for performance
 * 
 * @author CodeTop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProblemService {
    
    private final ProblemMapper problemMapper;
    private final FSRSCardMapper fsrsCardMapper;
    private final FSRSService fsrsService;
    private final CacheInvalidationManager cacheInvalidationManager;
    private final ApplicationEventPublisher eventPublisher;
    
    // 新增缓存相关依赖
    private final CacheService cacheService;
    private final CacheHelper cacheHelper;
    
    // AI 推荐相关依赖 (可选依赖)
    @Autowired(required = false)
    private RecommendationStrategyResolver recommendationStrategyResolver;
    
    // LLM Toggle Service for centralized feature control
    @Autowired(required = false)
    private LlmToggleService llmToggleService;
    
    @Autowired(required = false)
    private LlmProperties llmProperties;
    
    // 缓存相关常量
    private static final String CACHE_PREFIX_PROBLEM_SINGLE = "problem-single";
    private static final String CACHE_PREFIX_PROBLEM_LIST = "problem-list";
    private static final String CACHE_PREFIX_PROBLEM_SEARCH = "problem-search";
    private static final String CACHE_PREFIX_PROBLEM_ADVANCED = "problem-advanced";
    private static final String CACHE_PREFIX_PROBLEM_ENHANCED = "problem-enhanced";
    private static final String CACHE_PREFIX_PROBLEM_DIFFICULTY = "problem-difficulty";
    private static final String CACHE_PREFIX_PROBLEM_HOT = "problem-hot";
    private static final String CACHE_PREFIX_PROBLEM_RECENT = "problem-recent";
    private static final String CACHE_PREFIX_PROBLEM_STATS = "problem-stats";
    private static final String CACHE_PREFIX_TAG_STATS = "tag-stats";
    private static final String CACHE_PREFIX_USER_PROGRESS = "user-progress";
    private static final String CACHE_PREFIX_USER_MASTERY = "user-mastery";
    
    private static final Duration PROBLEM_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration STATS_CACHE_TTL = Duration.ofHours(1);
    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);

    /**
     * Get problem by ID with manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrCompute() 进行缓存操作
     * - 保持相同的缓存键和30分钟TTL
     */
    public Optional<Problem> findById(Long problemId) {
        if (problemId == null) {
            log.warn("findById called with null problemId");
            return Optional.empty();
        }
        
        TraceContext.setOperation("PROBLEM_FIND_BY_ID");
        
        String cacheKey = CacheKeyBuilder.problemSingle(problemId);
        
        Problem cachedProblem = cacheHelper.cacheOrCompute(
            cacheKey,
            Problem.class,
            PROBLEM_CACHE_TTL,
            () -> {
                long startTime = System.currentTimeMillis();
                log.debug("Cache MISS - Finding problem by ID: problemId={}", problemId);
                
                try {
                    Problem problem = problemMapper.selectById(problemId);
                    
                    long duration = System.currentTimeMillis() - startTime;
                    
                    if (problem != null) {
                        log.debug("Problem found successfully: problemId={}, title='{}', duration={}ms", 
                                problemId, problem.getTitle(), duration);
                    } else {
                        log.debug("Problem not found: problemId={}, duration={}ms", problemId, duration);
                    }
                    
                    return problem;
                } catch (Exception e) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Failed to find problem: problemId={}, duration={}ms, error={}", 
                            problemId, duration, e.getMessage(), e);
                    throw e;
                }
            }
        );
        
        return Optional.ofNullable(cachedProblem);
    }

    /**
     * Find all problems with optional filters and manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrCompute() 进行缓存操作
     * - 保持相同的缓存键和30分钟TTL
     */
    public Page<Problem> findAllProblems(Page<Problem> page, String difficulty, String search) {
        TraceContext.setOperation("PROBLEM_FIND_ALL");
        
        String cacheKey = CacheKeyBuilder.problemList(difficulty, (int)page.getCurrent(), (int)page.getSize(), search);
        
        return cacheHelper.cacheOrCompute(
            cacheKey,
            Page.class,
            PROBLEM_CACHE_TTL,
            () -> {
                long startTime = System.currentTimeMillis();
                log.debug("Cache MISS - Finding all problems: page={}, size={}, difficulty='{}', search='{}'", 
                        page.getCurrent(), page.getSize(), difficulty, search);
                
                try {
                    Page<Problem> result;
                    
                    if (search != null && !search.trim().isEmpty()) {
                        result = problemMapper.searchProblemsByKeyword(page, search.trim(), difficulty);
                        log.debug("Search query executed: keyword='{}', difficulty='{}'", search.trim(), difficulty);
                    } else if (difficulty != null && !difficulty.trim().isEmpty()) {
                        result = problemMapper.findByDifficultyWithPagination(page, difficulty);
                        log.debug("Difficulty filter applied: difficulty='{}'", difficulty);
                    } else {
                        result = problemMapper.selectPage(page, null);
                        log.debug("No filters applied, returning all problems");
                    }

                    long duration = System.currentTimeMillis() - startTime;
                    long actualRecords = result.getRecords().size();
                    
                    log.info("Problems found successfully: page={}, size={}, difficulty='{}', search='{}', " +
                            "actualRecords={}, totalRecords={}, duration={}ms", 
                            page.getCurrent(), page.getSize(), difficulty, search, 
                            actualRecords, result.getTotal(), duration);

                    return result;
                    
                } catch (Exception e) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Failed to find all problems: page={}, size={}, difficulty='{}', search='{}', " +
                            "duration={}ms, error={}", 
                            page.getCurrent(), page.getSize(), difficulty, search, duration, e.getMessage(), e);
                    throw e;
                }
            }
        );
    }

    /**
     * Search problems with pagination and manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrCompute() 进行缓存操作
     * - 保持相同的缓存键和30分钟TTL
     */
    public Page<Problem> searchProblems(String keyword, Page<Problem> page) {
        String cacheKey = CacheKeyBuilder.problemSearch(keyword, (int)page.getCurrent(), (int)page.getSize());
        
        return cacheHelper.cacheOrCompute(
            cacheKey,
            Page.class,
            PROBLEM_CACHE_TTL,
            () -> {
                log.debug("Cache MISS - Searching problems: keyword='{}', page={}, size={}", keyword, page.getCurrent(), page.getSize());
                
                if (keyword == null || keyword.trim().isEmpty()) {
                    return problemMapper.selectPage(page, null);
                }
                return problemMapper.searchProblems(page, keyword.trim());
            }
        );
    }

    /**
     * Advanced search with multiple filters and manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrCompute() 进行缓存操作
     * - 保持相同的缓存键和30分钟TTL
     */
    public Page<Problem> advancedSearch(AdvancedSearchRequest request, Page<Problem> page) {
        TraceContext.setOperation("PROBLEM_ADVANCED_SEARCH");
        
        String cacheKey = CacheKeyBuilder.problemAdvancedSearch(request, (int)page.getCurrent(), (int)page.getSize());
        
        return cacheHelper.cacheOrCompute(
            cacheKey,
            Page.class,
            PROBLEM_CACHE_TTL,
            () -> {
                long startTime = System.currentTimeMillis();
                log.debug("Cache MISS - Advanced search: keyword='{}', difficulty={}, tag='{}', difficulties={}, statuses={}, tags={}, userId={}", 
                        request.getKeyword(), request.getDifficulty(), request.getTag(), 
                        request.getDifficulties(), request.getStatuses(), request.getTags(), request.getUserId());
                
                try {
                    Page<Problem> baseResults;
                    
                    // Execute database query based on primary filters
                    if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                        String difficultyStr = request.getDifficulty() != null ? request.getDifficulty().name() : null;
                        baseResults = problemMapper.searchProblemsByKeyword(page, request.getKeyword().trim(), difficultyStr);
                    } else if (request.getTag() != null && !request.getTag().trim().isEmpty()) {
                        baseResults = problemMapper.findByTagWithPagination(page, request.getTag().trim());
                    } else if (request.getDifficulty() != null) {
                        baseResults = problemMapper.findByDifficultyWithPagination(page, request.getDifficulty().name());
                    } else {
                        baseResults = problemMapper.selectPage(page, null);
                    }
                    
                    // Apply additional filtering if needed
                    Page<Problem> filteredResults = enhancedFilterResults(baseResults, request);
                    
                    long duration = System.currentTimeMillis() - startTime;
                    
                    log.info("Advanced search completed: keyword='{}', difficulty={}, results={}, duration={}ms", 
                            request.getKeyword(), request.getDifficulty(), filteredResults.getRecords().size(), duration);
                    
                    return filteredResults;
                    
                } catch (Exception e) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Advanced search failed: keyword='{}', difficulty={}, duration={}ms, error={}", 
                            request.getKeyword(), request.getDifficulty(), duration, e.getMessage(), e);
                    throw e;
                }
            }
        );
    }

    /**
     * Enhanced search with comprehensive filters for CodeTop page and manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrCompute() 进行缓存操作
     * - 保持相同的缓存键和30分钟TTL
     */
    public Page<Problem> enhancedSearch(EnhancedSearchRequest request, Page<Problem> page) {
        TraceContext.setOperation("PROBLEM_ENHANCED_SEARCH");
        
        String cacheKey = CacheKeyBuilder.problemEnhancedSearch(request, (int)page.getCurrent(), (int)page.getSize());
        
        return cacheHelper.cacheOrCompute(
            cacheKey,
            Page.class,
            PROBLEM_CACHE_TTL,
            () -> {
                long startTime = System.currentTimeMillis();
                log.debug("Cache MISS - Enhanced search: search='{}', difficulties={}, statuses={}, tags={}, userId={}, sort='{}'", 
                        request.getSearch(), request.getDifficulties(), request.getStatuses(), 
                        request.getTags(), request.getUserId(), request.getSort());
                
                try {
                    Page<Problem> baseResults;
                    
                    // Execute database query based on search criteria
                    if (request.getSearch() != null && !request.getSearch().trim().isEmpty()) {
                        baseResults = problemMapper.searchProblemsByKeyword(page, request.getSearch().trim(), null);
                    } else {
                        baseResults = problemMapper.selectPage(page, null);
                    }
                    
                    // Apply enhanced filtering
                    Page<Problem> filteredResults = applyEnhancedFilters(baseResults, request, page);
                    
                    long duration = System.currentTimeMillis() - startTime;
                    
                    log.info("Enhanced search completed: search='{}', results={}, duration={}ms", 
                            request.getSearch(), filteredResults.getRecords().size(), duration);
                    
                    return filteredResults;
                    
                } catch (Exception e) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Enhanced search failed: search='{}', duration={}ms, error={}", 
                            request.getSearch(), duration, e.getMessage(), e);
                    throw e;
                }
            }
        );
    }

    /**
     * Get problems by difficulty with manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrCompute() 进行缓存操作
     * - 保持相同的缓存键和30分钟TTL
     */
    public Page<Problem> findByDifficulty(Difficulty difficulty, Page<Problem> page) {
        String cacheKey = CacheKeyBuilder.problemsByDifficulty(difficulty.name(), (int)page.getCurrent());
        
        return cacheHelper.cacheOrCompute(
            cacheKey,
            Page.class,
            PROBLEM_CACHE_TTL,
            () -> {
                log.debug("Cache MISS - Finding problems by difficulty: difficulty={}, page={}", difficulty.name(), page.getCurrent());
                return problemMapper.findByDifficultyWithPagination(page, difficulty.name());
            }
        );
    }

    /**
     * Get problems by tag.
     */
    public Page<Problem> findByTag(String tag, Page<Problem> page) {
        return problemMapper.findByTag(page, tag);
    }

    /**
     * Get problems by company.
     */
    public Page<Problem> findByCompany(Long companyId, Page<Problem> page) {
        return problemMapper.findByCompanyId(page, companyId);
    }

    /**
     * Get problems by company name.
     */
    public Page<Problem> findByCompanyName(String companyName, Page<Problem> page) {
        return problemMapper.findByCompanyName(page, companyName);
    }

    /**
     * Get hot problems (frequently asked) with manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrComputeList() 进行列表缓存操作
     * - 保持相同的缓存键和30分钟TTL
     */
    public List<ProblemMapper.HotProblem> getHotProblems(int minCompanies, int limit) {
        String cacheKey = CacheKeyBuilder.problemsHot(minCompanies, limit);
        
        return cacheHelper.cacheOrComputeList(
            cacheKey,
            ProblemMapper.HotProblem.class,
            PROBLEM_CACHE_TTL,
            () -> {
                log.debug("Cache MISS - Loading hot problems: minCompanies={}, limit={}", minCompanies, limit);
                
                try {
                    List<ProblemMapper.HotProblem> hotProblems = problemMapper.findHotProblems(minCompanies, limit);
                    
                    log.debug("Retrieved hot problems: count={}", hotProblems != null ? hotProblems.size() : 0);
                    
                    return hotProblems != null ? hotProblems : List.of();
                } catch (Exception e) {
                    log.error("Failed to get hot problems: minCompanies={}, limit={}, error={}", minCompanies, limit, e.getMessage(), e);
                    throw e;
                }
            }
        );
    }

    /**
     * Get recently added problems with manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrComputeList() 进行列表缓存操作
     * - 保持相同的缓存键和30分钟TTL
     */
    public List<Problem> getRecentProblems(int limit) {
        String cacheKey = CacheKeyBuilder.problemsRecent(limit);
        
        return cacheHelper.cacheOrComputeList(
            cacheKey,
            Problem.class,
            PROBLEM_CACHE_TTL,
            () -> {
                log.debug("Cache MISS - Loading recent problems: limit={}", limit);
                
                try {
                    List<Problem> recentProblems = problemMapper.findRecentProblems(limit);
                    
                    log.debug("Retrieved recent problems: count={}", recentProblems != null ? recentProblems.size() : 0);
                    
                    return recentProblems != null ? recentProblems : List.of();
                } catch (Exception e) {
                    log.error("Failed to get recent problems: limit={}, error={}", limit, e.getMessage(), e);
                    throw e;
                }
            }
        );
    }

    /**
     * Get all tags with usage statistics with manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrComputeList() 进行列表缓存操作
     * - 保持相同的缓存键和1小时TTL
     */
    public List<ProblemMapper.TagUsage> getTagStatistics() {
        String cacheKey = CacheKeyBuilder.tagStatistics();
        
        return cacheHelper.cacheOrComputeList(
            cacheKey,
            ProblemMapper.TagUsage.class,
            STATS_CACHE_TTL,
            () -> {
                log.debug("Cache MISS - Loading tag usage statistics from database");
                
                try {
                    List<ProblemMapper.TagUsage> tagStats = problemMapper.getTagUsageStatistics();
                    
                    log.debug("Retrieved tag usage statistics: count={}", tagStats != null ? tagStats.size() : 0);
                    
                    return tagStats != null ? tagStats : List.of();
                } catch (Exception e) {
                    log.error("Failed to get tag usage statistics: error={}", e.getMessage(), e);
                    throw e;
                }
            }
        );
    }

    /**
     * Get problem statistics with manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrCompute() 进行缓存操作
     * - 保持相同的缓存键和1小时TTL（统计数据适合较长缓存）
     */
    public ProblemStatisticsDTO getStatistics() {
        String cacheKey = CacheKeyBuilder.problemStatistics();
        
        return cacheHelper.cacheOrCompute(
            cacheKey,
            ProblemStatisticsDTO.class,
            STATS_CACHE_TTL,
            () -> {
                log.debug("Cache MISS - Loading problem statistics from database");
                
                try {
                    Long totalProblems = problemMapper.countActiveProblems();
                    Long easyCount = problemMapper.countByDifficulty(Difficulty.EASY.name());
                    Long mediumCount = problemMapper.countByDifficulty(Difficulty.MEDIUM.name());
                    Long hardCount = problemMapper.countByDifficulty(Difficulty.HARD.name());
                    
                    log.debug("Retrieved problem statistics: total={}, easy={}, medium={}, hard={}", 
                            totalProblems, easyCount, mediumCount, hardCount);
                    
                    return ProblemStatisticsDTO.builder()
                            .totalProblems(totalProblems != null ? totalProblems : 0L)
                            .easyProblems(easyCount != null ? easyCount : 0L)
                            .mediumProblems(mediumCount != null ? mediumCount : 0L)
                            .hardProblems(hardCount != null ? hardCount : 0L)
                            .build();
                } catch (Exception e) {
                    log.error("Failed to get problem statistics: error={}", e.getMessage(), e);
                    throw e;
                }
            }
        );
    }

    /**
     * Create new problem.
     */
    @Transactional
    public Problem createProblem(CreateProblemRequest request) {
        Problem problem = Problem.builder()
                .title(request.getTitle())
                .difficulty(request.getDifficulty())
                .tags(request.getTagsJson())
                .problemUrl(request.getProblemUrl())
                .leetcodeId(request.getLeetcodeId())
                .isPremium(request.getIsPremium())
                .build();

        problemMapper.insert(problem);
        log.info("Created new problem: {}", problem.getTitle());
        
        // Publish event for cache invalidation
        eventPublisher.publishEvent(new com.codetop.event.Events.ProblemEvent(
            com.codetop.event.Events.ProblemEvent.ProblemEventType.CREATED,
            problem.getId(),
            problem.getTitle()
        ));
        
        return problem;
    }

    /**
     * Update problem.
     */
    @Transactional
    public Problem updateProblem(Long problemId, UpdateProblemRequest request) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new IllegalArgumentException("Problem not found");
        }

        if (request.getTitle() != null) problem.setTitle(request.getTitle());
        if (request.getDifficulty() != null) problem.setDifficulty(request.getDifficulty());
        if (request.getTagsJson() != null) problem.setTags(request.getTagsJson());
        if (request.getProblemUrl() != null) problem.setProblemUrl(request.getProblemUrl());

        problemMapper.updateById(problem);
        log.info("Updated problem: {}", problem.getTitle());
        
        // Publish event for cache invalidation
        eventPublisher.publishEvent(new com.codetop.event.Events.ProblemEvent(
            com.codetop.event.Events.ProblemEvent.ProblemEventType.UPDATED,
            problem.getId(),
            problem.getTitle()
        ));
        
        return problem;
    }

    /**
     * Delete problem (soft delete).
     */
    @Transactional
    public void deleteProblem(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new IllegalArgumentException("Problem not found");
        }

        String problemTitle = problem.getTitle();
        problem.softDelete();
        problemMapper.updateById(problem);
        log.info("Soft deleted problem: {}", problemTitle);
        
        // Publish event for cache invalidation
        eventPublisher.publishEvent(new com.codetop.event.Events.ProblemEvent(
            com.codetop.event.Events.ProblemEvent.ProblemEventType.DELETED,
            problemId,
            problemTitle
        ));
    }

    // DTOs

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProblemStatistics {
        private Long totalProblems;
        private Long easyCount;
        private Long mediumCount;
        private Long hardCount;
        private Long premiumCount;
    }

    // Enhanced search DTOs and helper methods
    
    /**
     * Enhanced search request with comprehensive filtering options.
     */
    @lombok.Data
    public static class EnhancedSearchRequest {
        private String search;
        private List<String> difficulties;
        private List<String> statuses;
        private List<String> tags;
        private Long userId;
        private String sort;
    }
    
    /**
     * Apply enhanced filters to problem results.
     */
    private Page<Problem> applyEnhancedFilters(Page<Problem> result, EnhancedSearchRequest request, Page<Problem> page) {
        List<Problem> filteredProblems = result.getRecords().stream()
                .filter(problem -> matchesDifficultyFilter(problem, request.getDifficulties()))
                .filter(problem -> matchesTagFilter(problem, request.getTags()))
                .filter(problem -> matchesStatusFilter(problem, request.getStatuses(), request.getUserId()))
                .collect(Collectors.toList());
        
        // Create new page with filtered results
        Page<Problem> filteredPage = new Page<>(page.getCurrent(), page.getSize());
        filteredPage.setRecords(filteredProblems);
        filteredPage.setTotal(filteredProblems.size());
        filteredPage.setPages((long) Math.ceil((double) filteredProblems.size() / page.getSize()));
        
        return filteredPage;
    }
    
    /**
     * Apply enhanced filters to advanced search results.
     */
    private Page<Problem> enhancedFilterResults(Page<Problem> result, AdvancedSearchRequest request) {
        List<Problem> filteredProblems = result.getRecords().stream()
                .filter(problem -> matchesDifficultyFilter(problem, request.getDifficulties()))
                .filter(problem -> matchesTagFilter(problem, request.getTags()))
                .filter(problem -> matchesStatusFilter(problem, request.getStatuses(), request.getUserId()))
                .collect(Collectors.toList());
        
        // Update result with filtered problems
        result.setRecords(filteredProblems);
        result.setTotal(filteredProblems.size());
        result.setPages((long) Math.ceil((double) filteredProblems.size() / result.getSize()));
        
        return result;
    }
    
    /**
     * Check if problem matches difficulty filter.
     */
    private boolean matchesDifficultyFilter(Problem problem, List<String> difficulties) {
        if (difficulties == null || difficulties.isEmpty()) {
            return true;
        }
        
        String problemDifficulty = problem.getDifficulty().name().toLowerCase();
        return difficulties.stream()
                .map(String::toLowerCase)
                .anyMatch(difficulty -> difficulty.equals(problemDifficulty));
    }
    
    /**
     * Check if problem matches tag filter.
     */
    private boolean matchesTagFilter(Problem problem, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return true;
        }
        
        String problemTags = problem.getTags();
        if (problemTags == null || problemTags.trim().isEmpty()) {
            return false;
        }
        
        // Parse tags from JSON or comma-separated string
        List<String> problemTagList = parseTagsFromProblem(problemTags);
        
        return tags.stream()
                .anyMatch(tag -> problemTagList.stream()
                        .anyMatch(problemTag -> problemTag.contains(tag) || tag.contains(problemTag)));
    }
    
    /**
     * Check if problem matches status filter for a specific user.
     */
    private boolean matchesStatusFilter(Problem problem, List<String> statuses, Long userId) {
        if (statuses == null || statuses.isEmpty()) {
            return true;
        }
        
        if (userId == null) {
            // For anonymous users, only show "not_done" status
            return statuses.contains("not_done");
        }
        
        try {
            // Get user's status for this problem from FSRS
            FSRSCard card = fsrsService.getOrCreateCard(userId, problem.getId());
            String problemStatus = determineStatusFromCard(card);
            
            return statuses.contains(problemStatus);
            
        } catch (Exception e) {
            log.debug("Failed to get status for user {} problem {}: {}", userId, problem.getId(), e.getMessage());
            // Fallback to "not_done" if unable to determine status
            return statuses.contains("not_done");
        }
    }
    
    /**
     * Parse tags from problem tags field (JSON or comma-separated).
     */
    private List<String> parseTagsFromProblem(String tagsField) {
        if (tagsField == null || tagsField.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            // Try parsing as JSON array first
            if (tagsField.trim().startsWith("[")) {
                return List.of(tagsField.replaceAll("[\\[\\]\"]", "").split(","))
                        .stream()
                        .map(String::trim)
                        .filter(tag -> !tag.isEmpty())
                        .collect(Collectors.toList());
            } else {
                // Fall back to comma-separated parsing
                return List.of(tagsField.split(","))
                        .stream()
                        .map(String::trim)
                        .filter(tag -> !tag.isEmpty())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.debug("Failed to parse tags from '{}': {}", tagsField, e.getMessage());
            return List.of();
        }
    }

    @lombok.Data
    public static class AdvancedSearchRequest {
        private String keyword;
        private Difficulty difficulty;
        private String tag;
        private Boolean isPremium;
        
        // Enhanced fields for comprehensive filtering
        private List<String> difficulties;
        private List<String> statuses;
        private List<String> tags;
        private Long userId;
    }

    @lombok.Data
    public static class CreateProblemRequest {
        private String title;
        private Difficulty difficulty;
        private String tagsJson;
        private String problemUrl;
        private String leetcodeId;
        private Boolean isPremium = false;
    }

    @lombok.Data
    public static class UpdateProblemRequest {
        private String title;
        private Difficulty difficulty;
        private String tagsJson;
        private String problemUrl;
    }

    // User problem status methods

    /**
     * Get user's status for a specific problem.
     */
    public UserProblemStatusLegacyDTO getProblemStatus(Long userId, Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new IllegalArgumentException("Problem not found");
        }
        
        try {
            // Get or create FSRS card for this user-problem combination
            FSRSCard card = fsrsService.getOrCreateCard(userId, problemId);
            
            // Determine status based on FSRS card state and metrics
            String status = determineStatusFromCard(card);
            Integer mastery = calculateMasteryLevel(card);
            Double accuracy = calculateAccuracyFromFSRS(card);
            
            // Format dates for response
            String lastAttemptDate = card.getLastReview() != null ? 
                    card.getLastReview().toString() : null;
            String lastConsideredDate = card.getNextReview() != null ? 
                    card.getNextReview().toString() : lastAttemptDate;
            
            return UserProblemStatusLegacyDTO.builder()
                    .problemId(problemId)
                    .title(problem.getTitle())
                    .difficulty(problem.getDifficulty())
                    .status(status)
                    .mastery(mastery)
                    .lastAttemptDate(lastAttemptDate)
                    .lastConsideredDate(lastConsideredDate)
                    .attemptCount(card.getReviewCount() != null ? card.getReviewCount() : 0)
                    .accuracy(accuracy)
                    .notes(String.format("FSRS State: %s, Stability: %.1f days", 
                            card.getState().name(), card.getStabilityAsDouble()))
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to get FSRS status for user {} problem {}: {}", userId, problemId, e.getMessage());
            
            // Fallback to basic response for new problems
            return UserProblemStatusLegacyDTO.builder()
                    .problemId(problemId)
                    .title(problem.getTitle())
                    .difficulty(problem.getDifficulty())
                    .status("not_done")
                    .mastery(0)
                    .lastAttemptDate(null)
                    .lastConsideredDate(null)
                    .attemptCount(0)
                    .accuracy(0.0)
                    .notes("New problem - no attempts yet")
                    .build();
        }
    }

    /**
     * Get user's problem progress for all problems with manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrComputeList() 进行列表缓存操作
     * - 保持相同的缓存键和30分钟TTL
     */
    public List<UserProblemStatusDTO> getUserProblemProgress(Long userId) {
        if (userId == null) {
            log.warn("getUserProblemProgress called with null userId");
            return List.of();
        }
        
        String cacheKey = CacheKeyBuilder.userProblemProgress(userId);
        
        return cacheHelper.cacheOrComputeList(
            cacheKey,
            UserProblemStatusDTO.class,
            USER_CACHE_TTL,
            () -> {
                log.debug("Cache MISS - Loading user problem progress: userId={}", userId);
                
                try {
                    // Get all problems (limit to first 10 for now)
                    List<Problem> problems = problemMapper.selectList(null).stream()
                            .limit(10) // Limit for demo purposes
                            .collect(Collectors.toList());
                    
                    // Get user's FSRS cards for these problems
                    Map<Long, FSRSCard> userCards = fsrsCardMapper.findByUserId(userId).stream()
                            .collect(Collectors.toMap(FSRSCard::getProblemId, Function.identity()));
                    
                    List<UserProblemStatusDTO> progress = problems.stream()
                            .map(problem -> {
                                FSRSCard card = userCards.get(problem.getId());
                                
                                if (card == null) {
                                    // User hasn't started this problem yet
                                    return UserProblemStatusDTO.builder()
                                            .problemId(problem.getId())
                                            .title(problem.getTitle())
                                            .difficulty(problem.getDifficulty())
                                            .status("not_done")
                                            .mastery(0)
                                            .lastAttemptDate(null)
                                            .lastConsideredDate(null)
                                            .attemptCount(0)
                                            .accuracy(0.0)
                                            .notes("")
                                            .build();
                                } else {
                                    // User has a card for this problem - calculate status based on FSRS data
                                    String status = determineStatusFromFSRSCard(card);
                                    Integer mastery = calculateMasteryFromFSRSCard(card);
                                    Double accuracy = calculateAccuracyFromFSRS(card);
                                    
                                    return UserProblemStatusDTO.builder()
                                            .problemId(problem.getId())
                                            .title(problem.getTitle())
                                            .difficulty(problem.getDifficulty())
                                            .status(status)
                                            .mastery(mastery)
                                            .lastAttemptDate(card.getLastReview() != null ? card.getLastReview().toString() : null)
                                            .lastConsideredDate(card.getNextReview() != null ? card.getNextReview().toString() : null)
                                            .attemptCount(card.getReviewCount())
                                            .accuracy(accuracy)
                                            .notes("")
                                            .build();
                                }
                            })
                            .collect(Collectors.toList());
                    
                    log.debug("Retrieved user problem progress: userId={}, count={}", userId, progress.size());
                    
                    return progress;
                } catch (Exception e) {
                    log.error("Failed to get user problem progress: userId={}, error={}", userId, e.getMessage(), e);
                    throw e;
                }
            }
        );
    }

    /**
     * Update problem status for user.
     */
    @Transactional
    public UserProblemStatusLegacyDTO updateProblemStatus(
            Long userId, Long problemId, UpdateProblemStatusRequest request) {
        
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new IllegalArgumentException("Problem not found");
        }
        
        try {
            // Convert mastery level to FSRS rating (1-4 scale)
            Integer fsrsRating = convertMasteryToFSRSRating(request.getMastery(), request.getStatus());
            
            // Process FSRS review to update card state and scheduling
            FSRSReviewResultDTO reviewResult = fsrsService.processReview(
                    userId, problemId, fsrsRating, 
                    determineReviewType(request.getStatus())
            );
            
            FSRSCard card = reviewResult.getCard();
            
            // Calculate accuracy based on FSRS stability and difficulty
            Double accuracy = calculateAccuracyFromFSRS(card);
            
            log.debug("Updated problem {} status to {} for user {} with FSRS rating {} - next review: {}", 
                    problemId, request.getStatus(), userId, fsrsRating, reviewResult.getNextReviewTime());
            
            UserProblemStatusLegacyDTO result = UserProblemStatusLegacyDTO.builder()
                    .problemId(problemId)
                    .title(problem.getTitle())
                    .difficulty(problem.getDifficulty())
                    .status(request.getStatus())
                    .mastery(request.getMastery())
                    .lastAttemptDate(LocalDateTime.now().toString())
                    .lastConsideredDate(reviewResult.getNextReviewTime() != null ? 
                            reviewResult.getNextReviewTime().toString() : LocalDateTime.now().toString())
                    .attemptCount(card.getReviewCount())
                    .accuracy(accuracy)
                    .notes(request.getNotes())
                    .build();
            
            // 手动缓存失效 - 清理用户进度和掌握度缓存
            try {
                cacheService.delete(CacheKeyBuilder.userProblemProgress(userId));
                cacheService.delete(CacheKeyBuilder.userProblemMastery(userId, problemId));
                log.debug("Cache invalidated for user {} problem {} progress update", userId, problemId);
            } catch (Exception cacheException) {
                log.warn("Cache invalidation failed for user {} problem {}: {}", 
                        userId, problemId, cacheException.getMessage());
                // 缓存失效失败不影响主流程
            }
            
            // Publish event to clear user status cache
            eventPublisher.publishEvent(new Events.UserEvent(
                    Events.UserEvent.UserEventType.PROGRESS_UPDATED, userId, null));
            
            return result;
                    
        } catch (Exception e) {
            log.error("Failed to update FSRS status for user {} problem {}: {}", userId, problemId, e.getMessage());
            
            // Fallback to basic status update without FSRS integration
            UserProblemStatusLegacyDTO result = UserProblemStatusLegacyDTO.builder()
                    .problemId(problemId)
                    .title(problem.getTitle())
                    .difficulty(problem.getDifficulty())
                    .status(request.getStatus())
                    .mastery(request.getMastery())
                    .lastAttemptDate(LocalDateTime.now().toString())
                    .lastConsideredDate(LocalDateTime.now().toString())
                    .attemptCount(1)
                    .accuracy(0.0)
                    .notes(request.getNotes())
                    .build();
            
            // 手动缓存失效 - 即使是后备方案也需要清理缓存
            try {
                cacheService.delete(CacheKeyBuilder.userProblemProgress(userId));
                cacheService.delete(CacheKeyBuilder.userProblemMastery(userId, problemId));
                log.debug("Cache invalidated for user {} problem {} (fallback)", userId, problemId);
            } catch (Exception cacheException) {
                log.warn("Cache invalidation failed for user {} problem {} (fallback): {}", 
                        userId, problemId, cacheException.getMessage());
            }
            
            // Publish event to clear user status cache even for fallback case
            eventPublisher.publishEvent(new Events.UserEvent(
                    Events.UserEvent.UserEventType.PROGRESS_UPDATED, userId, null));
            
            return result;
        }
    }

    /**
     * Get problem mastery level for user with FSRS integration and manual Redis caching.
     * 
     * 迁移后的缓存实现：
     * - 移除 @Cacheable 注解
     * - 使用 CacheHelper.cacheOrCompute() 进行缓存操作
     * - 保持相同的缓存键和30分钟TTL
     */
    public ProblemMasteryDTO getProblemMastery(Long userId, Long problemId) {
        if (userId == null || problemId == null) {
            throw new IllegalArgumentException("userId and problemId cannot be null");
        }
        
        String cacheKey = CacheKeyBuilder.userProblemMastery(userId, problemId);
        
        return cacheHelper.cacheOrCompute(
            cacheKey,
            ProblemMasteryDTO.class,
            USER_CACHE_TTL,
            () -> {
                log.debug("Cache MISS - Loading problem mastery: userId={}, problemId={}", userId, problemId);
                
                Problem problem = problemMapper.selectById(problemId);
                if (problem == null) {
                    throw new IllegalArgumentException("Problem not found");
                }
                
                try {
                    // Get or create FSRS card for this user-problem combination
                    FSRSCard card = fsrsService.getOrCreateCard(userId, problemId);
                    
                    // Calculate mastery level based on FSRS state and stability
                    Integer masteryLevel = calculateMasteryLevel(card);
                    
                    // Calculate accuracy from FSRS metrics
                    Double accuracy = calculateAccuracyFromFSRS(card);
                    
                    // Format dates for response
                    String lastAttemptDate = card.getLastReview() != null ? 
                            card.getLastReview().toString() : null;
                    String nextReviewDate = card.getNextReview() != null ? 
                            card.getNextReview().toString() : null;
                    
                    ProblemMasteryDTO result = ProblemMasteryDTO.builder()
                            .problemId(problemId)
                            .masteryLevel(masteryLevel)
                            .attemptCount(card.getReviewCount() != null ? card.getReviewCount() : 0)
                            .accuracy(accuracy)
                            .lastAttemptDate(lastAttemptDate)
                            .nextReviewDate(nextReviewDate)
                            .difficulty(problem.getDifficulty().name())
                            .notes(String.format("Stability: %.2f, Difficulty: %.2f, State: %s, Lapses: %d", 
                                    card.getStabilityAsDouble(), 
                                    card.getDifficultyAsDouble(),
                                    card.getState().name(),
                                    card.getLapses() != null ? card.getLapses() : 0))
                            .build();
                    
                    log.debug("Retrieved problem mastery: userId={}, problemId={}, masteryLevel={}", 
                            userId, problemId, masteryLevel);
                    
                    return result;
                            
                } catch (Exception e) {
                    log.error("Failed to get FSRS mastery for user {} problem {}: {}", userId, problemId, e.getMessage());
                    
                    // Fallback to basic response
                    return ProblemMasteryDTO.builder()
                            .problemId(problemId)
                            .masteryLevel(0)
                            .attemptCount(0)
                            .accuracy(0.0)
                            .lastAttemptDate(null)
                            .nextReviewDate(null)
                            .difficulty(problem.getDifficulty().name())
                            .notes("FSRS data unavailable")
                            .build();
                }
            }
        );
    }

    /**
     * Get user's learning statistics across all problems.
     */
    public UserLearningStatistics getUserLearningStatistics(Long userId) {
        try {
            // Get FSRS learning stats
            var fsrsStats = fsrsService.getUserLearningStats(userId);
            
            // Get basic problem counts
            List<Problem> userProblems = findProblemsForUser(userId, null, null);
            
            // Calculate derived statistics
            int totalProblems = userProblems.size();
            int solvedProblems = (int) userProblems.stream()
                    .filter(p -> hasUserSolvedProblem(userId, p.getId()))
                    .count();
            
            Map<String, Integer> difficultyBreakdown = calculateDifficultyBreakdown(userProblems);
            
            return UserLearningStatistics.builder()
                    .totalProblems(totalProblems)
                    .solvedProblems(solvedProblems)
                    .newCards(fsrsStats.getNewCards().intValue())
                    .learningCards(fsrsStats.getLearningCards().intValue())
                    .reviewCards(fsrsStats.getReviewCards().intValue())
                    .relearningCards(fsrsStats.getRelearningCards().intValue())
                    .dueCards(fsrsStats.getDueCards().intValue())
                    .averageAccuracy(fsrsStats.getAvgStability() != null ? fsrsStats.getAvgStability() * 20 : 0.0) // Convert stability to percentage
                    .averageDifficulty(fsrsStats.getAvgDifficulty() != null ? fsrsStats.getAvgDifficulty() : 0.0)
                    .totalReviews(fsrsStats.getTotalCards().intValue())
                    .totalLapses(fsrsStats.getTotalLapses().intValue())
                    .difficultyBreakdown(difficultyBreakdown)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to get user learning statistics for user {}: {}", userId, e.getMessage());
            return UserLearningStatistics.builder()
                    .totalProblems(0)
                    .solvedProblems(0)
                    .newCards(0)
                    .learningCards(0)
                    .reviewCards(0)
                    .relearningCards(0)
                    .dueCards(0)
                    .averageAccuracy(0.0)
                    .averageDifficulty(0.0)
                    .totalReviews(0)
                    .totalLapses(0)
                    .difficultyBreakdown(Map.of())
                    .build();
        }
    }

    /**
     * Mark problem as mastered (highest proficiency).
     */
    @Transactional
    public void markProblemAsMastered(Long userId, Long problemId) {
        try {
            // Process with highest FSRS rating (Easy = 4)
            fsrsService.processReview(userId, problemId, 4, ReviewType.MANUAL);
            
            // Additional logic for mastery tracking could be added here
            log.info("Marked problem {} as mastered for user {}", problemId, userId);
            
        } catch (Exception e) {
            log.error("Failed to mark problem {} as mastered for user {}: {}", problemId, userId, e.getMessage());
            throw new RuntimeException("Failed to mark problem as mastered", e);
        }
    }

    /**
     * Reset problem learning progress.
     */
    @Transactional
    public void resetProblemProgress(Long userId, Long problemId) {
        try {
            // Get existing card
            FSRSCard card = fsrsService.getOrCreateCard(userId, problemId);
            
            // Reset to initial state by processing with "Again" rating
            fsrsService.processReview(userId, problemId, 1, ReviewType.MANUAL);
            
            log.info("Reset progress for problem {} for user {}", problemId, userId);
            
        } catch (Exception e) {
            log.error("Failed to reset problem {} progress for user {}: {}", problemId, userId, e.getMessage());
            throw new RuntimeException("Failed to reset problem progress", e);
        }
    }

    /**
     * Get recommended problems for user based on FSRS scheduling with optional AI enhancement.
     */
    public List<Problem> getRecommendedProblems(Long userId, int limit) {
        return getRecommendedProblems(userId, limit, RecommendationType.AUTO);
    }
    
    /**
     * Check if AI recommendations are enabled using centralized toggle service
     */
    private boolean isAIRecommendationEnabled(Long userId) {
        if (llmToggleService == null || llmProperties == null) {
            return false; // Fallback to disabled if services not available
        }
        
        try {
            // Build request context for toggle evaluation
            RequestContext ctx = new RequestContext();
            ctx.setUserId(userId);
            ctx.setTraceId(TraceContext.getTraceId());
            
            return llmToggleService.isEnabled(ctx, llmProperties);
        } catch (Exception e) {
            log.warn("Failed to check LLM toggle status for userId={}: {}", userId, e.getMessage());
            return false; // Fail safe to disabled
        }
    }

    /**
     * Get recommended problems for user with specific recommendation type.
     */
    public List<Problem> getRecommendedProblems(Long userId, int limit, RecommendationType recommendationType) {
        log.debug("Getting recommended problems for userId={}, limit={}, type={}", 
                  userId, limit, recommendationType);
        
        // Check if AI recommendations are enabled and available using centralized toggle service
        boolean useAI = isAIRecommendationEnabled(userId) && 
                        recommendationStrategyResolver != null && 
                        (recommendationType.requiresAI() || recommendationType == RecommendationType.AUTO);
        
        if (useAI) {
            try {
                // Use AI recommendation strategy
                RecommendationStrategy strategy = recommendationStrategyResolver.resolveStrategy(
                        recommendationType, userId, null);
                
                if (strategy != null && strategy.isAvailable()) {
                    AIRecommendationResponse aiResponse = strategy.getRecommendations(
                            userId, limit, null, null, null, null);
                    
                    if (aiResponse != null && 
                        aiResponse.getItems() != null && 
                        !aiResponse.getItems().isEmpty() && 
                        (aiResponse.getMeta() == null || !aiResponse.getMeta().isBusy())) {
                        
                        // Convert AI recommendations to Problem objects
                        List<Long> problemIds = aiResponse.getItems().stream()
                                .map(RecommendationItemDTO::getProblemId)
                                .toList();
                        
                        List<Problem> problems = problemIds.stream()
                                .map(problemMapper::selectById)
                                .filter(Objects::nonNull)
                                .toList();
                        
                        if (!problems.isEmpty()) {
                            log.debug("Successfully got {} AI-powered recommendations for userId={}", 
                                      problems.size(), userId);
                            return problems;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get AI recommendations for userId={}, falling back to FSRS: {}", 
                         userId, e.getMessage());
            }
        }
        
        // Fallback to traditional FSRS recommendations
        return getFsrsRecommendedProblems(userId, limit);
    }
    
    /**
     * Traditional FSRS-based recommendation logic (extracted for reuse).
     */
    private List<Problem> getFsrsRecommendedProblems(Long userId, int limit) {
        try {
            // Get FSRS review queue
            var reviewQueue = fsrsService.generateReviewQueue(userId, limit);
            
            if (reviewQueue.getCards().isEmpty()) {
                // If no FSRS cards available, return new problems
                return findProblemsForUser(userId, null, null)
                        .stream()
                        .filter(p -> !hasUserSolvedProblem(userId, p.getId()))
                        .limit(limit)
                        .toList();
            }
            
            // Convert FSRS cards to problems
            List<Long> problemIds = reviewQueue.getCards().stream()
                    .map(card -> card.getProblemId())
                    .toList();
            
            return problemIds.stream()
                    .map(problemMapper::selectById)
                    .filter(Objects::nonNull)
                    .toList();
                    
        } catch (Exception e) {
            log.error("Failed to get FSRS recommended problems for user {}: {}", userId, e.getMessage());
            // Fallback to random problems
            return findProblemsForUser(userId, null, null)
                    .stream()
                    .limit(limit)
                    .toList();
        }
    }

    /**
     * Get due problems for review.
     */
    public List<Problem> getDueProblems(Long userId, int limit) {
        try {
            var dueCards = fsrsService.getDueCards(userId, limit);
            
            List<Long> problemIds = dueCards.stream()
                    .map(card -> card.getProblemId())
                    .toList();
            
            return problemIds.stream()
                    .map(problemMapper::selectById)
                    .filter(Objects::nonNull)
                    .toList();
                    
        } catch (Exception e) {
            log.error("Failed to get due problems for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Convert mastery level and status to FSRS rating (1-4 scale).
     * 
     * FSRS Ratings:
     * 1 - Again (failed to remember)
     * 2 - Hard (remembered with difficulty)
     * 3 - Good (remembered easily)
     * 4 - Easy (remembered very easily)
     */
    private Integer convertMasteryToFSRSRating(Integer mastery, String status) {
        // Handle null mastery
        if (mastery == null) {
            mastery = 0;
        }
        
        // Consider status for rating adjustment
        boolean isSolved = "SOLVED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status);
        boolean isDone = "done".equalsIgnoreCase(status);
        boolean isAttempted = "ATTEMPTED".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status);
        
        // For "done" status, we need to be more careful about rating assignment
        if (isDone) {
            return switch (mastery) {
                case 0 -> 2;  // No mastery but done = Hard (attempted but struggled)
                case 1 -> 2;  // Low mastery + done = Hard (attempted but struggled) 
                case 2 -> 3;  // Medium mastery + done = Good
                case 3 -> 4;  // High mastery + done = Easy
                default -> 2; // Default to Hard for done status
            };
        }
        
        // Original logic for other statuses
        return switch (mastery) {
            case 0 -> isSolved ? 2 : 1;  // No mastery but solved = Hard, otherwise Again
            case 1 -> isSolved ? 3 : 2;  // Low mastery + solved = Good, otherwise Hard  
            case 2 -> isSolved ? 4 : 3;  // Medium mastery + solved = Easy, otherwise Good
            case 3 -> 4;                 // High mastery = Easy
            default -> isAttempted ? 2 : 1;  // Default: Hard if attempted, Again otherwise
        };
    }

    /**
     * Determine review type based on status.
     */
    private ReviewType determineReviewType(String status) {
        return switch (status != null ? status.toUpperCase() : "") {
            case "SOLVED", "COMPLETED", "DONE" -> ReviewType.SCHEDULED;  // Regular review
            case "IN_PROGRESS", "ATTEMPTED" -> ReviewType.MANUAL;  // User-initiated
            case "REVIEWING", "PRACTICING" -> ReviewType.EXTRA;   // Extra practice
            default -> ReviewType.MANUAL;  // Default to manual review
        };
    }

    /**
     * Calculate accuracy percentage based on FSRS stability and difficulty.
     * Higher stability and lower difficulty indicate better accuracy.
     */
    private Double calculateAccuracyFromFSRS(FSRSCard card) {
        if (card == null) {
            return 0.0;
        }

        double stability = card.getStabilityAsDouble();
        double difficulty = card.getDifficultyAsDouble();
        int reviewCount = card.getReviewCount() != null ? card.getReviewCount() : 0;
        int lapses = card.getLapses() != null ? card.getLapses() : 0;

        // Base accuracy from stability (higher stability = higher accuracy)
        double stabilityScore = Math.min(stability / 30.0, 1.0); // Normalize to 0-1, 30 days = 100%
        
        // Difficulty penalty (higher difficulty = lower accuracy)
        double difficultyPenalty = Math.min(difficulty / 10.0, 0.5); // Max 50% penalty
        
        // Review experience bonus (more reviews = slight accuracy boost)
        double experienceBonus = Math.min(reviewCount * 0.02, 0.2); // Max 20% bonus
        
        // Lapse penalty (more lapses = lower accuracy)  
        double lapsePenalty = Math.min(lapses * 0.1, 0.4); // Max 40% penalty

        // Calculate final accuracy (0.3-1.0 range)
        double accuracy = 0.3 + (stabilityScore * 0.7) - difficultyPenalty + experienceBonus - lapsePenalty;
        
        // Ensure bounds [0.0, 1.0] and convert to percentage
        return Math.max(0.0, Math.min(1.0, accuracy)) * 100.0;
    }

    /**
     * Determine status string based on FSRS card state and metrics.
     */
    private String determineStatusFromCard(FSRSCard card) {
        if (card == null || card.getReviewCount() == null || card.getReviewCount() == 0) {
            return "not_done";
        }
        
        double stability = card.getStabilityAsDouble();
        int reviewCount = card.getReviewCount();
        
        // Consider reviewed if has multiple reviews and good stability
        if (reviewCount >= 3 && stability >= 7.0) {
            return "reviewed";
        }
        
        // Consider done if has at least one review
        if (reviewCount >= 1) {
            return "done";
        }
        
        return "not_done";
    }

    /**
     * Calculate mastery level (0-3) based on FSRS card state and stability.
     */
    private Integer calculateMasteryLevel(FSRSCard card) {
        if (card == null) {
            return 0;
        }

        double stability = card.getStabilityAsDouble();
        int reviewCount = card.getReviewCount() != null ? card.getReviewCount() : 0;
        int lapses = card.getLapses() != null ? card.getLapses() : 0;
        
        // Base level from stability
        int baseLevel = 0;
        if (stability >= 30.0) baseLevel = 3;      // Excellent (1 month+)
        else if (stability >= 14.0) baseLevel = 2; // Good (2 weeks+)
        else if (stability >= 7.0) baseLevel = 1;  // Fair (1 week+)
        else baseLevel = 0;                        // Poor (<1 week)
        
        // Review experience adjustment
        if (reviewCount >= 10) baseLevel = Math.min(3, baseLevel + 1);
        else if (reviewCount >= 5) baseLevel = Math.min(3, baseLevel);
        
        // Lapse penalty
        if (lapses > 3) baseLevel = Math.max(0, baseLevel - 1);
        else if (lapses > 1) baseLevel = Math.max(0, baseLevel);
        
        return baseLevel;
    }

    /**
     * Find problems for user (stub implementation - to be replaced with actual logic).
     */
    private List<Problem> findProblemsForUser(Long userId, String difficulty, String search) {
        // This is a placeholder - in a real implementation, this would:
        // 1. Get problems user has attempted or interacted with
        // 2. Apply difficulty and search filters
        // 3. Return filtered results
        
        // For now, return all problems
        return problemMapper.selectList(null);
    }

    /**
     * Check if user has solved a problem (stub implementation).
     */
    private boolean hasUserSolvedProblem(Long userId, Long problemId) {
        try {
            FSRSCard card = fsrsService.getOrCreateCard(userId, problemId);
            // Consider solved if has reviews and reasonable stability
            return card.getReviewCount() != null && 
                   card.getReviewCount() > 0 && 
                   card.getStabilityAsDouble() > 1.0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculate difficulty breakdown for problems.
     */
    private Map<String, Integer> calculateDifficultyBreakdown(List<Problem> problems) {
        Map<String, Integer> breakdown = new java.util.HashMap<>();
        
        for (Problem problem : problems) {
            String difficulty = problem.getDifficulty().name();
            breakdown.merge(difficulty, 1, Integer::sum);
        }
        
        return breakdown;
    }

    // DTOs

    /**
     * User learning statistics DTO.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserLearningStatistics {
        private Integer totalProblems;
        private Integer solvedProblems;
        private Integer newCards;
        private Integer learningCards;
        private Integer reviewCards;
        private Integer relearningCards;
        private Integer dueCards;
        private Double averageAccuracy;
        private Double averageDifficulty;
        private Integer totalReviews;
        private Integer totalLapses;
        private Map<String, Integer> difficultyBreakdown;
        
        // Derived metrics
        public Double getSolveRate() {
            if (totalProblems == null || totalProblems == 0) return 0.0;
            return (solvedProblems != null ? solvedProblems : 0) * 100.0 / totalProblems;
        }
        
        public Double getRetentionRate() {
            if (totalReviews == null || totalReviews == 0) return 100.0;
            int lapses = totalLapses != null ? totalLapses : 0;
            return Math.max(0.0, (totalReviews - lapses) * 100.0 / totalReviews);
        }
        
        public Integer getActiveCards() {
            int learning = learningCards != null ? learningCards : 0;
            int review = reviewCards != null ? reviewCards : 0;
            int relearning = relearningCards != null ? relearningCards : 0;
            return learning + review + relearning;
        }
    }
    
    // Helper methods for getUserProblemProgress
    
    /**
     * Determine user status from FSRS card state with enhanced logic for attempted status.
     */
    private String determineStatusFromFSRSCard(FSRSCard card) {
        if (card == null || card.getReviewCount() == null || card.getReviewCount() == 0) {
            return "not_done";
        }
        
        switch (card.getState()) {
            case NEW:
                // If the card has been reviewed (review count > 0) but still in NEW state,
                // it means the user attempted but struggled (got Hard ratings)
                return "attempted";
            case LEARNING:
            case REVIEW:
                return "done";
            case RELEARNING:
                return "reviewed";
            default:
                return "not_done";
        }
    }
    
    /**
     * Calculate mastery level from FSRS card metrics.
     */
    private Integer calculateMasteryFromFSRSCard(FSRSCard card) {
        if (card.getReviewCount() == 0) {
            return 0;
        }
        
        // Calculate mastery based on stability and review count
        // Higher stability and more reviews = higher mastery
        double stability = card.getStability() != null ? card.getStability().doubleValue() : 0.0;
        int reviewCount = card.getReviewCount();
        int lapses = card.getLapses();
        
        // Simple mastery calculation (0-3 scale)
        if (stability > 30 && reviewCount >= 5 && lapses <= 1) {
            return 3; // Expert
        } else if (stability > 10 && reviewCount >= 3 && lapses <= 2) {
            return 2; // Good
        } else if (stability > 2 && reviewCount >= 1) {
            return 1; // Basic
        } else {
            return 0; // Learning
        }
    }

    /**
     * Get user's completed problems with pagination.
     * Completed means status is "done" or "reviewed".
     */
    @Cacheable(value = "userCompletedProblems", key = "#userId + ':' + #page + ':' + #size + ':' + (#sortBy ?: 'date')")
    public Page<UserProblemStatusDTO> getUserCompletedProblems(Long userId, int page, int size, String sortBy) {
        if (userId == null) {
            log.warn("getUserCompletedProblems called with null userId");
            return new Page<UserProblemStatusDTO>(page + 1, size).setRecords(new ArrayList<>()).setTotal(0);
        }
        
        log.debug("Getting completed problems for user: userId={}, page={}, size={}, sortBy={}", 
                userId, page, size, sortBy);
                
        try {
            // Get all problems that user has interacted with (has FSRS cards)
            List<FSRSCard> userCards = fsrsCardMapper.findByUserId(userId);
            
            // Filter for completed cards (LEARNING, REVIEW, or RELEARNING states)
            List<FSRSCard> completedCards = userCards.stream()
                    .filter(card -> card.getState() == FSRSState.LEARNING || 
                                   card.getState() == FSRSState.REVIEW || 
                                   card.getState() == FSRSState.RELEARNING)
                    .collect(Collectors.toList());
            
            // Get problem IDs
            Set<Long> completedProblemIds = completedCards.stream()
                    .map(FSRSCard::getProblemId)
                    .collect(Collectors.toSet());
            
            if (completedProblemIds.isEmpty()) {
                log.debug("No completed problems found for user: userId={}", userId);
                return new Page<UserProblemStatusDTO>(page + 1, size).setRecords(new ArrayList<>()).setTotal(0);
            }
            
            // Get problems by IDs
            List<Problem> problems = problemMapper.selectBatchIds(completedProblemIds);
            
            // Create map for quick lookup
            Map<Long, Problem> problemMap = problems.stream()
                    .collect(Collectors.toMap(Problem::getId, Function.identity()));
            Map<Long, FSRSCard> cardMap = completedCards.stream()
                    .collect(Collectors.toMap(FSRSCard::getProblemId, Function.identity()));
            
            // Build DTOs
            List<UserProblemStatusDTO> completedProblems = completedCards.stream()
                    .map(card -> {
                        Problem problem = problemMap.get(card.getProblemId());
                        if (problem == null) return null;
                        
                        String status = determineStatusFromFSRSCard(card);
                        Integer mastery = calculateMasteryFromFSRSCard(card);
                        Double accuracy = calculateAccuracyFromFSRS(card);
                        
                        return UserProblemStatusDTO.builder()
                                .problemId(problem.getId())
                                .title(problem.getTitle())
                                .difficulty(problem.getDifficulty())
                                .status(status)
                                .mastery(mastery)
                                .lastAttemptDate(card.getLastReview() != null ? card.getLastReview().toString() : null)
                                .lastConsideredDate(card.getNextReview() != null ? card.getNextReview().toString() : null)
                                .attemptCount(card.getReviewCount())
                                .accuracy(accuracy)
                                .notes("")
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            // Sort based on sortBy parameter
            Comparator<UserProblemStatusDTO> comparator;
            switch (sortBy != null ? sortBy.toLowerCase() : "date") {
                case "difficulty":
                    comparator = Comparator.comparing(dto -> dto.getDifficulty().ordinal());
                    break;
                case "mastery":
                    comparator = Comparator.comparing(UserProblemStatusDTO::getMastery).reversed();
                    break;
                case "title":
                    comparator = Comparator.comparing(UserProblemStatusDTO::getTitle);
                    break;
                default: // "date"
                    comparator = Comparator.comparing((UserProblemStatusDTO dto) -> {
                        String date = dto.getLastAttemptDate();
                        return date != null ? date : "0000-00-00";
                    }).reversed();
                    break;
            }
            
            List<UserProblemStatusDTO> sortedProblems = completedProblems.stream()
                    .sorted(comparator)
                    .collect(Collectors.toList());
            
            // Manual pagination
            int totalElements = sortedProblems.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);
            
            List<UserProblemStatusDTO> pageContent = startIndex < totalElements ? 
                    sortedProblems.subList(startIndex, endIndex) : new ArrayList<>();
            
            Page<UserProblemStatusDTO> result = new Page<UserProblemStatusDTO>(page + 1, size)
                    .setRecords(pageContent)
                    .setTotal(totalElements);
            
            log.debug("Retrieved completed problems: userId={}, total={}, page={}, size={}", 
                    userId, totalElements, page, pageContent.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to get completed problems: userId={}, error={}", userId, e.getMessage(), e);
            throw e;
        }
    }
}