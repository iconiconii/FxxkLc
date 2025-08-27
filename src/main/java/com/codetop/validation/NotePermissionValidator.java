package com.codetop.validation;

import com.codetop.entity.ProblemNote;
import com.codetop.exception.UnauthorizedException;
import com.codetop.exception.ResourceNotFoundException;
import com.codetop.mapper.ProblemNoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validator for note permissions and ownership.
 * 
 * Provides centralized permission validation logic for note operations.
 * 
 * @author CodeTop Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotePermissionValidator {
    
    private final ProblemNoteMapper problemNoteMapper;
    
    /**
     * Validate that user owns the specified note.
     * 
     * @param userId the user ID
     * @param noteId the note ID
     * @throws ResourceNotFoundException if note doesn't exist
     * @throws UnauthorizedException if user doesn't own the note
     */
    public void validateNoteOwnership(Long userId, Long noteId) {
        log.debug("Validating note ownership: userId={}, noteId={}", userId, noteId);
        
        ProblemNote note = problemNoteMapper.selectById(noteId);
        if (note == null || note.getDeleted()) {
            throw new ResourceNotFoundException("Note not found: " + noteId);
        }
        
        if (!note.getUserId().equals(userId)) {
            log.warn("User {} attempted to access note {} owned by user {}", userId, noteId, note.getUserId());
            throw new UnauthorizedException("User does not own this note");
        }
        
        log.debug("Note ownership validated: userId={}, noteId={}", userId, noteId);
    }
    
    /**
     * Validate that user can access the specified note.
     * This includes ownership or public visibility.
     * 
     * @param userId the user ID (can be null for anonymous access)
     * @param noteId the note ID
     * @throws ResourceNotFoundException if note doesn't exist
     * @throws UnauthorizedException if user cannot access the note
     */
    public void validateNoteAccess(Long userId, Long noteId) {
        log.debug("Validating note access: userId={}, noteId={}", userId, noteId);
        
        ProblemNote note = problemNoteMapper.selectById(noteId);
        if (note == null || note.getDeleted()) {
            throw new ResourceNotFoundException("Note not found: " + noteId);
        }
        
        // Owner can always access
        if (userId != null && note.getUserId().equals(userId)) {
            log.debug("Note access granted (owner): userId={}, noteId={}", userId, noteId);
            return;
        }
        
        // Check if note is public
        if (!note.getIsPublic()) {
            log.warn("User {} attempted to access private note {}", userId, noteId);
            throw new UnauthorizedException("User cannot access this private note");
        }
        
        log.debug("Note access validated (public): userId={}, noteId={}", userId, noteId);
    }
    
    /**
     * Check if user owns the specified note.
     * 
     * @param userId the user ID
     * @param noteId the note ID
     * @return true if user owns the note, false otherwise
     */
    public boolean isNoteOwner(Long userId, Long noteId) {
        if (userId == null) {
            return false;
        }
        
        ProblemNote note = problemNoteMapper.selectById(noteId);
        return note != null && !note.getDeleted() && note.getUserId().equals(userId);
    }
    
    /**
     * Check if user can access the specified note.
     * 
     * @param userId the user ID (can be null for anonymous access)
     * @param noteId the note ID
     * @return true if user can access the note, false otherwise
     */
    public boolean canAccessNote(Long userId, Long noteId) {
        ProblemNote note = problemNoteMapper.selectById(noteId);
        if (note == null || note.getDeleted()) {
            return false;
        }
        
        // Owner can always access
        if (userId != null && note.getUserId().equals(userId)) {
            return true;
        }
        
        // Check if note is public
        return note.getIsPublic();
    }
    
    /**
     * Validate that user can vote on the specified note.
     * Users cannot vote on their own notes.
     * 
     * @param userId the user ID
     * @param noteId the note ID
     * @throws ResourceNotFoundException if note doesn't exist
     * @throws UnauthorizedException if user cannot vote on the note
     */
    public void validateVotePermission(Long userId, Long noteId) {
        log.debug("Validating vote permission: userId={}, noteId={}", userId, noteId);
        
        ProblemNote note = problemNoteMapper.selectById(noteId);
        if (note == null || note.getDeleted()) {
            throw new ResourceNotFoundException("Note not found: " + noteId);
        }
        
        // Note must be public to vote
        if (!note.getIsPublic()) {
            throw new UnauthorizedException("Cannot vote on private note");
        }
        
        // User cannot vote on their own note
        if (note.getUserId().equals(userId)) {
            throw new UnauthorizedException("Cannot vote on your own note");
        }
        
        log.debug("Vote permission validated: userId={}, noteId={}", userId, noteId);
    }
    
    /**
     * Get the note with ownership validation.
     * 
     * @param userId the user ID
     * @param noteId the note ID
     * @return the note
     * @throws ResourceNotFoundException if note doesn't exist
     * @throws UnauthorizedException if user doesn't own the note
     */
    public ProblemNote getOwnedNote(Long userId, Long noteId) {
        validateNoteOwnership(userId, noteId);
        return problemNoteMapper.selectById(noteId);
    }
    
    /**
     * Get the note with access validation.
     * 
     * @param userId the user ID (can be null for anonymous access)
     * @param noteId the note ID
     * @return the note
     * @throws ResourceNotFoundException if note doesn't exist
     * @throws UnauthorizedException if user cannot access the note
     */
    public ProblemNote getAccessibleNote(Long userId, Long noteId) {
        validateNoteAccess(userId, noteId);
        return problemNoteMapper.selectById(noteId);
    }
}