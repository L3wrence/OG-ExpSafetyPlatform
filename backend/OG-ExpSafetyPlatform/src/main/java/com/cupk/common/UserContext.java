package com.cupk.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前登录用户上下文
 * 通过 LoginInterceptor 自动注入，请求结束后自动清理
 * 成员A交付（已整合 backend(1) 的完整版本）
 * 扩展了角色支持（兼容成员B的 interceptor.UserContext API）
 */
public class UserContext {
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> permissionsHolder = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> rolesHolder = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        userIdHolder.set(userId);
    }

    public static Long getUserId() {
        return userIdHolder.get();
    }

    /** B兼容：userId() = getUserId() */
    public static Long userId() {
        return getUserId();
    }

    public static void setPermissions(List<String> permissions) {
        permissionsHolder.set(permissions);
    }

    public static List<String> getPermissions() {
        return permissionsHolder.get();
    }

    public static void setRoles(List<String> roles) {
        rolesHolder.set(roles);
    }

    public static List<String> getRoles() {
        return rolesHolder.get();
    }

    /** B兼容：roles() = getRoles() */
    public static List<String> roles() {
        List<String> r = rolesHolder.get();
        return r != null ? r : new ArrayList<>();
    }

    /** B兼容：permissions() = getPermissions() */
    public static List<String> permissions() {
        List<String> p = permissionsHolder.get();
        return p != null ? p : new ArrayList<>();
    }

    public static boolean hasRole(String roleCode) {
        List<String> r = rolesHolder.get();
        return r != null && r.stream().anyMatch(role -> role.equalsIgnoreCase(roleCode));
    }

    public static boolean hasPermission(String permission) {
        List<String> p = permissionsHolder.get();
        return p != null && p.contains(permission);
    }

    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    public static boolean isTeacher() {
        return hasRole("ROLE_TEACHER");
    }

    public static boolean isStudent() {
        return hasRole("ROLE_STUDENT");
    }

    public static boolean isLabAdmin() {
        return hasRole("LAB_ADMIN");
    }

    public static void clear() {
        userIdHolder.remove();
        permissionsHolder.remove();
        rolesHolder.remove();
    }
}
