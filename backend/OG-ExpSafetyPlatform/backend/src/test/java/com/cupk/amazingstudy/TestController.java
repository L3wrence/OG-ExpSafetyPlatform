package com.cupk.amazingstudy;

import com.cupk.amazingstudy.common.CommonResult;
import com.cupk.amazingstudy.exception.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/error")
    public CommonResult<?> error() {
        throw new BusinessException(403, "你没有权限");
    }
}
