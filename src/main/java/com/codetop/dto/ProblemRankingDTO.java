package com.codetop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Problem ranking DTO with frequency and relevance information.
 * 
 * Used for CodeTop-style problem lists with comprehensive ranking data.
 * Includes user-specific status information when user is authenticated.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
public class ProblemRankingDTO {

    // Basic problem info
    private Long problemId;
    private String title;
    private String difficulty;
    private String problemUrl;
    private String leetcodeId;
    
    // User-specific status (populated when user is authenticated)
    private Integer mastery;                    // User's mastery level (0-3 stars)
    private String status;                      // User's completion status: "not_done", "done", "reviewed"
    private String notes;                       // User's notes for this problem
    private LocalDateTime lastAttemptDate;      // Last time user attempted this problem
    private LocalDateTime lastConsideredDate;  // Next review date for FSRS
    private Integer attemptCount;               // Number of times user attempted this problem
    private Double accuracy;                    // User's accuracy rate for this problem

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

    @JsonCreator
    public ProblemRankingDTO(
            @JsonProperty("problemId") Long problemId,
            @JsonProperty("title") String title,
            @JsonProperty("difficulty") String difficulty,
            @JsonProperty("problemUrl") String problemUrl,
            @JsonProperty("leetcodeId") String leetcodeId,
            @JsonProperty("mastery") Integer mastery,
            @JsonProperty("status") String status,
            @JsonProperty("notes") String notes,
            @JsonProperty("lastAttemptDate") LocalDateTime lastAttemptDate,
            @JsonProperty("lastConsideredDate") LocalDateTime lastConsideredDate,
            @JsonProperty("attemptCount") Integer attemptCount,
            @JsonProperty("accuracy") Double accuracy,
            @JsonProperty("frequencyScore") BigDecimal frequencyScore,
            @JsonProperty("interviewCount") Integer interviewCount,
            @JsonProperty("frequencyRank") Integer frequencyRank,
            @JsonProperty("percentile") BigDecimal percentile,
            @JsonProperty("lastAskedDate") LocalDate lastAskedDate,
            @JsonProperty("trend") String trend,
            @JsonProperty("recencyScore") Double recencyScore,
            @JsonProperty("isHotProblem") Boolean isHotProblem,
            @JsonProperty("isTrending") Boolean isTrending,
            @JsonProperty("isTopPercentile") Boolean isTopPercentile,
            @JsonProperty("primaryCategory") String primaryCategory,
            @JsonProperty("allCategories") String[] allCategories,
            @JsonProperty("relevanceScore") BigDecimal relevanceScore,
            @JsonProperty("isPrimary") Boolean isPrimary,
            @JsonProperty("sharedCategories") Integer sharedCategories,
            @JsonProperty("sharedCategoryNames") String sharedCategoryNames,
            @JsonProperty("companyName") String companyName,
            @JsonProperty("departmentName") String departmentName,
            @JsonProperty("positionName") String positionName,
            @JsonProperty("statsScope") String statsScope,
            @JsonProperty("credibilityScore") BigDecimal credibilityScore,
            @JsonProperty("verificationLevel") String verificationLevel,
            @JsonProperty("communityScore") Integer communityScore,
            @JsonProperty("addedDate") LocalDate addedDate,
            @JsonProperty("isPremium") Boolean isPremium,
            @JsonProperty("tags") String tags) {
        this.problemId = problemId;
        this.title = title;
        this.difficulty = difficulty;
        this.problemUrl = problemUrl;
        this.leetcodeId = leetcodeId;
        this.mastery = mastery;
        this.status = status;
        this.notes = notes;
        this.lastAttemptDate = lastAttemptDate;
        this.lastConsideredDate = lastConsideredDate;
        this.attemptCount = attemptCount;
        this.accuracy = accuracy;
        this.frequencyScore = frequencyScore;
        this.interviewCount = interviewCount;
        this.frequencyRank = frequencyRank;
        this.percentile = percentile;
        this.lastAskedDate = lastAskedDate;
        this.trend = trend;
        this.recencyScore = recencyScore;
        this.isHotProblem = isHotProblem;
        this.isTrending = isTrending;
        this.isTopPercentile = isTopPercentile;
        this.primaryCategory = primaryCategory;
        this.allCategories = allCategories;
        this.relevanceScore = relevanceScore;
        this.isPrimary = isPrimary;
        this.sharedCategories = sharedCategories;
        this.sharedCategoryNames = sharedCategoryNames;
        this.companyName = companyName;
        this.departmentName = departmentName;
        this.positionName = positionName;
        this.statsScope = statsScope;
        this.credibilityScore = credibilityScore;
        this.verificationLevel = verificationLevel;
        this.communityScore = communityScore;
        this.addedDate = addedDate;
        this.isPremium = isPremium;
        this.tags = tags;
    }
}