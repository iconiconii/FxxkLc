package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 幂等性记录实体
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@TableName("idempotency_records")
public class IdempotencyRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String requestId;
    
    private Long userId;
    
    private String operationType;
    
    private String status; // processing, completed, failed
    
    private String resultData; // JSON格式的结果数据
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}