package com.codetop.mapper;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.entity.FSRSCard;
import com.codetop.enums.FSRSState;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Mapper interface for FSRSCard entity operations using MyBatis-Plus.
 * 
 * Provides optimized queries for:
 * - FSRS algorithm scheduling and queue generation
 * - Review history and progress tracking
 * - User-specific card management
 * - Performance analytics and statistics
 * - Spaced repetition optimization
 * 
 * @author CodeTop Team
 */
@Mapper
@Repository
public interface FSRSCardMapper extends BaseMapper<FSRSCard> {

    // Basic card queries
    
    /**
     * Find card by user ID and problem ID.
     */
    @Select("SELECT * FROM fsrs_cards WHERE user_id = #{userId} AND problem_id = #{problemId}")
    Optional<FSRSCard> findByUserIdAndProblemId(@Param("userId") Long userId, @Param("problemId") Long problemId);

    /**
     * Find all cards for a user.
     */
    @Select("SELECT * FROM fsrs_cards WHERE user_id = #{userId} ORDER BY next_review_at ASC")
    List<FSRSCard> findByUserId(@Param("userId") Long userId);

    /**
     * Find cards by state for a user.
     */
    @Select("SELECT * FROM fsrs_cards WHERE user_id = #{userId} AND state = #{state} ORDER BY next_review_at ASC")
    List<FSRSCard> findByUserIdAndState(@Param("userId") Long userId, @Param("state") String state);
    
    // Review queue generation queries
    
    /**
     * Find due cards for review queue (optimized for FSRS scheduling).
     */
    @Select("""
            SELECT fc.*, p.title as problem_title, p.difficulty as problem_difficulty
            FROM fsrs_cards fc
            INNER JOIN problems p ON fc.problem_id = p.id
            WHERE fc.user_id = #{userId}
            AND (fc.next_review_at IS NULL OR fc.next_review_at <= #{now})
            AND p.deleted = 0
            ORDER BY 
                CASE fc.state
                    WHEN 'NEW' THEN 1
                    WHEN 'LEARNING' THEN 2
                    WHEN 'RELEARNING' THEN 3
                    WHEN 'REVIEW' THEN 4
                END,
                fc.next_review_at ASC
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "user_id", property = "userId"),
        @Result(column = "problem_id", property = "problemId"),
        @Result(column = "state", property = "state"),
        @Result(column = "difficulty", property = "difficulty"),
        @Result(column = "stability", property = "stability"),
        @Result(column = "next_review_at", property = "nextReview"),
        @Result(column = "review_count", property = "reviewCount"),
        @Result(column = "lapses", property = "lapses"),
        @Result(column = "problem_title", property = "problemTitle"),
        @Result(column = "problem_difficulty", property = "problemDifficulty")
    })
    List<ReviewQueueCard> findDueCards(@Param("userId") Long userId, 
                                      @Param("now") LocalDateTime now, 
                                      @Param("limit") int limit);

    /**
     * Find new cards for learning (never reviewed before).
     */
    @Select("""
            SELECT fc.*, p.title as problem_title, p.difficulty as problem_difficulty
            FROM fsrs_cards fc
            INNER JOIN problems p ON fc.problem_id = p.id
            WHERE fc.user_id = #{userId}
            AND fc.state = 'NEW'
            AND fc.review_count = 0
            AND p.deleted = 0
            ORDER BY fc.created_at ASC
            LIMIT #{limit}
            """)
    List<ReviewQueueCard> findNewCards(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * Find overdue cards for priority review.
     */
    @Select("""
            SELECT fc.*, p.title as problem_title, p.difficulty as problem_difficulty,
                   DATEDIFF(#{now}, fc.next_review_at) as days_overdue
            FROM fsrs_cards fc
            INNER JOIN problems p ON fc.problem_id = p.id
            WHERE fc.user_id = #{userId}
            AND fc.next_review_at < #{now}
            AND fc.state IN ('REVIEW', 'RELEARNING')
            AND p.deleted = 0
            ORDER BY DATEDIFF(#{now}, fc.next_review_at) DESC
            LIMIT #{limit}
            """)
    List<ReviewQueueCard> findOverdueCards(@Param("userId") Long userId, 
                                          @Param("now") LocalDateTime now, 
                                          @Param("limit") int limit);

    /**
     * Generate optimal review queue with mixed card types.
     */
    @Select("""
            (SELECT fc.*, p.title as problem_title, p.difficulty as problem_difficulty, 1 as priority
             FROM fsrs_cards fc
             INNER JOIN problems p ON fc.problem_id = p.id
             WHERE fc.user_id = #{userId}
             AND fc.state = 'NEW'
             AND p.deleted = 0
             ORDER BY fc.created_at ASC
             LIMIT #{newLimit})
            UNION ALL
            (SELECT fc.*, p.title as problem_title, p.difficulty as problem_difficulty, 2 as priority
             FROM fsrs_cards fc
             INNER JOIN problems p ON fc.problem_id = p.id
             WHERE fc.user_id = #{userId}
             AND fc.state IN ('LEARNING', 'RELEARNING')
             AND (fc.next_review_at IS NULL OR fc.next_review_at <= #{now})
             AND p.deleted = 0
             ORDER BY fc.next_review_at ASC
             LIMIT #{learningLimit})
            UNION ALL
            (SELECT fc.*, p.title as problem_title, p.difficulty as problem_difficulty, 3 as priority
             FROM fsrs_cards fc
             INNER JOIN problems p ON fc.problem_id = p.id
             WHERE fc.user_id = #{userId}
             AND fc.state = 'REVIEW'
             AND fc.next_review_at <= #{now}
             AND p.deleted = 0
             ORDER BY fc.next_review_at ASC
             LIMIT #{reviewLimit})
            ORDER BY priority, next_review_at ASC
            """)
    List<ReviewQueueCard> generateOptimalReviewQueue(@Param("userId") Long userId,
                                                    @Param("now") LocalDateTime now,
                                                    @Param("newLimit") int newLimit,
                                                    @Param("learningLimit") int learningLimit,
                                                    @Param("reviewLimit") int reviewLimit);
    
    // Statistics and analytics queries
    
    /**
     * Count cards by state for a user.
     */
    @Select("SELECT COUNT(*) FROM fsrs_cards WHERE user_id = #{userId} AND state = #{state}")
    Long countByUserIdAndState(@Param("userId") Long userId, @Param("state") String state);

    /**
     * Get user learning statistics.
     */
    @Select("""
            SELECT 
                COUNT(*) as total_cards,
                COUNT(CASE WHEN state = 'NEW' THEN 1 END) as new_cards,
                COUNT(CASE WHEN state = 'LEARNING' THEN 1 END) as learning_cards,
                COUNT(CASE WHEN state = 'REVIEW' THEN 1 END) as review_cards,
                COUNT(CASE WHEN state = 'RELEARNING' THEN 1 END) as relearning_cards,
                COUNT(CASE WHEN next_review_at <= #{now} THEN 1 END) as due_cards,
                AVG(review_count) as avg_reviews,
                AVG(difficulty) as avg_difficulty,
                AVG(stability) as avg_stability,
                SUM(lapses) as total_lapses
            FROM fsrs_cards 
            WHERE user_id = #{userId}
            """)
    @Results({
        @Result(column = "total_cards", property = "totalCards"),
        @Result(column = "new_cards", property = "newCards"),
        @Result(column = "learning_cards", property = "learningCards"),
        @Result(column = "review_cards", property = "reviewCards"),
        @Result(column = "relearning_cards", property = "relearningCards"),
        @Result(column = "due_cards", property = "dueCards"),
        @Result(column = "avg_reviews", property = "avgReviews"),
        @Result(column = "avg_difficulty", property = "avgDifficulty"),
        @Result(column = "avg_stability", property = "avgStability"),
        @Result(column = "total_lapses", property = "totalLapses")
    })
    UserLearningStats getUserLearningStats(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Get cards with highest difficulty for targeted practice.
     */
    @Select("""
            SELECT fc.*, p.title as problem_title
            FROM fsrs_cards fc
            INNER JOIN problems p ON fc.problem_id = p.id
            WHERE fc.user_id = #{userId}
            AND fc.difficulty >= #{minDifficulty}
            AND p.deleted = 0
            ORDER BY fc.difficulty DESC, fc.lapses DESC
            LIMIT #{limit}
            """)
    List<FSRSCard> findHighDifficultyCards(@Param("userId") Long userId, 
                                          @Param("minDifficulty") BigDecimal minDifficulty,
                                          @Param("limit") int limit);

    /**
     * Get cards with most lapses for review focus.
     */
    @Select("""
            SELECT fc.*, p.title as problem_title
            FROM fsrs_cards fc
            INNER JOIN problems p ON fc.problem_id = p.id
            WHERE fc.user_id = #{userId}
            AND fc.lapses > 0
            AND p.deleted = 0
            ORDER BY fc.lapses DESC, fc.difficulty DESC
            LIMIT #{limit}
            """)
    List<FSRSCard> findHighLapseCards(@Param("userId") Long userId, @Param("limit") int limit);
    
    // Bulk operations and updates
    
    /**
     * Update card after review with FSRS calculation results.
     */
    @Update("""
            UPDATE fsrs_cards SET
                state = #{state},
                difficulty = #{difficulty},
                stability = #{stability},
                last_review = #{lastReview},
                next_review_at = #{nextReview},
                review_count = review_count + 1,
                interval_days = #{intervalDays},
                elapsed_days = #{elapsedDays},
                grade = #{grade},
                lapses = CASE WHEN #{grade} = 1 THEN lapses + 1 ELSE lapses END,
                updated_at = #{lastReview}
            WHERE id = #{cardId}
            """)
    int updateAfterReview(@Param("cardId") Long cardId,
                         @Param("state") String state,
                         @Param("difficulty") BigDecimal difficulty,
                         @Param("stability") BigDecimal stability,
                         @Param("lastReview") LocalDateTime lastReview,
                         @Param("nextReview") LocalDateTime nextReview,
                         @Param("intervalDays") Integer intervalDays,
                         @Param("elapsedDays") Integer elapsedDays,
                         @Param("grade") Integer grade);

    /**
     * Batch update next review times for multiple cards.
     */
    @Update("""
            <script>
            UPDATE fsrs_cards SET next_review_at = 
            CASE id
            <foreach collection="updates" item="update" separator=" ">
                WHEN #{update.cardId} THEN #{update.nextReview}
            </foreach>
            END,
            updated_at = NOW()
            WHERE id IN 
            <foreach collection="updates" item="update" open="(" separator="," close=")">
                #{update.cardId}
            </foreach>
            </script>
            """)
    int batchUpdateNextReviewTimes(@Param("updates") List<NextReviewUpdate> updates);

    /**
     * Reset cards for parameter optimization testing.
     */
    @Update("""
            UPDATE fsrs_cards SET
                state = 'NEW',
                difficulty = 0,
                stability = 0,
                review_count = 0,
                lapses = 0,
                last_review = NULL,
                next_review_at = NULL,
                updated_at = NOW()
            WHERE user_id = #{userId} AND id IN 
            <foreach collection="cardIds" item="cardId" open="(" separator="," close=")">
                #{cardId}
            </foreach>
            """)
    int resetCardsForOptimization(@Param("userId") Long userId, @Param("cardIds") List<Long> cardIds);
    
    // Performance monitoring queries
    
    /**
     * Find cards needing stability recalculation.
     */
    @Select("""
            SELECT * FROM fsrs_cards 
            WHERE stability <= 0 OR stability IS NULL
            OR (state IN ('REVIEW', 'RELEARNING') AND stability < 0.01)
            ORDER BY updated_at ASC
            LIMIT #{limit}
            """)
    List<FSRSCard> findCardsNeedingRecalculation(@Param("limit") int limit);

    /**
     * Get system-wide FSRS performance metrics.
     */
    @Select("""
            SELECT 
                COUNT(*) as total_cards,
                AVG(review_count) as avg_reviews_per_card,
                AVG(difficulty) as avg_difficulty,
                AVG(stability) as avg_stability,
                COUNT(DISTINCT user_id) as active_users,
                COUNT(CASE WHEN next_review_at <= NOW() THEN 1 END) as due_cards_system,
                AVG(lapses) as avg_lapses
            FROM fsrs_cards
            WHERE updated_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            """)
    @Results({
        @Result(column = "total_cards", property = "totalCards"),
        @Result(column = "avg_reviews_per_card", property = "avgReviewsPerCard"),
        @Result(column = "avg_difficulty", property = "avgDifficulty"),
        @Result(column = "avg_stability", property = "avgStability"),
        @Result(column = "active_users", property = "activeUsers"),
        @Result(column = "due_cards_system", property = "dueCardsSystem"),
        @Result(column = "avg_lapses", property = "avgLapses")
    })
    SystemFSRSMetrics getSystemFSRSMetrics(@Param("days") int days);
    
    // Health check and monitoring queries
    
    /**
     * Count active users for health check.
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM fsrs_cards WHERE updated_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)")
    Long countActiveUsers();
    
    /**
     * Count total cards for health check.
     */
    @Select("SELECT COUNT(*) FROM fsrs_cards")
    Long countTotalCards();
    
    /**
     * Count recent reviews for health monitoring.
     */
    @Select("SELECT COUNT(*) FROM fsrs_cards WHERE last_review >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR)")
    Long countRecentReviews(@Param("hours") int hours);
    
    /**
     * Get average processing time for performance monitoring.
     */
    @Select("""
            SELECT AVG(TIMESTAMPDIFF(MICROSECOND, created_at, updated_at) / 1000) 
            FROM fsrs_cards 
            WHERE updated_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) 
            AND last_review IS NOT NULL
            """)
    Double getAverageProcessingTime(@Param("days") int days);
    
    /**
     * Count failed calculations (cards with invalid state).
     */
    @Select("""
            SELECT COUNT(*) FROM fsrs_cards 
            WHERE (stability <= 0 OR stability IS NULL OR difficulty <= 0) 
            AND updated_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR)
            AND state != 'NEW'
            """)
    Long countFailedCalculations(@Param("hours") int hours);
    
    // Helper classes for complex query results
    
    /**
     * Review queue card with problem information.
     */
    class ReviewQueueCard extends FSRSCard {
        @TableField(exist = false)
        private String problemTitle;
        
        @TableField(exist = false)
        private String problemDifficulty;
        
        @TableField(exist = false)
        private Integer daysOverdue;
        
        @TableField(exist = false)
        private Integer priority;
        
        // Getters and setters
        public String getProblemTitle() { return problemTitle; }
        public void setProblemTitle(String problemTitle) { this.problemTitle = problemTitle; }
        public String getProblemDifficulty() { return problemDifficulty; }
        public void setProblemDifficulty(String problemDifficulty) { this.problemDifficulty = problemDifficulty; }
        public int getDaysOverdue() { return daysOverdue != null ? daysOverdue : 0; }
        public void setDaysOverdue(Integer daysOverdue) { this.daysOverdue = daysOverdue; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
    }
    
    /**
     * User learning statistics.
     */
    class UserLearningStats {
        private Long totalCards;
        private Long newCards;
        private Long learningCards;
        private Long reviewCards;
        private Long relearningCards;
        private Long dueCards;
        private Double avgReviews;
        private Double avgDifficulty;
        private Double avgStability;
        private Long totalLapses;
        
        // Getters and setters
        public Long getTotalCards() { return totalCards; }
        public void setTotalCards(Long totalCards) { this.totalCards = totalCards; }
        public Long getNewCards() { return newCards; }
        public void setNewCards(Long newCards) { this.newCards = newCards; }
        public Long getLearningCards() { return learningCards; }
        public void setLearningCards(Long learningCards) { this.learningCards = learningCards; }
        public Long getReviewCards() { return reviewCards; }
        public void setReviewCards(Long reviewCards) { this.reviewCards = reviewCards; }
        public Long getRelearningCards() { return relearningCards; }
        public void setRelearningCards(Long relearningCards) { this.relearningCards = relearningCards; }
        public Long getDueCards() { return dueCards; }
        public void setDueCards(Long dueCards) { this.dueCards = dueCards; }
        public Double getAvgReviews() { return avgReviews; }
        public void setAvgReviews(Double avgReviews) { this.avgReviews = avgReviews; }
        public Double getAvgDifficulty() { return avgDifficulty; }
        public void setAvgDifficulty(Double avgDifficulty) { this.avgDifficulty = avgDifficulty; }
        public Double getAvgStability() { return avgStability; }
        public void setAvgStability(Double avgStability) { this.avgStability = avgStability; }
        public Long getTotalLapses() { return totalLapses; }
        public void setTotalLapses(Long totalLapses) { this.totalLapses = totalLapses; }
    }
    
    /**
     * System FSRS performance metrics.
     */
    class SystemFSRSMetrics {
        private Long totalCards;
        private Double avgReviewsPerCard;
        private Double avgDifficulty;
        private Double avgStability;
        private Long activeUsers;
        private Long dueCardsSystem;
        private Double avgLapses;
        
        // Getters and setters
        public Long getTotalCards() { return totalCards; }
        public void setTotalCards(Long totalCards) { this.totalCards = totalCards; }
        public Double getAvgReviewsPerCard() { return avgReviewsPerCard; }
        public void setAvgReviewsPerCard(Double avgReviewsPerCard) { this.avgReviewsPerCard = avgReviewsPerCard; }
        public Double getAvgDifficulty() { return avgDifficulty; }
        public void setAvgDifficulty(Double avgDifficulty) { this.avgDifficulty = avgDifficulty; }
        public Double getAvgStability() { return avgStability; }
        public void setAvgStability(Double avgStability) { this.avgStability = avgStability; }
        public Long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(Long activeUsers) { this.activeUsers = activeUsers; }
        public Long getDueCardsSystem() { return dueCardsSystem; }
        public void setDueCardsSystem(Long dueCardsSystem) { this.dueCardsSystem = dueCardsSystem; }
        public Double getAvgLapses() { return avgLapses; }
        public void setAvgLapses(Double avgLapses) { this.avgLapses = avgLapses; }
    }
    
    /**
     * Next review time update helper.
     */
    class NextReviewUpdate {
        private Long cardId;
        private LocalDateTime nextReview;
        
        public NextReviewUpdate(Long cardId, LocalDateTime nextReview) {
            this.cardId = cardId;
            this.nextReview = nextReview;
        }
        
        // Getters and setters
        public Long getCardId() { return cardId; }
        public void setCardId(Long cardId) { this.cardId = cardId; }
        public LocalDateTime getNextReview() { return nextReview; }
        public void setNextReview(LocalDateTime nextReview) { this.nextReview = nextReview; }
    }
}