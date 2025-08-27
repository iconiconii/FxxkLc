package com.codetop.repository.mongo;

import com.codetop.entity.ProblemNoteDetail;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemNoteDetailRepository extends MongoRepository<ProblemNoteDetail, String> {
    
    Optional<ProblemNoteDetail> findByProblemNoteId(Long problemNoteId);
    
    List<ProblemNoteDetail> findByUserId(Long userId);
    
    List<ProblemNoteDetail> findByProblemId(Long problemId);
    
    List<ProblemNoteDetail> findByUserIdAndProblemId(Long userId, Long problemId);
    
    @Query("{ 'patterns': { $in: ?0 } }")
    List<ProblemNoteDetail> findByPatternsIn(List<String> patterns);
    
    @Query("{ 'related_problems': { $in: ?0 } }")
    List<ProblemNoteDetail> findByRelatedProblemsIn(List<Long> problemIds);
    
    long countByUserId(Long userId);
    
    long countByProblemId(Long problemId);
}