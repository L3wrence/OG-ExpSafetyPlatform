package com.cupk.amazingstudy.controller;

import com.cupk.amazingstudy.common.CommonResult;
import com.cupk.amazingstudy.exception.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api/test")
public class TestController {

    @GetMapping("/")
    public String home() {
        return "Hello, 项目运行成功!";
    }

    @GetMapping("/test-error")
    public CommonResult<Void> testError() {
        try {
            throw new BusinessException(403, "你没有权限");
        } catch (BusinessException e) {
            return CommonResult.fail(e.getCode(), e.getMessage());
        }
    }

    @GetMapping("/test1")
    public CommonResult<String> test() {
        return CommonResult.success("测试成功");
    }
}
