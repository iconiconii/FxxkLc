package com.codetop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等性注解
 * 
 * 用于标记需要幂等性保障的接口方法
 * 
 * @author CodeTop Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    
    /**
     * 幂等性策略
     */
    IdempotentStrategy strategy() default IdempotentStrategy.TOKEN_BASED;
    
    /**
     * 幂等性key生成器名称
     */
    String keyGenerator() default "default";
    
    /**
     * 过期时间（秒）
     */
    long expireTime() default 300;
    
    /**
     * 是否返回缓存结果
     */
    boolean returnCachedResult() default true;
    
    /**
     * 操作类型描述
     */
    String operation() default "";
    
    /**
     * 是否记录日志
     */
    boolean logEnabled() default true;
    
    /**
     * 幂等性策略枚举
     */
    enum IdempotentStrategy {
        /**
         * 基于令牌的幂等性（适用于有前端传递requestId的场景）
         */
        TOKEN_BASED,
        
        /**
         * 基于业务参数的幂等性（适用于根据业务参数判断重复的场景）
         */
        BUSINESS_KEY_BASED,
        
        /**
         * 基于分布式锁的幂等性（适用于需要强一致性的场景）
         */
        DISTRIBUTED_LOCK_BASED
    }
}