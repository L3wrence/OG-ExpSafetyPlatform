package com.cupk.interceptor;

import com.cupk.common.RequirePermission;
import com.cupk.exception.BusinessException;
import com.cupk.common.UserContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限校验切面
 * 配合 @RequirePermission 注解使用
 * 成员A交付
 */
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requirePermission)")
    public void check(RequirePermission requirePermission) {
        List<String> perms = UserContext.getPermissions();
        if (perms == null || !perms.contains(requirePermission.value())) {
            throw new BusinessException(403, "无此操作权限");
        }
    }
}
