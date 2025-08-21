package com.codetop.service;

import org.springframework.stereotype.Component;
import java.util.StringJoiner;

/**
 * Enterprise Redis Cache Key Builder
 * 
 * Key Pattern: codetop:domain:operation:params
 * Examples:
 * - codetop:problem:single:123
 * - codetop:problem:list:difficulty_EASY:page_1:size_20
 * - codetop:user:profile:456
 * - codetop:fsrs:queue:userId_123:limit_50
 * 
 * @author CodeTop Team
 */
@Component
public class CacheKeyBuilder {
    
    private static final String NAMESPACE = "codetop";
    private static final String DELIMITER = ":";
    private static final String PARAM_DELIMITER = "_";
    
    // Problem domain keys
    public static String problemSingle(Long problemId) {
        return buildKey("problem", "single", String.valueOf(problemId));
    }
    
    public static String problemList(String difficulty, Integer page, Integer size, String search) {
        StringJoiner params = new StringJoiner(DELIMITER);
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            params.add("difficulty" + PARAM_DELIMITER + difficulty);
        }
        params.add("page" + PARAM_DELIMITER + page);
        params.add("size" + PARAM_DELIMITER + size);
        if (search != null && !search.trim().isEmpty()) {
            params.add("search" + PARAM_DELIMITER + sanitizeParam(search));
        }
        return buildKey("problem", "list", params.toString());
    }
    
    public static String problemSearch(String keyword, Integer page, Integer size) {
        return buildKey("problem", "search", 
                "keyword" + PARAM_DELIMITER + sanitizeParam(keyword),
                "page" + PARAM_DELIMITER + page,
                "size" + PARAM_DELIMITER + size);
    }
    
    public static String problemsByDifficulty(String difficulty, Integer page) {
        return buildKey("problem", "difficulty", difficulty, "page" + PARAM_DELIMITER + page);
    }
    
    public static String problemsHot(Integer minCompanies, Integer limit) {
        return buildKey("problem", "hot", 
                "minCompanies" + PARAM_DELIMITER + minCompanies,
                "limit" + PARAM_DELIMITER + limit);
    }
    
    public static String problemsRecent(Integer limit) {
        return buildKey("problem", "recent", "limit" + PARAM_DELIMITER + limit);
    }
    
    public static String problemStatistics() {
        return buildKey("problem", "stats", "global");
    }
    
    public static String problemAdvancedSearch(Object request, Integer page, Integer size) {
        StringJoiner params = new StringJoiner(DELIMITER);
        params.add("advanced");
        params.add("page" + PARAM_DELIMITER + page);
        params.add("size" + PARAM_DELIMITER + size);
        params.add("request" + PARAM_DELIMITER + request.hashCode());
        return buildKey("problem", "search", params.toString());
    }
    
    public static String problemEnhancedSearch(Object request, Integer page, Integer size) {
        StringJoiner params = new StringJoiner(DELIMITER);
        params.add("enhanced");
        params.add("page" + PARAM_DELIMITER + page);
        params.add("size" + PARAM_DELIMITER + size);
        params.add("request" + PARAM_DELIMITER + request.hashCode());
        return buildKey("problem", "search", params.toString());
    }
    
    public static String tagStatistics() {
        return buildKey("tag", "stats", "global");
    }
    
    // User domain keys
    public static String userProfile(Long userId) {
        return buildKey("user", "profile", String.valueOf(userId));
    }
    
    public static String userProblemProgress(Long userId) {
        return buildKey("user", "progress", String.valueOf(userId));
    }
    
    public static String userProblemStatus(Long userId, Long problemId) {
        return buildKey("user", "status", 
                "userId" + PARAM_DELIMITER + userId,
                "problemId" + PARAM_DELIMITER + problemId);
    }
    
    public static String userProblemMastery(Long userId, Long problemId) {
        return buildKey("user", "mastery", 
                "userId" + PARAM_DELIMITER + userId,
                "problemId" + PARAM_DELIMITER + problemId);
    }
    
    // FSRS domain keys
    public static String fsrsReviewQueue(Long userId, Integer limit) {
        return buildKey("fsrs", "queue", 
                "userId" + PARAM_DELIMITER + userId,
                "limit" + PARAM_DELIMITER + limit);
    }
    
    public static String fsrsUserStats(Long userId) {
        return buildKey("fsrs", "stats", "userId" + PARAM_DELIMITER + userId);
    }
    
    public static String fsrsMetrics(Integer days) {
        return buildKey("fsrs", "metrics", "days" + PARAM_DELIMITER + days);
    }
    
    // Leaderboard domain keys
    public static String leaderboardGlobal(Integer limit) {
        return buildKey("leaderboard", "global", "limit" + PARAM_DELIMITER + limit);
    }
    
    public static String leaderboardWeekly(Integer limit) {
        return buildKey("leaderboard", "weekly", "limit" + PARAM_DELIMITER + limit);
    }
    
    public static String leaderboardMonthly(Integer limit) {
        return buildKey("leaderboard", "monthly", "limit" + PARAM_DELIMITER + limit);
    }
    
    public static String leaderboardAccuracy(Integer limit, Integer days) {
        return buildKey("leaderboard", "accuracy", 
                "limit" + PARAM_DELIMITER + limit,
                "days" + PARAM_DELIMITER + days);
    }
    
    public static String leaderboardStreak(Integer limit) {
        return buildKey("leaderboard", "streak", "limit" + PARAM_DELIMITER + limit);
    }
    
    public static String userRank(Long userId) {
        return buildKey("user", "rank", "userId" + PARAM_DELIMITER + userId);
    }
    
    // CodeTop Filter domain keys
    public static String codetopFilter(Long companyId, Long departmentId, Long positionId, 
                                     String keyword, Integer page, Integer size, String sortBy) {
        StringJoiner params = new StringJoiner(DELIMITER);
        
        if (companyId != null) params.add("company" + PARAM_DELIMITER + companyId);
        if (departmentId != null) params.add("dept" + PARAM_DELIMITER + departmentId);  
        if (positionId != null) params.add("pos" + PARAM_DELIMITER + positionId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            params.add("keyword" + PARAM_DELIMITER + sanitizeParam(keyword));
        }
        params.add("page" + PARAM_DELIMITER + page);
        params.add("size" + PARAM_DELIMITER + size);
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            params.add("sort" + PARAM_DELIMITER + sortBy);
        }
        
        return buildKey("codetop", "filter", params.toString());
    }
    
    public static String codetopGlobalProblems(Integer page, Integer size, String sortBy, String sortOrder) {
        StringJoiner params = new StringJoiner(DELIMITER);
        params.add("page" + PARAM_DELIMITER + page);
        params.add("size" + PARAM_DELIMITER + size);
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            params.add("sort" + PARAM_DELIMITER + sortBy);
        }
        if (sortOrder != null && !sortOrder.trim().isEmpty()) {
            params.add("order" + PARAM_DELIMITER + sortOrder);
        }
        
        return buildKey("codetop", "global", params.toString());
    }
    
    public static String codetopGlobalProblemsWithUserStatus(Long userId, Integer page, Integer size, String sortBy, String sortOrder) {
        StringJoiner params = new StringJoiner(DELIMITER);
        params.add("userId" + PARAM_DELIMITER + userId);
        params.add("page" + PARAM_DELIMITER + page);
        params.add("size" + PARAM_DELIMITER + size);
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            params.add("sort" + PARAM_DELIMITER + sortBy);
        }
        if (sortOrder != null && !sortOrder.trim().isEmpty()) {
            params.add("order" + PARAM_DELIMITER + sortOrder);
        }
        
        return buildKey("codetop", "globaluser", params.toString());
    }
    
    // Cache invalidation patterns
    public static String problemDomain() {
        return NAMESPACE + DELIMITER + "problem" + DELIMITER + "*";
    }
    
    public static String userDomain(Long userId) {
        return NAMESPACE + DELIMITER + "user" + DELIMITER + "*" + DELIMITER + "*" + userId + "*";
    }
    
    public static String fsrsDomain(Long userId) {
        // Pattern to match Spring Cache keys format: cache-name::actual-key
        return "*fsrs*" + userId + "*";
    }
    
    public static String codetopFilterDomain() {
        return NAMESPACE + DELIMITER + "codetop" + DELIMITER + "*";
    }
    
    public static String codetopUserStatusDomain(Long userId) {
        return NAMESPACE + DELIMITER + "codetop" + DELIMITER + "globaluser" + DELIMITER + "userId" + PARAM_DELIMITER + userId + "*";
    }
    
    public static String leaderboardDomain() {
        return NAMESPACE + DELIMITER + "leaderboard" + DELIMITER + "*";
    }
    
    // Utility methods
    private static String buildKey(String domain, String operation, String... params) {
        StringJoiner keyBuilder = new StringJoiner(DELIMITER);
        keyBuilder.add(NAMESPACE);
        keyBuilder.add(domain);
        keyBuilder.add(operation);
        
        for (String param : params) {
            if (param != null && !param.trim().isEmpty()) {
                keyBuilder.add(param);
            }
        }
        
        return keyBuilder.toString();
    }
    
    private static String sanitizeParam(String param) {
        if (param == null) return "";
        return param.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "")
                   .substring(0, Math.min(param.length(), 50));
    }
}