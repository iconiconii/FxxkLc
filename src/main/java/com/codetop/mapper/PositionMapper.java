package com.codetop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.entity.Position;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Mapper interface for Position entity operations.
 * 
 * Provides optimized queries for:
 * - Position management and filtering
 * - Department-position associations
 * - Experience level and skill queries
 * 
 * @author CodeTop Team
 */
@Mapper
@Repository
public interface PositionMapper extends BaseMapper<Position> {

    /**
     * Find position by name.
     */
    @Select("SELECT * FROM positions WHERE name = #{name} AND deleted = 0")
    Optional<Position> findByName(@Param("name") String name);

    /**
     * Find all active positions by type.
     */
    @Select("SELECT * FROM positions WHERE type = #{type} AND is_active = true AND deleted = 0 ORDER BY name")
    List<Position> findByType(@Param("type") String type);

    /**
     * Find positions by level.
     */
    @Select("SELECT * FROM positions WHERE level = #{level} AND is_active = true AND deleted = 0 ORDER BY name")
    List<Position> findByLevel(@Param("level") String level);

    /**
     * Find positions by department ID.
     */
    @Select("""
            SELECT p.* FROM positions p
            INNER JOIN department_positions dp ON p.id = dp.position_id
            WHERE dp.department_id = #{departmentId} AND p.is_active = true AND p.deleted = 0
            ORDER BY dp.is_primary DESC, dp.demand_level DESC, p.name
            """)
    List<Position> findByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * Find positions by experience range.
     */
    @Select("""
            SELECT * FROM positions 
            WHERE experience_years_min <= #{maxYears} 
            AND experience_years_max >= #{minYears}
            AND is_active = true AND deleted = 0
            ORDER BY experience_years_min
            """)
    List<Position> findByExperienceRange(@Param("minYears") Integer minYears, @Param("maxYears") Integer maxYears);

    /**
     * Search positions by name or description.
     */
    @Select("""
            SELECT * FROM positions 
            WHERE (LOWER(name) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
               OR LOWER(display_name) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
               OR LOWER(description) LIKE LOWER(CONCAT('%', #{keyword}, '%')))
            AND is_active = true AND deleted = 0
            ORDER BY name
            """)
    Page<Position> searchPositions(Page<Position> page, @Param("keyword") String keyword);

    /**
     * Find positions for a specific company and department.
     */
    @Select("""
            SELECT DISTINCT p.* FROM positions p
            INNER JOIN department_positions dp ON p.id = dp.position_id
            INNER JOIN company_departments cd ON dp.department_id = cd.department_id
            WHERE cd.company_id = #{companyId} 
            AND dp.department_id = #{departmentId}
            AND p.is_active = true AND p.deleted = 0
            """)
    List<Position> findByCompanyAndDepartment(@Param("companyId") Long companyId, @Param("departmentId") Long departmentId);

    /**
     * Count positions by level.
     */
    @Select("SELECT COUNT(*) FROM positions WHERE level = #{level} AND deleted = 0")
    Long countByLevel(@Param("level") String level);

    /**
     * Find high-demand positions.
     */
    @Select("""
            SELECT p.*, dp.demand_level, dp.interview_frequency, COUNT(dp.department_id) as department_count
            FROM positions p
            INNER JOIN department_positions dp ON p.id = dp.position_id
            WHERE dp.demand_level IN ('HIGH', 'URGENT') 
            AND p.is_active = true AND p.deleted = 0
            GROUP BY p.id
            ORDER BY department_count DESC, dp.demand_level DESC
            """)
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "name", property = "name"),
        @Result(column = "demand_level", property = "demandLevel"),
        @Result(column = "interview_frequency", property = "interviewFrequency"),
        @Result(column = "department_count", property = "departmentCount")
    })
    List<HighDemandPosition> findHighDemandPositions();

    /**
     * Find positions suitable for entry level (junior).
     */
    @Select("""
            SELECT * FROM positions 
            WHERE level IN ('INTERN', 'JUNIOR') 
            AND experience_years_min <= 2
            AND is_active = true AND deleted = 0
            ORDER BY level, name
            """)
    List<Position> findEntryLevelPositions();

    /**
     * Find senior positions (expert level).
     */
    @Select("""
            SELECT * FROM positions 
            WHERE level IN ('EXPERT', 'PRINCIPAL') 
            AND experience_years_min >= 5
            AND is_active = true AND deleted = 0
            ORDER BY level DESC, name
            """)
    List<Position> findSeniorPositions();

    /**
     * Helper class for high-demand position results.
     */
    class HighDemandPosition extends Position {
        private String demandLevel;
        private String interviewFrequency;
        private Integer departmentCount;
        
        public String getDemandLevel() { return demandLevel; }
        public void setDemandLevel(String demandLevel) { this.demandLevel = demandLevel; }
        public String getInterviewFrequency() { return interviewFrequency; }
        public void setInterviewFrequency(String interviewFrequency) { this.interviewFrequency = interviewFrequency; }
        public Integer getDepartmentCount() { return departmentCount; }
        public void setDepartmentCount(Integer departmentCount) { this.departmentCount = departmentCount; }
    }
}