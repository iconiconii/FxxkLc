package com.codetop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codetop.dto.*;
import com.codetop.entity.ProblemNote;
import com.codetop.entity.ProblemNoteDocument;
import com.codetop.mapper.ProblemNoteMapper;
import com.codetop.repository.mongo.ProblemNoteContentRepository;
import com.codetop.exception.NoteServiceException;
import com.codetop.exception.ResourceNotFoundException;
import com.codetop.exception.UnauthorizedException;
import com.codetop.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.scheduling.annotation.Async;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Problem Note Service for managing user notes on algorithm problems.
 * 
 * This service coordinates between MySQL (metadata) and MongoDB (content)
 * to provide a unified note management experience.
 * 
 * Key Features:
 * - Hybrid storage architecture (MySQL + MongoDB)
 * - Transaction management across databases
 * - Data consistency guarantees
 * - Performance optimizations
 * - Comprehensive error handling
 * 
 * @author CodeTop Team
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProblemNoteService {
    
    private final ProblemNoteMapper problemNoteMapper;
    private final ProblemNoteContentRepository contentRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    // === Core CRUD Operations ===
    
    /**
     * Create or update a problem note.
     * Handles both MySQL metadata and MongoDB content synchronously.
     */
    @Transactional
    public ProblemNoteDTO createOrUpdateNote(@NotNull Long userId, @Valid CreateNoteRequestDTO request) {
        // Validate parameters
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getProblemId() == null) {
            throw new IllegalArgumentException("Problem ID cannot be null");
        }
        
        log.info("Creating or updating note for user: {}, problem: {}", userId, request.getProblemId());
        
        try {
            // 1. Check if note already exists
            Optional<ProblemNote> existing = problemNoteMapper.selectByUserAndProblem(userId, request.getProblemId());
            
            ProblemNote note;
            if (existing.isPresent()) {
                // Update existing note metadata
                note = updateExistingNote(existing.get(), request);
                log.debug("Updated existing note: {}", note.getId());
            } else {
                // Create new note metadata
                note = createNewNote(userId, request);
                log.debug("Created new note: {}", note.getId());
            }
            
            // 2. Sync content to MongoDB
            syncContentToMongoDB(note.getId(), request);
            
            // 3. Publish event for analytics/caching
            publishNoteUpdatedEvent(note.getId(), userId, note.getProblemId());
            
            // 4. Convert to DTO and return
            return convertToDTO(note);
            
        } catch (Exception e) {
            log.error("Failed to create/update note for user: {}, problem: {}", userId, request.getProblemId(), e);
            throw new NoteServiceException("Failed to save note", e);
        }
    }
    
    /**
     * Get user's note for a specific problem.
     */
    @Transactional(readOnly = true)
    public Optional<ProblemNoteDTO> getUserNote(@NotNull Long userId, @NotNull Long problemId) {
        // Validate parameters
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (problemId == null) {
            throw new IllegalArgumentException("Problem ID cannot be null");
        }
        log.debug("Getting user note for user: {}, problem: {}", userId, problemId);
        
        return problemNoteMapper.selectByUserAndProblem(userId, problemId)
            .map(note -> {
                // Get MongoDB content
                Optional<ProblemNoteDocument> content = contentRepository.findByProblemNoteId(note.getId());
                return convertToDTO(note, content);
            });
    }
    
    /**
     * Get paginated list of user's notes.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ProblemNoteDTO> getUserNotes(@NotNull Long userId, Pageable pageable) {
        log.debug("Getting user notes for user: {}", userId);
        
        Page<ProblemNote> notePage = problemNoteMapper.selectByUserId(Page.of((int) pageable.getOffset(), pageable.getPageSize()), userId);
        
        // Batch load MongoDB content
        List<Long> noteIds = notePage.getRecords().stream()
            .map(ProblemNote::getId)
            .collect(Collectors.toList());
        
        Map<Long, ProblemNoteDocument> contentMap = getContentBatch(noteIds);
        
        // Convert to DTOs
        List<ProblemNoteDTO> dtoList = notePage.getRecords().stream()
            .map(note -> convertToDTO(note, Optional.ofNullable(contentMap.get(note.getId()))))
            .collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, notePage.getTotal());
    }
    
    /**
     * Get paginated public notes for a specific problem.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ProblemNoteDTO> getPublicNotes(@NotNull Long problemId, Pageable pageable) {
        log.debug("Getting public notes for problem: {}", problemId);
        
        Page<ProblemNote> notePage = problemNoteMapper.selectPublicNotesByProblem(
            Page.of((int) pageable.getOffset(), pageable.getPageSize()), problemId);
        
        // Batch load MongoDB content
        List<Long> noteIds = notePage.getRecords().stream()
            .map(ProblemNote::getId)
            .collect(Collectors.toList());
        
        Map<Long, ProblemNoteDocument> contentMap = getContentBatch(noteIds);
        
        // Convert to DTOs with view count increment
        List<ProblemNoteDTO> dtoList = notePage.getRecords().stream()
            .map(note -> {
                // Increment view count asynchronously for public notes
                incrementViewCountAsync(note.getId());
                return convertToDTO(note, Optional.ofNullable(contentMap.get(note.getId())));
            })
            .collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, notePage.getTotal());
    }
    
    /**
     * Delete a note (soft delete).
     */
    @Transactional
    public void deleteNote(@NotNull Long userId, @NotNull Long noteId) {
        log.info("Deleting note: {} by user: {}", noteId, userId);
        
        // Verify ownership
        ProblemNote note = problemNoteMapper.selectById(noteId);
        if (note == null) {
            throw new ResourceNotFoundException("Note not found: " + noteId);
        }
        
        if (!note.getUserId().equals(userId)) {
            throw new UnauthorizedException("User " + userId + " does not own note " + noteId);
        }
        
        try {
            // Soft delete MySQL record
            problemNoteMapper.softDeleteById(noteId);
            
            // Delete MongoDB content (actual delete for content)
            contentRepository.deleteByProblemNoteId(noteId);
            
            log.info("Successfully deleted note: {}", noteId);
            
            // Publish event
            publishNoteDeletedEvent(noteId, userId, note.getProblemId());
            
        } catch (Exception e) {
            log.error("Failed to delete note: {}", noteId, e);
            throw new NoteServiceException("Failed to delete note", e);
        }
    }
    
    // === Engagement Operations ===
    
    /**
     * Vote for a note as helpful.
     */
    @Transactional
    public void voteHelpful(@NotNull Long userId, @NotNull Long noteId, boolean helpful) {
        log.info("User: {} voting {} for note: {}", userId, helpful ? "helpful" : "not helpful", noteId);
        
        // Verify note exists and is public
        ProblemNote note = problemNoteMapper.selectById(noteId);
        if (note == null || !note.getIsPublic()) {
            throw new ResourceNotFoundException("Public note not found: " + noteId);
        }
        
        // Prevent self-voting
        if (note.getUserId().equals(userId)) {
            throw new UnauthorizedException("Cannot vote on your own note");
        }
        
        try {
            if (helpful) {
                problemNoteMapper.incrementHelpfulVotes(noteId);
            } else {
                problemNoteMapper.decrementHelpfulVotes(noteId);
            }
            
            // Publish vote event for analytics
            publishVoteEvent(noteId, userId, helpful);
            
        } catch (Exception e) {
            log.error("Failed to process vote for note: {}", noteId, e);
            throw new NoteServiceException("Failed to process vote", e);
        }
    }
    
    /**
     * Update note visibility (public/private).
     */
    @Transactional
    public void updateVisibility(@NotNull Long userId, @NotNull Long noteId, boolean isPublic) {
        log.info("User: {} updating visibility of note: {} to {}", userId, noteId, isPublic ? "public" : "private");
        
        // Verify ownership
        ProblemNote note = problemNoteMapper.selectById(noteId);
        if (note == null) {
            throw new ResourceNotFoundException("Note not found: " + noteId);
        }
        
        if (!note.getUserId().equals(userId)) {
            throw new UnauthorizedException("User " + userId + " does not own note " + noteId);
        }
        
        try {
            problemNoteMapper.updateVisibility(noteId, isPublic);
            publishVisibilityChangedEvent(noteId, userId, isPublic);
            
        } catch (Exception e) {
            log.error("Failed to update visibility for note: {}", noteId, e);
            throw new NoteServiceException("Failed to update visibility", e);
        }
    }
    
    // === Private Helper Methods ===
    
    private ProblemNote createNewNote(Long userId, CreateNoteRequestDTO request) {
        ProblemNote note = ProblemNote.builder()
            .userId(userId)
            .problemId(request.getProblemId())
            .title(request.getTitle())
            .noteType(request.getNoteType())
            .isPublic(request.getIsPublic())
            .helpfulVotes(0)
            .viewCount(0)
            .build();
        
        problemNoteMapper.insert(note);
        return note;
    }
    
    private ProblemNote updateExistingNote(ProblemNote existing, CreateNoteRequestDTO request) {
        existing.setTitle(request.getTitle());
        existing.setNoteType(request.getNoteType());
        existing.setIsPublic(request.getIsPublic());
        existing.setUpdatedAt(LocalDateTime.now());
        
        problemNoteMapper.updateById(existing);
        return existing;
    }
    
    private void syncContentToMongoDB(Long noteId, CreateNoteRequestDTO request) {
        try {
            // Check if document already exists
            Optional<ProblemNoteDocument> existingDoc = contentRepository.findByProblemNoteId(noteId);
            
            ProblemNoteDocument document;
            if (existingDoc.isPresent()) {
                // Update existing document
                document = existingDoc.get();
                document.setContent(request.getContent());
                document.setSolutionApproach(request.getSolutionApproach());
                document.setTimeComplexity(request.getTimeComplexity());
                document.setSpaceComplexity(request.getSpaceComplexity());
                document.setPitfalls(request.getPitfalls());
                document.setTips(request.getTips());
                document.setTags(request.getTags());
                document.incrementVersion();
                document.updateWordCount();
                
                // Convert code snippets
                if (request.getCodeSnippets() != null) {
                    List<ProblemNoteDocument.CodeSnippet> snippets = request.getCodeSnippets().stream()
                        .map(this::convertCodeSnippet)
                        .collect(Collectors.toList());
                    document.setCodeSnippets(snippets);
                }
                
            } else {
                // Create new document
                List<ProblemNoteDocument.CodeSnippet> snippets = null;
                if (request.getCodeSnippets() != null) {
                    snippets = request.getCodeSnippets().stream()
                        .map(this::convertCodeSnippet)
                        .collect(Collectors.toList());
                }
                
                document = ProblemNoteDocument.builder()
                    .problemNoteId(noteId)
                    .content(request.getContent())
                    .solutionApproach(request.getSolutionApproach())
                    .timeComplexity(request.getTimeComplexity())
                    .spaceComplexity(request.getSpaceComplexity())
                    .pitfalls(request.getPitfalls())
                    .tips(request.getTips())
                    .tags(request.getTags())
                    .codeSnippets(snippets)
                    .lastModified(LocalDateTime.now())
                    .version(1)
                    .build();
                
                document.updateWordCount();
            }
            
            contentRepository.save(document);
            
        } catch (Exception e) {
            log.error("Failed to sync content to MongoDB for note: {}", noteId, e);
            throw new NoteServiceException("Failed to save note content", e);
        }
    }
    
    private ProblemNoteDocument.CodeSnippet convertCodeSnippet(CreateNoteRequestDTO.CodeSnippetRequestDTO dto) {
        return ProblemNoteDocument.CodeSnippet.builder()
            .language(dto.getLanguage())
            .code(dto.getCode())
            .explanation(dto.getExplanation())
            .type(dto.getType())
            .complexityNote(dto.getComplexityNote())
            .build();
    }
    
    private Map<Long, ProblemNoteDocument> getContentBatch(List<Long> noteIds) {
        if (noteIds.isEmpty()) {
            return new HashMap<>();
        }
        
        // Since MongoDB repository doesn't have batch by problemNoteId, we'll use individual queries
        // TODO: Optimize with custom aggregation pipeline
        Map<Long, ProblemNoteDocument> contentMap = new HashMap<>();
        for (Long noteId : noteIds) {
            contentRepository.findByProblemNoteId(noteId).ifPresent(doc -> contentMap.put(noteId, doc));
        }
        return contentMap;
    }
    
    private ProblemNoteDTO convertToDTO(ProblemNote note) {
        Optional<ProblemNoteDocument> content = contentRepository.findByProblemNoteId(note.getId());
        return convertToDTO(note, content);
    }
    
    private ProblemNoteDTO convertToDTO(ProblemNote note, Optional<ProblemNoteDocument> content) {
        if (content.isPresent()) {
            return ProblemNoteDTO.fromEntityAndDocument(note, content.get());
        } else {
            return ProblemNoteDTO.fromEntity(note);
        }
    }
    
    @Async
    private void incrementViewCountAsync(Long noteId) {
        try {
            problemNoteMapper.incrementViewCount(noteId);
        } catch (Exception e) {
            log.warn("Failed to increment view count for note: {}", noteId, e);
            // Don't throw - view count is not critical
        }
    }
    
    // === Event Publishing ===
    
    private void publishNoteUpdatedEvent(Long noteId, Long userId, Long problemId) {
        try {
            eventPublisher.publishEvent(new NoteUpdatedEvent(noteId, userId, problemId, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("Failed to publish note updated event for note: {}", noteId, e);
        }
    }
    
    private void publishNoteDeletedEvent(Long noteId, Long userId, Long problemId) {
        try {
            eventPublisher.publishEvent(new NoteDeletedEvent(noteId, userId, problemId, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("Failed to publish note deleted event for note: {}", noteId, e);
        }
    }
    
    private void publishVoteEvent(Long noteId, Long userId, boolean helpful) {
        try {
            eventPublisher.publishEvent(new NoteVotedEvent(noteId, userId, helpful, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("Failed to publish vote event for note: {}", noteId, e);
        }
    }
    
    private void publishVisibilityChangedEvent(Long noteId, Long userId, boolean isPublic) {
        try {
            eventPublisher.publishEvent(new NoteVisibilityChangedEvent(noteId, userId, isPublic, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("Failed to publish visibility changed event for note: {}", noteId, e);
        }
    }
    
    // === Advanced Query Methods ===
    
    /**
     * Search notes by content using MongoDB full-text search.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ProblemNoteDTO> searchNotes(String searchText, Pageable pageable) {
        log.debug("Searching notes with text: {}", searchText);
        
        try {
            org.springframework.data.domain.Page<ProblemNoteDocument> contentResults = 
                contentRepository.searchInContent(searchText, 
                    org.springframework.data.domain.PageRequest.of((int) pageable.getOffset(), pageable.getPageSize()));
            
            // Get corresponding MySQL records
            List<Long> noteIds = contentResults.getContent().stream()
                .map(ProblemNoteDocument::getProblemNoteId)
                .collect(Collectors.toList());
            
            if (noteIds.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            
            // Batch get MySQL notes (only public ones for search)
            List<ProblemNote> notes = problemNoteMapper.selectBatchIds(noteIds).stream()
                .filter(note -> note.getIsPublic() && !note.getDeleted())
                .collect(Collectors.toList());
            
            // Create content map for efficient lookup
            Map<Long, ProblemNoteDocument> contentMap = contentResults.getContent().stream()
                .collect(Collectors.toMap(ProblemNoteDocument::getProblemNoteId, Function.identity()));
            
            // Convert to DTOs
            List<ProblemNoteDTO> results = notes.stream()
                .map(note -> convertToDTO(note, Optional.ofNullable(contentMap.get(note.getId()))))
                .collect(Collectors.toList());
            
            return new PageImpl<>(results, pageable, contentResults.getTotalElements());
            
        } catch (Exception e) {
            log.error("Failed to search notes with text: {}", searchText, e);
            throw new NoteServiceException("Failed to search notes", e);
        }
    }
    
    /**
     * Get notes by tags.
     */
    @Transactional(readOnly = true)
    public List<ProblemNoteDTO> getNotesByTags(List<String> tags, int limit) {
        log.debug("Getting notes by tags: {}", tags);
        
        try {
            List<ProblemNoteDocument> contentResults = contentRepository.findByTagsIn(tags);
            
            // Limit results and get corresponding MySQL records
            List<Long> noteIds = contentResults.stream()
                .limit(limit)
                .map(ProblemNoteDocument::getProblemNoteId)
                .collect(Collectors.toList());
            
            if (noteIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<ProblemNote> notes = problemNoteMapper.selectBatchIds(noteIds).stream()
                .filter(note -> note.getIsPublic() && !note.getDeleted())
                .collect(Collectors.toList());
            
            // Create content map
            Map<Long, ProblemNoteDocument> contentMap = contentResults.stream()
                .collect(Collectors.toMap(ProblemNoteDocument::getProblemNoteId, Function.identity()));
            
            return notes.stream()
                .map(note -> convertToDTO(note, Optional.ofNullable(contentMap.get(note.getId()))))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Failed to get notes by tags: {}", tags, e);
            throw new NoteServiceException("Failed to get notes by tags", e);
        }
    }
    
    /**
     * Get user statistics.
     */
    @Transactional(readOnly = true)
    public UserNoteStatsDTO getUserNoteStats(Long userId) {
        log.debug("Getting note statistics for user: {}", userId);
        
        try {
            Long totalNotes = problemNoteMapper.countByUserId(userId);
            Long publicNotes = problemNoteMapper.countPublicNotesByUserId(userId);
            Long totalVotes = problemNoteMapper.getTotalHelpfulVotesByUserId(userId);
            Long totalViews = problemNoteMapper.getTotalViewsByUserId(userId);
            
            return UserNoteStatsDTO.builder()
                .userId(userId)
                .totalNotes(totalNotes)
                .publicNotes(publicNotes)
                .totalHelpfulVotes(totalVotes)
                .totalViews(totalViews)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to get user note stats for user: {}", userId, e);
            throw new NoteServiceException("Failed to get user statistics", e);
        }
    }
    
    /**
     * Get popular notes across all problems.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ProblemNoteDTO> getPopularNotes(int minVotes, int minViews, Pageable pageable) {
        log.debug("Getting popular notes with minVotes: {}, minViews: {}", minVotes, minViews);
        
        Page<ProblemNote> notePage = problemNoteMapper.selectPopularNotes(
            Page.of((int) pageable.getOffset(), pageable.getPageSize()), minVotes, minViews);
        
        // Batch load MongoDB content
        List<Long> noteIds = notePage.getRecords().stream()
            .map(ProblemNote::getId)
            .collect(Collectors.toList());
        
        Map<Long, ProblemNoteDocument> contentMap = getContentBatch(noteIds);
        
        // Convert to DTOs
        List<ProblemNoteDTO> dtoList = notePage.getRecords().stream()
            .map(note -> convertToDTO(note, Optional.ofNullable(contentMap.get(note.getId()))))
            .collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, notePage.getTotal());
    }
    
    // === Batch Operations ===
    
    /**
     * Batch update note visibility for a user.
     */
    @Transactional
    public void batchUpdateVisibility(Long userId, List<Long> noteIds, boolean isPublic) {
        log.info("Batch updating visibility for {} notes of user: {}", noteIds.size(), userId);
        
        // Verify all notes belong to the user
        List<ProblemNote> notes = problemNoteMapper.selectBatchIds(noteIds);
        for (ProblemNote note : notes) {
            if (!note.getUserId().equals(userId)) {
                throw new UnauthorizedException("User does not own note: " + note.getId());
            }
        }
        
        try {
            // Batch update visibility
            for (Long noteId : noteIds) {
                problemNoteMapper.updateVisibility(noteId, isPublic);
            }
            
            log.info("Successfully updated visibility for {} notes", noteIds.size());
            
        } catch (Exception e) {
            log.error("Failed to batch update visibility for user: {}", userId, e);
            throw new NoteServiceException("Failed to batch update visibility", e);
        }
    }
    
    /**
     * Batch delete notes for a user.
     */
    @Transactional
    public void batchDeleteNotes(Long userId, List<Long> noteIds) {
        log.info("Batch deleting {} notes for user: {}", noteIds.size(), userId);
        
        // Verify ownership
        List<ProblemNote> notes = problemNoteMapper.selectBatchIds(noteIds);
        for (ProblemNote note : notes) {
            if (!note.getUserId().equals(userId)) {
                throw new UnauthorizedException("User does not own note: " + note.getId());
            }
        }
        
        try {
            // Soft delete MySQL records
            for (Long noteId : noteIds) {
                problemNoteMapper.softDeleteById(noteId);
            }
            
            // Hard delete MongoDB documents
            for (Long noteId : noteIds) {
                contentRepository.deleteByProblemNoteId(noteId);
            }
            
            log.info("Successfully deleted {} notes", noteIds.size());
            
        } catch (Exception e) {
            log.error("Failed to batch delete notes for user: {}", userId, e);
            throw new NoteServiceException("Failed to batch delete notes", e);
        }
    }
    
    // === Performance Monitoring ===
    
    /**
     * Health check for the note service.
     */
    public ServiceHealthDTO checkHealth() {
        ServiceHealthDTO health = ServiceHealthDTO.builder()
            .serviceName("ProblemNoteService")
            .timestamp(LocalDateTime.now())
            .build();
        
        try {
            // Test MySQL connectivity
            problemNoteMapper.selectCount(null);
            health.setMysqlStatus("UP");
            
        } catch (Exception e) {
            log.error("MySQL health check failed", e);
            health.setMysqlStatus("DOWN");
            health.addError("MySQL: " + e.getMessage());
        }
        
        try {
            // Test MongoDB connectivity
            contentRepository.count();
            health.setMongodbStatus("UP");
            
        } catch (Exception e) {
            log.error("MongoDB health check failed", e);
            health.setMongodbStatus("DOWN");
            health.addError("MongoDB: " + e.getMessage());
        }
        
        health.setOverallStatus(health.getMysqlStatus().equals("UP") && health.getMongodbStatus().equals("UP") ? "UP" : "DOWN");
        
        return health;
    }
    
    // === Additional Service Methods for Controller ===
    
    /**
     * Update an existing note.
     */
    @Transactional
    public ProblemNoteDTO updateNote(@NotNull Long userId, @NotNull Long noteId, UpdateNoteRequestDTO request) {
        log.info("Updating specific note: userId={}, noteId={}", userId, noteId);
        
        try {
            // Get existing note
            ProblemNote existingNote = problemNoteMapper.selectById(noteId);
            if (existingNote == null || !existingNote.getUserId().equals(userId)) {
                throw new ResourceNotFoundException("Note not found or not owned by user");
            }
            
            // Update metadata if provided
            if (request.hasMetadataUpdates()) {
                if (request.getTitle() != null) {
                    existingNote.setTitle(request.getTitle());
                }
                if (request.getNoteType() != null) {
                    existingNote.setNoteType(request.getNoteType());
                }
                if (request.getIsPublic() != null) {
                    existingNote.setIsPublic(request.getIsPublic());
                }
                existingNote.setUpdatedAt(LocalDateTime.now());
                problemNoteMapper.updateById(existingNote);
            }
            
            // Update content if provided
            if (request.hasContentUpdates()) {
                updateNoteContent(noteId, request);
            }
            
            // Publish update event
            publishNoteUpdatedEvent(noteId, userId, existingNote.getProblemId());
            
            return convertToDTO(existingNote);
            
        } catch (Exception e) {
            log.error("Failed to update note: userId={}, noteId={}", userId, noteId, e);
            throw new NoteServiceException("Failed to update note", e);
        }
    }
    
    private void updateNoteContent(Long noteId, UpdateNoteRequestDTO request) {
        Optional<ProblemNoteDocument> existingDoc = contentRepository.findByProblemNoteId(noteId);
        
        if (existingDoc.isPresent()) {
            ProblemNoteDocument document = existingDoc.get();
            
            // Update fields if provided
            if (request.getContent() != null) {
                document.setContent(request.getContent());
            }
            if (request.getSolutionApproach() != null) {
                document.setSolutionApproach(request.getSolutionApproach());
            }
            if (request.getTimeComplexity() != null) {
                document.setTimeComplexity(request.getTimeComplexity());
            }
            if (request.getSpaceComplexity() != null) {
                document.setSpaceComplexity(request.getSpaceComplexity());
            }
            if (request.getPitfalls() != null) {
                document.setPitfalls(request.getPitfalls());
            }
            if (request.getTips() != null) {
                document.setTips(request.getTips());
            }
            if (request.getTags() != null) {
                document.setTags(request.getTags());
            }
            
            // Update code snippets if provided
            if (request.getCodeSnippets() != null) {
                List<ProblemNoteDocument.CodeSnippet> snippets = request.getCodeSnippets().stream()
                    .map(this::convertUpdateCodeSnippet)
                    .collect(Collectors.toList());
                document.setCodeSnippets(snippets);
            }
            
            document.incrementVersion();
            document.updateWordCount();
            
            contentRepository.save(document);
        }
    }
    
    private ProblemNoteDocument.CodeSnippet convertUpdateCodeSnippet(UpdateNoteRequestDTO.CodeSnippetUpdateDTO dto) {
        return ProblemNoteDocument.CodeSnippet.builder()
            .language(dto.getLanguage())
            .code(dto.getCode())
            .explanation(dto.getExplanation())
            .type(dto.getType())
            .complexityNote(dto.getComplexityNote())
            .build();
    }
    
    /**
     * Get public notes as ViewDTO for API endpoints.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PublicNoteViewDTO> getPublicNotesAsViewDTO(@NotNull Long problemId, Pageable pageable) {
        org.springframework.data.domain.Page<ProblemNoteDTO> notePage = getPublicNotes(problemId, pageable);
        
        List<PublicNoteViewDTO> viewDTOs = notePage.getContent().stream()
            .map(PublicNoteViewDTO::fromProblemNoteDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(viewDTOs, pageable, notePage.getTotalElements());
    }
    
    /**
     * Get popular notes as ViewDTO for API endpoints.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PublicNoteViewDTO> getPopularNotesAsViewDTO(int minVotes, int minViews, Pageable pageable) {
        org.springframework.data.domain.Page<ProblemNoteDTO> notePage = getPopularNotes(minVotes, minViews, pageable);
        
        List<PublicNoteViewDTO> viewDTOs = notePage.getContent().stream()
            .map(PublicNoteViewDTO::fromProblemNoteDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(viewDTOs, pageable, notePage.getTotalElements());
    }
    
    /**
     * Search notes as ViewDTO for API endpoints.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PublicNoteViewDTO> searchNotesAsViewDTO(String searchText, Pageable pageable) {
        org.springframework.data.domain.Page<ProblemNoteDTO> notePage = searchNotes(searchText, pageable);
        
        List<PublicNoteViewDTO> viewDTOs = notePage.getContent().stream()
            .map(PublicNoteViewDTO::fromProblemNoteDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(viewDTOs, pageable, notePage.getTotalElements());
    }
    
    /**
     * Get notes by tags as ViewDTO for API endpoints.
     */
    @Transactional(readOnly = true)
    public List<PublicNoteViewDTO> getNotesByTagsAsViewDTO(List<String> tags, int limit) {
        List<ProblemNoteDTO> notes = getNotesByTags(tags, limit);
        
        return notes.stream()
            .map(PublicNoteViewDTO::fromProblemNoteDTO)
            .collect(Collectors.toList());
    }
    
    // === Utility DTOs ===
    
    public static class UserNoteStatsDTO {
        private Long userId;
        private Long totalNotes;
        private Long publicNotes;
        private Long totalHelpfulVotes;
        private Long totalViews;
        
        public static UserNoteStatsDTOBuilder builder() {
            return new UserNoteStatsDTOBuilder();
        }
        
        // Getters and builder class would be here
        public Long getUserId() { return userId; }
        public Long getTotalNotes() { return totalNotes; }
        public Long getPublicNotes() { return publicNotes; }
        public Long getTotalHelpfulVotes() { return totalHelpfulVotes; }
        public Long getTotalViews() { return totalViews; }
        
        public static class UserNoteStatsDTOBuilder {
            private Long userId;
            private Long totalNotes;
            private Long publicNotes;
            private Long totalHelpfulVotes;
            private Long totalViews;
            
            public UserNoteStatsDTOBuilder userId(Long userId) { this.userId = userId; return this; }
            public UserNoteStatsDTOBuilder totalNotes(Long totalNotes) { this.totalNotes = totalNotes; return this; }
            public UserNoteStatsDTOBuilder publicNotes(Long publicNotes) { this.publicNotes = publicNotes; return this; }
            public UserNoteStatsDTOBuilder totalHelpfulVotes(Long totalHelpfulVotes) { this.totalHelpfulVotes = totalHelpfulVotes; return this; }
            public UserNoteStatsDTOBuilder totalViews(Long totalViews) { this.totalViews = totalViews; return this; }
            
            public UserNoteStatsDTO build() {
                UserNoteStatsDTO dto = new UserNoteStatsDTO();
                dto.userId = this.userId;
                dto.totalNotes = this.totalNotes;
                dto.publicNotes = this.publicNotes;
                dto.totalHelpfulVotes = this.totalHelpfulVotes;
                dto.totalViews = this.totalViews;
                return dto;
            }
        }
    }
    
    public static class ServiceHealthDTO {
        private String serviceName;
        private LocalDateTime timestamp;
        private String overallStatus;
        private String mysqlStatus;
        private String mongodbStatus;
        private List<String> errors = new ArrayList<>();
        
        public static ServiceHealthDTOBuilder builder() {
            return new ServiceHealthDTOBuilder();
        }
        
        public void addError(String error) {
            this.errors.add(error);
        }
        
        // Getters and builder
        public String getServiceName() { return serviceName; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getOverallStatus() { return overallStatus; }
        public String getMysqlStatus() { return mysqlStatus; }
        public String getMongodbStatus() { return mongodbStatus; }
        public List<String> getErrors() { return errors; }
        
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
        public void setMysqlStatus(String mysqlStatus) { this.mysqlStatus = mysqlStatus; }
        public void setMongodbStatus(String mongodbStatus) { this.mongodbStatus = mongodbStatus; }
        
        public static class ServiceHealthDTOBuilder {
            private String serviceName;
            private LocalDateTime timestamp;
            
            public ServiceHealthDTOBuilder serviceName(String serviceName) { this.serviceName = serviceName; return this; }
            public ServiceHealthDTOBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
            
            public ServiceHealthDTO build() {
                ServiceHealthDTO dto = new ServiceHealthDTO();
                dto.serviceName = this.serviceName;
                dto.timestamp = this.timestamp;
                return dto;
            }
        }
    }
}