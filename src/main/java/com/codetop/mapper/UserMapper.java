package com.codetop.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.entity.User;
import com.codetop.enums.AuthProvider;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Mapper interface for User entity operations using MyBatis-Plus.
 * 
 * Provides optimized queries for:
 * - Authentication and authorization
 * - User profile management  
 * - OAuth provider integration
 * - Account security operations
 * - User analytics and reporting
 * 
 * @author CodeTop Team
 */
@Mapper
@Repository
public interface UserMapper extends BaseMapper<User> {

    // Basic user lookup queries
    
    /**
     * Find user by email.
     */
    @Select("SELECT * FROM users WHERE email = #{email}")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * Find user by username.
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    Optional<User> findByUsername(@Param("username") String username);

    /**
     * Find user by email or username.
     */
    @Select("SELECT * FROM users WHERE email = #{identifier} OR username = #{identifier}")
    Optional<User> findByEmailOrUsername(@Param("identifier") String identifier);
    
    // OAuth provider queries
    
    /**
     * Find user by provider ID and auth provider.
     */
    @Select("SELECT * FROM users WHERE oauth_id = #{providerId} AND oauth_provider = #{authProvider}")
    Optional<User> findByProviderIdAndAuthProvider(@Param("providerId") String providerId, 
                                                   @Param("authProvider") String authProvider);

    /**
     * Find users by auth provider.
     */
    @Select("SELECT * FROM users WHERE oauth_provider = #{authProvider}")
    List<User> findByAuthProvider(@Param("authProvider") String authProvider);
    
    // Active user queries
    
    /**
     * Find all active users.
     */
    @Select("SELECT * FROM users WHERE is_active = true ORDER BY created_at DESC")
    List<User> findActiveUsers();
    
    /**
     * Find active users with pagination.
     */
    @Select("SELECT * FROM users WHERE is_active = true ORDER BY created_at DESC")
    Page<User> findActiveUsersWithPagination(Page<User> page);
    
    // Email verification queries
    
    // Verification token functionality disabled - column doesn't exist in current DB schema
    // /**
    //  * Find user by verification token.
    //  */
    // @Select("SELECT * FROM users WHERE verification_token = #{token}")
    // Optional<User> findByVerificationToken(@Param("token") String token);

    /**
     * Find unverified users.
     */
    @Select("SELECT * FROM users WHERE is_email_verified = false")
    List<User> findUnverifiedUsers();
    
    // Password reset queries
    
    // Reset token functionality disabled - column doesn't exist in current DB schema
    // /**
    //  * Find user by reset token.
    //  */
    // @Select("SELECT * FROM users WHERE reset_token = #{token}")
    // Optional<User> findByResetToken(@Param("token") String token);

    // Reset token functionality disabled - columns don't exist in current DB schema
    // /**
    //  * Find users with valid reset tokens.
    //  */
    // @Select("SELECT * FROM users WHERE reset_token_expires > #{now}")
    // List<User> findUsersWithValidResetToken(@Param("now") LocalDateTime now);
    
    // Security queries
    
    // Account locking functionality disabled - column doesn't exist in current DB schema
    // /**
    //  * Find locked users.
    //  */
    // @Select("SELECT * FROM users WHERE locked_until > #{now}")
    // List<User> findLockedUsers(@Param("now") LocalDateTime now);

    // Login attempts tracking disabled - column doesn't exist in current DB schema
    // /**
    //  * Find users with high login attempts.
    //  */
    // @Select("SELECT * FROM users WHERE login_attempts > #{attempts}")
    // List<User> findUsersWithHighLoginAttempts(@Param("attempts") int attempts);
    
    // Analytics queries
    
    /**
     * Count users created after a specific date.
     */
    @Select("SELECT COUNT(*) FROM users WHERE created_at >= #{startDate}")
    Long countUsersCreatedAfter(@Param("startDate") LocalDateTime startDate);

    /**
     * Count active users after a specific date.
     */
    @Select("SELECT COUNT(*) FROM users WHERE last_login_at >= #{startDate}")
    Long countActiveUsersAfter(@Param("startDate") LocalDateTime startDate);

    /**
     * Find recently active users.
     */
    @Select("SELECT * FROM users WHERE last_login_at >= #{startDate} ORDER BY last_login_at DESC LIMIT #{limit}")
    List<User> findRecentlyActiveUsers(@Param("startDate") LocalDateTime startDate, @Param("limit") int limit);
    
    // Bulk operations
    
    /**
     * Update last login time.
     */
    @Update("UPDATE users SET last_login_at = #{loginTime}, updated_at = #{loginTime} WHERE id = #{userId}")
    int updateLastLogin(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime);

    // Login attempts functionality disabled - columns don't exist in current DB schema
    // /**
    //  * Reset login attempts.
    //  */
    // @Update("UPDATE users SET login_attempts = 0, locked_until = NULL, updated_at = #{now} WHERE id = #{userId}")
    // int resetLoginAttempts(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // Login attempts functionality disabled - column doesn't exist in current DB schema
    // /**
    //  * Increment login attempts.
    //  */
    // @Update("UPDATE users SET login_attempts = login_attempts + 1, updated_at = #{now} WHERE id = #{userId}")
    // int incrementLoginAttempts(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // Account locking functionality disabled - column doesn't exist in current DB schema
    // /**
    //  * Lock user account.
    //  */
    // @Update("UPDATE users SET locked_until = #{lockTime}, updated_at = #{lockTime} WHERE id = #{userId}")
    // int lockUser(@Param("userId") Long userId, @Param("lockTime") LocalDateTime lockTime);

    /**
     * Verify email.
     */
    @Update("UPDATE users SET is_email_verified = true, updated_at = #{now} WHERE id = #{userId}")
    int verifyEmail(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    // Search queries
    
    /**
     * Search active users by keyword.
     */
    @Select("""
            SELECT * FROM users 
            WHERE (LOWER(username) LIKE LOWER(CONCAT('%', #{search}, '%')) OR 
                   LOWER(email) LIKE LOWER(CONCAT('%', #{search}, '%')) OR 
                   LOWER(first_name) LIKE LOWER(CONCAT('%', #{search}, '%')) OR 
                   LOWER(last_name) LIKE LOWER(CONCAT('%', #{search}, '%'))) 
            AND is_active = true 
            ORDER BY username
            """)
    Page<User> searchActiveUsers(Page<User> page, @Param("search") String search);
    
    // Statistics queries
    
    /**
     * Count active users.
     */
    @Select("SELECT COUNT(*) FROM users WHERE is_active = true")
    Long countActiveUsers();

    /**
     * Count verified users.
     */
    @Select("SELECT COUNT(*) FROM users WHERE is_email_verified = true")
    Long countVerifiedUsers();

    /**
     * Count users by auth provider.
     */
    @Select("SELECT oauth_provider, COUNT(*) as count FROM users WHERE is_active = true GROUP BY oauth_provider")
    @Results({
        @Result(column = "oauth_provider", property = "authProvider"),
        @Result(column = "count", property = "count")
    })
    List<AuthProviderCount> countUsersByAuthProvider();
    
    // Existence checks
    
    /**
     * Check if email exists.
     */
    @Select("SELECT COUNT(*) FROM users WHERE email = #{email}")
    int countByEmail(@Param("email") String email);

    /**
     * Check if username exists.
     */
    @Select("SELECT COUNT(*) FROM users WHERE username = #{username}")
    int countByUsername(@Param("username") String username);

    /**
     * Check if provider ID and auth provider combination exists.
     */
    @Select("SELECT COUNT(*) FROM users WHERE oauth_id = #{providerId} AND oauth_provider = #{authProvider}")
    int countByProviderIdAndAuthProvider(@Param("providerId") String providerId, 
                                        @Param("authProvider") String authProvider);
    
    // Cleanup queries
    
    /**
     * Find unverified users older than cutoff date.
     */
    @Select("SELECT * FROM users WHERE is_email_verified = false AND created_at < #{cutoffDate}")
    List<User> findUnverifiedUsersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Reset token functionality disabled - columns don't exist in current DB schema
    // /**
    //  * Find users with expired reset tokens.
    //  */
    // @Select("SELECT * FROM users WHERE reset_token IS NOT NULL AND reset_token_expires < #{now}")
    // List<User> findUsersWithExpiredResetTokens(@Param("now") LocalDateTime now);
    
    // Leaderboard queries
    
    /**
     * Get global leaderboard.
     */
    @Select("""
            SELECT u.id as userId, u.username, u.avatar_url as avatarUrl,
                   COALESCE(COUNT(rl.id), 0) as totalReviews,
                   COALESCE(SUM(CASE WHEN rl.rating >= 3 THEN 1 ELSE 0 END), 0) as correctReviews,
                   CASE WHEN COUNT(rl.id) > 0 THEN 
                       ROUND(SUM(CASE WHEN rl.rating >= 3 THEN 1 ELSE 0 END) * 100.0 / COUNT(rl.id), 1) 
                   ELSE 0 END as accuracy,
                   0 as streak,
                   ROW_NUMBER() OVER (ORDER BY COUNT(rl.id) DESC) as `rank`
            FROM users u
            LEFT JOIN fsrs_cards fc ON u.id = fc.user_id
            LEFT JOIN review_logs rl ON fc.id = rl.card_id
            WHERE u.is_active = true
            GROUP BY u.id, u.username, u.avatar_url
            ORDER BY COUNT(rl.id) DESC
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "userId", property = "userId"),
        @Result(column = "username", property = "username"),
        @Result(column = "avatarUrl", property = "avatarUrl"),
        @Result(column = "totalReviews", property = "totalReviews"),
        @Result(column = "correctReviews", property = "correctReviews"),
        @Result(column = "accuracy", property = "accuracy"),
        @Result(column = "streak", property = "streak"),
        @Result(column = "rank", property = "rank")
    })
    List<com.codetop.controller.LeaderboardController.LeaderboardEntry> getGlobalLeaderboard(@Param("limit") int limit);

    /**
     * Get weekly leaderboard.
     */
    @Select("""
            SELECT u.id as userId, u.username, u.avatar_url as avatarUrl,
                   COALESCE(COUNT(rl.id), 0) as totalReviews,
                   COALESCE(SUM(CASE WHEN rl.rating >= 3 THEN 1 ELSE 0 END), 0) as correctReviews,
                   CASE WHEN COUNT(rl.id) > 0 THEN 
                       ROUND(SUM(CASE WHEN rl.rating >= 3 THEN 1 ELSE 0 END) * 100.0 / COUNT(rl.id), 1) 
                   ELSE 0 END as accuracy,
                   0 as streak,
                   ROW_NUMBER() OVER (ORDER BY COUNT(rl.id) DESC) as `rank`
            FROM users u
            LEFT JOIN fsrs_cards fc ON u.id = fc.user_id
            LEFT JOIN review_logs rl ON fc.id = rl.card_id AND rl.reviewed_at >= #{startDate}
            WHERE u.is_active = true
            GROUP BY u.id, u.username, u.avatar_url
            HAVING COUNT(rl.id) > 0
            ORDER BY totalReviews DESC
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "userId", property = "userId"),
        @Result(column = "username", property = "username"),
        @Result(column = "avatarUrl", property = "avatarUrl"),
        @Result(column = "totalReviews", property = "totalReviews"),
        @Result(column = "correctReviews", property = "correctReviews"),
        @Result(column = "accuracy", property = "accuracy"),
        @Result(column = "streak", property = "streak"),
        @Result(column = "rank", property = "rank")
    })
    List<com.codetop.controller.LeaderboardController.LeaderboardEntry> getWeeklyLeaderboard(
            @Param("startDate") LocalDateTime startDate, @Param("limit") int limit);

    /**
     * Get monthly leaderboard.
     */
    @Select("""
            SELECT u.id as userId, u.username, u.avatar_url as avatarUrl,
                   COALESCE(COUNT(rl.id), 0) as totalReviews,
                   COALESCE(SUM(CASE WHEN rl.rating >= 3 THEN 1 ELSE 0 END), 0) as correctReviews,
                   CASE WHEN COUNT(rl.id) > 0 THEN 
                       ROUND(SUM(CASE WHEN rl.rating >= 3 THEN 1 ELSE 0 END) * 100.0 / COUNT(rl.id), 1) 
                   ELSE 0 END as accuracy,
                   0 as streak,
                   ROW_NUMBER() OVER (ORDER BY COUNT(rl.id) DESC) as `rank`
            FROM users u
            LEFT JOIN fsrs_cards fc ON u.id = fc.user_id
            LEFT JOIN review_logs rl ON fc.id = rl.card_id AND rl.reviewed_at >= #{startDate}
            WHERE u.is_active = true
            GROUP BY u.id, u.username, u.avatar_url
            HAVING COUNT(rl.id) > 0
            ORDER BY totalReviews DESC
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "userId", property = "userId"),
        @Result(column = "username", property = "username"),
        @Result(column = "avatarUrl", property = "avatarUrl"),
        @Result(column = "totalReviews", property = "totalReviews"),
        @Result(column = "correctReviews", property = "correctReviews"),
        @Result(column = "accuracy", property = "accuracy"),
        @Result(column = "streak", property = "streak"),
        @Result(column = "rank", property = "rank")
    })
    List<com.codetop.controller.LeaderboardController.LeaderboardEntry> getMonthlyLeaderboard(
            @Param("startDate") LocalDateTime startDate, @Param("limit") int limit);

    /**
     * Get accuracy leaderboard.
     */
    @Select("""
            SELECT u.id as userId, u.username, u.avatar_url as avatarUrl,
                   COALESCE(COUNT(rl.id), 0) as totalReviews,
                   COALESCE(SUM(CASE WHEN rl.rating >= 3 THEN 1 ELSE 0 END), 0) as correctReviews,
                   CASE WHEN COUNT(rl.id) > 0 THEN 
                       ROUND(SUM(CASE WHEN rl.rating >= 3 THEN 1 ELSE 0 END) * 100.0 / COUNT(rl.id), 1) 
                   ELSE 0 END as accuracy,
                   ROW_NUMBER() OVER (ORDER BY 
                       CASE WHEN COUNT(rl.id) > 0 THEN 
                           SUM(CASE WHEN rl.rating >= 3 THEN 1 ELSE 0 END) * 100.0 / COUNT(rl.id)
                       ELSE 0 END DESC) as rank
            FROM users u
            LEFT JOIN fsrs_cards fc ON u.id = fc.user_id
            LEFT JOIN review_logs rl ON fc.id = rl.card_id AND rl.reviewed_at >= #{startDate}
            WHERE u.is_active = true
            GROUP BY u.id, u.username, u.avatar_url
            HAVING COUNT(rl.id) >= 10
            ORDER BY accuracy DESC
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "userId", property = "userId"),
        @Result(column = "username", property = "username"),
        @Result(column = "avatarUrl", property = "avatarUrl"),
        @Result(column = "totalReviews", property = "totalReviews"),
        @Result(column = "correctReviews", property = "correctReviews"),
        @Result(column = "accuracy", property = "accuracy"),
        @Result(column = "rank", property = "rank")
    })
    List<com.codetop.controller.LeaderboardController.AccuracyLeaderboardEntry> getAccuracyLeaderboard(
            @Param("startDate") LocalDateTime startDate, @Param("limit") int limit);

    /**
     * Get streak leaderboard.
     */
    @Select("""
            SELECT u.id as userId, u.username, u.avatar_url as avatarUrl,
                   1 as currentStreak, 1 as longestStreak, 1 as totalActiveDays,
                   ROW_NUMBER() OVER (ORDER BY u.created_at DESC) as rank
            FROM users u
            WHERE u.is_active = true
            ORDER BY u.created_at DESC
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "userId", property = "userId"),
        @Result(column = "username", property = "username"),
        @Result(column = "avatarUrl", property = "avatarUrl"),
        @Result(column = "currentStreak", property = "currentStreak"),
        @Result(column = "longestStreak", property = "longestStreak"),
        @Result(column = "totalActiveDays", property = "totalActiveDays"),
        @Result(column = "rank", property = "rank")
    })
    List<com.codetop.controller.LeaderboardController.StreakLeaderboardEntry> getStreakLeaderboard(@Param("limit") int limit);

    // User rank queries
    
    /**
     * Get user's global rank.
     */
    @Select("""
            SELECT COALESCE(rank_info.user_rank, 0) as rank
            FROM (
                SELECT u.id, 
                       ROW_NUMBER() OVER (ORDER BY COALESCE(COUNT(rl.id), 0) DESC) as user_rank
                FROM users u
                LEFT JOIN fsrs_cards fc ON u.id = fc.user_id
                LEFT JOIN review_logs rl ON fc.id = rl.card_id
                WHERE u.is_active = true
                GROUP BY u.id
            ) rank_info
            WHERE rank_info.id = #{userId}
            """)
    Long getUserGlobalRank(@Param("userId") Long userId);

    /**
     * Get user's weekly rank.
     */
    @Select("""
            SELECT COALESCE(rank_info.user_rank, 0) as rank
            FROM (
                SELECT u.id, 
                       ROW_NUMBER() OVER (ORDER BY COALESCE(COUNT(rl.id), 0) DESC) as user_rank
                FROM users u
                LEFT JOIN fsrs_cards fc ON u.id = fc.user_id
                LEFT JOIN review_logs rl ON fc.id = rl.card_id AND rl.reviewed_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                WHERE u.is_active = true
                GROUP BY u.id
            ) rank_info
            WHERE rank_info.id = #{userId}
            """)
    Long getUserWeeklyRank(@Param("userId") Long userId);

    /**
     * Get user's monthly rank.
     */
    @Select("""
            SELECT COALESCE(rank_info.user_rank, 0) as rank
            FROM (
                SELECT u.id, 
                       ROW_NUMBER() OVER (ORDER BY COALESCE(COUNT(rl.id), 0) DESC) as user_rank
                FROM users u
                LEFT JOIN fsrs_cards fc ON u.id = fc.user_id
                LEFT JOIN review_logs rl ON fc.id = rl.card_id AND rl.reviewed_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                WHERE u.is_active = true
                GROUP BY u.id
            ) rank_info
            WHERE rank_info.id = #{userId}
            """)
    Long getUserMonthlyRank(@Param("userId") Long userId);

    /**
     * Get user's accuracy rank.
     */
    @Select("""
            SELECT COALESCE(rank_info.user_rank, 0) as rank
            FROM (
                SELECT u.id,
                       ROW_NUMBER() OVER (ORDER BY 
                           CASE WHEN COUNT(rl.id) > 0 THEN 
                               SUM(CASE WHEN rl.rating >= 3 THEN 1 ELSE 0 END) * 100.0 / COUNT(rl.id)
                           ELSE 0 END DESC) as user_rank
                FROM users u
                LEFT JOIN fsrs_cards fc ON u.id = fc.user_id
                LEFT JOIN review_logs rl ON fc.id = rl.card_id
                WHERE u.is_active = true
                GROUP BY u.id
                HAVING COUNT(rl.id) >= 10
            ) rank_info
            WHERE rank_info.id = #{userId}
            """)
    Long getUserAccuracyRank(@Param("userId") Long userId);

    /**
     * Get user's streak rank.
     */
    @Select("""
            SELECT COALESCE(rank_info.user_rank, 0) as rank
            FROM (
                SELECT u.id, 
                       ROW_NUMBER() OVER (ORDER BY u.created_at DESC) as user_rank
                FROM users u
                WHERE u.is_active = true
            ) rank_info
            WHERE rank_info.id = #{userId}
            """)
    Long getUserStreakRank(@Param("userId") Long userId);

    /**
     * Get user statistics.
     */
    @Select("""
            SELECT 
                COALESCE(COUNT(DISTINCT p.id), 0) as totalProblemsAttempted,
                COALESCE(COUNT(DISTINCT CASE WHEN rl.rating >= 3 THEN p.id END), 0) as totalProblemsSolved,
                COALESCE(COUNT(DISTINCT CASE WHEN p.difficulty = 'EASY' AND rl.rating >= 3 THEN p.id END), 0) as easyProblemsSolved,
                COALESCE(COUNT(DISTINCT CASE WHEN p.difficulty = 'MEDIUM' AND rl.rating >= 3 THEN p.id END), 0) as mediumProblemsSolved,
                COALESCE(COUNT(DISTINCT CASE WHEN p.difficulty = 'HARD' AND rl.rating >= 3 THEN p.id END), 0) as hardProblemsSolved,
                CASE WHEN COUNT(rl.id) > 0 THEN 
                    ROUND(SUM(CASE WHEN rl.rating >= 3 THEN 1 ELSE 0 END) * 100.0 / COUNT(rl.id), 1) 
                ELSE 0 END as overallAccuracy,
                1 as currentStreak,
                1 as longestStreak,
                COALESCE(SUM(rl.response_time_ms), 0) as totalReviewTime,
                COALESCE(COUNT(rl.id), 0) as totalReviews,
                COALESCE(AVG(rl.rating), 0) as averageRating
            FROM users u
            LEFT JOIN fsrs_cards fc ON u.id = fc.user_id
            LEFT JOIN problems p ON fc.problem_id = p.id
            LEFT JOIN review_logs rl ON fc.id = rl.card_id
            WHERE u.id = #{userId}
            GROUP BY u.id
            """)
    com.codetop.controller.UserController.UserStatistics getUserStatistics(@Param("userId") Long userId);

    /**
     * Helper class for auth provider count results.
     */
    class AuthProviderCount {
        private AuthProvider authProvider;
        private Long count;
        
        // Getters and setters
        public AuthProvider getAuthProvider() { return authProvider; }
        public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }
}