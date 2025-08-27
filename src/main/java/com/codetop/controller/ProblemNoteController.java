package com.codetop.controller;

import com.codetop.annotation.CurrentUserId;
import com.codetop.annotation.SimpleIdempotent;
import com.codetop.dto.CreateNoteRequestDTO;
import com.codetop.dto.ProblemNoteDTO;
import com.codetop.dto.PublicNoteViewDTO;
import com.codetop.dto.UpdateNoteRequestDTO;
import com.codetop.service.ProblemNoteService;
import com.codetop.validation.NotePermissionValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Problem Note Controller for managing user notes on algorithm problems.
 * 
 * Provides RESTful API endpoints for:
 * - Creating and updating problem notes
 * - Querying user's personal notes
 * - Browsing public notes
 * - Voting on helpful notes
 * - Managing note visibility
 * 
 * All endpoints require authentication except for public note browsing.
 * 
 * @author CodeTop Team
 */
@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Problem Notes", description = "算法题笔记管理API")
@SecurityRequirement(name = "Bearer Authentication")
public class ProblemNoteController {
    
    private final ProblemNoteService problemNoteService;
    private final NotePermissionValidator permissionValidator;
    
    // === Core CRUD Operations ===
    
    /**
     * Create or update a problem note.
     * This endpoint handles both creation of new notes and updates to existing ones.
     */
    @PostMapping
    @Operation(summary = "创建或更新笔记", description = "为指定题目创建或更新用户笔记")
    @SimpleIdempotent(operation = "CREATE_UPDATE_NOTE")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProblemNoteDTO> createOrUpdateNote(
            @CurrentUserId Long userId,
            @Valid @RequestBody CreateNoteRequestDTO request) {
        
        log.info("Creating/updating note: userId={}, problemId={}, title={}", 
                userId, request.getProblemId(), request.getTitle());
        
        try {
            ProblemNoteDTO result = problemNoteService.createOrUpdateNote(userId, request);
            
            log.info("Successfully created/updated note: noteId={}, userId={}, problemId={}", 
                    result.getId(), userId, request.getProblemId());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to create/update note: userId={}, problemId={}", 
                     userId, request.getProblemId(), e);
            throw e;
        }
    }
    
    /**
     * Update an existing note.
     */
    @PutMapping("/{noteId}")
    @Operation(summary = "更新笔记", description = "更新已存在的笔记")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProblemNoteDTO> updateNote(
            @CurrentUserId Long userId,
            @PathVariable @Parameter(description = "笔记ID") Long noteId,
            @Valid @RequestBody UpdateNoteRequestDTO request) {
        
        log.info("Updating note: userId={}, noteId={}", userId, noteId);
        
        // Validate ownership
        permissionValidator.validateNoteOwnership(userId, noteId);
        
        try {
            ProblemNoteDTO result = problemNoteService.updateNote(userId, noteId, request);
            
            log.info("Successfully updated note: noteId={}, userId={}", noteId, userId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to update note: userId={}, noteId={}", userId, noteId, e);
            throw e;
        }
    }
    
    /**
     * Get user's note for a specific problem.
     */
    @GetMapping("/problem/{problemId}")
    @Operation(summary = "获取用户笔记", description = "获取当前用户对指定题目的笔记")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProblemNoteDTO> getUserNote(
            @CurrentUserId Long userId,
            @PathVariable @Parameter(description = "题目ID") @Positive Long problemId) {
        
        log.debug("Getting user note: userId={}, problemId={}", userId, problemId);
        
        Optional<ProblemNoteDTO> note = problemNoteService.getUserNote(userId, problemId);
        
        return note.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all user's notes with pagination.
     */
    @GetMapping("/my")
    @Operation(summary = "获取我的笔记列表", description = "分页获取当前用户的所有笔记")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<ProblemNoteDTO>> getMyNotes(
            @CurrentUserId Long userId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "updatedAt,desc") @Parameter(description = "排序方式") String sort) {
        
        log.debug("Getting user notes: userId={}, page={}, size={}", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<ProblemNoteDTO> result = problemNoteService.getUserNotes(userId, pageable);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Delete a note.
     */
    @DeleteMapping("/{noteId}")
    @Operation(summary = "删除笔记", description = "删除用户的笔记")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteNote(
            @CurrentUserId Long userId,
            @PathVariable @Parameter(description = "笔记ID") Long noteId) {
        
        log.info("Deleting note: userId={}, noteId={}", userId, noteId);
        
        // Validate ownership is handled in the service layer
        problemNoteService.deleteNote(userId, noteId);
        
        log.info("Successfully deleted note: noteId={}, userId={}", noteId, userId);
        return ResponseEntity.noContent().build();
    }
    
    // === Public Note Operations ===
    
    /**
     * Get public notes for a problem.
     * This endpoint does not require authentication.
     */
    @GetMapping("/public/problem/{problemId}")
    @Operation(summary = "获取公开笔记", description = "获取指定题目的公开笔记列表")
    public ResponseEntity<Page<PublicNoteViewDTO>> getPublicNotes(
            @PathVariable @Parameter(description = "题目ID") @Positive Long problemId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "helpfulVotes,desc") @Parameter(description = "排序方式") String sort) {
        
        log.debug("Getting public notes: problemId={}, page={}, size={}", problemId, page, size);
        
        // Limit page size for performance
        size = Math.min(size, 100);
        
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<PublicNoteViewDTO> result = problemNoteService.getPublicNotesAsViewDTO(problemId, pageable);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get popular public notes across all problems.
     */
    @GetMapping("/public/popular")
    @Operation(summary = "获取热门笔记", description = "获取热门公开笔记")
    public ResponseEntity<Page<PublicNoteViewDTO>> getPopularNotes(
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "10") @Parameter(description = "最少点赞数") @Min(0) int minVotes,
            @RequestParam(defaultValue = "100") @Parameter(description = "最少浏览数") @Min(0) int minViews) {
        
        log.debug("Getting popular notes: page={}, size={}, minVotes={}, minViews={}", 
                page, size, minVotes, minViews);
        
        size = Math.min(size, 100);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<PublicNoteViewDTO> result = problemNoteService.getPopularNotesAsViewDTO(minVotes, minViews, pageable);
        
        return ResponseEntity.ok(result);
    }
    
    // === Note Interaction Operations ===
    
    /**
     * Vote on a note.
     */
    @PutMapping("/{noteId}/vote")
    @Operation(summary = "笔记投票", description = "对笔记进行有用投票")
    @SimpleIdempotent(operation = "VOTE_NOTE")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> voteNote(
            @CurrentUserId Long userId,
            @PathVariable @Parameter(description = "笔记ID") Long noteId,
            @RequestParam @Parameter(description = "是否有用") boolean helpful) {
        
        log.info("User voting on note: userId={}, noteId={}, helpful={}", userId, noteId, helpful);
        
        problemNoteService.voteHelpful(userId, noteId, helpful);
        
        log.info("Successfully processed vote: userId={}, noteId={}, helpful={}", userId, noteId, helpful);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Update note visibility.
     */
    @PutMapping("/{noteId}/visibility")
    @Operation(summary = "更新笔记可见性", description = "设置笔记为公开或私有")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> updateVisibility(
            @CurrentUserId Long userId,
            @PathVariable @Parameter(description = "笔记ID") Long noteId,
            @RequestParam @Parameter(description = "是否公开") boolean isPublic) {
        
        log.info("Updating note visibility: userId={}, noteId={}, isPublic={}", userId, noteId, isPublic);
        
        problemNoteService.updateVisibility(userId, noteId, isPublic);
        
        log.info("Successfully updated visibility: noteId={}, isPublic={}", noteId, isPublic);
        return ResponseEntity.ok().build();
    }
    
    // === Search and Analytics Operations ===
    
    /**
     * Search notes by content.
     */
    @GetMapping("/search")
    @Operation(summary = "搜索笔记", description = "根据内容搜索公开笔记")
    public ResponseEntity<Page<PublicNoteViewDTO>> searchNotes(
            @RequestParam @Parameter(description = "搜索关键词") String query,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") @Min(1) @Max(100) int size) {
        
        log.debug("Searching notes: query={}, page={}, size={}", query, page, size);
        
        // Validate search query
        if (query.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }
        
        size = Math.min(size, 100);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<PublicNoteViewDTO> result = problemNoteService.searchNotesAsViewDTO(query, pageable);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get notes by tags.
     */
    @GetMapping("/tags/{tag}")
    @Operation(summary = "按标签获取笔记", description = "获取包含指定标签的公开笔记")
    public ResponseEntity<List<PublicNoteViewDTO>> getNotesByTag(
            @PathVariable @Parameter(description = "标签名") String tag,
            @RequestParam(defaultValue = "50") @Parameter(description = "限制数量") @Min(1) @Max(100) int limit) {
        
        log.debug("Getting notes by tag: tag={}, limit={}", tag, limit);
        
        List<PublicNoteViewDTO> result = problemNoteService.getNotesByTagsAsViewDTO(List.of(tag), limit);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get user note statistics.
     */
    @GetMapping("/stats")
    @Operation(summary = "获取用户笔记统计", description = "获取当前用户的笔记统计信息")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProblemNoteService.UserNoteStatsDTO> getUserStats(
            @CurrentUserId Long userId) {
        
        log.debug("Getting user note stats: userId={}", userId);
        
        ProblemNoteService.UserNoteStatsDTO stats = problemNoteService.getUserNoteStats(userId);
        
        return ResponseEntity.ok(stats);
    }
    
    // === Batch Operations ===
    
    /**
     * Batch update note visibility.
     */
    @PutMapping("/batch/visibility")
    @Operation(summary = "批量更新可见性", description = "批量设置笔记的可见性")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> batchUpdateVisibility(
            @CurrentUserId Long userId,
            @RequestBody @Parameter(description = "笔记ID列表") List<Long> noteIds,
            @RequestParam @Parameter(description = "是否公开") boolean isPublic) {
        
        log.info("Batch updating visibility: userId={}, noteCount={}, isPublic={}", 
                userId, noteIds.size(), isPublic);
        
        // Validate request size
        if (noteIds.size() > 100) {
            return ResponseEntity.badRequest().build();
        }
        
        problemNoteService.batchUpdateVisibility(userId, noteIds, isPublic);
        
        log.info("Successfully batch updated visibility: userId={}, noteCount={}", userId, noteIds.size());
        return ResponseEntity.ok().build();
    }
    
    /**
     * Batch delete notes.
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除笔记", description = "批量删除用户的笔记")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> batchDeleteNotes(
            @CurrentUserId Long userId,
            @RequestBody @Parameter(description = "笔记ID列表") List<Long> noteIds) {
        
        log.info("Batch deleting notes: userId={}, noteCount={}", userId, noteIds.size());
        
        // Validate request size
        if (noteIds.size() > 100) {
            return ResponseEntity.badRequest().build();
        }
        
        problemNoteService.batchDeleteNotes(userId, noteIds);
        
        log.info("Successfully batch deleted notes: userId={}, noteCount={}", userId, noteIds.size());
        return ResponseEntity.ok().build();
    }
    
    // === Helper Methods ===
    
    /**
     * Parse sort parameter into Spring Sort object.
     */
    private Sort parseSort(String sort) {
        try {
            String[] parts = sort.split(",");
            String property = parts[0].trim();
            Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()) 
                    ? Sort.Direction.DESC 
                    : Sort.Direction.ASC;
            
            // Validate sort property to prevent injection
            return switch (property) {
                case "createdAt", "updatedAt", "title", "helpfulVotes", "viewCount" -> Sort.by(direction, property);
                default -> Sort.by(Sort.Direction.DESC, "updatedAt"); // Default sort
            };
        } catch (Exception e) {
            log.warn("Invalid sort parameter: {}, using default", sort);
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
    }
}