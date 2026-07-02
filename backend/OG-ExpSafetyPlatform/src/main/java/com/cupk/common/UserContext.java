package com.cupk.common;

/**
 * 当前登录用户上下文（占位符，等成员A的LoginInterceptor接入后自动生效）
 */
public class UserContext {
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();

    public static void setUserId(Long userId) { userIdHolder.set(userId); }
    public static Long getUserId() { return userIdHolder.get(); }
    public static void clear() { userIdHolder.remove(); }
}
