package com.codetop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codetop.entity.IdempotencyRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * 幂等性记录Mapper
 * 
 * @author CodeTop Team
 */
@Mapper
@Repository
public interface IdempotencyRecordMapper extends BaseMapper<IdempotencyRecord> {
    
    /**
     * 根据请求ID、用户ID和操作类型查找记录
     */
    @Select("SELECT * FROM idempotency_records WHERE request_id = #{requestId} AND user_id = #{userId} AND operation_type = #{operationType} LIMIT 1")
    IdempotencyRecord findByRequestIdAndUserIdAndOperationType(
            @Param("requestId") String requestId,
            @Param("userId") Long userId,
            @Param("operationType") String operationType
    );
    
    /**
     * 删除过期的幂等性记录
     */
    @Delete("DELETE FROM idempotency_records WHERE created_at < DATE_SUB(NOW(), INTERVAL #{daysToKeep} DAY)")
    int deleteExpiredRecords(@Param("daysToKeep") int daysToKeep);
    
    /**
     * 统计指定时间内的重复请求次数
     */
    @Select("SELECT COUNT(*) FROM idempotency_records WHERE user_id = #{userId} AND operation_type = #{operationType} AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR)")
    long countRecentDuplicates(@Param("userId") Long userId, @Param("operationType") String operationType, @Param("hours") int hours);
}