package com.codetop.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.annotation.SimpleIdempotent;
import com.codetop.annotation.CurrentUserId;
import com.codetop.dto.UserProblemStatusDTO;
import com.codetop.dto.UserProblemStatusLegacyDTO;
import com.codetop.dto.ProblemMasteryDTO;
import com.codetop.dto.UpdateProblemStatusRequest;
import com.codetop.dto.ProblemStatisticsDTO;
import com.codetop.dto.UserProblemStatusVO;
import com.codetop.dto.ProblemStatisticsVO;
import com.codetop.dto.ProblemMasteryVO;
import com.codetop.entity.Problem;
import com.codetop.enums.Difficulty;
import com.codetop.mapper.ProblemMapper;
import com.codetop.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Problem controller for managing algorithm problems.
 * 
 * @author CodeTop Team
 */
@RestController
@RequestMapping("/problems")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Problems", description = "Algorithm problem management")
public class ProblemController {

    private final ProblemService problemService;

    /**
     * Get all problems with pagination.
     */
    @GetMapping
    @Operation(summary = "Get all problems", description = "Get all problems with pagination and sorting")
    public ResponseEntity<Page<Problem>> getAllProblems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title,asc") String sort,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String search) {
        
        Page<Problem> pageRequest = new Page<>(page, Math.min(size, 100));
        Page<Problem> result = problemService.findAllProblems(pageRequest, difficulty, search);
        return ResponseEntity.ok(result);
    }

    /**
     * Get problem by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get problem", description = "Get problem by ID")
    public ResponseEntity<Problem> getProblem(@PathVariable Long id) {
        return problemService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search problems with pagination.
     */
    @GetMapping("/search")
    @Operation(summary = "Search problems", description = "Search problems with pagination")
    public ResponseEntity<Page<Problem>> searchProblems(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<Problem> pageRequest = new Page<>(page, Math.min(size, 100)); // Limit page size
        Page<Problem> result = problemService.searchProblems(keyword, pageRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * Advanced search with filters including status and tags.
     */
    @PostMapping("/search/advanced")
    @Operation(summary = "Advanced search", description = "Advanced problem search with multiple filters including difficulty, status, and tags")
    public ResponseEntity<Page<Problem>> advancedSearch(
            @RequestBody AdvancedSearchRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<Problem> pageRequest = new Page<>(page, Math.min(size, 100));
        
        ProblemService.AdvancedSearchRequest serviceRequest = new ProblemService.AdvancedSearchRequest();
        serviceRequest.setKeyword(request.getKeyword());
        serviceRequest.setDifficulty(request.getDifficulty());
        serviceRequest.setTag(request.getTag());
        serviceRequest.setIsPremium(request.getIsPremium());
        serviceRequest.setDifficulties(request.getDifficulties());
        serviceRequest.setTags(request.getTags());
        serviceRequest.setStatuses(request.getStatuses());
        serviceRequest.setUserId(request.getUserId());
        
        Page<Problem> result = problemService.advancedSearch(serviceRequest, pageRequest);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Enhanced search with comprehensive filters for CodeTop page.
     */
    @GetMapping("/search/enhanced")
    @Operation(summary = "Enhanced search", description = "Enhanced search with difficulty, status, tags filters and search functionality")
    public ResponseEntity<Page<Problem>> enhancedSearch(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> difficulties,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title,asc") String sort) {
        
        Page<Problem> pageRequest = new Page<>(page, Math.min(size, 100));
        
        ProblemService.EnhancedSearchRequest request = new ProblemService.EnhancedSearchRequest();
        request.setSearch(search);
        request.setDifficulties(difficulties);
        request.setStatuses(statuses);
        request.setTags(tags);
        request.setUserId(userId);
        request.setSort(sort);
        
        Page<Problem> result = problemService.enhancedSearch(request, pageRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * Get problems by difficulty.
     */
    @GetMapping("/difficulty/{difficulty}")
    @Operation(summary = "Get problems by difficulty", description = "Get problems filtered by difficulty")
    public ResponseEntity<Page<Problem>> getProblemsByDifficulty(
            @PathVariable Difficulty difficulty,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<Problem> pageRequest = new Page<>(page, Math.min(size, 100));
        Page<Problem> result = problemService.findByDifficulty(difficulty, pageRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * Get problems by tag.
     */
    @GetMapping("/tag/{tag}")
    @Operation(summary = "Get problems by tag", description = "Get problems filtered by tag")
    public ResponseEntity<Page<Problem>> getProblemsByTag(
            @PathVariable String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<Problem> pageRequest = new Page<>(page, Math.min(size, 100));
        Page<Problem> result = problemService.findByTag(tag, pageRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * Get problems by company.
     */
    @GetMapping("/company/{companyId}")
    @Operation(summary = "Get problems by company", description = "Get problems associated with specific company")
    public ResponseEntity<Page<Problem>> getProblemsByCompany(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<Problem> pageRequest = new Page<>(page, Math.min(size, 100));
        Page<Problem> result = problemService.findByCompany(companyId, pageRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * Get problems by company name.
     */
    @GetMapping("/company-name/{companyName}")
    @Operation(summary = "Get problems by company name", description = "Get problems by company name")
    public ResponseEntity<Page<Problem>> getProblemsByCompanyName(
            @PathVariable String companyName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<Problem> pageRequest = new Page<>(page, Math.min(size, 100));
        Page<Problem> result = problemService.findByCompanyName(companyName, pageRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * Get hot problems.
     */
    @GetMapping("/hot")
    @Operation(summary = "Get hot problems", description = "Get problems frequently asked by multiple companies")
    public ResponseEntity<List<ProblemMapper.HotProblem>> getHotProblems(
            @RequestParam(defaultValue = "3") int minCompanies,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<ProblemMapper.HotProblem> result = problemService.getHotProblems(minCompanies, Math.min(limit, 100));
        return ResponseEntity.ok(result);
    }

    /**
     * Get recent problems.
     */
    @GetMapping("/recent")
    @Operation(summary = "Get recent problems", description = "Get recently added problems")
    public ResponseEntity<List<Problem>> getRecentProblems(
            @RequestParam(defaultValue = "20") int limit) {
        
        List<Problem> result = problemService.getRecentProblems(Math.min(limit, 100));
        return ResponseEntity.ok(result);
    }

    /**
     * Get problem statistics.
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get problem statistics", description = "Get overall problem statistics")
    public ResponseEntity<ProblemStatisticsVO> getStatistics() {
        ProblemStatisticsDTO result = problemService.getStatistics();
        ProblemStatisticsVO response = convertStatisticsToVO(result);
        return ResponseEntity.ok(response);
    }

    /**
     * Get tag usage statistics.
     */
    @GetMapping("/tags/statistics")
    @Operation(summary = "Get tag statistics", description = "Get tag usage statistics")
    public ResponseEntity<List<ProblemMapper.TagUsage>> getTagStatistics() {
        List<ProblemMapper.TagUsage> result = problemService.getTagStatistics();
        return ResponseEntity.ok(result);
    }

    /**
     * Get user's problem progress.
     */
    @GetMapping("/user-progress")
    @Operation(summary = "Get user problem progress", description = "Get user's completion status for problems")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<UserProblemStatusVO>> getUserProblemProgress(
            @CurrentUserId Long userId) {
        
        List<UserProblemStatusDTO> progress = problemService.getUserProblemProgress(userId);
        List<UserProblemStatusVO> response = progress.stream()
                .map(this::convertUserStatusToVO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's problem status.
     */
    @GetMapping("/{problemId}/status")
    @Operation(summary = "Get problem status", description = "Get user's completion status for a problem")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserProblemStatusLegacyDTO> getProblemStatus(
            @PathVariable Long problemId,
            @CurrentUserId Long userId) {
        
        UserProblemStatusLegacyDTO status = problemService.getProblemStatus(userId, problemId);
        return ResponseEntity.ok(status);
    }

    /**
     * Update user's problem status.
     */
    @PutMapping("/{problemId}/status")
    @Operation(summary = "Update problem status", description = "Update user's completion status for a problem")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserProblemStatusLegacyDTO> updateProblemStatus(
            @PathVariable Long problemId,
            @CurrentUserId Long userId,
            @Valid @RequestBody com.codetop.dto.UpdateProblemStatusRequest request) {
        
        UserProblemStatusLegacyDTO status = problemService.updateProblemStatus(
                userId, problemId, request);
        return ResponseEntity.ok(status);
    }

    /**
     * Get user's mastery level for a problem.
     */
    @GetMapping("/{problemId}/mastery")
    @Operation(summary = "Get problem mastery", description = "Get user's mastery level for a specific problem")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ProblemMasteryVO> getProblemMastery(
            @PathVariable Long problemId,
            @CurrentUserId Long userId) {
        
        ProblemMasteryDTO mastery = problemService.getProblemMastery(userId, problemId);
        ProblemMasteryVO response = convertMasteryToVO(mastery);
        return ResponseEntity.ok(response);
    }

    // Converter methods
    
    /**
     * Convert UserProblemStatusDTO to VO.
     */
    private UserProblemStatusVO convertUserStatusToVO(UserProblemStatusDTO dto) {
        return UserProblemStatusVO.builder()
                .problemId(dto.getProblemId())
                .title(dto.getTitle())
                .difficulty(dto.getDifficulty())
                .status(dto.getStatus())
                .mastery(dto.getMastery())
                .lastAttemptDate(dto.getLastAttemptDate())
                .lastConsideredDate(dto.getLastConsideredDate())
                .attemptCount(dto.getAttemptCount())
                .accuracy(dto.getAccuracy())
                .notes(dto.getNotes())
                .build();
    }
    
    /**
     * Convert ProblemStatisticsDTO to VO.
     */
    private ProblemStatisticsVO convertStatisticsToVO(ProblemStatisticsDTO dto) {
        return ProblemStatisticsVO.builder()
                .totalProblems(dto.getTotalProblems())
                .easyCount(dto.getEasyCount())
                .mediumCount(dto.getMediumCount())
                .hardCount(dto.getHardCount())
                .premiumCount(dto.getPremiumCount())
                .build();
    }
    
    /**
     * Convert ProblemMasteryDTO to VO.
     */
    private ProblemMasteryVO convertMasteryToVO(ProblemMasteryDTO dto) {
        return ProblemMasteryVO.builder()
                .problemId(dto.getProblemId())
                .masteryLevel(dto.getMasteryLevel())
                .attemptCount(dto.getAttemptCount())
                .accuracy(dto.getAccuracy())
                .lastAttemptDate(dto.getLastAttemptDate())
                .nextReviewDate(dto.getNextReviewDate())
                .difficulty(dto.getDifficulty())
                .notes(dto.getNotes())
                .build();
    }

    // DTOs

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
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserProblemStatus {
        private Long problemId;
        private String title;
        private Difficulty difficulty;
        private String status; // "not_done", "done", "reviewed"
        private Integer mastery; // 0-3 stars
        private String lastAttemptDate;
        private String lastConsideredDate;
        private Integer attemptCount;
        private Double accuracy;
        private String notes;
    }

    @lombok.Data
    public static class UpdateProblemStatusRequest {
        private String status;
        private Integer mastery;
        private String notes;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProblemMastery {
        private Long problemId;
        private Integer masteryLevel; // 0-3
        private Integer attemptCount;
        private Double accuracy;
        private String lastAttemptDate;
        private String nextReviewDate;
        private String difficulty;
        private String notes;
    }
}