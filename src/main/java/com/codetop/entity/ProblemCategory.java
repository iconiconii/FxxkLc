package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Problem-Category association entity for structured category management.
 * 
 * Replaces simple JSON tags with normalized category relationships.
 * Supports primary category designation and relevance scoring.
 * 
 * @author CodeTop Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("problem_categories")
public class ProblemCategory {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Problem ID (logical reference to problems.id)
     */
    @TableField("problem_id")
    private Long problemId;

    /**
     * Category ID (logical reference to categories.id)
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * Whether this is a primary category for the problem
     */
    @TableField("is_primary")
    private Boolean isPrimary;

    /**
     * Relevance score (0-100) indicating how relevant this category is
     */
    @TableField("relevance_score")
    private BigDecimal relevanceScore;

    /**
     * Assignment type indicating how this association was created
     */
    @TableField("assignment_type")
    private AssignmentType assignmentType;

    /**
     * User ID who assigned this category (if manually assigned)
     */
    @TableField("assigned_by_user_id")
    private Long assignedByUserId;

    /**
     * Additional metadata in JSON format
     */
    @TableField("metadata")
    private String metadata;

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
     * Assignment type enumeration
     */
    public enum AssignmentType {
        SYSTEM,      // Auto-assigned by system
        MANUAL,      // Manually assigned by user
        COMMUNITY,   // Community consensus
        ML,          // Machine learning classification
        ADMIN        // Admin override
    }
}