package com.codetop.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.codetop.entity.FSRSCard;
import com.codetop.entity.Problem;
import com.codetop.entity.ReviewLog;
import com.codetop.mapper.FSRSCardMapper;
import com.codetop.mapper.ProblemMapper;
import com.codetop.mapper.ReviewLogMapper;
import com.codetop.recommendation.dto.DifficultyPref;
import com.codetop.recommendation.dto.DomainSkill;
import com.codetop.recommendation.dto.UserProfile;
import com.codetop.recommendation.config.UserProfilingProperties;
import com.codetop.service.CacheKeyBuilder;
import com.codetop.service.cache.CacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User Profiling Service for generating learning profiles from FSRS data.
 * 
 * Analyzes user's review history to create comprehensive profiles including:
 * - Domain-specific skill assessments
 * - Difficulty preferences and trends
 * - Learning patterns and recommendations
 * - Tag affinity based on interaction frequency
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfilingService {
    
    private final ReviewLogMapper reviewLogMapper;
    private final FSRSCardMapper fsrsCardMapper;
    private final ProblemMapper problemMapper;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final UserProfilingProperties config;
    
    // Constants kept for backward compatibility with tests - now configurable
    @Deprecated
    private static final int DEFAULT_REVIEW_LIMIT = 2000;
    @Deprecated
    private static final int RECENT_DAYS_WINDOW = 90;
    @Deprecated
    private static final double HALF_LIFE_DAYS = 30.0;
    
    public UserProfile getUserProfile(Long userId, boolean useCache) {
        if (userId == null) {
            log.warn("getUserProfile called with null userId, returning default profile");
            return createDefaultProfile(userId);
        }

        String cacheKey = CacheKeyBuilder.userProfile(userId);
        
        // Try cache first if requested and enabled
        if (useCache && config.getCache().isEnabled()) {
            UserProfile cached = cacheService.get(cacheKey, UserProfile.class);
            if (cached != null) {
                log.debug("User profile cache hit for userId={}", userId);
                return cached;
            }
        }
        
        log.debug("User profile cache miss{}, computing profile for userId={}", 
                 useCache ? "" : " (bypassed)", userId);
        
        try {
            long startTime = System.currentTimeMillis();
            UserProfile profile = computeUserProfile(userId);
            long computeTime = System.currentTimeMillis() - startTime;
            log.info("User profile computed in {}ms for userId={}", computeTime, userId);
            
            // Cache the result with configured TTL
            if (config.getCache().isEnabled()) {
                cacheService.put(cacheKey, profile, config.getCache().getTtl());
                log.debug("User profile cached for userId={} with TTL={}", userId, config.getCache().getTtl());
            }
            
            return profile;
        } catch (Exception e) {
            log.error("Failed to compute user profile for userId={}, returning default profile. Error: {}", 
                     userId, e.getMessage(), e);
            return createDefaultProfile(userId);
        }
    }
    
    /**
     * Compute user profile from FSRS data
     */
    protected UserProfile computeUserProfile(Long userId) {
        log.info("Computing user profile for user {}", userId);
        
        // Load recent review data with optimized SQL filtering
        LocalDateTime recentWindowStart = LocalDateTime.now().minusDays(config.getWindows().getRecentDays());
        List<ReviewLog> windowReviews = reviewLogMapper.findRecentByUserIdInWindow(
                userId, recentWindowStart, config.getWindows().getDefaultReviewLimit());
        
        if (windowReviews.isEmpty()) {
            return createDefaultProfile(userId);
        }
        
        // Load associated problems and FSRS cards
        Set<Long> problemIds = windowReviews.stream()
                .map(ReviewLog::getProblemId)
                .collect(Collectors.toSet());
        
        Map<Long, Problem> problemMap = loadProblems(problemIds);
        Map<Long, FSRSCard> cardMap = loadFSRSCards(userId, problemIds);
        
        // Group reviews by domain
        Map<String, List<ReviewLog>> domainReviews = groupReviewsByDomain(windowReviews, problemMap);
        
        // Compute domain skills
        Map<String, DomainSkill> domainSkills = computeDomainSkills(domainReviews, cardMap);
        
        // Compute difficulty preferences
        DifficultyPref difficultyPref = computeDifficultyPreferences(windowReviews, problemMap);
        
        // Compute tag affinity
        Map<String, Double> tagAffinity = computeTagAffinity(windowReviews, problemMap);
        
        // Compute overall metrics
        double overallMastery = computeOverallMastery(domainSkills);
        int totalProblems = problemIds.size();
        int totalAttempts = windowReviews.size();
        double avgAccuracy = computeAverageAccuracy(windowReviews);
        
        // Determine learning pattern
        UserProfile.LearningPattern learningPattern = determineLearningPattern(overallMastery);
        
        return UserProfile.builder()
                .userId(userId)
                .generatedAt(Instant.now())
                .window("recent" + config.getWindows().getRecentDays() + "d")
                .domainSkills(domainSkills)
                .difficultyPref(difficultyPref)
                .tagAffinity(tagAffinity)
                .overallMastery(overallMastery)
                .totalProblemsReviewed(totalProblems)
                .totalReviewAttempts(totalAttempts)
                .averageAccuracy(avgAccuracy)
                .learningPattern(learningPattern)
                .build();
    }
    
    /**
     * Create default profile for users with no data
     */
    private UserProfile createDefaultProfile(Long userId) {
        return UserProfile.builder()
                .userId(userId)
                .generatedAt(Instant.now())
                .window("recent" + config.getWindows().getRecentDays() + "d")
                .domainSkills(new HashMap<>())
                .difficultyPref(DifficultyPref.builder()
                        .easy(0.3)
                        .medium(0.5)
                        .hard(0.2)
                        .trend(DifficultyPref.DifficultyTrend.STABLE)
                        .preferredLevel(DifficultyPref.PreferredLevel.BALANCED)
                        .build())
                .tagAffinity(new HashMap<>())
                .overallMastery(0.5)
                .totalProblemsReviewed(0)
                .totalReviewAttempts(0)
                .averageAccuracy(0.5)
                .learningPattern(UserProfile.LearningPattern.STEADY_PROGRESS)
                .build();
    }
    
    /**
     * Load problems by IDs
     */
    private Map<Long, Problem> loadProblems(Set<Long> problemIds) {
        if (problemIds.isEmpty()) return new HashMap<>();
        
        List<Problem> problems = problemMapper.selectBatchIds(problemIds);
        return problems.stream()
                .collect(Collectors.toMap(Problem::getId, p -> p));
    }
    
    /**
     * Load FSRS cards for user and problems
     */
    private Map<Long, FSRSCard> loadFSRSCards(Long userId, Set<Long> problemIds) {
        if (problemIds.isEmpty()) return new HashMap<>();
        
        // Use wrapper to query cards by user and problem IDs
        QueryWrapper<FSRSCard> wrapper = new QueryWrapper<FSRSCard>()
                .eq("user_id", userId)
                .in("problem_id", problemIds);
        
        List<FSRSCard> cards = fsrsCardMapper.selectList(wrapper);
        return cards.stream()
                .collect(Collectors.toMap(FSRSCard::getProblemId, c -> c));
    }
    
    /**
     * Group reviews by knowledge domain
     */
    private Map<String, List<ReviewLog>> groupReviewsByDomain(List<ReviewLog> reviews, Map<Long, Problem> problemMap) {
        Map<String, List<ReviewLog>> domainGroups = new HashMap<>();
        
        for (ReviewLog review : reviews) {
            Problem problem = problemMap.get(review.getProblemId());
            if (problem == null || problem.getTags() == null) continue;
            
            Set<String> domains = extractDomainsFromTags(problem.getTags());
            
            // Add review to each domain (limit to avoid dilution)
            domains.stream().limit(2).forEach(domain -> {
                domainGroups.computeIfAbsent(domain, k -> new ArrayList<>()).add(review);
            });
        }
        
        return domainGroups;
    }
    
    /**
     * Extract knowledge domains from problem tags JSON
     */
    private Set<String> extractDomainsFromTags(String tagsJson) {
        try {
            List<String> tags = objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
            return tags.stream()
                    .map(tag -> config.getTagDomainMapping().getOrDefault(tag.toLowerCase(), "other"))
                    .filter(domain -> !"other".equals(domain))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Failed to parse tags JSON: {}", tagsJson);
            return Collections.emptySet();
        }
    }
    
    /**
     * Compute domain-specific skill statistics
     */
    private Map<String, DomainSkill> computeDomainSkills(Map<String, List<ReviewLog>> domainReviews, 
                                                        Map<Long, FSRSCard> cardMap) {
        Map<String, DomainSkill> domainSkills = new HashMap<>();
        
        for (Map.Entry<String, List<ReviewLog>> entry : domainReviews.entrySet()) {
            String domain = entry.getKey();
            List<ReviewLog> reviews = entry.getValue();
            
            DomainSkill skill = computeSingleDomainSkill(domain, reviews, cardMap);
            domainSkills.put(domain, skill);
        }
        
        return domainSkills;
    }
    
    /**
     * Compute skill statistics for a single domain
     */
    private DomainSkill computeSingleDomainSkill(String domain, List<ReviewLog> reviews, Map<Long, FSRSCard> cardMap) {
        if (reviews.isEmpty()) {
            return createDefaultDomainSkill(domain);
        }
        
        // Compute time-weighted statistics
        double totalWeight = 0.0;
        double weightedSuccessCount = 0.0;
        double weightedRetentionSum = 0.0;
        double retentionWeightSum = 0.0; // Track sum of weights where retention was observed
        double weightedRtSum = 0.0;
        double rtWeightSum = 0.0; // Track sum of weights where response time was observed
        int lapses = 0;
        int retentionCount = 0;
        
        Set<Long> uniqueProblems = new HashSet<>();
        
        for (ReviewLog review : reviews) {
            // Time decay weight using configurable half-life
            long daysSinceReview = ChronoUnit.DAYS.between(review.getReviewedAt(), LocalDateTime.now());
            double weight = Math.exp(-daysSinceReview / config.getWindows().getHalfLifeDays());
            
            totalWeight += weight;
            
            // Success rate (rating >= 3)
            if (review.getRating() >= 3) {
                weightedSuccessCount += weight;
            }
            
            // Response time
            if (review.getResponseTimeMs() != null && review.getResponseTimeMs() > 0) {
                weightedRtSum += review.getResponseTimeMs() * weight;
                rtWeightSum += weight; // Track weight for RT calculation
            }
            
            // Lapses (rating = 1)
            if (review.getRating() == 1) {
                lapses++;
            }
            
            // Retention probability from FSRS card
            FSRSCard card = cardMap.get(review.getProblemId());
            if (card != null) {
                double retention = card.calculateRetentionProbability();
                if (retention > 0) {
                    weightedRetentionSum += retention * weight;
                    retentionWeightSum += weight; // Track weight for retention calculation
                    retentionCount++;
                }
            }
            
            uniqueProblems.add(review.getProblemId());
        }
        
        // Calculate averages with configurable Beta smoothing
        double accuracy = (weightedSuccessCount + config.getThresholds().getBetaSmoothingAlpha()) / 
                         (totalWeight + config.getThresholds().getBetaSmoothingAlpha() + config.getThresholds().getBetaSmoothingBeta());
        
        double retention = retentionWeightSum > 0 ? weightedRetentionSum / retentionWeightSum : 0.6; // Default neutral
        double lapseRate = (double) lapses / reviews.size();
        long avgRtMs = rtWeightSum > 0 ? Math.round(weightedRtSum / rtWeightSum) : 0L;
        
        // Compute composite skill score
        DomainSkill skill = DomainSkill.builder()
                .domain(domain)
                .samples(uniqueProblems.size())
                .accuracy(accuracy)
                .retention(retention)
                .lapseRate(lapseRate)
                .avgRtMs(avgRtMs)
                .attempts(reviews.size())
                .minSamplesForReliability(config.getThresholds().getMinSamplesForReliability())
                .build();
        
        // Calculate skill score and determine strength level
        double skillScore = computeSkillScore(skill);
        skill.setSkillScore(skillScore);
        skill.setStrength(determineStrengthLevel(skillScore, uniqueProblems.size()));
        
        return skill;
    }
    
    /**
     * Create default domain skill for domains with no data
     */
    private DomainSkill createDefaultDomainSkill(String domain) {
        return DomainSkill.builder()
                .domain(domain)
                .samples(0)
                .accuracy(0.6)
                .retention(0.6)
                .lapseRate(0.2)
                .avgRtMs(0L)
                .attempts(0)
                .skillScore(0.55) // Neutral score
                .strength(DomainSkill.StrengthLevel.NORMAL)
                .minSamplesForReliability(config.getThresholds().getMinSamplesForReliability())
                .build();
    }
    
    /**
     * Compute composite skill score from individual metrics
     */
    private double computeSkillScore(DomainSkill skill) {
        double rtScore = skill.getResponseTimeScore();
        return Math.max(0.0, Math.min(1.0, 
                0.45 * skill.getAccuracy() + 
                0.25 * skill.getRetention() + 
                0.15 * (1.0 - skill.getLapseRate()) + 
                0.15 * rtScore));
    }
    
    /**
     * Determine strength level based on skill score and sample size
     */
    private DomainSkill.StrengthLevel determineStrengthLevel(double skillScore, int samples) {
        if (samples < config.getThresholds().getMinSamplesForReliability()) {
            return DomainSkill.StrengthLevel.NORMAL;
        }
        
        if (skillScore < config.getThresholds().getWeakSkillThreshold()) {
            return DomainSkill.StrengthLevel.WEAK;
        } else if (skillScore > config.getThresholds().getStrongSkillThreshold()) {
            return DomainSkill.StrengthLevel.STRONG;
        } else {
            return DomainSkill.StrengthLevel.NORMAL;
        }
    }
    
    /**
     * Compute difficulty preferences from review history
     */
    private DifficultyPref computeDifficultyPreferences(List<ReviewLog> reviews, Map<Long, Problem> problemMap) {
        Map<String, Integer> difficultyCounts = new HashMap<>();
        
        // Separate recent and prior windows for trend analysis using configurable window size
        LocalDateTime now = LocalDateTime.now();
        int trendDays = config.getWindows().getTrendComparisonDays();
        LocalDateTime recentCutoff = now.minusDays(trendDays);
        LocalDateTime priorCutoff = now.minusDays(trendDays * 2);
        
        Map<String, Integer> recentCounts = new HashMap<>();
        Map<String, Integer> priorCounts = new HashMap<>();
        
        for (ReviewLog review : reviews) {
            Problem problem = problemMap.get(review.getProblemId());
            if (problem != null && problem.getDifficulty() != null) {
                String difficulty = problem.getDifficulty().toString();
                difficultyCounts.merge(difficulty, 1, Integer::sum);
                
                // Track recent vs prior for trend analysis
                LocalDateTime reviewTime = review.getReviewedAt();
                if (reviewTime.isAfter(recentCutoff)) {
                    recentCounts.merge(difficulty, 1, Integer::sum);
                } else if (reviewTime.isAfter(priorCutoff)) {
                    priorCounts.merge(difficulty, 1, Integer::sum);
                }
            }
        }
        
        int total = difficultyCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            // Return default balanced preference
            return DifficultyPref.builder()
                    .easy(0.3)
                    .medium(0.5)
                    .hard(0.2)
                    .trend(DifficultyPref.DifficultyTrend.STABLE)
                    .preferredLevel(DifficultyPref.PreferredLevel.BALANCED)
                    .build();
        }
        
        // Compute difficulty trend by comparing hard problem percentages
        DifficultyPref.DifficultyTrend trend = computeDifficultyTrend(recentCounts, priorCounts);
        
        DifficultyPref pref = DifficultyPref.builder()
                .easy(difficultyCounts.getOrDefault("EASY", 0) / (double) total)
                .medium(difficultyCounts.getOrDefault("MEDIUM", 0) / (double) total)
                .hard(difficultyCounts.getOrDefault("HARD", 0) / (double) total)
                .trend(trend)
                .preferredLevel(determinePreferredLevel(difficultyCounts, total))
                .build();
        
        pref.normalize(); // Ensure proportions sum to 1.0
        return pref;
    }
    
    /**
     * Determine preferred difficulty level
     */
    private DifficultyPref.PreferredLevel determinePreferredLevel(Map<String, Integer> difficultyCounts, int total) {
        double hardRatio = difficultyCounts.getOrDefault("HARD", 0) / (double) total;
        double easyRatio = difficultyCounts.getOrDefault("EASY", 0) / (double) total;
        
        if (hardRatio > 0.4) {
            return DifficultyPref.PreferredLevel.SEEKING_CHALLENGE;
        } else if (easyRatio > 0.5) {
            return DifficultyPref.PreferredLevel.BUILDING_CONFIDENCE;
        } else {
            return DifficultyPref.PreferredLevel.BALANCED;
        }
    }
    
    /**
     * Compute difficulty trend by comparing recent vs prior windows
     */
    private DifficultyPref.DifficultyTrend computeDifficultyTrend(Map<String, Integer> recentCounts, Map<String, Integer> priorCounts) {
        int recentTotal = recentCounts.values().stream().mapToInt(Integer::intValue).sum();
        int priorTotal = priorCounts.values().stream().mapToInt(Integer::intValue).sum();
        
        // Need sufficient data in both windows for meaningful comparison
        if (recentTotal < 5 || priorTotal < 5) {
            return DifficultyPref.DifficultyTrend.STABLE;
        }
        
        // Compare hard problem percentages as indicator of difficulty progression
        double recentHardRatio = recentCounts.getOrDefault("HARD", 0) / (double) recentTotal;
        double priorHardRatio = priorCounts.getOrDefault("HARD", 0) / (double) priorTotal;
        
        double difficultyChange = recentHardRatio - priorHardRatio;
        
        // Use configurable threshold to determine significant trend changes
        final double trendThreshold = config.getThresholds().getDifficultyTrendThreshold();
        
        if (difficultyChange > trendThreshold) {
            return DifficultyPref.DifficultyTrend.INCREASING;
        } else if (difficultyChange < -trendThreshold) {
            return DifficultyPref.DifficultyTrend.DECREASING;
        } else {
            return DifficultyPref.DifficultyTrend.STABLE;
        }
    }
    
    /**
     * Compute tag affinity based on interaction frequency
     */
    private Map<String, Double> computeTagAffinity(List<ReviewLog> reviews, Map<Long, Problem> problemMap) {
        Map<String, Double> tagWeights = new HashMap<>();
        Set<String> seenPairs = new HashSet<>(); // Track unique (problemId, tag) pairs
        
        for (ReviewLog review : reviews) {
            Problem problem = problemMap.get(review.getProblemId());
            if (problem != null && problem.getTags() != null) {
                try {
                    List<String> tags = objectMapper.readValue(problem.getTags(), new TypeReference<List<String>>() {});
                    
                    // Apply time-based weight for recency using configurable half-life
                    long daysSinceReview = ChronoUnit.DAYS.between(review.getReviewedAt(), LocalDateTime.now());
                    double timeWeight = Math.exp(-daysSinceReview / config.getWindows().getHalfLifeDays());
                    
                    for (String tag : tags) {
                        String normalizedTag = tag.toLowerCase();
                        String uniqueKey = review.getProblemId() + "|" + normalizedTag;
                        
                        // Only count each (problem, tag) pair once, but with recency weighting
                        if (!seenPairs.contains(uniqueKey)) {
                            seenPairs.add(uniqueKey);
                            tagWeights.merge(normalizedTag, timeWeight, Double::sum);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse tags for problem {}: {}", problem.getId(), problem.getTags());
                }
            }
        }
        
        // Normalize by maximum weight instead of count
        double maxWeight = tagWeights.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        
        return tagWeights.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() / maxWeight
                ));
    }
    
    /**
     * Compute overall mastery as weighted average of domain skills
     */
    private double computeOverallMastery(Map<String, DomainSkill> domainSkills) {
        if (domainSkills.isEmpty()) return 0.5;
        
        double totalWeight = 0.0;
        double weightedSum = 0.0;
        
        for (DomainSkill skill : domainSkills.values()) {
            // Weight by sample size, with minimum weight for reliability
            double weight = Math.max(1.0, skill.getSamples());
            totalWeight += weight;
            weightedSum += skill.getSkillScore() * weight;
        }
        
        return totalWeight > 0 ? weightedSum / totalWeight : 0.5;
    }
    
    /**
     * Compute average accuracy across all reviews
     */
    private double computeAverageAccuracy(List<ReviewLog> reviews) {
        if (reviews.isEmpty()) return 0.5;
        
        long successCount = reviews.stream().mapToLong(r -> r.getRating() >= 3 ? 1 : 0).sum();
        return (successCount + config.getThresholds().getBetaSmoothingAlpha()) / 
               (reviews.size() + config.getThresholds().getBetaSmoothingAlpha() + config.getThresholds().getBetaSmoothingBeta());
    }
    
    /**
     * Determine learning pattern based on overall mastery
     */
    private UserProfile.LearningPattern determineLearningPattern(double overallMastery) {
        if (overallMastery < 0.4) {
            return UserProfile.LearningPattern.STRUGGLING;
        } else if (overallMastery > 0.7) {
            return UserProfile.LearningPattern.ADVANCED;
        } else {
            return UserProfile.LearningPattern.STEADY_PROGRESS;
        }
    }
    
    /**
     * Invalidate user profile cache
     */
    public void invalidateUserProfileCache(Long userId) {
        String cacheKey = CacheKeyBuilder.userProfile(userId);
        cacheService.delete(cacheKey);
        log.debug("Invalidated user profile cache for user {}", userId);
    }
}
