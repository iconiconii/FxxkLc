package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.CategoryMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Category metadata service for problem feature enhancement (P1).
 * Provides default complexity and representative techniques for categories.
 */
@Service
@Slf4j
public class CategoryMetadataService {
    
    private final Map<Long, CategoryMetadata> categoryMetadataMap = new HashMap<>();
    private final Map<String, CategoryMetadata> categoryNameMap = new HashMap<>();
    
    @PostConstruct
    private void initializeCategoryMetadata() {
        log.info("Initializing category metadata for problem feature enhancement");
        
        // Arrays category
        addCategoryMetadata(1L, "arrays", 
            CategoryMetadata.builder()
                .categoryId(1L)
                .name("arrays")
                .defaultComplexity(1) // Easy to Medium
                .complexityDescription("Basic to intermediate array manipulation")
                .representativeTechniques(Arrays.asList(
                    "Two pointers", "Sliding window", "Prefix sum", "Binary search"
                ))
                .commonPatterns(Arrays.asList(
                    "Linear scan", "Two-pass algorithm", "In-place modification"
                ))
                .typicalTimeComplexity("O(n) to O(n²)")
                .typicalSpaceComplexity("O(1) to O(n)")
                .learningDifficulty("beginner")
                .prerequisites(Collections.emptyList())
                .relatedCategories(Arrays.asList("two_pointers", "sorting"))
                .build());
        
        // Linked Lists category
        addCategoryMetadata(2L, "linked_lists",
            CategoryMetadata.builder()
                .categoryId(2L)
                .name("linked_lists")
                .defaultComplexity(2) // Medium
                .complexityDescription("Pointer manipulation and list operations")
                .representativeTechniques(Arrays.asList(
                    "Fast and slow pointers", "Dummy head", "Reverse operations"
                ))
                .commonPatterns(Arrays.asList(
                    "Cycle detection", "List merging", "In-place reversal"
                ))
                .typicalTimeComplexity("O(n)")
                .typicalSpaceComplexity("O(1)")
                .learningDifficulty("beginner")
                .prerequisites(Collections.emptyList())
                .relatedCategories(Arrays.asList("two_pointers"))
                .build());
        
        // Hash Tables category
        addCategoryMetadata(3L, "hash_tables",
            CategoryMetadata.builder()
                .categoryId(3L)
                .name("hash_tables")
                .defaultComplexity(1) // Easy to Medium
                .complexityDescription("Key-value mapping and fast lookups")
                .representativeTechniques(Arrays.asList(
                    "Frequency counting", "Key mapping", "Set operations"
                ))
                .commonPatterns(Arrays.asList(
                    "Count and lookup", "Deduplication", "Grouping"
                ))
                .typicalTimeComplexity("O(n)")
                .typicalSpaceComplexity("O(n)")
                .learningDifficulty("beginner")
                .prerequisites(Collections.emptyList())
                .relatedCategories(Arrays.asList("arrays", "strings"))
                .build());
        
        // Trees category
        addCategoryMetadata(4L, "trees",
            CategoryMetadata.builder()
                .categoryId(4L)
                .name("trees")
                .defaultComplexity(2) // Medium to Hard
                .complexityDescription("Hierarchical data structures and traversals")
                .representativeTechniques(Arrays.asList(
                    "DFS traversal", "BFS traversal", "Recursion", "Tree construction"
                ))
                .commonPatterns(Arrays.asList(
                    "Recursive solutions", "Level-order processing", "Path finding"
                ))
                .typicalTimeComplexity("O(n)")
                .typicalSpaceComplexity("O(h) where h is height")
                .learningDifficulty("intermediate")
                .prerequisites(Arrays.asList("recursion"))
                .relatedCategories(Arrays.asList("graphs", "recursion"))
                .build());
        
        // Graphs category
        addCategoryMetadata(5L, "graphs",
            CategoryMetadata.builder()
                .categoryId(5L)
                .name("graphs")
                .defaultComplexity(3) // Hard
                .complexityDescription("Complex graph algorithms and pathfinding")
                .representativeTechniques(Arrays.asList(
                    "DFS", "BFS", "Dijkstra", "Union-Find", "Topological sort"
                ))
                .commonPatterns(Arrays.asList(
                    "Graph traversal", "Shortest path", "Connected components"
                ))
                .typicalTimeComplexity("O(V + E)")
                .typicalSpaceComplexity("O(V)")
                .learningDifficulty("advanced")
                .prerequisites(Arrays.asList("trees", "recursion"))
                .relatedCategories(Arrays.asList("trees", "dynamic_programming"))
                .build());
        
        // Dynamic Programming category
        addCategoryMetadata(6L, "dynamic_programming",
            CategoryMetadata.builder()
                .categoryId(6L)
                .name("dynamic_programming")
                .defaultComplexity(3) // Hard
                .complexityDescription("Optimization problems with overlapping subproblems")
                .representativeTechniques(Arrays.asList(
                    "Memoization", "Bottom-up DP", "State compression", "DP on trees"
                ))
                .commonPatterns(Arrays.asList(
                    "1D/2D DP", "State transition", "Optimization problems"
                ))
                .typicalTimeComplexity("O(n²) to O(n³)")
                .typicalSpaceComplexity("O(n) to O(n²)")
                .learningDifficulty("advanced")
                .prerequisites(Arrays.asList("recursion"))
                .relatedCategories(Arrays.asList("recursion", "graphs"))
                .build());
        
        // Two Pointers category
        addCategoryMetadata(7L, "two_pointers",
            CategoryMetadata.builder()
                .categoryId(7L)
                .name("two_pointers")
                .defaultComplexity(2) // Medium
                .complexityDescription("Efficient array and string processing")
                .representativeTechniques(Arrays.asList(
                    "Left-right pointers", "Fast-slow pointers", "Sliding window"
                ))
                .commonPatterns(Arrays.asList(
                    "Target sum problems", "Palindrome checking", "Cycle detection"
                ))
                .typicalTimeComplexity("O(n)")
                .typicalSpaceComplexity("O(1)")
                .learningDifficulty("intermediate")
                .prerequisites(Arrays.asList("arrays"))
                .relatedCategories(Arrays.asList("arrays", "strings"))
                .build());
        
        // Binary Search category
        addCategoryMetadata(8L, "binary_search",
            CategoryMetadata.builder()
                .categoryId(8L)
                .name("binary_search")
                .defaultComplexity(2) // Medium
                .complexityDescription("Logarithmic search in sorted data")
                .representativeTechniques(Arrays.asList(
                    "Classic binary search", "Search space reduction", "Template matching"
                ))
                .commonPatterns(Arrays.asList(
                    "Find target", "Find boundary", "Minimize/maximize"
                ))
                .typicalTimeComplexity("O(log n)")
                .typicalSpaceComplexity("O(1)")
                .learningDifficulty("intermediate")
                .prerequisites(Arrays.asList("arrays"))
                .relatedCategories(Arrays.asList("arrays", "divide_conquer"))
                .build());
        
        log.info("Initialized {} category metadata entries", categoryMetadataMap.size());
    }
    
    private void addCategoryMetadata(Long categoryId, String categoryName, CategoryMetadata metadata) {
        categoryMetadataMap.put(categoryId, metadata);
        categoryNameMap.put(categoryName, metadata);
    }
    
    /**
     * Get category metadata by category ID
     */
    public Optional<CategoryMetadata> getCategoryMetadata(Long categoryId) {
        return Optional.ofNullable(categoryMetadataMap.get(categoryId));
    }
    
    /**
     * Get category metadata by category name
     */
    public Optional<CategoryMetadata> getCategoryMetadata(String categoryName) {
        return Optional.ofNullable(categoryNameMap.get(categoryName));
    }
    
    /**
     * Get metadata for multiple categories
     */
    public List<CategoryMetadata> getCategoryMetadata(List<Long> categoryIds) {
        return categoryIds.stream()
                .map(this::getCategoryMetadata)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
    
    /**
     * Get all available category metadata
     */
    public Collection<CategoryMetadata> getAllCategoryMetadata() {
        return categoryMetadataMap.values();
    }
    
    /**
     * Get categories by learning difficulty
     */
    public List<CategoryMetadata> getCategoriesByDifficulty(String learningDifficulty) {
        return categoryMetadataMap.values().stream()
                .filter(meta -> learningDifficulty.equals(meta.getLearningDifficulty()))
                .toList();
    }
    
    /**
     * Get categories that are prerequisites for the given category
     */
    public List<CategoryMetadata> getPrerequisites(Long categoryId) {
        return getCategoryMetadata(categoryId)
                .map(meta -> meta.getPrerequisites().stream()
                        .map(this::getCategoryMetadata)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList())
                .orElse(Collections.emptyList());
    }
    
    /**
     * Get related categories
     */
    public List<CategoryMetadata> getRelatedCategories(Long categoryId) {
        return getCategoryMetadata(categoryId)
                .map(meta -> meta.getRelatedCategories().stream()
                        .map(this::getCategoryMetadata)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList())
                .orElse(Collections.emptyList());
    }
}
