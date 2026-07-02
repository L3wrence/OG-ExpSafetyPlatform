package com.cupk.amazingstudy.common;

import lombok.Data;

@Data
public class CommonResult<T> {
    private int code;
    private String message;
    private T data;

    private CommonResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功，无数据
    public static <T> CommonResult<T> success() {
        return new CommonResult<>(200, "操作成功", null);
    }
    // 成功，带数据
    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(200, "操作成功", data);
    }
    // 失败，自定义消息
    public static <T> CommonResult<T> fail(int code, String message) {
        return new CommonResult<>(code, message, null);
    }
    // 常用失败，比如参数错误
    public static <T> CommonResult<T> badRequest(String message) {
        return new CommonResult<>(400, message, null);
    }
    // 未授权
    public static <T> CommonResult<T> unauthorized(String message) {
        return new CommonResult<>(401, message, null);
    }
    // 禁止
    public static <T> CommonResult<T> forbidden(String message) {
        return new CommonResult<>(403, message, null);
    }
}