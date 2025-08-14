package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Department entity for CodeTop-style filtering.
 * 
 * Represents standardized departments for company organization hierarchy.
 * Supports nested department structures and display customization.
 * 
 * @author CodeTop Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("departments")
public class Department {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Department name (e.g., "后端开发", "前端开发", "算法工程师")
     */
    @TableField("name")
    private String name;

    /**
     * Display name for UI
     */
    @TableField("display_name")
    private String displayName;

    /**
     * Department description
     */
    @TableField("description")
    private String description;

    /**
     * Department type
     */
    @TableField("type")
    private DepartmentType type;

    /**
     * Parent department ID for nested structure
     */
    @TableField("parent_department_id")
    private Long parentDepartmentId;

    /**
     * Department level in hierarchy (1=top level)
     */
    @TableField("level")
    private Integer level;

    /**
     * Sort order for display
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * Department active status
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
     * Department type enumeration
     */
    public enum DepartmentType {
        ENGINEERING,
        PRODUCT,
        DATA,
        BUSINESS,
        OTHER
    }
}