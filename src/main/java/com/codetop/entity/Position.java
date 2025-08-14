package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Position entity for CodeTop-style filtering.
 * 
 * Represents standardized positions within departments.
 * Supports experience level tracking and skill requirements.
 * 
 * @author CodeTop Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("positions")
public class Position {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Position name (e.g., "Java工程师", "前端工程师", "算法工程师")
     */
    @TableField("name")
    private String name;

    /**
     * Display name for UI
     */
    @TableField("display_name")
    private String displayName;

    /**
     * Position description
     */
    @TableField("description")
    private String description;

    /**
     * Position level
     */
    @TableField("level")
    private PositionLevel level;

    /**
     * Position type
     */
    @TableField("type")
    private PositionType type;

    /**
     * Required skills in JSON format
     */
    @TableField("skills_required")
    private String skillsRequired;

    /**
     * Minimum years of experience required
     */
    @TableField("experience_years_min")
    private Integer experienceYearsMin;

    /**
     * Maximum years of experience
     */
    @TableField("experience_years_max")
    private Integer experienceYearsMax;

    /**
     * Position active status
     */
    @TableField("is_active")
    private Boolean isActive;

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
     * Position level enumeration
     */
    public enum PositionLevel {
        INTERN,
        JUNIOR,
        MIDDLE,
        SENIOR,
        EXPERT,
        PRINCIPAL
    }

    /**
     * Position type enumeration
     */
    public enum PositionType {
        ENGINEERING,
        PRODUCT,
        DATA,
        BUSINESS,
        OTHER
    }
}