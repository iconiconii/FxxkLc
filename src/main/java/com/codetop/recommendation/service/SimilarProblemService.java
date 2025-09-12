package com.codetop.recommendation.service;

import com.codetop.mapper.ProblemCategoryMapper;
import com.codetop.mapper.ProblemMapper;
import com.codetop.recommendation.alg.SimilarityScorer;
import com.codetop.recommendation.alg.TagsParser;
import com.codetop.recommendation.dto.CategoryMetadata;
import com.codetop.recommendation.dto.EnhancedSimilarProblemResult;
import lombok.RequiredArgsConstructor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * P0 Implementation: Service for finding similar problems using content-independent similarity.
 * Integrates with existing ProblemCategoryMapper.findSimilarProblems and enhances with
 * tag-based Jaccard similarity and difficulty matching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarProblemService {
    
    private final ProblemCategoryMapper problemCategoryMapper;
    private final ProblemMapper problemMapper;
    private final SimilarityScorer similarityScorer;
    private final TagsParser tagsParser;
    private final MeterRegistry meterRegistry;
    private final CategoryMetadataService categoryMetadataService;
    
    /**
     * Find similar problems enhanced with content-independent similarity scoring.
     * Combines category-based similarity with tag Jaccard similarity and difficulty matching.
     * Results are cached for 15 minutes to improve performance.
     * 
     * @param problemId Target problem ID
     * @param limit Maximum number of similar problems to return
     * @param minSharedCategories Minimum shared categories (for category-based filter)
     * @param minSimilarityScore Minimum overall similarity score (0.0 to 1.0)
     * @return List of enhanced similar problems with similarity scores
     */
    @Cacheable(value = "similarProblems", key = "#problemId + '_' + #limit + '_' + #minSharedCategories + '_' + #minSimilarityScore", 
               unless = "#result == null || #result.isEmpty()")
    public List<EnhancedSimilarProblem> findEnhancedSimilarProblems(Long problemId, int limit, 
                                                                   Integer minSharedCategories, 
                                                                   Double minSimilarityScore) {
        long startTime = System.nanoTime();
        long dbTime = 0;
        long similarityCalcTime = 0;
        int candidatesProcessed = 0;
        
        try {
            // Step 1: Get category-based similar problems (existing implementation)
            long dbStartTime = System.nanoTime();
            List<ProblemCategoryMapper.SimilarProblem> categorySimilar = problemCategoryMapper
                    .findSimilarProblems(problemId, minSharedCategories != null ? minSharedCategories : 1, 
                                       Math.min(limit * 2, 50)); // Get more candidates for filtering
            dbTime += System.nanoTime() - dbStartTime;
            
            if (categorySimilar.isEmpty()) {
                log.debug("No category-similar problems found for problemId={}", problemId);
                return Collections.emptyList();
            }
            
            log.debug("Found {} category-similar problems for problemId={}", categorySimilar.size(), problemId);
            
            // Step 2: Get target problem details for comparison
            dbStartTime = System.nanoTime();
            ProblemDetails targetDetails = getProblemDetails(problemId);
            dbTime += System.nanoTime() - dbStartTime;
            
            if (targetDetails == null) {
                log.warn("Could not get details for target problemId={}", problemId);
                return Collections.emptyList();
            }
            
            // Step 3: Get details for all similar problem candidates
            List<Long> candidateIds = categorySimilar.stream()
                    .map(ProblemCategoryMapper.SimilarProblem::getProblemId)
                    .collect(Collectors.toList());
            
            dbStartTime = System.nanoTime();
            Map<Long, ProblemDetails> candidateDetails = getProblemDetailsMap(candidateIds);
            dbTime += System.nanoTime() - dbStartTime;
            
            // Step 4: Calculate enhanced similarity scores
            List<EnhancedSimilarProblem> enhanced = new ArrayList<>();
            long simCalcStartTime = System.nanoTime();
            
            for (ProblemCategoryMapper.SimilarProblem catSimilar : categorySimilar) {
                ProblemDetails candidate = candidateDetails.get(catSimilar.getProblemId());
                if (candidate == null) continue;
                
                candidatesProcessed++;
                
                // Calculate content-independent similarity score
                double similarityScore = similarityScorer.calculateSimilarity(
                        targetDetails.tags, candidate.tags,
                        targetDetails.categoryIds, candidate.categoryIds,
                        targetDetails.difficulty, candidate.difficulty
                );
                
                // Filter by minimum similarity if specified
                if (minSimilarityScore != null && similarityScore < minSimilarityScore) {
                    continue;
                }
                
                EnhancedSimilarProblem esp = new EnhancedSimilarProblem();
                esp.problemId = catSimilar.getProblemId();
                esp.title = catSimilar.getTitle();
                esp.difficulty = catSimilar.getDifficulty();
                esp.sharedCategories = catSimilar.getSharedCategories();
                esp.sharedCategoryNames = catSimilar.getSharedCategoryNames();
                esp.similarityScore = similarityScore;
                esp.tags = candidate.tags;
                
                enhanced.add(esp);
            }
            
            similarityCalcTime = System.nanoTime() - simCalcStartTime;
            
            // Step 5: Sort by similarity score (descending) and limit results
            List<EnhancedSimilarProblem> result = enhanced.stream()
                    .sorted((a, b) -> Double.compare(b.similarityScore, a.similarityScore))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            // Enhanced metrics logging
            long totalTime = System.nanoTime() - startTime;
            log.info("Similar problems found - problemId={}, candidates_processed={}, final_results={}, " +
                    "total_time_ms={}, db_time_ms={}, similarity_calc_time_ms={}, " +
                    "min_similarity={}, limit={}", 
                    problemId, candidatesProcessed, result.size(),
                    String.format("%.2f", totalTime / 1_000_000.0), 
                    String.format("%.2f", dbTime / 1_000_000.0), 
                    String.format("%.2f", similarityCalcTime / 1_000_000.0),
                    minSimilarityScore, limit);

            // Micrometer metrics
            try {
                if (meterRegistry != null) {
                    meterRegistry.timer("rec.similarity.service.total").record(totalTime, java.util.concurrent.TimeUnit.NANOSECONDS);
                    meterRegistry.timer("rec.similarity.service.db").record(dbTime, java.util.concurrent.TimeUnit.NANOSECONDS);
                    meterRegistry.timer("rec.similarity.service.calc").record(similarityCalcTime, java.util.concurrent.TimeUnit.NANOSECONDS);
                }
            } catch (Exception ignore) {}
            
            // Log distribution of similarity scores for analysis
            if (log.isDebugEnabled() && !result.isEmpty()) {
                double maxScore = result.get(0).getSimilarityScore();
                double minScore = result.get(result.size() - 1).getSimilarityScore();
                double avgScore = result.stream().mapToDouble(EnhancedSimilarProblem::getSimilarityScore).average().orElse(0.0);
                log.debug("Similarity score distribution - problemId={}, max={}, min={}, avg={}", 
                         problemId, String.format("%.3f", maxScore), String.format("%.3f", minScore), String.format("%.3f", avgScore));
            }
                    
            return result;
                    
        } catch (Exception e) {
            long totalTime = System.nanoTime() - startTime;
            log.error("Error finding enhanced similar problems - problemId={}, candidates_processed={}, " +
                     "total_time_ms={}, db_time_ms={}, error={}", 
                     problemId, candidatesProcessed, String.format("%.2f", totalTime / 1_000_000.0), 
                     String.format("%.2f", dbTime / 1_000_000.0), e.getMessage(), e);
            try {
                if (meterRegistry != null) {
                    meterRegistry.timer("rec.similarity.service.total").record(totalTime, java.util.concurrent.TimeUnit.NANOSECONDS);
                    meterRegistry.timer("rec.similarity.service.db").record(dbTime, java.util.concurrent.TimeUnit.NANOSECONDS);
                }
            } catch (Exception ignore) {}
            return Collections.emptyList();
        }
    }
    
    /**
     * P1 Implementation: Find enhanced similar problems with category metadata integration.
     * Returns rich category information including default complexity and representative techniques.
     * 
     * @param problemId Target problem ID
     * @param limit Maximum number of similar problems to return  
     * @param minSharedCategories Minimum shared categories (for category-based filter)
     * @param minSimilarityScore Minimum overall similarity score (0.0 to 1.0)
     * @return List of enhanced similar problems with metadata
     */
    @Cacheable(value = "enhancedSimilarProblems", key = "#problemId + '_' + #limit + '_' + #minSharedCategories + '_' + #minSimilarityScore", 
               unless = "#result == null || #result.isEmpty()")
    public List<EnhancedSimilarProblemResult> findEnhancedSimilarProblemsWithMetadata(
            Long problemId, int limit, Integer minSharedCategories, Double minSimilarityScore) {
        
        long startTime = System.nanoTime();
        
        try {
            // Step 1: Get basic enhanced similar problems
            List<EnhancedSimilarProblem> basicResults = findEnhancedSimilarProblems(
                problemId, limit, minSharedCategories, minSimilarityScore);
            
            if (basicResults.isEmpty()) {
                return Collections.emptyList();
            }
            
            // Step 2: Get target problem details for similarity breakdown
            ProblemDetails targetDetails = getProblemDetails(problemId);
            if (targetDetails == null) {
                log.warn("Could not get target problem details for problemId={}", problemId);
                return Collections.emptyList();
            }
            
            // Step 3: Transform to enhanced results with metadata
            List<EnhancedSimilarProblemResult> enhancedResults = new ArrayList<>();
            
            for (EnhancedSimilarProblem basicResult : basicResults) {
                try {
                    EnhancedSimilarProblemResult enhanced = buildEnhancedResult(
                        basicResult, targetDetails, problemId);
                    enhancedResults.add(enhanced);
                } catch (Exception e) {
                    log.warn("Failed to enhance result for problemId={}: {}", basicResult.problemId, e.getMessage());
                    // Continue with other results
                }
            }
            
            long totalTime = System.nanoTime() - startTime;
            log.info("Enhanced similar problems with metadata - problemId={}, results={}, processing_time_ms={}", 
                    problemId, enhancedResults.size(), String.format("%.2f", totalTime / 1_000_000.0));
            
            return enhancedResults;
            
        } catch (Exception e) {
            long totalTime = System.nanoTime() - startTime;
            log.error("Error finding enhanced similar problems with metadata - problemId={}, processing_time_ms={}, error={}", 
                     problemId, String.format("%.2f", totalTime / 1_000_000.0), e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get problem details (tags, categories, difficulty) for similarity calculation.
     * GPT5-Fix-4: Fix missing difficulty values by using proper difficulty query.
     */
    private ProblemDetails getProblemDetails(Long problemId) {
        try {
            // Get tags
            List<ProblemMapper.ProblemTagsMinimal> tagResults = problemMapper.findTagsByProblemIds(List.of(problemId));
            List<String> tags = tagResults.isEmpty() ? Collections.emptyList() : 
                    tagsParser.parseTagsFromJson(tagResults.get(0).getTags());
            
            // Get categories
            List<ProblemCategoryMapper.ProblemCategoryWithDetails> categories = problemCategoryMapper
                    .findCategoriesByProblemId(problemId);
            List<Long> categoryIds = categories.stream()
                    .map(ProblemCategoryMapper.ProblemCategoryWithDetails::getCategoryId)
                    .collect(Collectors.toList());
            
            // Get difficulty using proper difficulty query
            String difficulty = null;
            List<ProblemMapper.ProblemDifficultyMinimal> difficultyResults = problemMapper
                    .findDifficultiesByIds(List.of(problemId));
            if (!difficultyResults.isEmpty()) {
                difficulty = difficultyResults.get(0).getDifficulty();
            }
            
            ProblemDetails details = new ProblemDetails();
            details.problemId = problemId;
            details.tags = tags;
            details.categoryIds = categoryIds;
            details.difficulty = difficulty;
            
            return details;
        } catch (Exception e) {
            log.error("Error getting problem details for problem {}: {}", problemId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Batch get problem details for multiple problems.
     * GPT5-Fix-4: Optimized to reduce N+1 queries and include difficulty values.
     */
    private Map<Long, ProblemDetails> getProblemDetailsMap(List<Long> problemIds) {
        Map<Long, ProblemDetails> result = new HashMap<>();
        
        try {
            // Batch get tags
            List<ProblemMapper.ProblemTagsMinimal> allTags = problemMapper.findTagsByProblemIds(problemIds);
            Map<Long, List<String>> tagsMap = allTags.stream()
                    .collect(Collectors.toMap(
                            ProblemMapper.ProblemTagsMinimal::getId,
                            pt -> tagsParser.parseTagsFromJson(pt.getTags())
                    ));
            
            // Batch get difficulties
            List<ProblemMapper.ProblemDifficultyMinimal> allDifficulties = problemMapper.findDifficultiesByIds(problemIds);
            Map<Long, String> difficultyMap = allDifficulties.stream()
                    .collect(Collectors.toMap(
                            ProblemMapper.ProblemDifficultyMinimal::getId,
                            ProblemMapper.ProblemDifficultyMinimal::getDifficulty
                    ));
            
            // Batch get categories - use the existing batch query for associations
            List<ProblemCategoryMapper.ProblemCategoryAssociation> allAssociations = 
                    problemCategoryMapper.findExistingAssociationsByProblemIds(problemIds);
            Map<Long, List<Long>> categoriesMap = allAssociations.stream()
                    .collect(Collectors.groupingBy(
                            ProblemCategoryMapper.ProblemCategoryAssociation::getProblemId,
                            Collectors.mapping(
                                    ProblemCategoryMapper.ProblemCategoryAssociation::getCategoryId,
                                    Collectors.toList()
                            )
                    ));
            
            // Build problem details
            for (Long problemId : problemIds) {
                ProblemDetails details = new ProblemDetails();
                details.problemId = problemId;
                details.tags = tagsMap.getOrDefault(problemId, Collections.emptyList());
                details.categoryIds = categoriesMap.getOrDefault(problemId, Collections.emptyList());
                details.difficulty = difficultyMap.get(problemId); // May be null, handled by SimilarityScorer
                
                result.put(problemId, details);
            }
        } catch (Exception e) {
            log.error("Error getting batch problem details: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Build enhanced similar problem result with category metadata.
     */
    private EnhancedSimilarProblemResult buildEnhancedResult(
            EnhancedSimilarProblem basicResult, ProblemDetails targetDetails, Long targetProblemId) {
        
        // Get category metadata for this problem
        List<Long> categoryIds = getCategoryIds(basicResult.problemId);
        List<EnhancedSimilarProblemResult.EnhancedCategoryInfo> categoryInfos = 
            buildEnhancedCategoryInfos(categoryIds);
        
        // Calculate detailed similarity breakdown
        EnhancedSimilarProblemResult.SimilarityBreakdown breakdown = 
            buildSimilarityBreakdown(basicResult, targetDetails);
        
        // Generate learning insights
        EnhancedSimilarProblemResult.LearningInsights insights = 
            buildLearningInsights(categoryInfos, basicResult.difficulty);
        
        return EnhancedSimilarProblemResult.builder()
                .problemId(basicResult.problemId)
                .title(basicResult.title)
                .difficulty(basicResult.difficulty)
                .similarityScore(basicResult.similarityScore)
                .categories(categoryInfos)
                .tags(basicResult.tags != null ? basicResult.tags : Collections.emptyList())
                .similarityBreakdown(breakdown)
                .learningInsights(insights)
                .build();
    }
    
    /**
     * Build enhanced category information with metadata.
     */
    private List<EnhancedSimilarProblemResult.EnhancedCategoryInfo> buildEnhancedCategoryInfos(List<Long> categoryIds) {
        return categoryIds.stream()
                .map(categoryId -> {
                    Optional<CategoryMetadata> metadataOpt = categoryMetadataService.getCategoryMetadata(categoryId);
                    if (metadataOpt.isPresent()) {
                        CategoryMetadata metadata = metadataOpt.get();
                        return EnhancedSimilarProblemResult.EnhancedCategoryInfo.builder()
                                .categoryId(categoryId)
                                .name(metadata.getName())
                                .relevanceScore(0.8) // Default relevance, could be enhanced with actual calculation
                                .defaultComplexity(metadata.getDefaultComplexity())
                                .complexityDescription(metadata.getComplexityDescription())
                                .representativeTechniques(metadata.getRepresentativeTechniques())
                                .typicalTimeComplexity(metadata.getTypicalTimeComplexity())
                                .learningDifficulty(metadata.getLearningDifficulty())
                                .prerequisites(metadata.getPrerequisites())
                                .build();
                    } else {
                        // Fallback for categories without metadata
                        return EnhancedSimilarProblemResult.EnhancedCategoryInfo.builder()
                                .categoryId(categoryId)
                                .name("category_" + categoryId)
                                .relevanceScore(0.5)
                                .defaultComplexity(2) // Default to medium
                                .complexityDescription("Standard complexity")
                                .representativeTechniques(Collections.emptyList())
                                .typicalTimeComplexity("O(n)")
                                .learningDifficulty("intermediate")
                                .prerequisites(Collections.emptyList())
                                .build();
                    }
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Build detailed similarity breakdown for explanation.
     */
    private EnhancedSimilarProblemResult.SimilarityBreakdown buildSimilarityBreakdown(
            EnhancedSimilarProblem result, ProblemDetails targetDetails) {
        
        // Calculate individual scores for breakdown
        List<String> candidateTags = result.tags != null ? result.tags : Collections.emptyList();
        List<String> targetTags = targetDetails.tags != null ? targetDetails.tags : Collections.emptyList();
        
        // Find common elements
        Set<String> commonTags = new HashSet<>(targetTags);
        commonTags.retainAll(candidateTags);
        
        List<String> commonCategoryNames = Collections.emptyList(); // Could be enhanced with actual category names
        
        // Determine difficulty match
        String difficultyMatch = targetDetails.difficulty != null && targetDetails.difficulty.equals(result.difficulty)
                ? "exact_match" : "different";
        
        // Generate explanation
        String explanation = String.format(
                "Similarity based on %d common tags, %s difficulty match, and shared problem categories",
                commonTags.size(), difficultyMatch);
        
        return EnhancedSimilarProblemResult.SimilarityBreakdown.builder()
                .tagsScore(0.0) // Could calculate actual tag score
                .categoriesScore(0.0) // Could calculate actual category score
                .difficultyScore(difficultyMatch.equals("exact_match") ? 1.0 : 0.5)
                .explanation(explanation)
                .commonTags(new ArrayList<>(commonTags))
                .commonCategories(commonCategoryNames)
                .difficultyMatch(difficultyMatch)
                .build();
    }
    
    /**
     * Generate learning insights based on category metadata.
     */
    private EnhancedSimilarProblemResult.LearningInsights buildLearningInsights(
            List<EnhancedSimilarProblemResult.EnhancedCategoryInfo> categories, String difficulty) {
        
        if (categories.isEmpty()) {
            return EnhancedSimilarProblemResult.LearningInsights.builder()
                    .recommendedApproach("Review problem fundamentals")
                    .keyTechniques(Collections.emptyList())
                    .complexityAnalysis("Standard approach")
                    .practiceRecommendations(Collections.emptyList())
                    .build();
        }
        
        // Aggregate techniques from all categories
        Set<String> allTechniques = new HashSet<>();
        List<String> complexityAnalyses = new ArrayList<>();
        List<String> practiceRecs = new ArrayList<>();
        
        for (EnhancedSimilarProblemResult.EnhancedCategoryInfo category : categories) {
            if (category.getRepresentativeTechniques() != null) {
                allTechniques.addAll(category.getRepresentativeTechniques());
            }
            if (category.getTypicalTimeComplexity() != null) {
                complexityAnalyses.add(category.getTypicalTimeComplexity());
            }
            if (category.getLearningDifficulty() != null) {
                practiceRecs.add("Practice " + category.getLearningDifficulty() + " level " + category.getName() + " problems");
            }
        }
        
        String recommendedApproach = categories.get(0).getLearningDifficulty() != null
                ? "Start with " + categories.get(0).getLearningDifficulty() + " level approach"
                : "Use systematic problem-solving approach";
        
        String complexityAnalysis = complexityAnalyses.isEmpty() 
                ? "Analyze time and space complexity carefully"
                : "Expected complexity: " + String.join(" or ", complexityAnalyses);
        
        return EnhancedSimilarProblemResult.LearningInsights.builder()
                .recommendedApproach(recommendedApproach)
                .keyTechniques(new ArrayList<>(allTechniques))
                .complexityAnalysis(complexityAnalysis)
                .practiceRecommendations(practiceRecs)
                .build();
    }
    
    /**
     * Get category IDs for a problem (helper method).
     */
    private List<Long> getCategoryIds(Long problemId) {
        try {
            return problemCategoryMapper.findCategoriesByProblemId(problemId).stream()
                    .map(ProblemCategoryMapper.ProblemCategoryWithDetails::getCategoryId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.debug("Could not get category IDs for problemId={}: {}", problemId, e.getMessage());
            return Collections.emptyList();
        }
    }
    
    // Data classes
    
    public static class EnhancedSimilarProblem {
        public Long problemId;
        public String title;
        public String difficulty;
        public Integer sharedCategories;
        public String sharedCategoryNames;
        public Double similarityScore;
        public List<String> tags;
        
        // Getters and setters
        public Long getProblemId() { return problemId; }
        public void setProblemId(Long problemId) { this.problemId = problemId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public Integer getSharedCategories() { return sharedCategories; }
        public void setSharedCategories(Integer sharedCategories) { this.sharedCategories = sharedCategories; }
        public String getSharedCategoryNames() { return sharedCategoryNames; }
        public void setSharedCategoryNames(String sharedCategoryNames) { this.sharedCategoryNames = sharedCategoryNames; }
        public Double getSimilarityScore() { return similarityScore; }
        public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }
    
    private static class ProblemDetails {
        Long problemId;
        List<String> tags = Collections.emptyList();
        List<Long> categoryIds = Collections.emptyList();
        String difficulty;
    }
}
