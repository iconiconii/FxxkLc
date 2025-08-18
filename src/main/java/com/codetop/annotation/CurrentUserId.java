package com.codetop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动注入当前登录用户ID的注解
 * 
 * 使用示例:
 * @PostMapping("/example")
 * public ResponseEntity<?> example(@CurrentUserId Long userId) {
 *     // userId 自动从ThreadLocal中获取
 * }
 * 
 * @author CodeTop Team
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}