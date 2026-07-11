package com.cupk.service;

import com.cupk.pojo.TeachingResource;

public interface ResourceAccessService {
    void assertReadable(TeachingResource resource);
    void assertWritable(TeachingResource resource);
    void assertCourseMemberOrManager(Long courseId);
    boolean isCourseMember(Long courseId, Long userId);
    boolean isCourseManager(Long courseId, Long userId);
}
