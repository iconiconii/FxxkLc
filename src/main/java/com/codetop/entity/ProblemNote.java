package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Problem Note entity for user-specific notes on problems (Metadata only).
 * 
 * This entity stores metadata and basic information about problem notes,
 * while detailed content is stored in MongoDB via ProblemNoteDocument.
 * 
 * Features:
 * - Basic note metadata and visibility settings
 * - Statistics and engagement metrics
 * - Soft delete support
 * - Association with User and Problem entities
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

    @NotNull
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    @TableField("title")
    private String title;

    @Builder.Default
    @TableField("is_public")
    private Boolean isPublic = false;

    @TableField("note_type")
    private String noteType; // SOLUTION, EXPLANATION, TIPS, PATTERN, etc.

    @Builder.Default
    @TableField("helpful_votes")
    private Integer helpfulVotes = 0;

    @Builder.Default
    @TableField("view_count")
    private Integer viewCount = 0;

    @Builder.Default
    @TableLogic
    @TableField("deleted")
    private Boolean deleted = false;

    // Associations (not mapped to database in MyBatis-Plus)
    @TableField(exist = false)
    @JsonIgnore
    private User user;

    @TableField(exist = false)
    @JsonIgnore
    private Problem problem;

    // Derived methods for business logic
    public boolean isHelpful() {
        return helpfulVotes != null && helpfulVotes > 0;
    }

    public boolean isPopular() {
        return (viewCount != null && viewCount > 100) || (helpfulVotes != null && helpfulVotes > 10);
    }

    public boolean hasTitle() {
        return title != null && !title.trim().isEmpty();
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

    /**
     * Mark as deleted (soft delete).
     */
    public void markDeleted() {
        this.deleted = true;
    }

    /**
     * Restore from deleted state.
     */
    public void restore() {
        this.deleted = false;
    }

    @Override
    public String toString() {
        return "ProblemNote{" +
                "id=" + getId() +
                ", userId=" + userId +
                ", problemId=" + problemId +
                ", title='" + title + '\'' +
                ", isPublic=" + isPublic +
                ", noteType='" + noteType + '\'' +
                ", helpfulVotes=" + helpfulVotes +
                ", viewCount=" + viewCount +
                ", deleted=" + deleted +
                '}';
    }
}