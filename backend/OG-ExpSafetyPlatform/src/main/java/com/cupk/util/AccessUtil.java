package com.cupk.util;

import com.cupk.pojo.LabCourse;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;

public final class AccessUtil {
    private AccessUtil() {}

    public static void requireTeacherOrAdmin() {
        if (!UserContext.isTeacher() && !UserContext.isAdmin()) {
            throw new BusinessException(403, "只有教师或管理员可以执行该操作");
        }
    }

    public static void requireStudent() {
        if (!UserContext.isStudent()) {
            throw new BusinessException(403, "只有学生可以执行该操作");
        }
    }

    public static void assertCourseWritable(LabCourse course) {
        requireTeacherOrAdmin();
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (UserContext.isTeacher() && !UserContext.userId().equals(course.getTeacherId())) {
            throw new BusinessException(403, "不能修改其他教师负责的课程");
        }
    }

    public static Long currentTeacherScope() {
        if (UserContext.isTeacher()) return UserContext.userId();
        if (UserContext.isAdmin() || UserContext.isLabAdmin()) return null;
        throw new BusinessException(403, "无权查看教师统计数据");
    }
}
