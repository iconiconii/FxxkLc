package com.codetop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.entity.ProblemCategory;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper interface for ProblemCategory association operations.
 * 
 * Provides optimized queries for:
 * - Problem-category relationship management
 * - Category-based problem filtering
 * - Primary category designation
 * 
 * @author CodeTop Team
 */
@Mapper
@Repository
public interface ProblemCategoryMapper extends BaseMapper<ProblemCategory> {

    /**
     * Find categories for a specific problem.
     */
    @Select("""
            SELECT pc.*, c.name as category_name, c.display_name as category_display_name, c.color_code
            FROM problem_categories pc
            INNER JOIN categories c ON pc.category_id = c.id
            WHERE pc.problem_id = #{problemId}
            ORDER BY pc.is_primary DESC, pc.relevance_score DESC
            """)
    @Results({
        @Result(column = "category_name", property = "categoryName"),
        @Result(column = "category_display_name", property = "categoryDisplayName"),
        @Result(column = "color_code", property = "colorCode")
    })
    List<ProblemCategoryWithDetails> findCategoriesByProblemId(@Param("problemId") Long problemId);

    /**
     * Find problems for a specific category.
     */
    @Select("""
            SELECT pc.*, p.title, p.difficulty
            FROM problem_categories pc
            INNER JOIN problems p ON pc.problem_id = p.id
            WHERE pc.category_id = #{categoryId} AND p.deleted = 0
            ORDER BY pc.is_primary DESC, pc.relevance_score DESC, p.created_at DESC
            """)
    Page<ProblemCategoryWithProblem> findProblemsByCategoryId(Page<ProblemCategory> page, @Param("categoryId") Long categoryId);

    /**
     * Find primary category for a problem.
     */
    @Select("""
            SELECT pc.*, c.name as category_name, c.display_name as category_display_name
            FROM problem_categories pc
            INNER JOIN categories c ON pc.category_id = c.id
            WHERE pc.problem_id = #{problemId} AND pc.is_primary = true
            """)
    @Results({
        @Result(column = "category_name", property = "categoryName"),
        @Result(column = "category_display_name", property = "categoryDisplayName")
    })
    ProblemCategoryWithDetails findPrimaryCategoryByProblemId(@Param("problemId") Long problemId);

    /**
     * Find problems by multiple categories (intersection).
     */
    @Select("""
            <script>
            SELECT DISTINCT p.*, COUNT(pc.category_id) as matched_categories
            FROM problems p
            INNER JOIN problem_categories pc ON p.id = pc.problem_id
            WHERE pc.category_id IN
            <foreach collection="categoryIds" item="categoryId" open="(" separator="," close=")">
                #{categoryId}
            </foreach>
            AND p.deleted = 0
            GROUP BY p.id
            HAVING matched_categories = #{categoryCount}
            ORDER BY matched_categories DESC, p.created_at DESC
            </script>
            """)
    Page<ProblemWithCategoryCount> findProblemsByMultipleCategories(
            Page<ProblemCategory> page,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("categoryCount") Integer categoryCount);

    /**
     * Get category usage statistics.
     */
    @Select("""
            SELECT 
                c.id,
                c.name,
                c.display_name,
                COUNT(pc.problem_id) as problem_count,
                COUNT(CASE WHEN pc.is_primary = true THEN 1 END) as primary_count,
                AVG(pc.relevance_score) as avg_relevance_score
            FROM categories c
            LEFT JOIN problem_categories pc ON c.id = pc.category_id
            WHERE c.deleted = 0
            GROUP BY c.id, c.name, c.display_name
            ORDER BY problem_count DESC, primary_count DESC
            """)
    @Results({
        @Result(column = "id", property = "categoryId"),
        @Result(column = "name", property = "categoryName"),
        @Result(column = "display_name", property = "categoryDisplayName"),
        @Result(column = "problem_count", property = "problemCount"),
        @Result(column = "primary_count", property = "primaryCount"),
        @Result(column = "avg_relevance_score", property = "avgRelevanceScore")
    })
    List<CategoryUsageStats> getCategoryUsageStatistics();

    /**
     * Update primary category for a problem.
     */
    @Update("""
            UPDATE problem_categories 
            SET is_primary = CASE 
                WHEN category_id = #{newPrimaryCategoryId} THEN true 
                ELSE false 
            END
            WHERE problem_id = #{problemId}
            """)
    int updatePrimaryCategory(@Param("problemId") Long problemId, @Param("newPrimaryCategoryId") Long newPrimaryCategoryId);

    /**
     * Find similar problems based on shared categories.
     */
    @Select("""
            SELECT 
                p2.*,
                COUNT(pc2.category_id) as shared_categories,
                GROUP_CONCAT(c.name) as shared_category_names
            FROM problems p1
            INNER JOIN problem_categories pc1 ON p1.id = pc1.problem_id
            INNER JOIN problem_categories pc2 ON pc1.category_id = pc2.category_id
            INNER JOIN problems p2 ON pc2.problem_id = p2.id
            INNER JOIN categories c ON pc1.category_id = c.id
            WHERE p1.id = #{problemId} 
            AND p2.id != #{problemId}
            AND p2.deleted = 0
            GROUP BY p2.id
            HAVING shared_categories >= #{minSharedCategories}
            ORDER BY shared_categories DESC
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "shared_categories", property = "sharedCategories"),
        @Result(column = "shared_category_names", property = "sharedCategoryNames")
    })
    List<SimilarProblem> findSimilarProblems(
            @Param("problemId") Long problemId,
            @Param("minSharedCategories") Integer minSharedCategories,
            @Param("limit") Integer limit);

    /**
     * Batch update relevance scores.
     */
    @Update("""
            <script>
            <foreach collection="updates" item="update" separator=";">
                UPDATE problem_categories 
                SET relevance_score = #{update.relevanceScore}
                WHERE problem_id = #{update.problemId} AND category_id = #{update.categoryId}
            </foreach>
            </script>
            """)
    int batchUpdateRelevanceScores(@Param("updates") List<RelevanceScoreUpdate> updates);

    // Helper classes for complex query results

    /**
     * Problem category with category details.
     */
    class ProblemCategoryWithDetails extends ProblemCategory {
        private String categoryName;
        private String categoryDisplayName;
        private String colorCode;
        
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getCategoryDisplayName() { return categoryDisplayName; }
        public void setCategoryDisplayName(String categoryDisplayName) { this.categoryDisplayName = categoryDisplayName; }
        public String getColorCode() { return colorCode; }
        public void setColorCode(String colorCode) { this.colorCode = colorCode; }
    }

    /**
     * Problem category with problem details.
     */
    class ProblemCategoryWithProblem extends ProblemCategory {
        private String problemTitle;
        private String problemDifficulty;
        
        public String getProblemTitle() { return problemTitle; }
        public void setProblemTitle(String problemTitle) { this.problemTitle = problemTitle; }
        public String getProblemDifficulty() { return problemDifficulty; }
        public void setProblemDifficulty(String problemDifficulty) { this.problemDifficulty = problemDifficulty; }
    }

    /**
     * Problem with category count.
     */
    class ProblemWithCategoryCount {
        private Long problemId;
        private String title;
        private String difficulty;
        private Integer matchedCategories;
        
        public Long getProblemId() { return problemId; }
        public void setProblemId(Long problemId) { this.problemId = problemId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public Integer getMatchedCategories() { return matchedCategories; }
        public void setMatchedCategories(Integer matchedCategories) { this.matchedCategories = matchedCategories; }
    }

    /**
     * Category usage statistics.
     */
    class CategoryUsageStats {
        private Long categoryId;
        private String categoryName;
        private String categoryDisplayName;
        private Integer problemCount;
        private Integer primaryCount;
        private Double avgRelevanceScore;
        
        // Getters and setters
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getCategoryDisplayName() { return categoryDisplayName; }
        public void setCategoryDisplayName(String categoryDisplayName) { this.categoryDisplayName = categoryDisplayName; }
        public Integer getProblemCount() { return problemCount; }
        public void setProblemCount(Integer problemCount) { this.problemCount = problemCount; }
        public Integer getPrimaryCount() { return primaryCount; }
        public void setPrimaryCount(Integer primaryCount) { this.primaryCount = primaryCount; }
        public Double getAvgRelevanceScore() { return avgRelevanceScore; }
        public void setAvgRelevanceScore(Double avgRelevanceScore) { this.avgRelevanceScore = avgRelevanceScore; }
    }

    /**
     * Similar problem result.
     */
    class SimilarProblem {
        private Long problemId;
        private String title;
        private String difficulty;
        private Integer sharedCategories;
        private String sharedCategoryNames;
        
        // Getters and setters
        public Long getProblemId() { return problemId; }
        public void setProblemId(Long problemId) { this.problemId = problemId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public Integer getSharedCategories() { return sharedCategories; }
        public void setSharedCategories(Integer sharedCategories) { this.sharedCategories = sharedCategories; }
        public String getSharedCategoryNames() { return sharedCategoryNames; }
        public void setSharedCategoryNames(String sharedCategoryNames) { this.sharedCategoryNames = sharedCategoryNames; }
    }

    /**
     * Relevance score update DTO.
     */
    class RelevanceScoreUpdate {
        private Long problemId;
        private Long categoryId;
        private Double relevanceScore;
        
        // Getters and setters
        public Long getProblemId() { return problemId; }
        public void setProblemId(Long problemId) { this.problemId = problemId; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public Double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }
    }
}