package com.cupk.exception;

import lombok.Getter;

/**
 * 业务异常类
 * 成员A交付
 */
@Getter
public class BusinessException extends RuntimeException {
    private int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
