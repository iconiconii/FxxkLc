package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codetop.enums.Difficulty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Problem entity representing algorithm coding problems.
 * 
 * Features:
 * - Simplified structure for FSRS algorithm
 * - Full-text search support on title
 * - JSON storage for tags
 * - Company association tracking
 * - Difficulty categorization (Easy, Medium, Hard)
 * - Soft delete support
 * - Problem content accessed via problem_url
 * 
 * @author CodeTop Team
 */
@TableName("problems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Problem extends BaseEntity {

    @NotBlank
    @Size(min = 1, max = 200)
    @TableField("title")
    private String title;

    @TableField("difficulty")
    private Difficulty difficulty;

    @TableField("tags")
    private String tags; // Store as JSON string

    @TableField("problem_url")
    private String problemUrl;

    @TableField("leetcode_id")
    private String leetcodeId;

    @Builder.Default
    @TableLogic // This field will be used for logical deletion
    @TableField("deleted")
    private Boolean isDeleted = false;

    @Builder.Default
    @TableField("is_premium")
    private Boolean isPremium = false;

    // Derived fields for convenience
    @JsonIgnore
    public boolean isEasy() {
        return difficulty == Difficulty.EASY;
    }

    @JsonIgnore
    public boolean isMedium() {
        return difficulty == Difficulty.MEDIUM;
    }

    @JsonIgnore
    public boolean isHard() {
        return difficulty == Difficulty.HARD;
    }

    /**
     * Check if problem has specific tag (for JSON tags field).
     */
    public boolean hasTag(String tag) {
        return tags != null && tags.contains("\"" + tag + "\"");
    }

    /**
     * Soft delete the problem.
     */
    public void softDelete() {
        this.isDeleted = true;
    }

    /**
     * Restore soft deleted problem.
     */
    public void restore() {
        this.isDeleted = false;
    }

    @Override
    public String toString() {
        return "Problem{" +
                "id=" + getId() +
                ", title='" + title + '\'' +
                ", difficulty=" + difficulty +
                ", leetcodeId='" + leetcodeId + '\'' +
                ", isDeleted=" + isDeleted +
                '}';
    }
}