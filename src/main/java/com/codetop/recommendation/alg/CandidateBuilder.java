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

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateBuilder {
    private final FSRSService fsrsService;
    private final ProblemMapper problemMapper;

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
                return out.subList(0, cap);
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
            return out.size() > cap ? out.subList(0, cap) : out;
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
            return out;
        }
    }

    private List<Problem> recentProblems(int cap) {
        List<Problem> minimal = problemMapper.findRecentProblemsMinimal(cap);
        return minimal != null ? minimal : List.of();
    }

    private LlmProvider.ProblemCandidate toCandidate(FSRSCardMapper.ReviewQueueCard card) {
        LlmProvider.ProblemCandidate pc = new LlmProvider.ProblemCandidate();
        pc.id = card.getProblemId();
        pc.topic = card.getProblemTitle();
        pc.difficulty = card.getProblemDifficulty();
        pc.tags = List.of(); // tags not loaded here to keep query light
        pc.attempts = card.getReviewCount() != null ? card.getReviewCount() : 0;
        pc.recentAccuracy = estimateAccuracy(card);
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
}
