package com.cupk.amazingstudy.exception;

import com.cupk.amazingstudy.common.CommonResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResult<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity.ok(CommonResult.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResult<Void>> handleException(Exception e) {
        e.printStackTrace();
        return ResponseEntity.ok(CommonResult.fail(500, "服务器内部错误"));
    }
}
