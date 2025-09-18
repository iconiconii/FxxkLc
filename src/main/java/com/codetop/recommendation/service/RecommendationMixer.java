package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.RecommendationItemDTO;
import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.config.MixingStrategyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-dimensional recommendation mixer that combines different learning strategies
 * based on learning objectives and user preferences.
 * 
 * Implements strategic quota allocation across multiple recommendation dimensions:
 * - Weakness-focused recommendations for skill gaps
 * - Progressive difficulty for learning curves
 * - Topic coverage for comprehensive mastery
 * - Exam preparation for targeted practice
 * - Mastery refresh for retention
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationMixer {
    
    private final MixingStrategyProperties mixingConfig;
    
    /**
     * Mix recommendations using multi-dimensional strategy based on learning objective.
     * 
     * @param hybridItems Pre-ranked items from HybridRankingService
     * @param candidateMap Problem candidate features
     * @param userProfile User learning profile and preferences  
     * @param objective Primary learning objective for strategy selection
     * @param totalLimit Total number of recommendations to return
     * @return Mixed recommendations with strategic distribution
     */
    public List<RecommendationItemDTO> mixRecommendations(
            List<RecommendationItemDTO> hybridItems,
            Map<Long, LlmProvider.ProblemCandidate> candidateMap,
            UserProfile userProfile,
            LearningObjective objective,
            int totalLimit) {
        
        if (hybridItems == null || hybridItems.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (!mixingConfig.isEnabled()) {
            log.debug("Mixing strategy disabled, returning top {} hybrid items", totalLimit);
            return hybridItems.subList(0, Math.min(totalLimit, hybridItems.size()));
        }
        
        log.debug("Mixing {} recommendations with {} strategy for {} items", 
                totalLimit, objective, hybridItems.size());
        
        // Get strategy quotas based on learning objective
        StrategyQuotas quotas = calculateStrategyQuotas(objective, userProfile, totalLimit);
        
        // Categorize items by strategy dimensions
        CategorizedItems categorized = categorizeItemsByStrategy(hybridItems, candidateMap, userProfile);
        
        // Allocate items according to quotas
        List<RecommendationItemDTO> mixedItems = new ArrayList<>();
        
        // 1. Weakness-focused allocation
        if (quotas.weaknessFocus > 0) {
            List<RecommendationItemDTO> weaknessItems = selectWeaknessItems(
                    categorized.weaknessCandidates, quotas.weaknessFocus);
            addWithSource(mixedItems, weaknessItems, "WEAKNESS_FOCUS");
        }
        
        // 2. Progressive difficulty allocation  
        if (quotas.progressiveDifficulty > 0) {
            List<RecommendationItemDTO> difficultyItems = selectProgressiveDifficultyItems(
                    categorized.progressiveCandidates, userProfile, quotas.progressiveDifficulty);
            addWithSource(mixedItems, difficultyItems, "PROGRESSIVE");
        }
        
        // 3. Topic coverage allocation
        if (quotas.topicCoverage > 0) {
            List<RecommendationItemDTO> coverageItems = selectTopicCoverageItems(
                    categorized.coverageCandidates, candidateMap, userProfile, quotas.topicCoverage);
            addWithSource(mixedItems, coverageItems, "COVERAGE");
        }
        
        // 4. Exam preparation allocation
        if (quotas.examPrep > 0) {
            List<RecommendationItemDTO> examItems = selectExamPrepItems(
                    categorized.examCandidates, userProfile, quotas.examPrep);
            addWithSource(mixedItems, examItems, "EXAM_PREP");
        }
        
        // 5. Mastery refresh allocation
        if (quotas.masteryRefresh > 0) {
            List<RecommendationItemDTO> refreshItems = selectMasteryRefreshItems(
                    categorized.refreshCandidates, userProfile, quotas.masteryRefresh);
            addWithSource(mixedItems, refreshItems, "MASTERY_REFRESH");
        }
        
        // 6. Fill remaining slots with top hybrid items
        if (mixedItems.size() < totalLimit) {
            int remaining = totalLimit - mixedItems.size();
            Set<Long> usedIds = mixedItems.stream()
                    .map(RecommendationItemDTO::getProblemId)
                    .collect(Collectors.toSet());
            
            List<RecommendationItemDTO> fillItems = hybridItems.stream()
                    .filter(item -> !usedIds.contains(item.getProblemId()))
                    .limit(remaining)
                    .collect(Collectors.toList());
            
            addWithSource(mixedItems, fillItems, "HYBRID_FILL");
        }
        
        // Re-sort final list by hybrid score to maintain quality
        mixedItems.sort(Comparator.comparingDouble(
                (RecommendationItemDTO item) -> item.getScore() != null ? item.getScore() : 0.0
        ).reversed());
        
        log.debug("Mixed recommendations: weakness={}, progressive={}, coverage={}, exam={}, refresh={}, fill={}, total={}",
                quotas.weaknessFocus, quotas.progressiveDifficulty, quotas.topicCoverage, 
                quotas.examPrep, quotas.masteryRefresh, 
                Math.max(0, totalLimit - quotas.getTotalQuota()), mixedItems.size());
        
        return mixedItems.subList(0, Math.min(totalLimit, mixedItems.size()));
    }
    
    /**
     * Calculate strategy quotas based on learning objective and user profile.
     */
    private StrategyQuotas calculateStrategyQuotas(LearningObjective objective, UserProfile userProfile, int totalLimit) {
        StrategyQuotas quotas = new StrategyQuotas();
        
        switch (objective) {
            case WEAKNESS_FOCUS:
                quotas.weaknessFocus = (int)(totalLimit * 0.6);
                quotas.progressiveDifficulty = (int)(totalLimit * 0.2);
                quotas.topicCoverage = (int)(totalLimit * 0.2);
                break;
                
            case PROGRESSIVE_DIFFICULTY:
                quotas.progressiveDifficulty = (int)(totalLimit * 0.5);
                quotas.weaknessFocus = (int)(totalLimit * 0.3);
                quotas.topicCoverage = (int)(totalLimit * 0.2);
                break;
                
            case TOPIC_COVERAGE:
                quotas.topicCoverage = (int)(totalLimit * 0.5);
                quotas.progressiveDifficulty = (int)(totalLimit * 0.3);
                quotas.weaknessFocus = (int)(totalLimit * 0.2);
                break;
                
            case EXAM_PREP:
                quotas.examPrep = (int)(totalLimit * 0.6);
                quotas.weaknessFocus = (int)(totalLimit * 0.25);
                quotas.masteryRefresh = (int)(totalLimit * 0.15);
                break;
                
            case REFRESH_MASTERED:
                quotas.masteryRefresh = (int)(totalLimit * 0.6);
                quotas.topicCoverage = (int)(totalLimit * 0.25);
                quotas.progressiveDifficulty = (int)(totalLimit * 0.15);
                break;
                
            default:
                // Balanced allocation for unknown objectives
                quotas.weaknessFocus = (int)(totalLimit * 0.25);
                quotas.progressiveDifficulty = (int)(totalLimit * 0.25);
                quotas.topicCoverage = (int)(totalLimit * 0.25);
                quotas.masteryRefresh = (int)(totalLimit * 0.25);
        }
        
        return quotas;
    }
    
    /**
     * Categorize items by strategy dimensions for targeted selection.
     */
    private CategorizedItems categorizeItemsByStrategy(
            List<RecommendationItemDTO> hybridItems,
            Map<Long, LlmProvider.ProblemCandidate> candidateMap,
            UserProfile userProfile) {
        
        CategorizedItems categorized = new CategorizedItems();
        
        for (RecommendationItemDTO item : hybridItems) {
            LlmProvider.ProblemCandidate candidate = candidateMap.get(item.getProblemId());
            if (candidate == null) continue;
            
            // Weakness candidates: problems in user's weak domains
            if (isWeaknessCandidate(candidate, userProfile)) {
                categorized.weaknessCandidates.add(item);
            }
            
            // Progressive difficulty candidates: problems matching learning curve
            if (isProgressiveCandidate(candidate, userProfile)) {
                categorized.progressiveCandidates.add(item);
            }
            
            // Topic coverage candidates: problems in underrepresented domains
            if (isTopicCoverageCandidate(candidate, userProfile)) {
                categorized.coverageCandidates.add(item);
            }
            
            // Exam preparation candidates: high-frequency, interview-style problems
            if (isExamPrepCandidate(candidate, userProfile)) {
                categorized.examCandidates.add(item);
            }
            
            // Mastery refresh candidates: previously mastered problems needing review
            if (isMasteryRefreshCandidate(candidate, userProfile)) {
                categorized.refreshCandidates.add(item);
            }
        }
        
        log.debug("Categorized items: weakness={}, progressive={}, coverage={}, exam={}, refresh={}",
                categorized.weaknessCandidates.size(), categorized.progressiveCandidates.size(),
                categorized.coverageCandidates.size(), categorized.examCandidates.size(),
                categorized.refreshCandidates.size());
        
        return categorized;
    }
    
    /**
     * Select items focusing on user's weak domains.
     */
    private List<RecommendationItemDTO> selectWeaknessItems(
            List<RecommendationItemDTO> candidates, int limit) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble(
                        (RecommendationItemDTO item) -> item.getScore() != null ? item.getScore() : 0.0
                ).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Select items for progressive difficulty learning.
     */
    private List<RecommendationItemDTO> selectProgressiveDifficultyItems(
            List<RecommendationItemDTO> candidates, UserProfile userProfile, int limit) {
        // Sort by difficulty appropriateness for user's current level
        return candidates.stream()
                .sorted((a, b) -> {
                    // Prefer problems slightly above current mastery level
                    double scoreA = a.getScore() != null ? a.getScore() : 0.0;
                    double scoreB = b.getScore() != null ? b.getScore() : 0.0;
                    return Double.compare(scoreB, scoreA);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Select items for comprehensive topic coverage.
     */
    private List<RecommendationItemDTO> selectTopicCoverageItems(
            List<RecommendationItemDTO> candidates, 
            Map<Long, LlmProvider.ProblemCandidate> candidateMap,
            UserProfile userProfile, int limit) {
        // Diversify by topics to ensure broad coverage
        Map<String, List<RecommendationItemDTO>> byTopic = new HashMap<>();
        
        for (RecommendationItemDTO item : candidates) {
            // Use candidate tags for reliable topic extraction
            String topic = extractTopicFromCandidate(item, candidateMap);
            byTopic.computeIfAbsent(topic, k -> new ArrayList<>()).add(item);
        }
        
        // Round-robin selection across topics
        List<RecommendationItemDTO> selected = new ArrayList<>();
        List<String> topics = new ArrayList<>(byTopic.keySet());
        int topicIndex = 0;
        
        while (selected.size() < limit && !byTopic.isEmpty()) {
            String topic = topics.get(topicIndex % topics.size());
            List<RecommendationItemDTO> topicItems = byTopic.get(topic);
            
            if (!topicItems.isEmpty()) {
                selected.add(topicItems.remove(0));
                if (topicItems.isEmpty()) {
                    byTopic.remove(topic);
                    topics.remove(topic);
                    if (topics.isEmpty()) break;
                }
            }
            
            topicIndex++;
        }
        
        return selected;
    }
    
    /**
     * Select items optimized for exam preparation.
     */
    private List<RecommendationItemDTO> selectExamPrepItems(
            List<RecommendationItemDTO> candidates, UserProfile userProfile, int limit) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble(
                        (RecommendationItemDTO item) -> item.getScore() != null ? item.getScore() : 0.0
                ).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Select items for mastery refresh and retention.
     */
    private List<RecommendationItemDTO> selectMasteryRefreshItems(
            List<RecommendationItemDTO> candidates, UserProfile userProfile, int limit) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble(
                        (RecommendationItemDTO item) -> item.getScore() != null ? item.getScore() : 0.0
                ).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Helper methods for candidate classification
     */
    private boolean isWeaknessCandidate(LlmProvider.ProblemCandidate candidate, UserProfile userProfile) {
        if (candidate.tags == null || candidate.tags.isEmpty() || userProfile == null) {
            return false;
        }
        
        // Check if problem tags overlap with user's weak domains
        Set<String> candidateTags = new HashSet<>(candidate.tags);
        Set<String> weakDomains = new HashSet<>(userProfile.getWeakDomains());
        candidateTags.retainAll(weakDomains);
        
        return !candidateTags.isEmpty();
    }
    
    private boolean isProgressiveCandidate(LlmProvider.ProblemCandidate candidate, UserProfile userProfile) {
        if (candidate.difficulty == null || userProfile == null || userProfile.getDifficultyPref() == null) {
            return true; // Default to include when data unavailable
        }
        
        String targetDifficulty = userProfile.getDifficultyPref().getPreferredLevel().name();
        return candidate.difficulty.equals(targetDifficulty) || isAdjacentDifficulty(candidate.difficulty, targetDifficulty);
    }
    
    private boolean isTopicCoverageCandidate(LlmProvider.ProblemCandidate candidate, UserProfile userProfile) {
        // All candidates can contribute to topic coverage
        return true;
    }
    
    private boolean isExamPrepCandidate(LlmProvider.ProblemCandidate candidate, UserProfile userProfile) {
        // High-frequency problems are good for exam prep
        return candidate.attempts != null && candidate.attempts >= 0;
    }
    
    private boolean isMasteryRefreshCandidate(LlmProvider.ProblemCandidate candidate, UserProfile userProfile) {
        // Problems with good past accuracy are candidates for refresh
        return candidate.recentAccuracy != null && candidate.recentAccuracy >= 0.7;
    }
    
    private boolean isAdjacentDifficulty(String candidateDifficulty, String targetDifficulty) {
        List<String> difficulties = Arrays.asList("EASY", "MEDIUM", "HARD");
        int candidateIndex = difficulties.indexOf(candidateDifficulty);
        int targetIndex = difficulties.indexOf(targetDifficulty);
        
        return candidateIndex != -1 && targetIndex != -1 && Math.abs(candidateIndex - targetIndex) <= 1;
    }
    
    /**
     * Extract topic from candidate using tags/categories instead of parsing reason.
     */
    private String extractTopicFromCandidate(RecommendationItemDTO item, 
                                           Map<Long, LlmProvider.ProblemCandidate> candidateMap) {
        if (item.getProblemId() != null && candidateMap != null) {
            LlmProvider.ProblemCandidate candidate = candidateMap.get(item.getProblemId());
            if (candidate != null) {
                // Use first available tag as topic (most specific domain)
                if (candidate.tags != null && !candidate.tags.isEmpty()) {
                    return candidate.tags.get(0);
                }
                // Fallback to candidate topic if available
                if (candidate.topic != null && !candidate.topic.isEmpty()) {
                    return candidate.topic;
                }
            }
        }
        
        // Legacy fallback: use source or extract from reason as last resort
        if (item.getSource() != null) {
            return item.getSource();
        }
        
        // Final fallback for edge cases
        return "general";
    }
    
    private void addWithSource(List<RecommendationItemDTO> target, 
                              List<RecommendationItemDTO> items, 
                              String sourceTag) {
        for (RecommendationItemDTO item : items) {
            // Enhance source with strategy information
            String currentSource = item.getSource() != null ? item.getSource() : "UNKNOWN";
            item.setSource(currentSource + "_" + sourceTag);
            target.add(item);
        }
    }
    
    /**
     * Internal data classes for organization
     */
    private static class StrategyQuotas {
        int weaknessFocus = 0;
        int progressiveDifficulty = 0;
        int topicCoverage = 0;
        int examPrep = 0;
        int masteryRefresh = 0;
        
        int getTotalQuota() {
            return weaknessFocus + progressiveDifficulty + topicCoverage + examPrep + masteryRefresh;
        }
    }
    
    private static class CategorizedItems {
        List<RecommendationItemDTO> weaknessCandidates = new ArrayList<>();
        List<RecommendationItemDTO> progressiveCandidates = new ArrayList<>();
        List<RecommendationItemDTO> coverageCandidates = new ArrayList<>();
        List<RecommendationItemDTO> examCandidates = new ArrayList<>();
        List<RecommendationItemDTO> refreshCandidates = new ArrayList<>();
    }
}
