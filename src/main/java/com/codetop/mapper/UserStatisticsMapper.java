package com.codetop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codetop.entity.UserStatistics;
import com.codetop.controller.LeaderboardController;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Mapper interface for UserStatistics entity operations.
 * 
 * Provides optimized queries for:
 * - User statistics aggregation
 * - Leaderboard calculations
 * - Streak analysis
 * - Performance metrics
 * 
 * @author CodeTop Team
 */
@Mapper
@Repository
public interface UserStatisticsMapper extends BaseMapper<UserStatistics> {

    /**
     * Get streak leaderboard with real statistics data.
     */
    @Select("""
            SELECT 
                u.id as userId, 
                u.username, 
                u.avatar_url as avatarUrl,
                COALESCE(us.current_streak_days, 0) as currentStreak,
                COALESCE(us.longest_streak_days, 0) as longestStreak,
                COALESCE(DATEDIFF(COALESCE(us.last_review_date, u.created_at), u.created_at), 0) as totalActiveDays,
                ROW_NUMBER() OVER (ORDER BY 
                    COALESCE(us.current_streak_days, 0) DESC,
                    COALESCE(us.longest_streak_days, 0) DESC,
                    COALESCE(us.total_reviews, 0) DESC
                ) as `rank`
            FROM users u
            LEFT JOIN user_statistics us ON u.id = us.user_id
            WHERE u.is_active = true AND u.deleted = 0
            ORDER BY 
                COALESCE(us.current_streak_days, 0) DESC,
                COALESCE(us.longest_streak_days, 0) DESC,
                COALESCE(us.total_reviews, 0) DESC
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
    List<LeaderboardController.StreakLeaderboardEntry> getStreakLeaderboard(@Param("limit") int limit);

    /**
     * Get user's streak rank.
     */
    @Select("""
            SELECT COALESCE(rank_info.user_rank, 0) as `rank`
            FROM (
                SELECT u.id,
                       ROW_NUMBER() OVER (ORDER BY 
                           COALESCE(us.current_streak_days, 0) DESC,
                           COALESCE(us.longest_streak_days, 0) DESC,
                           COALESCE(us.total_reviews, 0) DESC
                       ) as user_rank
                FROM users u
                LEFT JOIN user_statistics us ON u.id = us.user_id
                WHERE u.is_active = true AND u.deleted = 0
            ) rank_info
            WHERE rank_info.id = #{userId}
            """)
    Long getUserStreakRank(@Param("userId") Long userId);

    /**
     * Update user statistics after review.
     */
    @Insert("""
            INSERT INTO user_statistics (
                user_id, total_problems, problems_mastered, problems_learning, 
                problems_new, problems_relearning, total_reviews, correct_reviews,
                total_study_time_ms, current_streak_days, longest_streak_days,
                last_review_date, overall_accuracy_rate, average_response_time_ms,
                retention_rate, daily_review_target, preferred_review_time
            ) VALUES (
                #{userId}, #{totalProblems}, #{problemsMastered}, #{problemsLearning},
                #{problemsNew}, #{problemsRelearning}, #{totalReviews}, #{correctReviews},
                #{totalStudyTimeMs}, #{currentStreakDays}, #{longestStreakDays},
                #{lastReviewDate}, #{overallAccuracyRate}, #{averageResponseTimeMs},
                #{retentionRate}, #{dailyReviewTarget}, #{preferredReviewTime}
            )
            ON DUPLICATE KEY UPDATE
                total_problems = VALUES(total_problems),
                problems_mastered = VALUES(problems_mastered),
                problems_learning = VALUES(problems_learning),
                problems_new = VALUES(problems_new),
                problems_relearning = VALUES(problems_relearning),
                total_reviews = VALUES(total_reviews),
                correct_reviews = VALUES(correct_reviews),
                total_study_time_ms = VALUES(total_study_time_ms),
                current_streak_days = VALUES(current_streak_days),
                longest_streak_days = VALUES(longest_streak_days),
                last_review_date = VALUES(last_review_date),
                overall_accuracy_rate = VALUES(overall_accuracy_rate),
                average_response_time_ms = VALUES(average_response_time_ms),
                retention_rate = VALUES(retention_rate),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsertUserStatistics(UserStatistics statistics);

    /**
     * Calculate and update streak for a user.
     */
    @Update("""
            UPDATE user_statistics us
            SET 
                current_streak_days = (
                    SELECT COALESCE(streak_calc.current_streak, 0)
                    FROM (
                        SELECT 
                            CASE 
                                WHEN us.last_review_date = CURDATE() - INTERVAL 1 DAY 
                                     OR us.last_review_date = CURDATE() THEN
                                    us.current_streak_days + CASE WHEN us.last_review_date < CURDATE() THEN 1 ELSE 0 END
                                WHEN us.last_review_date IS NULL 
                                     OR us.last_review_date < CURDATE() - INTERVAL 1 DAY THEN 1
                                ELSE us.current_streak_days
                            END as current_streak
                    ) streak_calc
                ),
                longest_streak_days = GREATEST(
                    COALESCE(longest_streak_days, 0),
                    CASE 
                        WHEN us.last_review_date = CURDATE() - INTERVAL 1 DAY 
                             OR us.last_review_date = CURDATE() THEN
                            us.current_streak_days + CASE WHEN us.last_review_date < CURDATE() THEN 1 ELSE 0 END
                        WHEN us.last_review_date IS NULL 
                             OR us.last_review_date < CURDATE() - INTERVAL 1 DAY THEN 1
                        ELSE us.current_streak_days
                    END
                ),
                last_review_date = CURDATE(),
                updated_at = CURRENT_TIMESTAMP
            WHERE user_id = #{userId}
            """)
    int updateUserStreak(@Param("userId") Long userId);

    /**
     * Get user statistics summary.
     */
    @Select("""
            SELECT 
                user_id,
                total_problems,
                problems_mastered,
                problems_learning,
                problems_new,
                problems_relearning,
                total_reviews,
                correct_reviews,
                total_study_time_ms,
                current_streak_days,
                longest_streak_days,
                last_review_date,
                overall_accuracy_rate,
                average_response_time_ms,
                retention_rate,
                daily_review_target,
                preferred_review_time,
                created_at,
                updated_at
            FROM user_statistics
            WHERE user_id = #{userId}
            """)
    UserStatistics getUserStatistics(@Param("userId") Long userId);

    /**
     * Get top performers by different metrics.
     */
    @Select("""
            SELECT 
                u.id as userId,
                u.username,
                u.avatar_url as avatarUrl,
                us.total_reviews,
                us.correct_reviews,
                us.overall_accuracy_rate as accuracy,
                us.current_streak_days as streak,
                ROW_NUMBER() OVER (ORDER BY #{orderBy} DESC) as rank
            FROM users u
            INNER JOIN user_statistics us ON u.id = us.user_id
            WHERE u.is_active = true AND u.deleted = 0
            ORDER BY #{orderBy} DESC
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "userId", property = "userId"),
        @Result(column = "username", property = "username"),
        @Result(column = "avatarUrl", property = "avatarUrl"),
        @Result(column = "total_reviews", property = "totalReviews"),
        @Result(column = "correct_reviews", property = "correctReviews"),
        @Result(column = "accuracy", property = "accuracy"),
        @Result(column = "streak", property = "streak"),
        @Result(column = "rank", property = "rank")
    })
    List<LeaderboardController.LeaderboardEntry> getTopPerformersByMetric(
            @Param("orderBy") String orderBy, 
            @Param("limit") int limit);

    /**
     * Get leaderboard statistics summary.
     */
    @Select("""
            SELECT 
                COUNT(DISTINCT u.id) as totalUsers,
                COUNT(DISTINCT CASE WHEN us.last_review_date >= CURDATE() - INTERVAL 7 DAY THEN u.id END) as weeklyActiveUsers,
                COUNT(DISTINCT CASE WHEN us.current_streak_days > 0 THEN u.id END) as usersWithStreak,
                AVG(COALESCE(us.overall_accuracy_rate, 0)) as averageAccuracy,
                MAX(COALESCE(us.current_streak_days, 0)) as maxCurrentStreak,
                MAX(COALESCE(us.longest_streak_days, 0)) as maxLongestStreak,
                SUM(COALESCE(us.total_reviews, 0)) as totalReviews
            FROM users u
            LEFT JOIN user_statistics us ON u.id = us.user_id
            WHERE u.is_active = true AND u.deleted = 0
            """)
    LeaderboardStatsSummary getLeaderboardStats();

    /**
     * Find users who need streak reset (haven't reviewed yesterday or today).
     */
    @Select("""
            SELECT us.*
            FROM user_statistics us
            INNER JOIN users u ON us.user_id = u.id
            WHERE u.is_active = true 
            AND u.deleted = 0
            AND us.current_streak_days > 0
            AND (us.last_review_date IS NULL OR us.last_review_date < CURDATE() - INTERVAL 1 DAY)
            """)
    List<UserStatistics> findUsersNeedingStreakReset();

    /**
     * Reset streak for inactive users.
     */
    @Update("""
            UPDATE user_statistics us
            INNER JOIN users u ON us.user_id = u.id
            SET 
                us.current_streak_days = 0,
                us.updated_at = CURRENT_TIMESTAMP
            WHERE u.is_active = true 
            AND u.deleted = 0
            AND us.current_streak_days > 0
            AND (us.last_review_date IS NULL OR us.last_review_date < CURDATE() - INTERVAL 1 DAY)
            """)
    int resetInactiveUserStreaks();

    /**
     * Get users with highest improvement rates.
     */
    @Select("""
            SELECT 
                u.id as userId,
                u.username,
                u.avatar_url as avatarUrl,
                us.total_reviews,
                us.overall_accuracy_rate as currentAccuracy,
                prev_stats.accuracy as previousAccuracy,
                (us.overall_accuracy_rate - COALESCE(prev_stats.accuracy, 0)) as improvement,
                ROW_NUMBER() OVER (ORDER BY (us.overall_accuracy_rate - COALESCE(prev_stats.accuracy, 0)) DESC) as rank
            FROM users u
            INNER JOIN user_statistics us ON u.id = us.user_id
            LEFT JOIN (
                SELECT 
                    rl.user_id,
                    AVG(CASE WHEN rl.rating >= 3 THEN 100.0 ELSE 0.0 END) as accuracy
                FROM review_logs rl
                WHERE rl.reviewed_at < CURDATE() - INTERVAL 7 DAY
                GROUP BY rl.user_id
            ) prev_stats ON u.id = prev_stats.user_id
            WHERE u.is_active = true 
            AND u.deleted = 0
            AND us.total_reviews >= 50
            AND prev_stats.accuracy IS NOT NULL
            ORDER BY improvement DESC
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "userId", property = "userId"),
        @Result(column = "username", property = "username"),
        @Result(column = "avatarUrl", property = "avatarUrl"),
        @Result(column = "total_reviews", property = "totalReviews"),
        @Result(column = "currentAccuracy", property = "accuracy"),
        @Result(column = "improvement", property = "improvement"),
        @Result(column = "rank", property = "rank")
    })
    List<ImprovementLeaderboardEntry> getImprovementLeaderboard(@Param("limit") int limit);

    /**
     * Summary statistics for leaderboard display.
     */
    @lombok.Data
    @lombok.Builder
    public static class LeaderboardStatsSummary {
        private Long totalUsers;
        private Long weeklyActiveUsers;
        private Long usersWithStreak;
        private Double averageAccuracy;
        private Integer maxCurrentStreak;
        private Integer maxLongestStreak;
        private Long totalReviews;
    }

    /**
     * Improvement leaderboard entry.
     */
    @lombok.Data
    @lombok.Builder
    public static class ImprovementLeaderboardEntry {
        private Long userId;
        private String username;
        private String avatarUrl;
        private Long totalReviews;
        private Double accuracy;
        private Double improvement;
        private Long rank;
    }
}