package com.codetop.recommendation.alg;

import com.codetop.mapper.FSRSCardMapper;
import com.codetop.mapper.ProblemMapper;
import com.codetop.entity.Problem;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.service.FSRSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateBuilder {
    private final FSRSService fsrsService;
    private final ProblemMapper problemMapper;
    private final TagsParser tagsParser;

    /**
     * Build candidate problems for a user using FSRS signals.
     * Returns up to 'limit' items prioritizing due/learning/relearn cards.
     */
    public List<LlmProvider.ProblemCandidate> buildForUser(Long userId, int limit) {
        int cap = Math.max(1, Math.min(50, limit));
        try {
            List<FSRSCardMapper.ReviewQueueCard> cards = fsrsService.getAllDueProblems(userId, cap);
            List<LlmProvider.ProblemCandidate> out = new ArrayList<>();
            for (FSRSCardMapper.ReviewQueueCard c : cards) {
                out.add(toCandidate(c));
            }
            // Ensure stable order: more urgent first (earlier nextReview), then lower accuracy
            out.sort(Comparator
                    .comparing((LlmProvider.ProblemCandidate pc) -> pc.attempts == null ? 0 : pc.attempts)
                    .thenComparing(pc -> pc.recentAccuracy == null ? 0.0 : pc.recentAccuracy));
            if (out.size() > cap) {
                out = out.subList(0, cap);
            }
            if (out.isEmpty()) {
                // Fallback: seed from recent problems when FSRS data not available (e.g., dev/H2)
                List<Problem> recent = recentProblems(cap);
                for (Problem p : recent) {
                    LlmProvider.ProblemCandidate pc = new LlmProvider.ProblemCandidate();
                    pc.id = p.getId().longValue();
                    pc.topic = p.getTitle();
                    pc.difficulty = p.getDifficulty() != null ? p.getDifficulty().name() : null;
                    pc.tags = List.of();
                    pc.attempts = 0;
                    pc.recentAccuracy = 0.5; // neutral prior
                    out.add(pc);
                }
            }
            
            // P0 Enhancement: Load tags for all candidates
            enhanceCandidatesWithTags(out);
            
            return out;
        } catch (Exception e) {
            log.warn("CandidateBuilder FSRS path failed for user {}: {} — using recent problems fallback", userId, e.getMessage());
            List<LlmProvider.ProblemCandidate> out = new ArrayList<>();
            List<Problem> recent = recentProblems(cap);
            for (Problem p : recent) {
                LlmProvider.ProblemCandidate pc = new LlmProvider.ProblemCandidate();
                pc.id = p.getId().longValue();
                pc.topic = p.getTitle();
                pc.difficulty = p.getDifficulty() != null ? p.getDifficulty().name() : null;
                pc.tags = List.of();
                pc.attempts = 0;
                pc.recentAccuracy = 0.5;
                out.add(pc);
            }
            
            // P0 Enhancement: Load tags for fallback candidates too
            enhanceCandidatesWithTags(out);
            
            return out;
        }
    }

    private List<Problem> recentProblems(int cap) {
        try {
            List<Problem> minimal = problemMapper.findRecentProblemsMinimal(cap);
            return minimal != null ? minimal : List.of();
        } catch (Exception e) {
            log.warn("Recent problems fallback failed: {}", e.getMessage());
            return List.of();
        }
    }

    private LlmProvider.ProblemCandidate toCandidate(FSRSCardMapper.ReviewQueueCard card) {
        LlmProvider.ProblemCandidate pc = new LlmProvider.ProblemCandidate();
        pc.id = card.getProblemId();
        pc.topic = card.getProblemTitle();
        pc.difficulty = card.getProblemDifficulty();
        pc.tags = List.of(); // tags not loaded here to keep query light
        pc.attempts = card.getReviewCount() != null ? card.getReviewCount() : 0;
        pc.recentAccuracy = estimateAccuracy(card);
        
        // Calculate FSRS urgency signals (v3 enhancement)
        pc.retentionProbability = calculateRetentionProbability(card);
        pc.daysOverdue = card.getDaysOverdue(); // Already handles null internally
        pc.urgencyScore = calculateUrgencyScore(pc.retentionProbability, pc.daysOverdue);
        
        return pc;
    }

    /**
     * Heuristic estimation adapted from ProblemService.calculateAccuracyFromFSRS, scaled to 0..1.
     */
    private double estimateAccuracy(FSRSCardMapper.ReviewQueueCard card) {
        double stability = safe(card.getStability());
        double difficulty = safe(card.getDifficulty());
        int reviewCount = card.getReviewCount() != null ? card.getReviewCount() : 0;
        int lapses = card.getLapses() != null ? card.getLapses() : 0;

        double stabilityScore = Math.min(stability / 30.0, 1.0);
        double difficultyPenalty = Math.min(difficulty / 10.0, 0.5);
        double experienceBonus = Math.min(reviewCount * 0.02, 0.2);
        double lapsePenalty = Math.min(lapses * 0.1, 0.4);
        double accuracy = 0.3 + (stabilityScore * 0.7) - difficultyPenalty + experienceBonus - lapsePenalty;
        return Math.max(0.0, Math.min(1.0, accuracy));
    }

    private double safe(BigDecimal v) { return v != null ? v.doubleValue() : 0.0; }

    
    /**
     * Calculate retention probability from FSRS card data.
     * Uses the card's built-in calculation if available, otherwise estimates.
     */
    private double calculateRetentionProbability(FSRSCardMapper.ReviewQueueCard card) {
        try {
            // Try to use the card's built-in retention probability calculation
            return card.calculateRetentionProbability();
        } catch (Exception e) {
            log.debug("Failed to calculate retention probability for card {}, using fallback", card.getProblemId());
            
            // Fallback calculation based on stability and elapsed time
            double stability = safe(card.getStability());
            int elapsedDays = card.getElapsedDays() != null ? card.getElapsedDays() : 0;
            
            if (stability <= 0) {
                return 0.5; // Default for cards with no stability data
            }
            
            // Simple retention calculation: R = exp(-elapsedDays/stability)
            double retention = Math.exp(-elapsedDays / stability);
            return Math.max(0.0, Math.min(1.0, retention)); // Clamp to [0,1]
        }
    }
    
    /**
     * Calculate normalized urgency score combining retention probability and overdue status.
     * Higher score indicates more urgent need for review.
     * 
     * @param retentionProbability Current retention probability (0-1)
     * @param daysOverdue Days overdue for review (0+)
     * @return Urgency score (0-1) where 1 is most urgent
     */
    private double calculateUrgencyScore(double retentionProbability, int daysOverdue) {
        // Base urgency from low retention (1 - retention)
        double baseUrgency = 1.0 - retentionProbability;
        
        // Add overdue bonus - more days overdue increases urgency
        double overdueBonus = 0.0;
        if (daysOverdue > 0) {
            // Logarithmic scaling to prevent extreme values
            overdueBonus = Math.min(0.3, Math.log(daysOverdue + 1) / 10.0);
        }
        
        // Combine and clamp to [0,1]
        double urgency = baseUrgency + overdueBonus;
        return Math.max(0.0, Math.min(1.0, urgency));
    }

    /**
     * P0 Enhancement: Load tags for candidates using lightweight batch query.
     * Updates candidates in-place with parsed tags from JSON.
     */
    private void enhanceCandidatesWithTags(List<LlmProvider.ProblemCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        
        try {
            // Extract problem IDs
            List<Long> problemIds = candidates.stream()
                    .map(c -> c.id)
                    .collect(Collectors.toList());
            
            // Batch load tags
            List<ProblemMapper.ProblemTagsMinimal> problemTags = problemMapper.findTagsByProblemIds(problemIds);
            
            // Create lookup map for efficiency
            Map<Long, List<String>> tagsMap = problemTags.stream()
                    .collect(Collectors.toMap(
                            ProblemMapper.ProblemTagsMinimal::getId,
                            pt -> tagsParser.parseTagsFromJson(pt.getTags())
                    ));
            
            // Enhance candidates with tags
            for (LlmProvider.ProblemCandidate candidate : candidates) {
                candidate.tags = tagsMap.getOrDefault(candidate.id, List.of());
            }
            
            log.debug("Enhanced {} candidates with tags, {} problems had tags data", 
                    candidates.size(), problemTags.size());
        } catch (Exception e) {
            log.warn("Failed to enhance candidates with tags: {}", e.getMessage());
            // Leave tags empty on failure - graceful degradation
        }
    }
}
