package com.cupk.service;

import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.interceptor.UserSession;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.TeachingClassMapper;
import com.cupk.pojo.TeachingResource;
import com.cupk.service.impl.ResourceAccessServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceAccessServiceTest {
    private CourseStudentMapper studentMapper;
    private ResourceAccessService accessService;

    @BeforeEach void setUp() {
        studentMapper = mock(CourseStudentMapper.class);
        accessService = new ResourceAccessServiceImpl(studentMapper, mock(LabCourseMapper.class), mock(TeachingClassMapper.class));
        UserContext.set(new UserSession(9L, List.of("USER"), List.of("resource:view")));
    }

    @AfterEach void cleanUp() { UserContext.clear(); }

    @Test void loggedInUserCanReadPublishedPublicResource() {
        assertDoesNotThrow(() -> accessService.assertReadable(resource("PUBLIC", null, 1, 0)));
    }

    @Test void nonMemberCannotReadCourseResource() {
        when(studentMapper.selectCount(any())).thenReturn(0L);
        assertThrows(BusinessException.class, () -> accessService.assertReadable(resource("COURSE", 3L, 1, 0)));
    }

    @Test void activeMemberCanReadCourseResource() {
        when(studentMapper.selectCount(any())).thenReturn(1L);
        assertDoesNotThrow(() -> accessService.assertReadable(resource("COURSE", 3L, 1, 0)));
    }

    @Test void learnerCannotReadDraftOrInvalidResource() {
        assertThrows(BusinessException.class, () -> accessService.assertReadable(resource("PUBLIC", null, 0, 0)));
        assertThrows(BusinessException.class, () -> accessService.assertReadable(resource("PUBLIC", null, 1, 1)));
    }

    private TeachingResource resource(String scope, Long courseId, int status, int invalid) {
        TeachingResource resource = new TeachingResource();
        resource.setOpenScope(scope); resource.setCourseId(courseId); resource.setStatus(status); resource.setInvalidFlag(invalid);
        return resource;
    }
}
