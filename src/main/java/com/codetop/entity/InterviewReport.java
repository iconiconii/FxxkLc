package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Interview report entity for community-driven interview data collection.
 * 
 * Enhanced with comprehensive data source management and quality control.
 * Supports multiple verification levels and community validation.
 * 
 * @author CodeTop Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("interview_reports")
public class InterviewReport {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * User ID who submitted the report
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Company name
     */
    @TableField("company_name")
    private String companyName;

    /**
     * Department name
     */
    @TableField("department")
    private String department;

    /**
     * Position name
     */
    @TableField("position")
    private String position;

    /**
     * Problem title
     */
    @TableField("problem_title")
    private String problemTitle;

    /**
     * LeetCode problem ID
     */
    @TableField("problem_leetcode_id")
    private String problemLeetcodeId;

    /**
     * Interview date
     */
    @TableField("interview_date")
    private LocalDate interviewDate;

    /**
     * Interview round
     */
    @TableField("interview_round")
    private InterviewRound interviewRound;

    /**
     * Difficulty rating (1-5 scale)
     */
    @TableField("difficulty_rating")
    private Integer difficultyRating;

    /**
     * Additional notes about the interview
     */
    @TableField("additional_notes")
    private String additionalNotes;

    /**
     * Whether the report is verified
     */
    @TableField("is_verified")
    private Boolean isVerified;

    /**
     * Verification notes
     */
    @TableField("verification_notes")
    private String verificationNotes;

    /**
     * Report status
     */
    @TableField("status")
    private ReportStatus status;

    /**
     * Standardized department ID
     */
    @TableField("department_id")
    private Long departmentId;

    /**
     * Standardized position ID
     */
    @TableField("position_id")
    private Long positionId;

    // Enhanced data source management fields

    /**
     * Source of the interview report data
     */
    @TableField("data_source")
    private DataSource dataSource;

    /**
     * URL source of the report (if applicable)
     */
    @TableField("source_url")
    private String sourceUrl;

    /**
     * Credibility score (0-100) based on source and verification
     */
    @TableField("credibility_score")
    private BigDecimal credibilityScore;

    /**
     * Level of verification
     */
    @TableField("verification_level")
    private VerificationLevel verificationLevel;

    /**
     * User ID who verified this report
     */
    @TableField("verified_by_user_id")
    private Long verifiedByUserId;

    /**
     * Timestamp when verification was completed
     */
    @TableField("verified_at")
    private LocalDateTime verifiedAt;

    /**
     * Number of upvotes from community
     */
    @TableField("upvote_count")
    private Integer upvoteCount;

    /**
     * Number of downvotes from community
     */
    @TableField("downvote_count")
    private Integer downvoteCount;

    /**
     * Quality score based on completeness and accuracy
     */
    @TableField("report_quality_score")
    private BigDecimal reportQualityScore;

    /**
     * Whether this report is a duplicate of another report
     */
    @TableField("is_duplicate")
    private Boolean isDuplicate;

    /**
     * Reference to original report if this is a duplicate
     */
    @TableField("original_report_id")
    private Long originalReportId;

    /**
     * Status of duplicate checking
     */
    @TableField("duplicate_check_status")
    private DuplicateCheckStatus duplicateCheckStatus;

    /**
     * Language code of the report
     */
    @TableField("language_code")
    private String languageCode;

    /**
     * Geographic region where interview took place
     */
    @TableField("reporting_region")
    private String reportingRegion;

    /**
     * Method used to collect this data
     */
    @TableField("data_collection_method")
    private DataCollectionMethod dataCollectionMethod;

    /**
     * Record creation time
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Last update time
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * Logical delete flag
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * Interview round enumeration
     */
    public enum InterviewRound {
        PHONE,
        TECHNICAL,
        ONSITE,
        FINAL,
        OTHER
    }

    /**
     * Report status enumeration
     */
    public enum ReportStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    /**
     * Data source enumeration
     */
    public enum DataSource {
        USER_REPORT,
        OFFICIAL_COMPANY,
        CROWDSOURCING,
        AUTOMATED_SCRAPING,
        ADMIN_INPUT
    }

    /**
     * Verification level enumeration
     */
    public enum VerificationLevel {
        UNVERIFIED,
        COMMUNITY_VERIFIED,
        MODERATOR_VERIFIED,
        OFFICIAL_VERIFIED
    }

    /**
     * Duplicate check status enumeration
     */
    public enum DuplicateCheckStatus {
        PENDING,
        CHECKED,
        CONFIRMED_DUPLICATE,
        CONFIRMED_UNIQUE
    }

    /**
     * Data collection method enumeration
     */
    public enum DataCollectionMethod {
        MANUAL_ENTRY,
        FORM_SUBMISSION,
        API_IMPORT,
        BULK_UPLOAD,
        SOCIAL_MEDIA_SCRAPING
    }

    // Business logic methods

    /**
     * Calculate community sentiment score based on votes
     */
    public double getCommunityScore() {
        int totalVotes = upvoteCount + downvoteCount;
        if (totalVotes == 0) return 0.0;
        
        return ((double) upvoteCount / totalVotes) * 100.0;
    }

    /**
     * Check if this report is highly credible
     */
    public boolean isHighlyCredible() {
        return credibilityScore != null && 
               credibilityScore.compareTo(new BigDecimal("80.0")) >= 0 &&
               verificationLevel != VerificationLevel.UNVERIFIED;
    }

    /**
     * Check if this report needs review
     */
    public boolean needsReview() {
        return status == ReportStatus.PENDING || 
               verificationLevel == VerificationLevel.UNVERIFIED ||
               duplicateCheckStatus == DuplicateCheckStatus.PENDING;
    }

    /**
     * Mark as verified by moderator
     */
    public void verifyByModerator(Long moderatorId) {
        this.verificationLevel = VerificationLevel.MODERATOR_VERIFIED;
        this.verifiedByUserId = moderatorId;
        this.verifiedAt = LocalDateTime.now();
        this.status = ReportStatus.APPROVED;
        this.isVerified = true;
        
        // Boost credibility score for verified reports
        if (this.credibilityScore != null) {
            this.credibilityScore = this.credibilityScore.add(new BigDecimal("20.0"));
            if (this.credibilityScore.compareTo(new BigDecimal("100.0")) > 0) {
                this.credibilityScore = new BigDecimal("100.0");
            }
        }
    }

    /**
     * Add community vote
     */
    public void addVote(boolean isUpvote) {
        if (isUpvote) {
            this.upvoteCount++;
        } else {
            this.downvoteCount++;
        }
        
        // Adjust credibility based on community sentiment
        updateCredibilityFromVotes();
    }

    /**
     * Update credibility score based on community votes
     */
    private void updateCredibilityFromVotes() {
        double communityScore = getCommunityScore();
        if (communityScore > 70.0 && upvoteCount >= 5) {
            // Boost credibility for well-received reports
            this.credibilityScore = this.credibilityScore.add(new BigDecimal("5.0"));
        } else if (communityScore < 30.0 && downvoteCount >= 3) {
            // Reduce credibility for poorly received reports
            this.credibilityScore = this.credibilityScore.subtract(new BigDecimal("10.0"));
        }
        
        // Ensure credibility stays within bounds
        if (this.credibilityScore.compareTo(BigDecimal.ZERO) < 0) {
            this.credibilityScore = BigDecimal.ZERO;
        }
        if (this.credibilityScore.compareTo(new BigDecimal("100.0")) > 0) {
            this.credibilityScore = new BigDecimal("100.0");
        }
    }
}