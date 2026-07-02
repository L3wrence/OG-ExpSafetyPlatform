package com.cupk.amazingstudy.interceptor;



import com.cupk.amazingstudy.common.RequirePermission;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requirePermission)")
    public void check(RequirePermission requirePermission) {
//        List<String> perms = UserContext.getPermissions();
//        if (perms == null || !perms.contains(requirePermission.value())) {
//            throw new BusinessException(403, "无此操作权限");
//        }
    }
}
