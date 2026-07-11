package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.TeachingClassMapper;
import com.cupk.pojo.CourseStudent;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.TeachingClass;
import com.cupk.pojo.TeachingResource;
import com.cupk.service.ResourceAccessService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ResourceAccessServiceImpl implements ResourceAccessService {
    private final CourseStudentMapper studentMapper;
    private final LabCourseMapper courseMapper;
    private final TeachingClassMapper classMapper;

    public ResourceAccessServiceImpl(CourseStudentMapper studentMapper, LabCourseMapper courseMapper, TeachingClassMapper classMapper) {
        this.studentMapper = studentMapper;
        this.courseMapper = courseMapper;
        this.classMapper = classMapper;
    }

    @Override public void assertReadable(TeachingResource resource) {
        if (resource == null) throw new BusinessException(404, "教学资源不存在");
        boolean manager = UserContext.isAdmin() || (resource.getCourseId() != null && isCourseManager(resource.getCourseId(), UserContext.userId()));
        if ("PUBLIC".equalsIgnoreCase(resource.getOpenScope())) {
            if (manager || UserContext.isAdmin()) return;
        } else if ("COURSE".equalsIgnoreCase(resource.getOpenScope())) {
            assertCourseMemberOrManager(resource.getCourseId());
            if (manager) return;
        } else throw new BusinessException(403, "无权访问该资源");
        if (!Integer.valueOf(1).equals(resource.getStatus()) || Integer.valueOf(1).equals(resource.getInvalidFlag())) {
            throw new BusinessException(403, "资源未开放或已失效");
        }
        LocalDateTime now = LocalDateTime.now();
        if (resource.getOpenTime() != null && resource.getOpenTime().isAfter(now)) throw new BusinessException(403, "资源尚未开放");
        if (resource.getCloseTime() != null && resource.getCloseTime().isBefore(now)) throw new BusinessException(403, "资源已过期");
    }

    @Override public void assertWritable(TeachingResource resource) {
        if (resource == null) throw new BusinessException(404, "教学资源不存在");
        if (UserContext.isAdmin()) return;
        if (!"COURSE".equalsIgnoreCase(resource.getOpenScope()) || resource.getCourseId() == null
                || !isCourseManager(resource.getCourseId(), UserContext.userId())) {
            throw new BusinessException(403, "无权维护该课堂资源");
        }
    }

    @Override public void assertCourseMemberOrManager(Long courseId) {
        if (courseId == null) throw new BusinessException(400, "课堂资源必须指定课程");
        if (UserContext.isAdmin() || isCourseMember(courseId, UserContext.userId()) || isCourseManager(courseId, UserContext.userId())) return;
        throw new BusinessException(403, "不是该课堂成员");
    }

    @Override public boolean isCourseMember(Long courseId, Long userId) {
        return studentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, userId).eq(CourseStudent::getStatus, 1)) > 0;
    }

    @Override public boolean isCourseManager(Long courseId, Long userId) {
        LabCourse course = courseMapper.selectById(courseId);
        if (course != null && userId.equals(course.getTeacherId())) return true;
        return classMapper.selectCount(new LambdaQueryWrapper<TeachingClass>().eq(TeachingClass::getCourseId, courseId)
                .and(w -> w.eq(TeachingClass::getTeacherId, userId).or().eq(TeachingClass::getAssistantId, userId))) > 0;
    }
}
