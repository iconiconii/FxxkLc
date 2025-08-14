package com.codetop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.entity.ProblemFrequencyStats;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Mapper interface for ProblemFrequencyStats entity operations.
 * 
 * Provides optimized queries for CodeTop-style filtering and ranking:
 * - Frequency-based problem ranking
 * - Multi-level scope filtering (global, company, department, position)
 * - Trending analysis and hot problem detection
 * 
 * @author CodeTop Team
 */
@Mapper
@Repository
public interface ProblemFrequencyStatsMapper extends BaseMapper<ProblemFrequencyStats> {

    /**
     * Find top problems by frequency score for a specific scope.
     */
    @Select("""
            SELECT pfs.*, p.title, p.difficulty
            FROM problem_frequency_stats pfs
            INNER JOIN problems p ON pfs.problem_id = p.id
            WHERE pfs.stats_scope = #{scope}
            <if test="companyId != null">AND pfs.company_id = #{companyId}</if>
            <if test="departmentId != null">AND pfs.department_id = #{departmentId}</if>
            <if test="positionId != null">AND pfs.position_id = #{positionId}</if>
            ORDER BY pfs.total_frequency_score DESC, pfs.interview_count DESC
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "problem_id", property = "problemId"),
        @Result(column = "title", property = "problemTitle"),
        @Result(column = "difficulty", property = "problemDifficulty")
    })
    List<ProblemFrequencyWithDetails> findTopProblemsByFrequency(
            @Param("scope") String scope,
            @Param("companyId") Long companyId,
            @Param("departmentId") Long departmentId,
            @Param("positionId") Long positionId,
            @Param("limit") Integer limit);

    /**
     * Find trending problems (increasing frequency trend).
     */
    @Select("""
            SELECT pfs.*, p.title, p.difficulty
            FROM problem_frequency_stats pfs
            INNER JOIN problems p ON pfs.problem_id = p.id
            WHERE pfs.frequency_trend = 'INCREASING'
            AND pfs.last_asked_date >= #{sinceDate}
            <if test="scope != null">AND pfs.stats_scope = #{scope}</if>
            <if test="companyId != null">AND pfs.company_id = #{companyId}</if>
            ORDER BY pfs.total_frequency_score DESC, pfs.last_asked_date DESC
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "problem_id", property = "problemId"),
        @Result(column = "title", property = "problemTitle"),
        @Result(column = "difficulty", property = "problemDifficulty")
    })
    List<ProblemFrequencyWithDetails> findTrendingProblems(
            @Param("sinceDate") LocalDate sinceDate,
            @Param("scope") String scope,
            @Param("companyId") Long companyId,
            @Param("limit") Integer limit);

    /**
     * Find problems by company with department breakdown.
     */
    @Select("""
            SELECT 
                pfs.*,
                p.title,
                p.difficulty,
                d.name as department_name,
                pos.name as position_name
            FROM problem_frequency_stats pfs
            INNER JOIN problems p ON pfs.problem_id = p.id
            LEFT JOIN departments d ON pfs.department_id = d.id
            LEFT JOIN positions pos ON pfs.position_id = pos.id
            WHERE pfs.company_id = #{companyId}
            AND pfs.stats_scope IN ('COMPANY', 'DEPARTMENT', 'POSITION')
            ORDER BY pfs.stats_scope, pfs.total_frequency_score DESC
            """)
    @Results({
        @Result(column = "problem_id", property = "problemId"),
        @Result(column = "title", property = "problemTitle"),
        @Result(column = "difficulty", property = "problemDifficulty"),
        @Result(column = "department_name", property = "departmentName"),
        @Result(column = "position_name", property = "positionName")
    })
    List<CompanyProblemBreakdown> findCompanyProblemBreakdown(@Param("companyId") Long companyId);

    /**
     * Get global problem rankings.
     */
    @Select("""
            SELECT pfs.*, p.title, p.difficulty
            FROM problem_frequency_stats pfs
            INNER JOIN problems p ON pfs.problem_id = p.id
            WHERE pfs.stats_scope = 'GLOBAL'
            ORDER BY pfs.frequency_rank
            """)
    Page<ProblemFrequencyWithDetails> getGlobalRankings(Page<ProblemFrequencyStats> page);

    /**
     * Find hot problems (top percentile + recent activity).
     */
    @Select("""
            SELECT pfs.*, p.title, p.difficulty
            FROM problem_frequency_stats pfs
            INNER JOIN problems p ON pfs.problem_id = p.id
            WHERE pfs.percentile >= #{minPercentile}
            AND pfs.last_asked_date >= #{sinceDate}
            <if test="scope != null">AND pfs.stats_scope = #{scope}</if>
            <if test="companyId != null">AND pfs.company_id = #{companyId}</if>
            ORDER BY pfs.percentile DESC, pfs.total_frequency_score DESC
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "problem_id", property = "problemId"),
        @Result(column = "title", property = "problemTitle"),
        @Result(column = "difficulty", property = "problemDifficulty")
    })
    List<ProblemFrequencyWithDetails> findHotProblems(
            @Param("minPercentile") BigDecimal minPercentile,
            @Param("sinceDate") LocalDate sinceDate,
            @Param("scope") String scope,
            @Param("companyId") Long companyId,
            @Param("limit") Integer limit);

    /**
     * Get frequency statistics for a specific problem across all scopes.
     */
    @Select("""
            SELECT 
                pfs.*,
                c.name as company_name,
                d.name as department_name,
                pos.name as position_name
            FROM problem_frequency_stats pfs
            LEFT JOIN companies c ON pfs.company_id = c.id
            LEFT JOIN departments d ON pfs.department_id = d.id
            LEFT JOIN positions pos ON pfs.position_id = pos.id
            WHERE pfs.problem_id = #{problemId}
            ORDER BY pfs.stats_scope, pfs.total_frequency_score DESC
            """)
    @Results({
        @Result(column = "company_name", property = "companyName"),
        @Result(column = "department_name", property = "departmentName"),
        @Result(column = "position_name", property = "positionName")
    })
    List<ProblemStatsBreakdown> getProblemStatsBreakdown(@Param("problemId") Long problemId);

    /**
     * Search problems with frequency filtering.
     */
    @Select("""
            <script>
            SELECT pfs.*, p.title, p.difficulty
            FROM problem_frequency_stats pfs
            INNER JOIN problems p ON pfs.problem_id = p.id
            WHERE 1=1
            <if test="keyword != null and keyword != ''">
                AND LOWER(p.title) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="scope != null">AND pfs.stats_scope = #{scope}</if>
            <if test="companyId != null">AND pfs.company_id = #{companyId}</if>
            <if test="departmentId != null">AND pfs.department_id = #{departmentId}</if>
            <if test="positionId != null">AND pfs.position_id = #{positionId}</if>
            <if test="minFrequencyScore != null">AND pfs.total_frequency_score >= #{minFrequencyScore}</if>
            <if test="difficulty != null">AND p.difficulty = #{difficulty}</if>
            <if test="trendFilter != null">AND pfs.frequency_trend = #{trendFilter}</if>
            ORDER BY pfs.total_frequency_score DESC, pfs.last_asked_date DESC
            </script>
            """)
    Page<ProblemFrequencyWithDetails> searchProblemsWithFrequency(
            Page<ProblemFrequencyStats> page,
            @Param("keyword") String keyword,
            @Param("scope") String scope,
            @Param("companyId") Long companyId,
            @Param("departmentId") Long departmentId,
            @Param("positionId") Long positionId,
            @Param("minFrequencyScore") BigDecimal minFrequencyScore,
            @Param("difficulty") String difficulty,
            @Param("trendFilter") String trendFilter);

    /**
     * Update frequency statistics for a problem.
     */
    @Update("""
            UPDATE problem_frequency_stats 
            SET total_frequency_score = #{frequencyScore},
                interview_count = #{interviewCount},
                last_asked_date = #{lastAskedDate},
                frequency_trend = #{trend},
                calculation_date = #{calculationDate}
            WHERE problem_id = #{problemId} 
            AND stats_scope = #{scope}
            AND (company_id = #{companyId} OR (company_id IS NULL AND #{companyId} IS NULL))
            AND (department_id = #{departmentId} OR (department_id IS NULL AND #{departmentId} IS NULL))
            AND (position_id = #{positionId} OR (position_id IS NULL AND #{positionId} IS NULL))
            """)
    int updateFrequencyStats(
            @Param("problemId") Long problemId,
            @Param("scope") String scope,
            @Param("companyId") Long companyId,
            @Param("departmentId") Long departmentId,
            @Param("positionId") Long positionId,
            @Param("frequencyScore") BigDecimal frequencyScore,
            @Param("interviewCount") Integer interviewCount,
            @Param("lastAskedDate") LocalDate lastAskedDate,
            @Param("trend") String trend,
            @Param("calculationDate") LocalDate calculationDate);

    // Helper classes for complex query results

    /**
     * Problem frequency stats with problem details.
     */
    class ProblemFrequencyWithDetails extends ProblemFrequencyStats {
        private String problemTitle;
        private String problemDifficulty;
        
        public String getProblemTitle() { return problemTitle; }
        public void setProblemTitle(String problemTitle) { this.problemTitle = problemTitle; }
        public String getProblemDifficulty() { return problemDifficulty; }
        public void setProblemDifficulty(String problemDifficulty) { this.problemDifficulty = problemDifficulty; }
    }

    /**
     * Company problem breakdown with department and position details.
     */
    class CompanyProblemBreakdown extends ProblemFrequencyWithDetails {
        private String departmentName;
        private String positionName;
        
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public String getPositionName() { return positionName; }
        public void setPositionName(String positionName) { this.positionName = positionName; }
    }

    /**
     * Problem stats breakdown across all scopes.
     */
    class ProblemStatsBreakdown extends ProblemFrequencyStats {
        private String companyName;
        private String departmentName;
        private String positionName;
        
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public String getPositionName() { return positionName; }
        public void setPositionName(String positionName) { this.positionName = positionName; }
    }
}