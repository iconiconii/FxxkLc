package com.codetop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.entity.ProblemNote;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Mapper interface for ProblemNote entity operations using MyBatis-Plus.
 * 
 * Provides optimized queries for:
 * - User-specific problem notes
 * - Public note discovery and sharing
 * - Note statistics and engagement metrics
 * - Soft delete operations
 * 
 * @author CodeTop Team
 */
@Mapper
@Repository
public interface ProblemNoteMapper extends BaseMapper<ProblemNote> {

    // Basic note queries
    
    /**
     * Find user's note for a specific problem.
     */
    @Select("SELECT * FROM problem_notes WHERE user_id = #{userId} AND problem_id = #{problemId} AND deleted = 0")
    Optional<ProblemNote> selectByUserAndProblem(@Param("userId") Long userId, @Param("problemId") Long problemId);
    
    /**
     * Find all notes by user ID.
     */
    @Select("SELECT * FROM problem_notes WHERE user_id = #{userId} AND deleted = 0 ORDER BY updated_at DESC")
    Page<ProblemNote> selectByUserId(Page<ProblemNote> page, @Param("userId") Long userId);
    
    /**
     * Find all notes for a specific problem.
     */
    @Select("SELECT * FROM problem_notes WHERE problem_id = #{problemId} AND deleted = 0 ORDER BY helpful_votes DESC, updated_at DESC")
    Page<ProblemNote> selectByProblemId(Page<ProblemNote> page, @Param("problemId") Long problemId);
    
    // Public note queries
    
    /**
     * Find public notes for a specific problem.
     */
    @Select("SELECT * FROM problem_notes WHERE problem_id = #{problemId} AND is_public = 1 AND deleted = 0 ORDER BY helpful_votes DESC, view_count DESC")
    Page<ProblemNote> selectPublicNotesByProblem(Page<ProblemNote> page, @Param("problemId") Long problemId);
    
    /**
     * Find all public notes by user.
     */
    @Select("SELECT * FROM problem_notes WHERE user_id = #{userId} AND is_public = 1 AND deleted = 0 ORDER BY helpful_votes DESC, updated_at DESC")
    Page<ProblemNote> selectPublicNotesByUser(Page<ProblemNote> page, @Param("userId") Long userId);
    
    /**
     * Find popular public notes (high votes or views).
     */
    @Select("SELECT * FROM problem_notes WHERE is_public = 1 AND deleted = 0 AND (helpful_votes > #{minVotes} OR view_count > #{minViews}) ORDER BY helpful_votes DESC, view_count DESC")
    Page<ProblemNote> selectPopularNotes(Page<ProblemNote> page, @Param("minVotes") Integer minVotes, @Param("minViews") Integer minViews);
    
    /**
     * Find recent public notes.
     */
    @Select("SELECT * FROM problem_notes WHERE is_public = 1 AND deleted = 0 ORDER BY created_at DESC LIMIT #{limit}")
    List<ProblemNote> selectRecentPublicNotes(@Param("limit") int limit);
    
    // Search queries
    
    /**
     * Search notes by title.
     */
    @Select("""
            SELECT * FROM problem_notes 
            WHERE LOWER(title) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
            AND deleted = 0 
            ORDER BY helpful_votes DESC, updated_at DESC
            """)
    Page<ProblemNote> searchNotesByTitle(Page<ProblemNote> page, @Param("keyword") String keyword);
    
    /**
     * Search public notes by title.
     */
    @Select("""
            SELECT * FROM problem_notes 
            WHERE LOWER(title) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
            AND is_public = 1 
            AND deleted = 0 
            ORDER BY helpful_votes DESC, view_count DESC
            """)
    Page<ProblemNote> searchPublicNotesByTitle(Page<ProblemNote> page, @Param("keyword") String keyword);
    
    /**
     * Search notes by type.
     */
    @Select("SELECT * FROM problem_notes WHERE note_type = #{noteType} AND deleted = 0 ORDER BY updated_at DESC")
    Page<ProblemNote> selectByNoteType(Page<ProblemNote> page, @Param("noteType") String noteType);
    
    /**
     * Search public notes by type.
     */
    @Select("SELECT * FROM problem_notes WHERE note_type = #{noteType} AND is_public = 1 AND deleted = 0 ORDER BY helpful_votes DESC")
    Page<ProblemNote> selectPublicNotesByType(Page<ProblemNote> page, @Param("noteType") String noteType);
    
    // Statistics queries
    
    /**
     * Count notes by user.
     */
    @Select("SELECT COUNT(*) FROM problem_notes WHERE user_id = #{userId} AND deleted = 0")
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * Count public notes by user.
     */
    @Select("SELECT COUNT(*) FROM problem_notes WHERE user_id = #{userId} AND is_public = 1 AND deleted = 0")
    Long countPublicNotesByUserId(@Param("userId") Long userId);
    
    /**
     * Count notes for a specific problem.
     */
    @Select("SELECT COUNT(*) FROM problem_notes WHERE problem_id = #{problemId} AND deleted = 0")
    Long countByProblemId(@Param("problemId") Long problemId);
    
    /**
     * Count public notes for a specific problem.
     */
    @Select("SELECT COUNT(*) FROM problem_notes WHERE problem_id = #{problemId} AND is_public = 1 AND deleted = 0")
    Long countPublicNotesByProblemId(@Param("problemId") Long problemId);
    
    /**
     * Get total helpful votes for user's notes.
     */
    @Select("SELECT COALESCE(SUM(helpful_votes), 0) FROM problem_notes WHERE user_id = #{userId} AND deleted = 0")
    Long getTotalHelpfulVotesByUserId(@Param("userId") Long userId);
    
    /**
     * Get total view count for user's public notes.
     */
    @Select("SELECT COALESCE(SUM(view_count), 0) FROM problem_notes WHERE user_id = #{userId} AND is_public = 1 AND deleted = 0")
    Long getTotalViewsByUserId(@Param("userId") Long userId);
    
    // Engagement operations
    
    /**
     * Increment helpful votes for a note.
     */
    @Update("UPDATE problem_notes SET helpful_votes = helpful_votes + 1, updated_at = NOW() WHERE id = #{noteId} AND deleted = 0")
    int incrementHelpfulVotes(@Param("noteId") Long noteId);
    
    /**
     * Decrement helpful votes for a note.
     */
    @Update("UPDATE problem_notes SET helpful_votes = GREATEST(helpful_votes - 1, 0), updated_at = NOW() WHERE id = #{noteId} AND deleted = 0")
    int decrementHelpfulVotes(@Param("noteId") Long noteId);
    
    /**
     * Increment view count for a note.
     */
    @Update("UPDATE problem_notes SET view_count = view_count + 1 WHERE id = #{noteId} AND deleted = 0")
    int incrementViewCount(@Param("noteId") Long noteId);
    
    /**
     * Update note visibility.
     */
    @Update("UPDATE problem_notes SET is_public = #{isPublic}, updated_at = NOW() WHERE id = #{noteId} AND deleted = 0")
    int updateVisibility(@Param("noteId") Long noteId, @Param("isPublic") Boolean isPublic);
    
    // Soft delete operations
    
    /**
     * Soft delete a note.
     */
    @Update("UPDATE problem_notes SET deleted = 1, updated_at = NOW() WHERE id = #{noteId}")
    int softDeleteById(@Param("noteId") Long noteId);
    
    /**
     * Restore a soft-deleted note.
     */
    @Update("UPDATE problem_notes SET deleted = 0, updated_at = NOW() WHERE id = #{noteId}")
    int restoreById(@Param("noteId") Long noteId);
    
    /**
     * Soft delete all notes by user.
     */
    @Update("UPDATE problem_notes SET deleted = 1, updated_at = NOW() WHERE user_id = #{userId}")
    int softDeleteByUserId(@Param("userId") Long userId);
    
    /**
     * Find soft-deleted notes by user.
     */
    @Select("SELECT * FROM problem_notes WHERE user_id = #{userId} AND deleted = 1 ORDER BY updated_at DESC")
    Page<ProblemNote> selectDeletedByUserId(Page<ProblemNote> page, @Param("userId") Long userId);
    
    // Analytics and reporting
    
    /**
     * Get note type distribution for a user.
     */
    @Select("""
            SELECT note_type, COUNT(*) as count 
            FROM problem_notes 
            WHERE user_id = #{userId} AND deleted = 0 
            GROUP BY note_type 
            ORDER BY count DESC
            """)
    @Results({
        @Result(column = "note_type", property = "noteType"),
        @Result(column = "count", property = "count")
    })
    List<NoteTypeStats> getNoteTypeStatsByUserId(@Param("userId") Long userId);
    
    /**
     * Get most helpful notes by user.
     */
    @Select("SELECT * FROM problem_notes WHERE user_id = #{userId} AND is_public = 1 AND deleted = 0 ORDER BY helpful_votes DESC LIMIT #{limit}")
    List<ProblemNote> getMostHelpfulNotesByUserId(@Param("userId") Long userId, @Param("limit") int limit);
    
    /**
     * Get most viewed notes by user.
     */
    @Select("SELECT * FROM problem_notes WHERE user_id = #{userId} AND is_public = 1 AND deleted = 0 ORDER BY view_count DESC LIMIT #{limit}")
    List<ProblemNote> getMostViewedNotesByUserId(@Param("userId") Long userId, @Param("limit") int limit);
    
    // Helper class for statistics
    
    /**
     * Helper class for note type statistics.
     */
    class NoteTypeStats {
        private String noteType;
        private Long count;
        
        // Getters and setters
        public String getNoteType() { return noteType; }
        public void setNoteType(String noteType) { this.noteType = noteType; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }
}