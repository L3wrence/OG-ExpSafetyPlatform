package com.cupk.controller;

import com.cupk.common.PageResult;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.CourseCreateDTO;
import com.cupk.dto.CourseQueryDTO;
import com.cupk.dto.ClassInviteCreateDTO;
import com.cupk.dto.ClassJoinDTO;
import com.cupk.dto.CourseStudentImportDTO;
import com.cupk.dto.CourseStudentRemoveDTO;
import com.cupk.dto.CourseUpdateDTO;
import com.cupk.dto.TeachingClassCreateDTO;
import com.cupk.dto.TeachingClassUpdateDTO;
import com.cupk.service.ClassInviteService;
import com.cupk.service.CourseService;
import com.cupk.vo.ClassInviteVO;
import com.cupk.vo.CourseDetailVO;
import com.cupk.vo.CourseJoinVO;
import com.cupk.vo.CourseListVO;
import com.cupk.vo.CourseStudentImportResultVO;
import com.cupk.vo.CourseStudentVO;
import com.cupk.vo.TeachingClassVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courseService;
    private final ClassInviteService classInviteService;

    public CourseController(CourseService courseService, ClassInviteService classInviteService) {
        this.courseService = courseService;
        this.classInviteService = classInviteService;
    }

    @GetMapping
    @RequirePermission("course:view")
    public Result<PageResult<CourseListVO>> page(@Valid CourseQueryDTO dto) {
        return Result.success(courseService.page(dto));
    }

    @GetMapping("/{id}")
    @RequirePermission("course:view")
    public Result<CourseDetailVO> detail(@PathVariable Long id) {
        return Result.success(courseService.detail(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("course:create")
    public Result<Long> create(@Valid @RequestPart("metadata") CourseCreateDTO dto,
                               @RequestPart(value = "cover", required = false) MultipartFile cover) {
        return Result.success(courseService.create(dto, cover));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("course:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestPart("metadata") CourseUpdateDTO dto,
                               @RequestPart(value = "cover", required = false) MultipartFile cover,
                               @RequestParam(defaultValue = "false") Boolean removeCover) {
        courseService.update(id, dto, cover, removeCover);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("course:delete")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequirePermission("course:update")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        courseService.changeStatus(id, status);
        return Result.success();
    }

    @PutMapping("/{id}/publish")
    @RequirePermission("course:publish")
    public Result<Void> publish(@PathVariable Long id,
                                @RequestParam(required = false, defaultValue = "false") Boolean allowEmpty) {
        courseService.publish(id, allowEmpty);
        return Result.success();
    }

    @PutMapping("/{id}/archive")
    @RequirePermission("course:archive")
    public Result<Void> archive(@PathVariable Long id) {
        courseService.archive(id);
        return Result.success();
    }

    @PostMapping("/{id}/copy")
    @RequirePermission("course:copy")
    public Result<Long> copy(@PathVariable Long id) {
        return Result.success(courseService.copy(id));
    }

    @GetMapping("/{courseId}/classes")
    @RequirePermission("course:view")
    public Result<List<TeachingClassVO>> listClasses(@PathVariable Long courseId) {
        return Result.success(courseService.listClasses(courseId));
    }

    @PostMapping("/{courseId}/classes")
    @RequirePermission("course:class:manage")
    public Result<Long> createClass(@PathVariable Long courseId, @Valid @RequestBody TeachingClassCreateDTO dto) {
        return Result.success(courseService.createClass(courseId, dto));
    }

    @PutMapping("/{courseId}/classes/{classId}")
    @RequirePermission("course:class:manage")
    public Result<Void> updateClass(@PathVariable Long courseId, @PathVariable Long classId,
                                    @Valid @RequestBody TeachingClassUpdateDTO dto) {
        courseService.updateClass(courseId, classId, dto);
        return Result.success();
    }

    @DeleteMapping("/{courseId}/classes/{classId}")
    @RequirePermission("course:class:manage")
    public Result<Void> deleteClass(@PathVariable Long courseId, @PathVariable Long classId) {
        courseService.deleteClass(courseId, classId);
        return Result.success();
    }

    @GetMapping("/{courseId}/students")
    @RequirePermission("course:view")
    public Result<List<CourseStudentVO>> listStudents(@PathVariable Long courseId,
                                                      @RequestParam(required = false) Long teachingClassId,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String groupName) {
        return Result.success(courseService.listStudents(courseId, teachingClassId, keyword, groupName));
    }

    @PostMapping("/{courseId}/students/import")
    @RequirePermission("course:student:manage")
    public Result<CourseStudentImportResultVO> importStudents(@PathVariable Long courseId,
                                                             @Valid @RequestBody CourseStudentImportDTO dto) {
        return Result.success(courseService.importStudents(courseId, dto));
    }

    @DeleteMapping("/{courseId}/students")
    @RequirePermission("course:student:manage")
    public Result<Void> removeStudents(@PathVariable Long courseId, @Valid @RequestBody CourseStudentRemoveDTO dto) {
        courseService.removeStudents(courseId, dto);
        return Result.success();
    }

    @PostMapping("/{courseId}/invites")
    @RequirePermission("course:invite:manage")
    public Result<Long> createInvite(@PathVariable Long courseId, @Valid @RequestBody ClassInviteCreateDTO dto) {
        return Result.success(classInviteService.create(courseId, dto));
    }

    @GetMapping("/{courseId}/invites")
    @RequirePermission("course:invite:manage")
    public Result<List<ClassInviteVO>> listInvites(@PathVariable Long courseId) {
        return Result.success(classInviteService.list(courseId));
    }

    @PutMapping("/invites/{inviteId}/disable")
    @RequirePermission("course:invite:manage")
    public Result<Void> disableInvite(@PathVariable Long inviteId) {
        classInviteService.disable(inviteId);
        return Result.success();
    }

    @PostMapping("/join")
    @RequirePermission("course:join")
    public Result<CourseJoinVO> join(@Valid @RequestBody ClassJoinDTO dto) {
        return Result.success(classInviteService.join(dto));
    }
}
