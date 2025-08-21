package com.codetop.service;

import com.codetop.entity.IdempotencyRecord;
import com.codetop.mapper.IdempotencyRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 简化的幂等性服务
 * 
 * 基于MySQL唯一约束实现幂等性保障：
 * - 前端传递requestId
 * - MySQL唯一约束防重复
 * - 简单可靠，性能优异
 * 
 * @author CodeTop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SimpleIdempotencyService {
    
    private final IdempotencyRecordMapper idempotencyRecordMapper;
    private final ObjectMapper objectMapper;
    
    /**
     * 检查请求是否重复，并记录幂等性信息
     * 
     * @param requestId 前端传递的请求ID
     * @param userId 用户ID
     * @param operationType 操作类型
     * @return true=重复请求, false=首次请求
     */
    @Transactional
    public boolean checkAndRecordRequest(String requestId, Long userId, String operationType) {
        if (requestId == null || requestId.trim().isEmpty()) {
            // 无requestId时直接放行，由业务层处理
            log.debug("No requestId provided for userId={}, operation={}", userId, operationType);
            return false;
        }
        
        try {
            // 尝试插入幂等性记录
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .requestId(requestId.trim())
                    .userId(userId)
                    .operationType(operationType)
                    .status("processing")
                    .build();
            
            idempotencyRecordMapper.insert(record);
            log.debug("First request recorded: requestId={}, userId={}, operation={}", 
                    requestId, userId, operationType);
            return false; // 首次请求
            
        } catch (DuplicateKeyException e) {
            // 唯一约束冲突 = 重复请求
            log.info("Duplicate request detected: requestId={}, userId={}, operation={}", 
                    requestId, userId, operationType);
            return true; // 重复请求
        }
    }
    
    /**
     * 完成幂等性处理，记录结果
     */
    @Transactional
    public void completeRequest(String requestId, Long userId, String operationType, 
                               Object result, boolean success) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return;
        }
        
        try {
            IdempotencyRecord record = idempotencyRecordMapper.findByRequestIdAndUserIdAndOperationType(
                    requestId.trim(), userId, operationType);
            
            if (record != null) {
                record.setStatus(success ? "completed" : "failed");
                if (result != null) {
                    String resultJson = objectMapper.writeValueAsString(result);
                    record.setResultData(resultJson);
                }
                idempotencyRecordMapper.updateById(record);
                
                log.debug("Request completed: requestId={}, userId={}, operation={}, success={}", 
                        requestId, userId, operationType, success);
            }
        } catch (Exception e) {
            log.warn("Failed to complete request record: requestId={}, userId={}, operation={}, error={}", 
                    requestId, userId, operationType, e.getMessage());
        }
    }
    
    /**
     * 获取已处理请求的结果
     */
    public Object getCachedResult(String requestId, Long userId, String operationType) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return null;
        }
        
        try {
            IdempotencyRecord record = idempotencyRecordMapper.findByRequestIdAndUserIdAndOperationType(
                    requestId.trim(), userId, operationType);
            
            if (record != null && "completed".equals(record.getStatus()) && record.getResultData() != null) {
                return objectMapper.readValue(record.getResultData(), Object.class);
            }
        } catch (Exception e) {
            log.warn("Failed to get cached result: requestId={}, userId={}, operation={}, error={}", 
                    requestId, userId, operationType, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 清理过期的幂等性记录
     */
    @Transactional
    public void cleanupExpiredRecords(int daysToKeep) {
        try {
            int deletedCount = idempotencyRecordMapper.deleteExpiredRecords(daysToKeep);
            log.info("Cleaned up {} expired idempotency records older than {} days", 
                    deletedCount, daysToKeep);
        } catch (Exception e) {
            log.error("Failed to cleanup expired idempotency records: {}", e.getMessage(), e);
        }
    }
}