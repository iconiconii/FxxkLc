package com.codetop.mapper;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.entity.Problem;
import com.codetop.enums.Difficulty;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Mapper interface for Problem entity operations using MyBatis-Plus.
 * 
 * Provides optimized queries for:
 * - Problem search and filtering
 * - Difficulty-based queries
 * - Company association queries
 * - Statistics and analytics
 * - Tag-based searches
 * 
 * @author CodeTop Team
 */
@Mapper
@Repository
public interface ProblemMapper extends BaseMapper<Problem> {

    // Basic problem queries
    
    /**
     * Find problem by title.
     */
    @Select("SELECT * FROM problems WHERE title = #{title} AND deleted = 0")
    Optional<Problem> findByTitle(@Param("title") String title);

    /**
     * Find problem by LeetCode ID.
     */
    @Select("SELECT * FROM problems WHERE leetcode_id = #{leetcodeId} AND deleted = 0")
    Optional<Problem> findByLeetcodeId(@Param("leetcodeId") String leetcodeId);
    
    // Difficulty-based queries
    
    /**
     * Find problems by difficulty.
     */
    @Select("SELECT * FROM problems WHERE difficulty = #{difficulty} AND deleted = 0 ORDER BY created_at DESC")
    List<Problem> findByDifficulty(@Param("difficulty") String difficulty);

    /**
     * Find problems by difficulty with pagination.
     */
    @Select("SELECT * FROM problems WHERE difficulty = #{difficulty} AND deleted = 0 ORDER BY created_at DESC")
    Page<Problem> findByDifficultyWithPagination(Page<Problem> page, @Param("difficulty") String difficulty);
    
    // Search queries
    
    /**
     * Search problems by title.
     */
    @Select("""
            SELECT * FROM problems 
            WHERE LOWER(title) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
            AND deleted = 0 
            ORDER BY created_at DESC
            """)
    Page<Problem> searchProblems(Page<Problem> page, @Param("keyword") String keyword);

    /**
     * Search problems by keyword with optional difficulty filter.
     */
    @Select("""
            <script>
            SELECT * FROM problems 
            WHERE LOWER(title) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
            AND deleted = 0 
            <if test="difficulty != null and difficulty != ''">
                AND difficulty = #{difficulty}
            </if>
            ORDER BY created_at DESC
            </script>
            """)
    Page<Problem> searchProblemsByKeyword(Page<Problem> page, @Param("keyword") String keyword, @Param("difficulty") String difficulty);

    /**
     * Find problems by difficulty and search keyword.
     */
    @Select("""
            SELECT * FROM problems 
            WHERE difficulty = #{difficulty} 
            AND LOWER(title) LIKE LOWER(CONCAT('%', #{search}, '%'))
            AND deleted = 0 
            ORDER BY created_at DESC
            """)
    Page<Problem> findByDifficultyAndSearch(Page<Problem> page, @Param("difficulty") String difficulty, @Param("search") String search);

    /**
     * Search problems by tags.
     */
    @Select("SELECT * FROM problems WHERE JSON_CONTAINS(tags, JSON_QUOTE(#{tag})) AND deleted = 0 ORDER BY created_at DESC")
    Page<Problem> findByTag(Page<Problem> page, @Param("tag") String tag);

    /**
     * Find problems by tag with pagination.
     */
    @Select("SELECT * FROM problems WHERE JSON_CONTAINS(tags, JSON_QUOTE(#{tag})) AND deleted = 0 ORDER BY created_at DESC")
    Page<Problem> findByTagWithPagination(Page<Problem> page, @Param("tag") String tag);

    /**
     * Advanced search with multiple filters.
     */
    @Select("""
            <script>
            SELECT * FROM problems 
            WHERE deleted = 0
            <if test="keyword != null and keyword != ''">
                AND LOWER(title) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="difficulty != null">
                AND difficulty = #{difficulty}
            </if>
            <if test="tag != null and tag != ''">
                AND JSON_CONTAINS(tags, JSON_QUOTE(#{tag}))
            </if>
            <if test="isPremium != null">
                AND is_premium = #{isPremium}
            </if>
            ORDER BY created_at DESC
            </script>
            """)
    Page<Problem> advancedSearch(Page<Problem> page, 
                               @Param("keyword") String keyword,
                               @Param("difficulty") String difficulty,
                               @Param("tag") String tag,
                               @Param("isPremium") Boolean isPremium);
    
    // Statistics queries
    
    /**
     * Count problems by difficulty.
     */
    @Select("SELECT COUNT(*) FROM problems WHERE difficulty = #{difficulty} AND deleted = 0")
    Long countByDifficulty(@Param("difficulty") String difficulty);

    /**
     * Count all active problems.
     */
    @Select("SELECT COUNT(*) FROM problems WHERE deleted = 0")
    Long countActiveProblems();

    /**
     * Count premium problems.
     */
    @Select("SELECT COUNT(*) FROM problems WHERE is_premium = true AND deleted = 0")
    Long countPremiumProblems();

    /**
     * Find recently added problems.
     */
    @Select("SELECT * FROM problems WHERE deleted = 0 ORDER BY created_at DESC LIMIT #{limit}")
    List<Problem> findRecentProblems(@Param("limit") int limit);
    
    // Company association queries
    
    /**
     * Find problems by company ID.
     */
    @Select("""
            SELECT p.* FROM problems p 
            INNER JOIN problem_companies pc ON p.id = pc.problem_id 
            WHERE pc.company_id = #{companyId} AND p.deleted = 0 
            ORDER BY pc.frequency DESC, p.created_at DESC
            """)
    Page<Problem> findByCompanyId(Page<Problem> page, @Param("companyId") Long companyId);

    /**
     * Find problems by company name.
     */
    @Select("""
            SELECT p.* FROM problems p 
            INNER JOIN problem_companies pc ON p.id = pc.problem_id 
            INNER JOIN companies c ON pc.company_id = c.id 
            WHERE LOWER(c.name) = LOWER(#{companyName}) AND p.deleted = 0 
            ORDER BY pc.frequency DESC, p.created_at DESC
            """)
    Page<Problem> findByCompanyName(Page<Problem> page, @Param("companyName") String companyName);

    /**
     * Find hot problems (high frequency in multiple companies).
     */
    @Select("""
            SELECT p.*, COUNT(pc.company_id) as company_count
            FROM problems p 
            INNER JOIN problem_companies pc ON p.id = pc.problem_id 
            WHERE p.deleted = 0 
            GROUP BY p.id 
            HAVING company_count >= #{minCompanies}
            ORDER BY company_count DESC
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "title", property = "title"),
        @Result(column = "difficulty", property = "difficulty"),
        @Result(column = "company_count", property = "companyCount")
    })
    List<HotProblem> findHotProblems(@Param("minCompanies") int minCompanies, @Param("limit") int limit);
    
    // Tag operations
    
    /**
     * Get all distinct tags.
     */
    @Select("""
            SELECT DISTINCT JSON_UNQUOTE(JSON_EXTRACT(tags, CONCAT('$[', idx, ']'))) as tag
            FROM problems p
            CROSS JOIN JSON_TABLE(JSON_KEYS(tags), '$[*]' COLUMNS (idx INT PATH '$')) AS jt
            WHERE p.deleted = 0 AND p.tags IS NOT NULL
            ORDER BY tag
            """)
    List<String> findAllTags();

    /**
     * Get tag usage statistics.
     */
    @Select("""
            SELECT 
                JSON_UNQUOTE(JSON_EXTRACT(tags, CONCAT('$[', idx, ']'))) as tag,
                COUNT(*) as usage_count
            FROM problems p
            CROSS JOIN JSON_TABLE(JSON_KEYS(tags), '$[*]' COLUMNS (idx INT PATH '$')) AS jt
            WHERE p.deleted = 0 AND p.tags IS NOT NULL
            GROUP BY tag
            HAVING usage_count > 0
            ORDER BY usage_count DESC, tag ASC
            """)
    @Results({
        @Result(column = "tag", property = "tag"),
        @Result(column = "usage_count", property = "usageCount")
    })
    List<TagUsage> getTagUsageStatistics();
    
    // Helper classes for complex query results
    
    /**
     * Helper class for hot problem results.
     */
    class HotProblem extends Problem {
        @TableField(exist = false)
        private Integer companyCount;
        
        // Getters and setters
        public Integer getCompanyCount() { return companyCount; }
        public void setCompanyCount(Integer companyCount) { this.companyCount = companyCount; }
    }
    
    /**
     * Helper class for tag usage statistics.
     */
    class TagUsage {
        private String tag;
        private Integer usageCount;
        
        // Getters and setters
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
        public Integer getUsageCount() { return usageCount; }
        public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    }
}