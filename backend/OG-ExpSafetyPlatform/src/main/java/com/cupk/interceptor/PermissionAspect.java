package com.cupk.interceptor;

import com.cupk.common.RequirePermission;
import com.cupk.exception.BusinessException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {
    @Before("@annotation(requirePermission)")
    public void check(RequirePermission requirePermission) {
        if (!UserContext.hasPermission(requirePermission.value())) {
            throw new BusinessException(403, "无此操作权限");
        }
    }
}
