package com.cupk.controller;

import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.ResourceCreateDTO;
import com.cupk.dto.ResourceUpdateDTO;
import com.cupk.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/courses/{courseId}/resources")
public class CourseResourceController {
    private final ResourceService resourceService;
    public CourseResourceController(ResourceService resourceService) { this.resourceService = resourceService; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("resource:create")
    public Result<Long> create(@PathVariable Long courseId, @Valid @RequestPart("metadata") ResourceCreateDTO metadata,
                               @RequestPart("file") MultipartFile file) {
        return Result.success(resourceService.createCourseResource(courseId, metadata, file));
    }

    @PutMapping(value = "/{resourceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("resource:update")
    public Result<Void> update(@PathVariable Long courseId, @PathVariable Long resourceId,
                               @Valid @RequestPart("metadata") ResourceUpdateDTO metadata,
                               @RequestPart(value = "file", required = false) MultipartFile file) {
        resourceService.updateCourseResource(courseId, resourceId, metadata, file);
        return Result.success();
    }

    @DeleteMapping("/{resourceId}")
    @RequirePermission("resource:delete")
    public Result<Void> delete(@PathVariable Long courseId, @PathVariable Long resourceId) {
        resourceService.delete(resourceId);
        return Result.success();
    }
}
