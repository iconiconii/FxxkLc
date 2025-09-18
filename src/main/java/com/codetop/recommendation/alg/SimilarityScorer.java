package com.codetop.recommendation.alg;

import com.codetop.enums.Difficulty;
import com.codetop.recommendation.config.SimilarityProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimilarityScorer {
    
    private final SimilarityProperties config;
    private final MeterRegistry meterRegistry;
    
    /**
     * Calculate similarity between two problems using tags, categories, and difficulty.
     * Uses configurable weights from application.yml for A/B testing support.
     * 
     * @param tags1 Tags of first problem
     * @param tags2 Tags of second problem
     * @param categories1 Category IDs of first problem
     * @param categories2 Category IDs of second problem
     * @param difficulty1 Difficulty of first problem
     * @param difficulty2 Difficulty of second problem
     * @return Similarity score between 0.0 and 1.0
     */
    public double calculateSimilarity(List<String> tags1, List<String> tags2,
                                    List<Long> categories1, List<Long> categories2,
                                    String difficulty1, String difficulty2) {
        
        long startTime = System.nanoTime();
        SimilarityProperties.Weights weights = config.getWeights();
        
        double tagsScore = calculateJaccardSimilarity(tags1, tags2);
        double categoriesScore = calculateCategoriesSimilarity(categories1, categories2);
        double difficultyScore = calculateDifficultySimilarity(difficulty1, difficulty2);
        
        double totalScore = weights.getTagsWeight() * tagsScore +
                           weights.getCategoriesWeight() * categoriesScore +
                           weights.getDifficultyWeight() * difficultyScore;
        
        double finalScore = Math.max(0.0, Math.min(1.0, totalScore));
        long durationNanos = System.nanoTime() - startTime;
        
        // Enhanced logging for similarity calculation metrics
        if (log.isDebugEnabled()) {
            log.debug("Similarity calculation - tags_score={}, categories_score={}, difficulty_score={}, " +
                     "final_score={}, duration_ns={}, weights=[tags:{}, cats:{}, diff:{}]",
                     String.format("%.3f", tagsScore), String.format("%.3f", categoriesScore), 
                     String.format("%.3f", difficultyScore), String.format("%.3f", finalScore), durationNanos,
                     String.format("%.2f", weights.getTagsWeight()), String.format("%.2f", weights.getCategoriesWeight()), 
                     String.format("%.2f", weights.getDifficultyWeight()));
        }
        
        // Metrics logging for performance monitoring
        if (durationNanos > 1_000_000) { // Log if calculation takes more than 1ms
            log.warn("Slow similarity calculation detected: {} ns for tags={}, categories={}", 
                    durationNanos, tags1 != null ? tags1.size() : 0, categories1 != null ? categories1.size() : 0);
        }
        try {
            if (meterRegistry != null) {
                meterRegistry.timer("rec.similarity.scorer.duration").record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
            }
        } catch (Exception ignore) {}
        
        return finalScore;
    }
    
    /**
     * Overloaded method with configurable weights for A/B testing.
     */
    public double calculateSimilarity(List<String> tags1, List<String> tags2,
                                    List<Long> categories1, List<Long> categories2,
                                    String difficulty1, String difficulty2,
                                    double tagsWeight, double categoriesWeight, double difficultyWeight) {
        
        double tagsScore = calculateJaccardSimilarity(tags1, tags2);
        double categoriesScore = calculateCategoriesSimilarity(categories1, categories2);
        double difficultyScore = calculateDifficultySimilarity(difficulty1, difficulty2);
        
        double totalScore = tagsWeight * tagsScore +
                           categoriesWeight * categoriesScore +
                           difficultyWeight * difficultyScore;
        
        return Math.max(0.0, Math.min(1.0, totalScore));
    }
    
    /**
     * Calculate Jaccard similarity for tags: |intersection| / |union|
     * Includes normalization (lowercase, trim) for better matching accuracy.
     */
    private double calculateJaccardSimilarity(List<String> tags1, List<String> tags2) {
        if ((tags1 == null || tags1.isEmpty()) && (tags2 == null || tags2.isEmpty())) {
            return config.getThresholds().getEmptyFeatureSimilarity(); // Configurable empty-feature handling
        }
        if (tags1 == null || tags1.isEmpty() || tags2 == null || tags2.isEmpty()) {
            return 0.0; // One empty, one not = no match
        }
        
        Set<String> set1 = normalizeTagSet(tags1);
        Set<String> set2 = normalizeTagSet(tags2);
        
        // Calculate intersection
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        // Calculate union
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Normalize tag set for better matching: lowercase, trim whitespace, remove empty strings.
     */
    private Set<String> normalizeTagSet(List<String> tags) {
        return tags.stream()
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .map(tag -> tag.toLowerCase().trim())
                .collect(Collectors.toSet());
    }
    
    /**
     * Calculate shared categories similarity: shared_count / max(count1, count2)
     * Normalized by the maximum count to avoid bias toward problems with many categories.
     */
    private double calculateCategoriesSimilarity(List<Long> categories1, List<Long> categories2) {
        if ((categories1 == null || categories1.isEmpty()) && (categories2 == null || categories2.isEmpty())) {
            return config.getThresholds().getEmptyFeatureSimilarity(); // Configurable empty-feature handling
        }
        if (categories1 == null || categories1.isEmpty() || categories2 == null || categories2.isEmpty()) {
            return 0.0; // One empty, one not = no match
        }
        
        Set<Long> set1 = new HashSet<>(categories1);
        Set<Long> set2 = new HashSet<>(categories2);
        
        // Calculate intersection
        Set<Long> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        // Normalize by max size to avoid bias toward problems with many categories
        int maxSize = Math.max(set1.size(), set2.size());
        return maxSize == 0 ? 0.0 : (double) intersection.size() / maxSize;
    }
    
    /**
     * Calculate difficulty matching score based on guidance:
     * - Same difficulty: 1.0
     * - Adjacent difficulty: 0.5
     * - Two levels apart: 0.0
     */
    private double calculateDifficultySimilarity(String difficulty1, String difficulty2) {
        if (difficulty1 == null || difficulty2 == null) {
            return 0.5; // Neutral score for missing difficulty
        }
        
        if (difficulty1.equals(difficulty2)) {
            return 1.0; // Perfect match
        }
        
        try {
            Difficulty diff1 = Difficulty.valueOf(difficulty1.toUpperCase());
            Difficulty diff2 = Difficulty.valueOf(difficulty2.toUpperCase());
            
            int level1 = getDifficultyLevel(diff1);
            int level2 = getDifficultyLevel(diff2);
            int levelDiff = Math.abs(level1 - level2);
            
            if (levelDiff == 1) {
                return 0.5; // Adjacent levels
            } else if (levelDiff >= 2) {
                return 0.0; // Two or more levels apart
            }
            
            return 1.0; // Same level (should not reach here due to equals check above)
        } catch (IllegalArgumentException e) {
            log.debug("Invalid difficulty values: '{}', '{}'", difficulty1, difficulty2);
            return 0.5; // Neutral score for invalid difficulties
        }
    }
    
    /**
     * Map difficulty enum to numeric level for distance calculation.
     */
    private int getDifficultyLevel(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
        };
    }
    
    /**
     * Batch calculate similarities between a target problem and a list of candidates.
     * Returns similarity scores in the same order as candidates.
     * Optimized to avoid O(n^2) complexity by using index-based iteration.
     */
    public List<Double> calculateSimilarities(List<String> targetTags, List<Long> targetCategories, String targetDifficulty,
                                            List<List<String>> candidateTags, List<List<Long>> candidateCategories, 
                                            List<String> candidateDifficulties) {
        if (candidateTags.size() != candidateCategories.size() || candidateTags.size() != candidateDifficulties.size()) {
            throw new IllegalArgumentException("All candidate lists must have the same size");
        }
        
        List<Double> similarities = new ArrayList<>(candidateTags.size());
        
        // Use index-based loop to avoid O(n^2) indexOf operations
        for (int i = 0; i < candidateTags.size(); i++) {
            List<String> tags = candidateTags.get(i);
            List<Long> categories = candidateCategories.get(i);
            String difficulty = candidateDifficulties.get(i);
            
            double similarity = calculateSimilarity(targetTags, tags, targetCategories, categories, targetDifficulty, difficulty);
            similarities.add(similarity);
        }
        
        return similarities;
    }
}
