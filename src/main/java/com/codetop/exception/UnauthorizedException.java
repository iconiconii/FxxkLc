package com.codetop.exception;

import lombok.Getter;

/**
 * 用户未授权异常
 * 当用户未登录或权限不足时抛出
 * 
 * @author CodeTop Team
 */
@Getter
public class UnauthorizedException extends RuntimeException {
    
    private final int errorCode;
    
    public UnauthorizedException(String message) {
        super(message);
        this.errorCode = 401;
    }
    
    public UnauthorizedException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}