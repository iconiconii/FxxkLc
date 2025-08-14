package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Problem ranking DTO with frequency and relevance information.
 * 
 * Used for CodeTop-style problem lists with comprehensive ranking data.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProblemRankingDTO {

    // Basic problem info
    private Long problemId;
    private String title;
    private String difficulty;
    private String problemUrl;
    private String leetcodeId;

    // Frequency statistics
    private BigDecimal frequencyScore;
    private Integer interviewCount;
    private Integer frequencyRank;
    private BigDecimal percentile;
    private LocalDate lastAskedDate;
    private String trend;

    // Analysis scores
    private Double recencyScore;
    private Boolean isHotProblem;
    private Boolean isTrending;
    private Boolean isTopPercentile;

    // Category information
    private String primaryCategory;
    private String[] allCategories;
    private BigDecimal relevanceScore;
    private Boolean isPrimary;

    // Similar problems data
    private Integer sharedCategories;
    private String sharedCategoryNames;

    // Company-specific data
    private String companyName;
    private String departmentName;
    private String positionName;
    private String statsScope;

    // Quality indicators
    private BigDecimal credibilityScore;
    private String verificationLevel;
    private Integer communityScore;

    // Additional metadata
    private LocalDate addedDate;
    private Boolean isPremium;
    private String tags;
}