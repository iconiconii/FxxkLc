package com.codetop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.entity.Department;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Mapper interface for Department entity operations.
 * 
 * Provides optimized queries for:
 * - Department hierarchy management
 * - Company-department associations
 * - Department filtering and search
 * 
 * @author CodeTop Team
 */
@Mapper
@Repository
public interface DepartmentMapper extends BaseMapper<Department> {

    /**
     * Find department by name.
     */
    @Select("SELECT * FROM departments WHERE name = #{name} AND deleted = 0")
    Optional<Department> findByName(@Param("name") String name);

    /**
     * Find all active departments by type.
     */
    @Select("SELECT * FROM departments WHERE type = #{type} AND is_active = true AND deleted = 0 ORDER BY sort_order")
    List<Department> findByType(@Param("type") String type);

    /**
     * Find departments by company ID.
     */
    @Select("""
            SELECT d.* FROM departments d
            INNER JOIN company_departments cd ON d.id = cd.department_id
            WHERE cd.company_id = #{companyId} AND d.is_active = true AND d.deleted = 0
            ORDER BY cd.priority_level DESC, d.sort_order
            """)
    List<Department> findByCompanyId(@Param("companyId") Long companyId);

    /**
     * Find sub-departments by parent department ID.
     */
    @Select("SELECT * FROM departments WHERE parent_department_id = #{parentId} AND is_active = true AND deleted = 0 ORDER BY sort_order")
    List<Department> findByParentId(@Param("parentId") Long parentId);

    /**
     * Find top-level departments (no parent).
     */
    @Select("SELECT * FROM departments WHERE parent_department_id IS NULL AND is_active = true AND deleted = 0 ORDER BY sort_order")
    List<Department> findTopLevelDepartments();

    /**
     * Search departments by name or display name.
     */
    @Select("""
            SELECT * FROM departments 
            WHERE (LOWER(name) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
               OR LOWER(display_name) LIKE LOWER(CONCAT('%', #{keyword}, '%')))
            AND is_active = true AND deleted = 0
            ORDER BY sort_order
            """)
    Page<Department> searchDepartments(Page<Department> page, @Param("keyword") String keyword);

    /**
     * Count departments by type.
     */
    @Select("SELECT COUNT(*) FROM departments WHERE type = #{type} AND deleted = 0")
    Long countByType(@Param("type") String type);

    /**
     * Find departments with position count.
     */
    @Select("""
            SELECT d.*, COUNT(dp.position_id) as position_count
            FROM departments d
            LEFT JOIN department_positions dp ON d.id = dp.department_id
            WHERE d.is_active = true AND d.deleted = 0
            GROUP BY d.id
            ORDER BY d.sort_order
            """)
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "name", property = "name"),
        @Result(column = "display_name", property = "displayName"),
        @Result(column = "position_count", property = "positionCount")
    })
    List<DepartmentWithPositionCount> findDepartmentsWithPositionCount();

    /**
     * Helper class for department with position count.
     */
    class DepartmentWithPositionCount extends Department {
        private Integer positionCount;
        
        public Integer getPositionCount() { return positionCount; }
        public void setPositionCount(Integer positionCount) { this.positionCount = positionCount; }
    }
}