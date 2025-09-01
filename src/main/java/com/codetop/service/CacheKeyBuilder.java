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

    // ========== 新增通用缓存键构建方法 ==========
    
    /**
     * 通用缓存键构建器
     * 
     * @param prefix 前缀
     * @param parts 键组成部分
     * @return 完整的缓存键
     */
    public static String buildKey(String prefix, Object... parts) {
        StringJoiner keyBuilder = new StringJoiner(DELIMITER);
        keyBuilder.add(NAMESPACE);
        keyBuilder.add(prefix);
        
        for (Object part : parts) {
            if (part != null) {
                String partStr = part.toString();
                if (!partStr.trim().isEmpty()) {
                    keyBuilder.add(sanitizeParam(partStr));
                }
            }
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 构建用户相关的缓存键
     * 
     * @param prefix 前缀
     * @param userId 用户ID
     * @param parts 其他键组成部分
     * @return 包含用户ID的缓存键
     */
    public static String buildUserKey(String prefix, Long userId, Object... parts) {
        Object[] allParts = new Object[parts.length + 1];
        allParts[0] = "userId" + PARAM_DELIMITER + userId;
        System.arraycopy(parts, 0, allParts, 1, parts.length);
        
        return buildKey(prefix, allParts);
    }
    
    /**
     * 构建分页缓存键
     * 
     * @param prefix 前缀
     * @param page 页码
     * @param size 每页大小
     * @param additionalParts 额外的键组成部分
     * @return 包含分页信息的缓存键
     */
    public static String buildPageKey(String prefix, Integer page, Integer size, Object... additionalParts) {
        Object[] allParts = new Object[additionalParts.length + 2];
        allParts[0] = "page" + PARAM_DELIMITER + page;
        allParts[1] = "size" + PARAM_DELIMITER + size;
        System.arraycopy(additionalParts, 0, allParts, 2, additionalParts.length);
        
        return buildKey(prefix, allParts);
    }
    
    /**
     * 构建时间范围缓存键
     * 
     * @param prefix 前缀
     * @param timeUnit 时间单位 (hour, day, week, month)
     * @param value 时间值
     * @param additionalParts 额外的键组成部分
     * @return 包含时间信息的缓存键
     */
    public static String buildTimeKey(String prefix, String timeUnit, Integer value, Object... additionalParts) {
        Object[] allParts = new Object[additionalParts.length + 1];
        allParts[0] = timeUnit + PARAM_DELIMITER + value;
        System.arraycopy(additionalParts, 0, allParts, 1, additionalParts.length);
        
        return buildKey(prefix, allParts);
    }
    
    /**
     * 构建基于哈希的缓存键（用于复杂对象）
     * 
     * @param prefix 前缀
     * @param object 要哈希的对象
     * @param additionalParts 额外的键组成部分
     * @return 包含对象哈希的缓存键
     */
    public static String buildHashKey(String prefix, Object object, Object... additionalParts) {
        Object[] allParts = new Object[additionalParts.length + 1];
        allParts[0] = "hash" + PARAM_DELIMITER + Math.abs(object.hashCode());
        System.arraycopy(additionalParts, 0, allParts, 1, additionalParts.length);
        
        return buildKey(prefix, allParts);
    }
    
    /**
     * 构建通用域模式 (用于批量删除)
     * 
     * @param domain 域名
     * @return 域通配符模式
     */
    public static String buildDomainPattern(String domain) {
        return NAMESPACE + DELIMITER + domain + DELIMITER + "*";
    }
    
    /**
     * 构建用户相关域模式 (用于用户相关缓存的批量删除)
     * 
     * @param domain 域名
     * @param userId 用户ID
     * @return 用户相关的通配符模式
     */
    public static String buildUserDomainPattern(String domain, Long userId) {
        return NAMESPACE + DELIMITER + domain + DELIMITER + "*userId" + PARAM_DELIMITER + userId + "*";
    }
    
    /**
     * 验证缓存键格式
     * 
     * @param key 缓存键
     * @return 是否符合规范
     */
    public static boolean validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否以命名空间开头
        if (!key.startsWith(NAMESPACE + DELIMITER)) {
            return false;
        }
        
        // 检查键长度 (Redis键建议不超过512字节)
        if (key.length() > 500) {
            return false;
        }
        
        // 检查是否包含非法字符
        return !key.matches(".*[\\s\\n\\r\\t].*");
    }
    
    /**
     * 从缓存键提取域名
     * 
     * @param key 缓存键
     * @return 域名，如果格式不正确返回null
     */
    public static String extractDomain(String key) {
        if (key == null || !key.startsWith(NAMESPACE + DELIMITER)) {
            return null;
        }
        
        String[] parts = key.split(DELIMITER);
        return parts.length > 1 ? parts[1] : null;
    }
    
    /**
     * 从缓存键提取操作名
     * 
     * @param key 缓存键
     * @return 操作名，如果格式不正确返回null
     */
    public static String extractOperation(String key) {
        if (key == null || !key.startsWith(NAMESPACE + DELIMITER)) {
            return null;
        }
        
        String[] parts = key.split(DELIMITER);
        return parts.length > 2 ? parts[2] : null;
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