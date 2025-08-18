package com.codetop.validation;

import com.codetop.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Component;

import jakarta.validation.Valid;
import java.lang.reflect.Parameter;

/**
 * 简化的AOP验证切面
 * 只处理业务数据验证，跳过Security对象和已有@Valid验证的参数
 * 
 * @author CodeTop Team
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationAspect {

    private final InputSanitizer inputSanitizer;

    /**
     * 对Controller方法参数进行安全验证
     * 跳过Security相关参数和已有@Valid验证的参数
     */
    @Before("execution(* com.codetop.controller..*(..))")
    public void validateControllerInput(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        
        if (args == null || args.length == 0) {
            return;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        try {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                Parameter param = parameters[i];
                
                // 跳过以下参数类型：
                // 1. 已经有@Valid验证的参数
                // 2. @CurrentUserId参数
                // 3. @AuthenticationPrincipal参数（向后兼容）
                // 4. null参数
                if (shouldSkipParameter(param, arg)) {
                    continue;
                }
                
                // 只对字符串参数进行基本的安全验证
                if (arg instanceof String) {
                    String stringValue = (String) arg;
                    validateString(stringValue, className + "." + methodName + ".param" + i);
                }
            }
        } catch (Exception e) {
            log.warn("Input validation failed for {}.{}: {}", className, methodName, e.getMessage());
            throw new InputSanitizer.ValidationException("Input validation failed: " + e.getMessage());
        }
    }

    /**
     * 判断是否应该跳过该参数的验证
     */
    private boolean shouldSkipParameter(Parameter parameter, Object argument) {
        // 跳过null参数
        if (argument == null) {
            return true;
        }
        
        // 跳过已经有@Valid验证的参数
        if (parameter.isAnnotationPresent(Valid.class)) {
            return true;
        }
        
        // 跳过@CurrentUserId参数
        if (parameter.isAnnotationPresent(CurrentUserId.class)) {
            return true;
        }
        
        // 跳过@AuthenticationPrincipal参数（向后兼容）
        if (parameter.isAnnotationPresent(AuthenticationPrincipal.class)) {
            return true;
        }
        
        // 跳过Security相关类型
        String typeName = parameter.getType().getName();
        if (typeName.startsWith("org.springframework.security.") ||
            typeName.contains("UserPrincipal") ||
            typeName.contains("Authentication")) {
            return true;
        }
        
        return false;
    }

    /**
     * 对字符串进行基本的安全验证
     */
    private void validateString(String value, String context) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        try {
            // 基本的XSS和注入攻击防护
            inputSanitizer.validateNotMalicious(value);
        } catch (Exception e) {
            log.warn("SECURITY_EVENT: Suspicious input detected in {}: {}", context, e.getMessage());
            throw e;
        }
    }
}