package com.codetop.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO for CodeTop-style filtering.
 * 
 * Supports three-level filtering hierarchy and advanced search options.
 * 
 * @author CodeTop Team
 */
@Data
public class CodeTopFilterRequest {

    // Pagination
    private Integer page = 1;
    private Integer size = 20;

    // Basic search
    private String keyword;
    private String difficulty; // EASY, MEDIUM, HARD

    // Three-level filtering hierarchy
    private Long companyId;
    private Long departmentId;
    private Long positionId;

    // Category filtering
    private Long categoryId;
    private Boolean primaryCategoryOnly = false;

    // Frequency filtering
    private BigDecimal minFrequencyScore;
    private String trendFilter; // INCREASING, STABLE, DECREASING, NEW
    private Integer minInterviewCount;

    // Date filtering
    private Integer daysSinceLastAsked;
    private Boolean hotProblemsOnly = false;
    private Boolean trendingOnly = false;

    // Sorting options
    private String sortBy = "frequency_score"; // frequency_score, interview_count, last_asked_date, title
    private String sortOrder = "desc"; // asc, desc

    // Quality filtering
    private BigDecimal minCredibilityScore;
    private String verificationLevel; // UNVERIFIED, COMMUNITY_VERIFIED, MODERATOR_VERIFIED, OFFICIAL_VERIFIED
}