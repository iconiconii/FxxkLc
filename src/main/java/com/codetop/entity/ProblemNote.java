package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Problem Note entity for user-specific notes on problems.
 * 
 * Features:
 * - Personal notes and insights on problems
 * - Solution approaches and tips
 * - Time complexity and space complexity notes
 * - Pitfalls and common mistakes
 * - Pattern recognition notes
 * 
 * @author CodeTop Team
 */
@TableName("problem_notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemNote extends BaseEntity {

    @NotNull
    @TableField("user_id")
    private Long userId;

    @NotNull
    @TableField("problem_id")
    private Long problemId;

    @TableField("content")
    private String content;

    @TableField("solution_approach")
    private String solutionApproach;

    @TableField("time_complexity")
    private String timeComplexity;

    @TableField("space_complexity")
    private String spaceComplexity;

    @TableField("pitfalls")
    private String pitfalls;

    @TableField("tips")
    private String tips;

    @Builder.Default
    @TableField("is_public")
    private Boolean isPublic = false;

    @TableField("tags")
    private String tags; // Comma-separated tags

    @Builder.Default
    @TableField("helpful_votes")
    private Integer helpfulVotes = 0;

    @Builder.Default
    @TableField("view_count")
    private Integer viewCount = 0;

    // Associations (not mapped to database in MyBatis-Plus)
    @TableField(exist = false)
    @JsonIgnore
    private User user;

    @TableField(exist = false)
    @JsonIgnore
    private Problem problem;

    // Derived methods
    public boolean hasContent() {
        return content != null && !content.trim().isEmpty();
    }

    public boolean hasSolutionApproach() {
        return solutionApproach != null && !solutionApproach.trim().isEmpty();
    }

    public boolean hasComplexityAnalysis() {
        return (timeComplexity != null && !timeComplexity.trim().isEmpty()) ||
               (spaceComplexity != null && !spaceComplexity.trim().isEmpty());
    }

    public boolean isHelpful() {
        return helpfulVotes > 0;
    }

    public boolean isPopular() {
        return viewCount > 100 || helpfulVotes > 10;
    }

    /**
     * Add a helpful vote.
     */
    public void addHelpfulVote() {
        this.helpfulVotes++;
    }

    /**
     * Remove a helpful vote.
     */
    public void removeHelpfulVote() {
        if (this.helpfulVotes > 0) {
            this.helpfulVotes--;
        }
    }

    /**
     * Increment view count.
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * Make note public.
     */
    public void makePublic() {
        this.isPublic = true;
    }

    /**
     * Make note private.
     */
    public void makePrivate() {
        this.isPublic = false;
    }

    @Override
    public String toString() {
        return "ProblemNote{" +
                "id=" + getId() +
                ", userId=" + userId +
                ", problemId=" + problemId +
                ", isPublic=" + isPublic +
                ", helpfulVotes=" + helpfulVotes +
                ", viewCount=" + viewCount +
                '}';
    }
}