package com.codetop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.entity.Company;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Mapper interface for Company entity operations.
 * 
 * Provides optimized queries for:
 * - Company management and filtering
 * - Company search and pagination
 * - Active company queries for dropdowns
 * 
 * @author CodeTop Team
 */
@Mapper
@Repository
public interface CompanyMapper extends BaseMapper<Company> {

    /**
     * Find company by name.
     */
    @Select("SELECT * FROM companies WHERE name = #{name} AND deleted = 0")
    Optional<Company> findByName(@Param("name") String name);

    /**
     * Find all active companies for dropdown selection.
     */
    @Select("SELECT * FROM companies WHERE is_active = true AND deleted = 0 ORDER BY display_name")
    List<Company> findAllActiveCompanies();

    /**
     * Find companies by industry.
     */
    @Select("SELECT * FROM companies WHERE industry = #{industry} AND is_active = true AND deleted = 0 ORDER BY display_name")
    List<Company> findByIndustry(@Param("industry") String industry);

    /**
     * Find companies by size category.
     */
    @Select("SELECT * FROM companies WHERE size_category = #{sizeCategory} AND is_active = true AND deleted = 0 ORDER BY display_name")
    List<Company> findBySizeCategory(@Param("sizeCategory") String sizeCategory);

    /**
     * Search companies by name or display name.
     */
    @Select("""
            SELECT * FROM companies 
            WHERE (LOWER(name) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
               OR LOWER(display_name) LIKE LOWER(CONCAT('%', #{keyword}, '%')))
            AND is_active = true AND deleted = 0
            ORDER BY display_name
            """)
    Page<Company> searchCompanies(Page<Company> page, @Param("keyword") String keyword);

    /**
     * Find companies that have departments (with problem associations).
     */
    @Select("""
            SELECT DISTINCT c.* FROM companies c
            INNER JOIN company_departments cd ON c.id = cd.company_id
            WHERE c.is_active = true AND c.deleted = 0
            ORDER BY c.display_name
            """)
    List<Company> findCompaniesWithDepartments();

    /**
     * Find companies with problem count.
     */
    @Select("""
            SELECT c.*, COUNT(DISTINCT pc.problem_id) as problem_count
            FROM companies c
            LEFT JOIN problem_companies pc ON c.id = pc.company_id
            WHERE c.is_active = true AND c.deleted = 0
            GROUP BY c.id
            ORDER BY problem_count DESC, c.display_name
            """)
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "name", property = "name"),
        @Result(column = "display_name", property = "displayName"),
        @Result(column = "problem_count", property = "problemCount")
    })
    List<CompanyWithProblemCount> findCompaniesWithProblemCount();

    /**
     * Count companies by industry.
     */
    @Select("SELECT COUNT(*) FROM companies WHERE industry = #{industry} AND deleted = 0")
    Long countByIndustry(@Param("industry") String industry);

    /**
     * Check if company exists by name.
     */
    @Select("SELECT COUNT(*) FROM companies WHERE name = #{name}")
    int countByName(@Param("name") String name);

    /**
     * Helper class for company with problem count results.
     */
    class CompanyWithProblemCount extends Company {
        private Integer problemCount;
        
        public Integer getProblemCount() { return problemCount; }
        public void setProblemCount(Integer problemCount) { this.problemCount = problemCount; }
    }
}