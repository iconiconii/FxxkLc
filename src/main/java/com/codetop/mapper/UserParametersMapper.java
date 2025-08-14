package com.codetop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codetop.entity.UserParameters;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis-Plus mapper for UserParameters entity.
 * 
 * Provides optimized queries for:
 * - FSRS parameter retrieval and management
 * - Parameter optimization candidate selection
 * - Performance tracking and analytics
 * - Bulk operations for parameter updates
 * 
 * @author CodeTop Team
 */
@Mapper
public interface UserParametersMapper extends BaseMapper<UserParameters> {

    /**
     * Get active parameters for a user.
     * Returns the currently active parameter set or null if none exists.
     */
    @Select("SELECT * FROM user_parameters WHERE user_id = #{userId} AND is_active = true ORDER BY created_at DESC LIMIT 1")
    UserParameters getActiveParametersByUserId(@Param("userId") Long userId);

    /**
     * Get parameter history for a user.
     * Returns all parameter sets ordered by creation date (newest first).
     */
    @Select("SELECT * FROM user_parameters WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<UserParameters> getParameterHistory(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * Find users who are candidates for parameter optimization.
     * Users need at least minReviews and haven't been optimized recently.
     */
    @Select("SELECT DISTINCT up.* FROM user_parameters up " +
            "JOIN users u ON up.user_id = u.id " +
            "LEFT JOIN review_logs rl ON up.user_id = rl.user_id " +
            "WHERE u.is_active = true AND u.deleted = 0 " +
            "AND up.is_active = true " +
            "AND up.training_count >= #{minReviews} " +
            "AND (up.optimized_at IS NULL OR up.optimized_at < #{cutoffDate}) " +
            "GROUP BY up.user_id, up.id " +
            "HAVING COUNT(rl.id) >= #{minReviews} " +
            "ORDER BY COUNT(rl.id) DESC " +
            "LIMIT #{limit}")
    List<UserParameters> findOptimizationCandidates(
            @Param("minReviews") int minReviews,
            @Param("cutoffDate") LocalDateTime cutoffDate,
            @Param("limit") int limit
    );

    /**
     * Get users with optimized parameters for comparison analysis.
     */
    @Select("SELECT * FROM user_parameters WHERE is_optimized = true AND is_active = true " +
            "AND performance_improvement IS NOT NULL " +
            "ORDER BY performance_improvement DESC " +
            "LIMIT #{limit}")
    List<UserParameters> getOptimizedParametersForAnalysis(@Param("limit") int limit);

    /**
     * Deactivate all existing parameters for a user (before creating new active ones).
     */
    @Update("UPDATE user_parameters SET is_active = false, updated_at = NOW() WHERE user_id = #{userId} AND is_active = true")
    int deactivateUserParameters(@Param("userId") Long userId);

    /**
     * Update training count for active parameters after new reviews.
     */
    @Update("UPDATE user_parameters SET training_count = training_count + #{additionalReviews}, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND is_active = true")
    int incrementTrainingCount(@Param("userId") Long userId, @Param("additionalReviews") int additionalReviews);

    /**
     * Get statistics about parameter optimization across all users.
     */
    @Select("SELECT " +
            "COUNT(*) as total_users, " +
            "COUNT(CASE WHEN is_optimized = true THEN 1 END) as optimized_users, " +
            "AVG(CASE WHEN is_optimized = true THEN performance_improvement END) as avg_improvement, " +
            "COUNT(CASE WHEN training_count >= #{minReviews} THEN 1 END) as ready_for_optimization " +
            "FROM user_parameters WHERE is_active = true")
    OptimizationStats getOptimizationStats(@Param("minReviews") int minReviews);

    /**
     * Find parameters that need re-optimization due to new data.
     */
    @Select("SELECT up.* FROM user_parameters up " +
            "JOIN (" +
            "  SELECT user_id, COUNT(*) as recent_reviews " +
            "  FROM review_logs " +
            "  WHERE reviewed_at > #{sinceDate} " +
            "  GROUP BY user_id " +
            "  HAVING COUNT(*) >= #{minNewReviews}" +
            ") rl ON up.user_id = rl.user_id " +
            "WHERE up.is_active = true " +
            "AND up.is_optimized = true " +
            "AND (up.optimized_at IS NULL OR up.optimized_at < #{reoptimizationCutoff}) " +
            "ORDER BY rl.recent_reviews DESC " +
            "LIMIT #{limit}")
    List<UserParameters> findReoptimizationCandidates(
            @Param("sinceDate") LocalDateTime sinceDate,
            @Param("minNewReviews") int minNewReviews,
            @Param("reoptimizationCutoff") LocalDateTime reoptimizationCutoff,
            @Param("limit") int limit
    );

    /**
     * Get parameter distribution statistics for analysis.
     */
    @Select("SELECT " +
            "AVG(w0) as avg_w0, STDDEV(w0) as std_w0, " +
            "AVG(w1) as avg_w1, STDDEV(w1) as std_w1, " +
            "AVG(w2) as avg_w2, STDDEV(w2) as std_w2, " +
            "AVG(w3) as avg_w3, STDDEV(w3) as std_w3, " +
            "AVG(w4) as avg_w4, STDDEV(w4) as std_w4, " +
            "AVG(w5) as avg_w5, STDDEV(w5) as std_w5, " +
            "AVG(w6) as avg_w6, STDDEV(w6) as std_w6, " +
            "AVG(w7) as avg_w7, STDDEV(w7) as std_w7, " +
            "AVG(w8) as avg_w8, STDDEV(w8) as std_w8, " +
            "AVG(w9) as avg_w9, STDDEV(w9) as std_w9, " +
            "AVG(w10) as avg_w10, STDDEV(w10) as std_w10, " +
            "AVG(w11) as avg_w11, STDDEV(w11) as std_w11, " +
            "AVG(w12) as avg_w12, STDDEV(w12) as std_w12, " +
            "AVG(w13) as avg_w13, STDDEV(w13) as std_w13, " +
            "AVG(w14) as avg_w14, STDDEV(w14) as std_w14, " +
            "AVG(w15) as avg_w15, STDDEV(w15) as std_w15, " +
            "AVG(w16) as avg_w16, STDDEV(w16) as std_w16, " +
            "AVG(request_retention) as avg_retention, STDDEV(request_retention) as std_retention " +
            "FROM user_parameters WHERE is_optimized = true AND is_active = true")
    ParameterDistributionStats getParameterDistributionStats();

    /**
     * DTO for optimization statistics.
     */
    record OptimizationStats(
            int totalUsers,
            int optimizedUsers,
            Double avgImprovement,
            int readyForOptimization
    ) {}

    /**
     * DTO for parameter distribution statistics.
     */
    record ParameterDistributionStats(
            Double avgW0, Double stdW0, Double avgW1, Double stdW1, Double avgW2, Double stdW2,
            Double avgW3, Double stdW3, Double avgW4, Double stdW4, Double avgW5, Double stdW5,
            Double avgW6, Double stdW6, Double avgW7, Double stdW7, Double avgW8, Double stdW8,
            Double avgW9, Double stdW9, Double avgW10, Double stdW10, Double avgW11, Double stdW11,
            Double avgW12, Double stdW12, Double avgW13, Double stdW13, Double avgW14, Double stdW14,
            Double avgW15, Double stdW15, Double avgW16, Double stdW16,
            Double avgRetention, Double stdRetention
    ) {}
}