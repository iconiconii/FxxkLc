package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.dto.RecommendationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncRecommendationService {
    
    private final RecommendationStrategyResolver strategyResolver;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String TASK_PREFIX = "async_rec_task:";
    private static final String RESULT_PREFIX = "async_rec_result:";
    private static final String RATE_LIMIT_PREFIX = "rec_rate_limit:";
    private static final Duration TASK_TTL = Duration.ofHours(2);
    private static final Duration RESULT_TTL = Duration.ofHours(24);
    
    /**
     * 检查用户今日是否已达到推荐限制
     */
    public boolean checkDailyLimit(Long userId) {
        String today = LocalDate.now().toString();
        String key = RATE_LIMIT_PREFIX + userId + ":" + today;
        Object countObj = redisTemplate.opsForValue().get(key);
        if (countObj == null) {
            return true; // 没有记录，允许
        }
        int count = countObj instanceof Integer ? (Integer) countObj : Integer.parseInt(countObj.toString());
        return count < 1; // 每日限制1次
    }
    
    /**
     * 记录用户今日推荐次数
     */
    public void recordDailyUsage(Long userId) {
        String today = LocalDate.now().toString();
        String key = RATE_LIMIT_PREFIX + userId + ":" + today;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(1));
    }
    
    /**
     * 启动异步推荐任务
     */
    public String startAsyncRecommendation(RecommendationRequest request) {
        String taskId = UUID.randomUUID().toString();
        
        // 检查每日限制
        if (!checkDailyLimit(request.getUserId())) {
            String errorMsg = "Daily recommendation limit reached. You can only generate 1 AI recommendation per day.";
            AIRecommendationResponse.AsyncTaskResponse failedTask = 
                AIRecommendationResponse.AsyncTaskResponse.failed(taskId, request.getUserId(), errorMsg);
            saveTaskStatus(taskId, failedTask);
            return taskId;
        }
        
        // 保存任务状态为PENDING
        AIRecommendationResponse.AsyncTaskResponse pendingTask = 
            AIRecommendationResponse.AsyncTaskResponse.pending(taskId, request.getUserId());
        saveTaskStatus(taskId, pendingTask);
        
        // 异步执行推荐生成
        executeRecommendationAsync(taskId, request);
        
        return taskId;
    }
    
    /**
     * 异步执行推荐生成
     */
    @Async
    public CompletableFuture<Void> executeRecommendationAsync(String taskId, RecommendationRequest request) {
        try {
            log.info("Starting async recommendation generation - taskId: {}, userId: {}", taskId, request.getUserId());
            
            // 更新状态为PROCESSING
            AIRecommendationResponse.AsyncTaskResponse processingTask = 
                AIRecommendationResponse.AsyncTaskResponse.processing(taskId, request.getUserId());
            saveTaskStatus(taskId, processingTask);
            
            // 执行推荐生成
            RecommendationStrategy strategy = strategyResolver.resolveStrategy(
                request.getRequestedType(), 
                request.getUserId(), 
                request.getObjective()
            );
            
            AIRecommendationResponse result = strategy.getRecommendations(request);
            
            // 保存结果到缓存
            String cacheKey = saveRecommendationResult(taskId, result);
            
            // 更新状态为COMPLETED
            AIRecommendationResponse.AsyncTaskResponse completedTask = 
                AIRecommendationResponse.AsyncTaskResponse.completed(taskId, request.getUserId(), cacheKey);
            saveTaskStatus(taskId, completedTask);
            
            // 记录每日使用次数
            recordDailyUsage(request.getUserId());
            
            log.info("Async recommendation completed - taskId: {}, userId: {}, items: {}", 
                taskId, request.getUserId(), result.getItems().size());
            
        } catch (Exception e) {
            log.error("Async recommendation failed - taskId: {}, userId: {}", taskId, request.getUserId(), e);
            
            // 更新状态为FAILED
            AIRecommendationResponse.AsyncTaskResponse failedTask = 
                AIRecommendationResponse.AsyncTaskResponse.failed(taskId, request.getUserId(), e.getMessage());
            saveTaskStatus(taskId, failedTask);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 查询任务状态
     */
    public AIRecommendationResponse.AsyncTaskStatus getTaskStatus(String taskId) {
        String key = TASK_PREFIX + taskId;
        AIRecommendationResponse.AsyncTaskResponse task = 
            (AIRecommendationResponse.AsyncTaskResponse) redisTemplate.opsForValue().get(key);
        
        if (task == null) {
            return AIRecommendationResponse.AsyncTaskStatus.builder()
                .taskId(taskId)
                .status("NOT_FOUND")
                .message("Task not found or expired")
                .build();
        }
        
        AIRecommendationResponse.AsyncTaskStatus.AsyncTaskStatusBuilder builder = 
            AIRecommendationResponse.AsyncTaskStatus.builder()
                .taskId(taskId)
                .status(task.getStatus())
                .message(task.getMessage())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt());
        
        // 如果任务完成，加载结果
        if ("COMPLETED".equals(task.getStatus()) && task.getCacheKey() != null) {
            AIRecommendationResponse result = getRecommendationResult(task.getCacheKey());
            builder.result(result);
        }
        
        // 设置进度
        switch (task.getStatus()) {
            case "PENDING":
                builder.progress(10);
                break;
            case "PROCESSING":
                builder.progress(50);
                break;
            case "COMPLETED":
                builder.progress(100);
                break;
            case "FAILED":
                builder.progress(0);
                break;
            default:
                builder.progress(0);
        }
        
        return builder.build();
    }
    
    /**
     * 保存任务状态
     */
    private void saveTaskStatus(String taskId, AIRecommendationResponse.AsyncTaskResponse task) {
        String key = TASK_PREFIX + taskId;
        redisTemplate.opsForValue().set(key, task, TASK_TTL);
    }
    
    /**
     * 保存推荐结果
     */
    private String saveRecommendationResult(String taskId, AIRecommendationResponse result) {
        String cacheKey = RESULT_PREFIX + taskId;
        redisTemplate.opsForValue().set(cacheKey, result, RESULT_TTL);
        return cacheKey;
    }
    
    /**
     * 获取推荐结果
     */
    private AIRecommendationResponse getRecommendationResult(String cacheKey) {
        return (AIRecommendationResponse) redisTemplate.opsForValue().get(cacheKey);
    }
    
    /**
     * 清理过期任务
     */
    public void cleanupExpiredTasks() {
        // Redis TTL会自动处理过期数据，这里可以添加额外的清理逻辑
        log.debug("Cleanup expired async recommendation tasks");
    }
}