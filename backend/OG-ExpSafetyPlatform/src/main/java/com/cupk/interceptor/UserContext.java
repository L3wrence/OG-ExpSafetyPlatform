package com.cupk.interceptor;

import com.cupk.exception.BusinessException;

import java.util.List;

public final class UserContext {
    private static final ThreadLocal<UserSession> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(UserSession session) { HOLDER.set(session); }
    public static UserSession get() { return HOLDER.get(); }
    public static Long userId() {
        if (get() == null) throw new BusinessException(401, "用户未登录");
        return get().userId();
    }
    public static Long getUserId() { return userId(); }
    public static List<String> roles() {
        if (get() == null) throw new BusinessException(401, "用户未登录");
        return get().roles();
    }
    public static List<String> getRoles() { return roles(); }
    public static List<String> permissions() {
        if (get() == null) throw new BusinessException(401, "用户未登录");
        return get().permissions();
    }
    public static List<String> getPermissions() { return permissions(); }
    public static boolean hasPermission(String permission) { return permissions().contains(permission); }
    public static boolean hasRole(String roleCode) {
        return roles().stream().anyMatch(role -> role.equalsIgnoreCase(roleCode));
    }
    public static boolean isAdmin() { return hasRole("ADMIN"); }
    public static boolean isTeacher() {
        return !isAdmin() && (hasPermission("course:create")
                || hasPermission("course:update")
                || hasPermission("course:class:manage"));
    }
    public static boolean isStudent() { return isUser() && !isTeacher() && !isAdmin(); }
    public static boolean isUser() { return hasRole("USER"); }
    public static boolean isLearner() {
        return isUser() && !isAdmin();
    }
    public static void clear() { HOLDER.remove(); }
}
