package com.codetop.config;

import com.codetop.annotation.CurrentUserId;
import com.codetop.exception.UnauthorizedException;
import com.codetop.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @CurrentUserId注解的参数解析器
 * 自动从ThreadLocal中获取当前用户ID并注入到Controller方法参数中
 * 
 * @author CodeTop Team
 */
@Component
@Slf4j
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * 判断是否支持该参数
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class) 
               && parameter.getParameterType().equals(Long.class);
    }

    /**
     * 解析参数值
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, 
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, 
                                WebDataBinderFactory binderFactory) throws Exception {
        
        Long userId = UserContext.getCurrentUserId();
        
        log.debug("CurrentUserIdArgumentResolver.resolveArgument: 尝试获取用户ID - userId={}, hasUser={}, username={}, thread={}", 
                 userId, UserContext.hasUser(), UserContext.getCurrentUsername(), Thread.currentThread().getId());
        
        if (userId == null) {
            log.error("用户未登录或登录已过期 - 请求路径: {}, UserContext.hasUser={}, thread={}", 
                    webRequest.getDescription(false), UserContext.hasUser(), Thread.currentThread().getId());
            throw new UnauthorizedException("用户未登录或登录已过期");
        }
        
        log.debug("成功从ThreadLocal获取用户ID: {}", userId);
        return userId;
    }
}