package com.codetop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 简单幂等性注解
 * 
 * 基于MySQL唯一约束的幂等性保障，适用于中低并发场景
 * 
 * @author CodeTop Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SimpleIdempotent {
    
    /**
     * 操作类型描述
     */
    String operation() default "";
    
    /**
     * 是否返回缓存结果
     */
    boolean returnCachedResult() default true;
    
    /**
     * 是否记录日志
     */
    boolean logEnabled() default true;
}