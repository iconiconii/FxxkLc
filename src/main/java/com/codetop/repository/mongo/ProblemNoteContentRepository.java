package com.codetop.repository.mongo;

import com.codetop.entity.ProblemNoteDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository interface for ProblemNoteDocument operations.
 * 
 * Provides MongoDB-specific operations for:
 * - Content document CRUD operations
 * - Text search within note content
 * - Tag-based queries
 * - Code snippet searches
 * - Content statistics and analytics
 * 
 * @author CodeTop Team
 */
@Repository
public interface ProblemNoteContentRepository extends MongoRepository<ProblemNoteDocument, String> {

    // Basic document operations
    
    /**
     * Find content document by problem note ID.
     */
    Optional<ProblemNoteDocument> findByProblemNoteId(Long problemNoteId);
    
    /**
     * Check if content exists for a problem note.
     */
    boolean existsByProblemNoteId(Long problemNoteId);
    
    /**
     * Delete content document by problem note ID.
     */
    void deleteByProblemNoteId(Long problemNoteId);
    
    // Text search queries
    
    /**
     * Search in content using full-text search.
     */
    @Query("{ '$text': { '$search': ?0 } }")
    List<ProblemNoteDocument> searchInContent(String searchText);
    
    /**
     * Search in content with pagination.
     */
    @Query("{ '$text': { '$search': ?0 } }")
    Page<ProblemNoteDocument> searchInContent(String searchText, Pageable pageable);
    
    /**
     * Search in solution approaches.
     */
    @Query("{ 'solution_approach': { '$regex': ?0, '$options': 'i' } }")
    List<ProblemNoteDocument> searchInSolutionApproach(String keyword);
    
    /**
     * Search in tips and pitfalls.
     */
    @Query("{ '$or': [ " +
           "{ 'tips': { '$regex': ?0, '$options': 'i' } }, " +
           "{ 'pitfalls': { '$regex': ?0, '$options': 'i' } } " +
           "] }")
    List<ProblemNoteDocument> searchInTipsAndPitfalls(String keyword);
    
    // Tag-based queries
    
    /**
     * Find documents by tag.
     */
    List<ProblemNoteDocument> findByTagsContaining(String tag);
    
    /**
     * Find documents containing any of the specified tags.
     */
    @Query("{ 'tags': { '$in': ?0 } }")
    List<ProblemNoteDocument> findByTagsIn(List<String> tags);
    
    /**
     * Find documents containing all specified tags.
     */
    @Query("{ 'tags': { '$all': ?0 } }")
    List<ProblemNoteDocument> findByTagsContainingAll(List<String> tags);
    
    /**
     * Get all distinct tags.
     */
    @Query(value = "{}", fields = "{ 'tags': 1 }")
    List<ProblemNoteDocument> findAllTags();
    
    // Code snippet queries
    
    /**
     * Find documents by programming language.
     */
    @Query("{ 'code_snippets.language': ?0 }")
    List<ProblemNoteDocument> findByCodeLanguage(String language);
    
    /**
     * Find documents by code snippet type.
     */
    @Query("{ 'code_snippets.type': ?0 }")
    List<ProblemNoteDocument> findByCodeType(String type);
    
    /**
     * Search in code snippets.
     */
    @Query("{ 'code_snippets.code': { '$regex': ?0, '$options': 'i' } }")
    List<ProblemNoteDocument> searchInCode(String codeKeyword);
    
    /**
     * Find documents with code snippets.
     */
    @Query("{ 'code_snippets': { '$exists': true, '$ne': [] } }")
    List<ProblemNoteDocument> findWithCodeSnippets();
    
    /**
     * Find documents without code snippets.
     */
    @Query("{ '$or': [ " +
           "{ 'code_snippets': { '$exists': false } }, " +
           "{ 'code_snippets': { '$eq': [] } } " +
           "] }")
    List<ProblemNoteDocument> findWithoutCodeSnippets();
    
    // Complexity analysis queries
    
    /**
     * Find documents with complexity analysis.
     */
    @Query("{ '$or': [ " +
           "{ 'time_complexity': { '$exists': true, '$ne': null, '$ne': '' } }, " +
           "{ 'space_complexity': { '$exists': true, '$ne': null, '$ne': '' } } " +
           "] }")
    List<ProblemNoteDocument> findWithComplexityAnalysis();
    
    /**
     * Find documents by time complexity pattern.
     */
    @Query("{ 'time_complexity': { '$regex': ?0, '$options': 'i' } }")
    List<ProblemNoteDocument> findByTimeComplexity(String complexityPattern);
    
    /**
     * Find documents by space complexity pattern.
     */
    @Query("{ 'space_complexity': { '$regex': ?0, '$options': 'i' } }")
    List<ProblemNoteDocument> findBySpaceComplexity(String complexityPattern);
    
    // Content quality and statistics
    
    /**
     * Find documents by minimum word count.
     */
    @Query("{ 'word_count': { '$gte': ?0 } }")
    List<ProblemNoteDocument> findByMinWordCount(Integer minWordCount);
    
    /**
     * Find documents by content length range.
     */
    @Query("{ '$where': 'this.content && this.content.length >= ?0 && this.content.length <= ?1' }")
    List<ProblemNoteDocument> findByContentLengthRange(Integer minLength, Integer maxLength);
    
    /**
     * Find recently modified documents.
     */
    @Query("{ 'last_modified': { '$gte': ?0 } }")
    List<ProblemNoteDocument> findRecentlyModified(LocalDateTime since);
    
    /**
     * Find documents by version.
     */
    List<ProblemNoteDocument> findByVersion(Integer version);
    
    /**
     * Find documents with multiple versions (version > 1).
     */
    @Query("{ 'version': { '$gt': 1 } }")
    List<ProblemNoteDocument> findMultiVersionDocuments();
    
    // Aggregation and analytics
    
    /**
     * Count documents with non-empty content.
     */
    @Query(value = "{ 'content': { '$exists': true, '$ne': null, '$ne': '' } }", count = true)
    long countWithContent();
    
    /**
     * Count documents with code snippets.
     */
    @Query(value = "{ 'code_snippets': { '$exists': true, '$ne': [] } }", count = true)
    long countWithCodeSnippets();
    
    /**
     * Count documents with specific tag.
     */
    @Query(value = "{ 'tags': ?0 }", count = true)
    long countByTag(String tag);
    
    /**
     * Count documents by programming language.
     */
    @Query(value = "{ 'code_snippets.language': ?0 }", count = true)
    long countByProgrammingLanguage(String language);
    
    // Update operations
    
    /**
     * Update document version and last modified time.
     */
    @Query("{ 'problem_note_id': ?0 }")
    @Update("{ '$inc': { 'version': 1 }, '$set': { 'last_modified': ?1 } }")
    void incrementVersionAndUpdateTime(Long problemNoteId, LocalDateTime lastModified);
    
    /**
     * Update word count for a document.
     */
    @Query("{ 'problem_note_id': ?0 }")
    @Update("{ '$set': { 'word_count': ?1 } }")
    void updateWordCount(Long problemNoteId, Integer wordCount);
    
    /**
     * Add tag to document.
     */
    @Query("{ 'problem_note_id': ?0 }")
    @Update("{ '$addToSet': { 'tags': ?1 } }")
    void addTag(Long problemNoteId, String tag);
    
    /**
     * Remove tag from document.
     */
    @Query("{ 'problem_note_id': ?0 }")
    @Update("{ '$pull': { 'tags': ?1 } }")
    void removeTag(Long problemNoteId, String tag);
    
    // Batch operations
    
    /**
     * Find all documents for bulk processing.
     */
    @Query(value = "{}", fields = "{ 'problem_note_id': 1, 'version': 1, 'last_modified': 1 }")
    List<ProblemNoteDocument> findAllForBulkProcessing();
    
    /**
     * Find documents needing word count update.
     */
    @Query("{ '$or': [ " +
           "{ 'word_count': { '$exists': false } }, " +
           "{ 'word_count': null }, " +
           "{ 'word_count': 0 } " +
           "] }")
    List<ProblemNoteDocument> findDocumentsNeedingWordCountUpdate();
}