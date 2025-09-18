package com.codetop.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codetop.entity.ProblemCategory;
import com.codetop.mapper.ProblemMapper;
import com.codetop.mapper.ProblemCategoryMapper;
import com.codetop.recommendation.alg.TagsParser;
import com.codetop.recommendation.config.UserProfilingProperties;
import com.codetop.recommendation.config.SimilarityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P0-3 Implementation: Bulk populate problem_categories using tag→domain mapping.
 * Creates SYSTEM-assigned category relationships based on existing problem tags
 * and the configured tag-to-domain mapping from application.yml.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemCategoryBulkPopulateService extends ServiceImpl<ProblemCategoryMapper, ProblemCategory> {
    
    private final ProblemMapper problemMapper;
    private final ProblemCategoryMapper problemCategoryMapper;
    private final TagsParser tagsParser;
    private final UserProfilingProperties userProfilingProperties;
    private final SimilarityProperties similarityProperties;
    
    // Default relevance score for auto-assigned categories as per guidance
    private static final BigDecimal DEFAULT_RELEVANCE = new BigDecimal("0.8");
    
    /**
     * Bulk populate problem_categories based on existing problem tags and tag→domain mapping.
     * Following guidance: assignment_type=SYSTEM, is_primary=true, relevance≈0.8
     * 
     * @param dryRun If true, only log what would be done without making changes
     * @return Summary of the bulk population operation
     */
    @Transactional
    public BulkPopulateResult bulkPopulateFromTags(boolean dryRun) {
        log.info("Starting bulk populate problem_categories from tags (dryRun={})", dryRun);
        
        BulkPopulateResult result = new BulkPopulateResult();
        
        try {
            // Step 1: Get all problems with tags
            List<ProblemMapper.ProblemTagsMinimal> problemsWithTags = getAllProblemsWithTags();
            log.info("Found {} problems with tags", problemsWithTags.size());
            
            // Step 2: Get tag→domain mapping from configuration
            Map<String, String> tagDomainMapping = userProfilingProperties.getTagDomainMapping();
            log.info("Using {} tag→domain mappings", tagDomainMapping.size());
            
            // Step 3: Get category name to ID mapping
            Map<String, Long> categoryNameToId = getCategoryNameToIdMapping();
            log.info("Found {} existing categories", categoryNameToId.size());
            
            // Step 4: Process each problem
            List<ProblemCategory> newAssignments = new ArrayList<>();
            for (ProblemMapper.ProblemTagsMinimal problem : problemsWithTags) {
                List<ProblemCategory> assignments = createCategoryAssignments(
                        problem, tagDomainMapping, categoryNameToId);
                newAssignments.addAll(assignments);
                result.problemsProcessed++;
                result.categoriesAssigned += assignments.size();
            }
            
            // Step 5: Filter out existing assignments to avoid duplicates
            List<ProblemCategory> filteredAssignments = filterExistingAssignments(newAssignments);
            result.newAssignments = filteredAssignments.size();
            result.duplicatesSkipped = newAssignments.size() - filteredAssignments.size();
            
            // Step 6: Insert new assignments if not dry run
            if (!dryRun && !filteredAssignments.isEmpty()) {
                batchInsertAssignments(filteredAssignments);
                log.info("Inserted {} new category assignments", filteredAssignments.size());
            } else if (dryRun) {
                log.info("DRY RUN: Would insert {} new category assignments", filteredAssignments.size());
                // Log some examples
                filteredAssignments.stream().limit(5).forEach(assignment -> 
                    log.info("Example assignment: problem_id={}, category_id={}, assignment_type=SYSTEM, relevance=0.8", 
                            assignment.getProblemId(), assignment.getCategoryId()));
            }
            
            result.success = true;
            log.info("Bulk populate completed successfully: {}", result);
            
        } catch (Exception e) {
            log.error("Error during bulk populate: {}", e.getMessage(), e);
            result.success = false;
            result.errorMessage = e.getMessage();
        }
        
        return result;
    }
    
    /**
     * Get all problems that have tags data.
     * GPT5-Fix-1: Use the correct query to get problems with tags.
     */
    private List<ProblemMapper.ProblemTagsMinimal> getAllProblemsWithTags() {
        return problemMapper.findAllProblemsWithTags();
    }
    
    /**
     * Create category assignments for a problem based on its tags and the mapping.
     */
    private List<ProblemCategory> createCategoryAssignments(ProblemMapper.ProblemTagsMinimal problem,
                                                          Map<String, String> tagDomainMapping,
                                                          Map<String, Long> categoryNameToId) {
        List<String> tags = tagsParser.parseTagsFromJson(problem.getTags());
        List<ProblemCategory> assignments = new ArrayList<>();
        
        // Track if we've assigned a primary category
        boolean hasPrimary = false;
        
        for (String tag : tags) {
            // Check if this tag maps to a domain/category
            String domainName = tagDomainMapping.get(tag);
            if (domainName != null) {
                Long categoryId = categoryNameToId.get(domainName);
                if (categoryId != null) {
                    ProblemCategory assignment = new ProblemCategory();
                    assignment.setProblemId(problem.getId());
                    assignment.setCategoryId(categoryId);
                    assignment.setAssignmentType(ProblemCategory.AssignmentType.SYSTEM);
                    assignment.setIsPrimary(!hasPrimary); // First mapping becomes primary
                    assignment.setRelevanceScore(DEFAULT_RELEVANCE);
                    assignment.setAssignedByUserId(null); // System assignment
                    
                    assignments.add(assignment);
                    hasPrimary = true; // Only first one gets to be primary
                    
                } else {
                    log.debug("Category not found for domain '{}' from tag '{}' in problem {}", 
                             domainName, tag, problem.getId());
                }
            }
        }
        
        return assignments;
    }
    
    /**
     * Get mapping from category names to IDs.
     * GPT5-Fix-2: Replace hardcoded mapping with dynamic query.
     */
    private Map<String, Long> getCategoryNameToIdMapping() {
        List<ProblemCategoryMapper.CategoryNameId> categories = problemCategoryMapper.findAllCategoryNameIds();
        return categories.stream()
                .collect(Collectors.toMap(
                        ProblemCategoryMapper.CategoryNameId::getName,
                        ProblemCategoryMapper.CategoryNameId::getId
                ));
    }
    
    /**
     * Filter out assignments that already exist to avoid duplicates.
     * GPT5-Fix-3: Use batch querying for existing associations to reduce N+1 queries.
     */
    private List<ProblemCategory> filterExistingAssignments(List<ProblemCategory> newAssignments) {
        if (newAssignments.isEmpty()) {
            return newAssignments;
        }
        
        // Get unique problem IDs
        List<Long> problemIds = newAssignments.stream()
                .map(ProblemCategory::getProblemId)
                .distinct()
                .collect(Collectors.toList());
        
        // Batch query existing associations
        List<ProblemCategoryMapper.ProblemCategoryAssociation> existingAssociations = 
                problemCategoryMapper.findExistingAssociationsByProblemIds(problemIds);
        
        // Build set of existing (problemId, categoryId) pairs for O(1) lookup
        Set<String> existingPairs = existingAssociations.stream()
                .map(assoc -> assoc.getProblemId() + ":" + assoc.getCategoryId())
                .collect(Collectors.toSet());
        
        // Filter out existing assignments
        return newAssignments.stream()
                .filter(assignment -> {
                    String pair = assignment.getProblemId() + ":" + assignment.getCategoryId();
                    return !existingPairs.contains(pair);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Batch insert category assignments.
     */
    @Transactional
    private void batchInsertAssignments(List<ProblemCategory> assignments) {
        if (assignments.isEmpty()) {
            log.debug("No assignments to insert");
            return;
        }
        
        // Get batch size from configuration
        int batchSize = similarityProperties.getBatch().getInsertBatchSize();
        long startTime = System.currentTimeMillis();
        
        try {
            // Use MyBatis-Plus batch insert for optimal performance
            boolean success = this.saveBatch(assignments, batchSize);
            
            long duration = System.currentTimeMillis() - startTime;
            if (success) {
                log.info("Successfully batch inserted {} problem category assignments in {}ms using batch size {}", 
                        assignments.size(), duration, batchSize);
            } else {
                log.warn("Batch insert completed with some failures for {} assignments", assignments.size());
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to batch insert {} assignments after {}ms: {}", 
                    assignments.size(), duration, e.getMessage());
            
            // Fallback to individual inserts with error handling
            log.info("Attempting fallback individual inserts...");
            int successCount = 0;
            int failureCount = 0;
            
            for (ProblemCategory assignment : assignments) {
                try {
                    problemCategoryMapper.insert(assignment);
                    successCount++;
                } catch (Exception individualError) {
                    failureCount++;
                    log.debug("Failed to insert individual assignment problemId={}, categoryId={}: {}", 
                            assignment.getProblemId(), assignment.getCategoryId(), individualError.getMessage());
                }
            }
            
            log.info("Fallback completed: {} successful, {} failed insertions", successCount, failureCount);
            if (failureCount > 0) {
                throw new RuntimeException(String.format("Batch insert failed with %d failures out of %d assignments", 
                        failureCount, assignments.size()));
            }
        }
    }
    
    /**
     * Result of bulk populate operation.
     */
    public static class BulkPopulateResult {
        public boolean success = false;
        public int problemsProcessed = 0;
        public int categoriesAssigned = 0;
        public int newAssignments = 0;
        public int duplicatesSkipped = 0;
        public String errorMessage;
        
        @Override
        public String toString() {
            return String.format("BulkPopulateResult{success=%s, problemsProcessed=%d, categoriesAssigned=%d, newAssignments=%d, duplicatesSkipped=%d, error='%s'}", 
                    success, problemsProcessed, categoriesAssigned, newAssignments, duplicatesSkipped, errorMessage);
        }
    }
}