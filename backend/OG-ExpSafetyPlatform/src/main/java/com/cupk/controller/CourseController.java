package com.cupk.controller;

import com.cupk.common.PageResult;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.CourseCreateDTO;
import com.cupk.dto.CourseQueryDTO;
import com.cupk.dto.CourseUpdateDTO;
import com.cupk.service.CourseService;
import com.cupk.vo.CourseDetailVO;
import com.cupk.vo.CourseListVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
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

    @PostMapping
    @RequirePermission("course:create")
    public Result<Long> create(@Valid @RequestBody CourseCreateDTO dto) {
        return Result.success(courseService.create(dto));
    }

    @PutMapping("/{id}")
    @RequirePermission("course:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CourseUpdateDTO dto) {
        courseService.update(id, dto);
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
}
