package com.codetop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.entity.ReviewLog;
import com.codetop.enums.FSRSState;
import com.codetop.enums.ReviewType;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper interface for ReviewLog entity operations using MyBatis-Plus.
 * 
 * Provides optimized queries for:
 * - Review history tracking
 * - FSRS algorithm optimization
 * - Performance analytics
 * - Learning progress analysis
 * 
 * @author CodeTop Team
 */
@Mapper
@Repository
public interface ReviewLogMapper extends BaseMapper<ReviewLog> {

    /**
     * Find review logs by user ID for parameter optimization.
     */
    @Select("""
            SELECT * FROM review_logs 
            WHERE user_id = #{userId} 
            AND old_stability IS NOT NULL 
            AND new_stability IS NOT NULL
            AND elapsed_days IS NOT NULL
            ORDER BY reviewed_at DESC 
            LIMIT #{limit}
            """)
    List<ReviewLog> findByUserIdForOptimization(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * Find recent review logs by user ID.
     */
    @Select("SELECT * FROM review_logs WHERE user_id = #{userId} ORDER BY reviewed_at DESC LIMIT #{limit}")
    List<ReviewLog> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * Find recent review logs by user ID within a specific time window.
     * Optimized version that pushes date filtering to database level.
     */
    @Select("SELECT * FROM review_logs WHERE user_id = #{userId} AND reviewed_at >= #{windowStart} ORDER BY reviewed_at DESC LIMIT #{limit}")
    List<ReviewLog> findRecentByUserIdInWindow(@Param("userId") Long userId, 
                                              @Param("windowStart") LocalDateTime windowStart,
                                              @Param("limit") int limit);

    /**
     * Find review logs by problem ID.
     */
    @Select("SELECT * FROM review_logs WHERE problem_id = #{problemId} ORDER BY reviewed_at DESC")
    Page<ReviewLog> findByProblemId(Page<ReviewLog> page, @Param("problemId") Long problemId);

    /**
     * Find review logs by card ID.
     */
    @Select("SELECT * FROM review_logs WHERE card_id = #{cardId} ORDER BY reviewed_at ASC")
    List<ReviewLog> findByCardId(@Param("cardId") Long cardId);

    /**
     * Get user review statistics for a date range.
     */
    @Select("""
            SELECT 
                COUNT(*) as total_reviews,
                COUNT(CASE WHEN rating = 1 THEN 1 END) as again_count,
                COUNT(CASE WHEN rating = 2 THEN 1 END) as hard_count,
                COUNT(CASE WHEN rating = 3 THEN 1 END) as good_count,
                COUNT(CASE WHEN rating = 4 THEN 1 END) as easy_count,
                COUNT(CASE WHEN rating >= 3 THEN 1 END) as success_count,
                AVG(CASE WHEN response_time_ms IS NOT NULL THEN response_time_ms END) as avg_response_time,
                COUNT(DISTINCT problem_id) as unique_problems,
                COUNT(DISTINCT DATE(reviewed_at)) as active_days
            FROM review_logs 
            WHERE user_id = #{userId} 
            AND reviewed_at >= #{startDate} 
            AND reviewed_at <= #{endDate}
            """)
    @Results({
        @Result(column = "total_reviews", property = "totalReviews"),
        @Result(column = "again_count", property = "againCount"),
        @Result(column = "hard_count", property = "hardCount"),
        @Result(column = "good_count", property = "goodCount"),
        @Result(column = "easy_count", property = "easyCount"),
        @Result(column = "success_count", property = "successCount"),
        @Result(column = "avg_response_time", property = "avgResponseTime"),
        @Result(column = "unique_problems", property = "uniqueProblems"),
        @Result(column = "active_days", property = "activeDays")
    })
    UserReviewStats getUserReviewStats(@Param("userId") Long userId, 
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Get daily review activity for user.
     */
    @Select("""
            SELECT 
                DATE(reviewed_at) as review_date,
                COUNT(*) as review_count,
                COUNT(CASE WHEN rating >= 3 THEN 1 END) as success_count,
                AVG(rating) as avg_rating,
                COUNT(DISTINCT problem_id) as unique_problems
            FROM review_logs 
            WHERE user_id = #{userId} 
            AND reviewed_at >= #{startDate}
            GROUP BY DATE(reviewed_at)
            ORDER BY review_date ASC
            """)
    @Results({
        @Result(column = "review_date", property = "reviewDate"),
        @Result(column = "review_count", property = "reviewCount"),
        @Result(column = "success_count", property = "successCount"),
        @Result(column = "avg_rating", property = "avgRating"),
        @Result(column = "unique_problems", property = "uniqueProblems")
    })
    List<DailyReviewActivity> getDailyReviewActivity(@Param("userId") Long userId, 
                                                    @Param("startDate") LocalDateTime startDate);

    /**
     * Get problem difficulty performance.
     */
    @Select("""
            SELECT 
                p.difficulty,
                COUNT(rl.*) as total_reviews,
                COUNT(CASE WHEN rl.rating >= 3 THEN 1 END) as success_count,
                AVG(rl.rating) as avg_rating,
                AVG(CASE WHEN rl.response_time_ms IS NOT NULL THEN rl.response_time_ms END) as avg_response_time
            FROM review_logs rl
            INNER JOIN problems p ON rl.problem_id = p.id
            WHERE rl.user_id = #{userId}
            AND rl.reviewed_at >= #{startDate}
            GROUP BY p.difficulty
            ORDER BY 
                CASE p.difficulty 
                    WHEN 'EASY' THEN 1 
                    WHEN 'MEDIUM' THEN 2 
                    WHEN 'HARD' THEN 3 
                END
            """)
    @Results({
        @Result(column = "difficulty", property = "difficulty"),
        @Result(column = "total_reviews", property = "totalReviews"),
        @Result(column = "success_count", property = "successCount"),
        @Result(column = "avg_rating", property = "avgRating"),
        @Result(column = "avg_response_time", property = "avgResponseTime")
    })
    List<DifficultyPerformance> getDifficultyPerformance(@Param("userId") Long userId, 
                                                        @Param("startDate") LocalDateTime startDate);

    /**
     * Get learning progress over time.
     */
    @Select("""
            SELECT 
                DATE_FORMAT(reviewed_at, '%Y-%m') as month,
                COUNT(*) as total_reviews,
                COUNT(CASE WHEN rating >= 3 THEN 1 END) as success_count,
                AVG(rating) as avg_rating,
                COUNT(DISTINCT problem_id) as unique_problems,
                COUNT(CASE WHEN old_state != new_state THEN 1 END) as state_changes,
                COUNT(CASE WHEN new_state = 'REVIEW' AND old_state != 'REVIEW' THEN 1 END) as graduations
            FROM review_logs 
            WHERE user_id = #{userId}
            GROUP BY DATE_FORMAT(reviewed_at, '%Y-%m')
            ORDER BY month ASC
            """)
    @Results({
        @Result(column = "month", property = "month"),
        @Result(column = "total_reviews", property = "totalReviews"),
        @Result(column = "success_count", property = "successCount"),
        @Result(column = "avg_rating", property = "avgRating"),
        @Result(column = "unique_problems", property = "uniqueProblems"),
        @Result(column = "state_changes", property = "stateChanges"),
        @Result(column = "graduations", property = "graduations")
    })
    List<MonthlyProgress> getMonthlyProgress(@Param("userId") Long userId);

    /**
     * Get system-wide review statistics.
     */
    @Select("""
            SELECT 
                COUNT(*) as total_reviews,
                COUNT(DISTINCT user_id) as active_users,
                COUNT(DISTINCT problem_id) as problems_reviewed,
                AVG(rating) as avg_rating,
                COUNT(CASE WHEN rating >= 3 THEN 1 END) as success_count,
                AVG(CASE WHEN response_time_ms IS NOT NULL THEN response_time_ms END) as avg_response_time
            FROM review_logs 
            WHERE reviewed_at >= #{startDate}
            """)
    @Results({
        @Result(column = "total_reviews", property = "totalReviews"),
        @Result(column = "active_users", property = "activeUsers"),
        @Result(column = "problems_reviewed", property = "problemsReviewed"),
        @Result(column = "avg_rating", property = "avgRating"),
        @Result(column = "success_count", property = "successCount"),
        @Result(column = "avg_response_time", property = "avgResponseTime")
    })
    SystemReviewStats getSystemReviewStats(@Param("startDate") LocalDateTime startDate);

    /**
     * Find reviews with specific rating.
     */
    @Select("SELECT * FROM review_logs WHERE user_id = #{userId} AND rating = #{rating} ORDER BY reviewed_at DESC LIMIT #{limit}")
    List<ReviewLog> findByUserIdAndRating(@Param("userId") Long userId, @Param("rating") Integer rating, @Param("limit") int limit);

    /**
     * Find reviews by review type.
     */
    @Select("SELECT * FROM review_logs WHERE user_id = #{userId} AND review_type = #{reviewType} ORDER BY reviewed_at DESC")
    Page<ReviewLog> findByUserIdAndReviewType(Page<ReviewLog> page, @Param("userId") Long userId, @Param("reviewType") String reviewType);

    /**
     * Count reviews by user in date range.
     */
    @Select("SELECT COUNT(*) FROM review_logs WHERE user_id = #{userId} AND reviewed_at >= #{startDate} AND reviewed_at <= #{endDate}")
    Long countByUserIdInDateRange(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Helper classes for complex query results

    class UserReviewStats {
        private Long totalReviews;
        private Long againCount;
        private Long hardCount;
        private Long goodCount;
        private Long easyCount;
        private Long successCount;
        private Double avgResponseTime;
        private Long uniqueProblems;
        private Long activeDays;

        // Getters and setters
        public Long getTotalReviews() { return totalReviews; }
        public void setTotalReviews(Long totalReviews) { this.totalReviews = totalReviews; }
        public Long getAgainCount() { return againCount; }
        public void setAgainCount(Long againCount) { this.againCount = againCount; }
        public Long getHardCount() { return hardCount; }
        public void setHardCount(Long hardCount) { this.hardCount = hardCount; }
        public Long getGoodCount() { return goodCount; }
        public void setGoodCount(Long goodCount) { this.goodCount = goodCount; }
        public Long getEasyCount() { return easyCount; }
        public void setEasyCount(Long easyCount) { this.easyCount = easyCount; }
        public Long getSuccessCount() { return successCount; }
        public void setSuccessCount(Long successCount) { this.successCount = successCount; }
        public Double getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(Double avgResponseTime) { this.avgResponseTime = avgResponseTime; }
        public Long getUniqueProblems() { return uniqueProblems; }
        public void setUniqueProblems(Long uniqueProblems) { this.uniqueProblems = uniqueProblems; }
        public Long getActiveDays() { return activeDays; }
        public void setActiveDays(Long activeDays) { this.activeDays = activeDays; }
    }

    class DailyReviewActivity {
        private String reviewDate;
        private Long reviewCount;
        private Long successCount;
        private Double avgRating;
        private Long uniqueProblems;

        // Getters and setters
        public String getReviewDate() { return reviewDate; }
        public void setReviewDate(String reviewDate) { this.reviewDate = reviewDate; }
        public Long getReviewCount() { return reviewCount; }
        public void setReviewCount(Long reviewCount) { this.reviewCount = reviewCount; }
        public Long getSuccessCount() { return successCount; }
        public void setSuccessCount(Long successCount) { this.successCount = successCount; }
        public Double getAvgRating() { return avgRating; }
        public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }
        public Long getUniqueProblems() { return uniqueProblems; }
        public void setUniqueProblems(Long uniqueProblems) { this.uniqueProblems = uniqueProblems; }
    }

    class DifficultyPerformance {
        private String difficulty;
        private Long totalReviews;
        private Long successCount;
        private Double avgRating;
        private Double avgResponseTime;

        // Getters and setters
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public Long getTotalReviews() { return totalReviews; }
        public void setTotalReviews(Long totalReviews) { this.totalReviews = totalReviews; }
        public Long getSuccessCount() { return successCount; }
        public void setSuccessCount(Long successCount) { this.successCount = successCount; }
        public Double getAvgRating() { return avgRating; }
        public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }
        public Double getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(Double avgResponseTime) { this.avgResponseTime = avgResponseTime; }
    }

    class MonthlyProgress {
        private String month;
        private Long totalReviews;
        private Long successCount;
        private Double avgRating;
        private Long uniqueProblems;
        private Long stateChanges;
        private Long graduations;

        // Getters and setters
        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public Long getTotalReviews() { return totalReviews; }
        public void setTotalReviews(Long totalReviews) { this.totalReviews = totalReviews; }
        public Long getSuccessCount() { return successCount; }
        public void setSuccessCount(Long successCount) { this.successCount = successCount; }
        public Double getAvgRating() { return avgRating; }
        public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }
        public Long getUniqueProblems() { return uniqueProblems; }
        public void setUniqueProblems(Long uniqueProblems) { this.uniqueProblems = uniqueProblems; }
        public Long getStateChanges() { return stateChanges; }
        public void setStateChanges(Long stateChanges) { this.stateChanges = stateChanges; }
        public Long getGraduations() { return graduations; }
        public void setGraduations(Long graduations) { this.graduations = graduations; }
    }

    class SystemReviewStats {
        private Long totalReviews;
        private Long activeUsers;
        private Long problemsReviewed;
        private Double avgRating;
        private Long successCount;
        private Double avgResponseTime;

        // Getters and setters
        public Long getTotalReviews() { return totalReviews; }
        public void setTotalReviews(Long totalReviews) { this.totalReviews = totalReviews; }
        public Long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(Long activeUsers) { this.activeUsers = activeUsers; }
        public Long getProblemsReviewed() { return problemsReviewed; }
        public void setProblemsReviewed(Long problemsReviewed) { this.problemsReviewed = problemsReviewed; }
        public Double getAvgRating() { return avgRating; }
        public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }
        public Long getSuccessCount() { return successCount; }
        public void setSuccessCount(Long successCount) { this.successCount = successCount; }
        public Double getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(Double avgResponseTime) { this.avgResponseTime = avgResponseTime; }
    }
}