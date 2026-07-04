package com.cupk.controller;

import com.cupk.common.PageResult;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.ResourceCreateDTO;
import com.cupk.dto.ResourceQueryDTO;
import com.cupk.dto.ResourceUpdateDTO;
import com.cupk.pojo.TeachingResource;
import com.cupk.service.LearningRecordService;
import com.cupk.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {
    private final ResourceService resourceService;
    private final LearningRecordService learningRecordService;

    public ResourceController(ResourceService resourceService, LearningRecordService learningRecordService) {
        this.resourceService = resourceService;
        this.learningRecordService = learningRecordService;
    }

    @GetMapping
    @RequirePermission("resource:view")
    public Result<PageResult<TeachingResource>> page(@Valid ResourceQueryDTO dto) {
        return Result.success(resourceService.page(dto));
    }

    @PostMapping
    @RequirePermission("resource:create")
    public Result<Long> create(@Valid @RequestBody ResourceCreateDTO dto) {
        return Result.success(resourceService.create(dto));
    }

    @PutMapping("/{id}")
    @RequirePermission("resource:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ResourceUpdateDTO dto) {
        resourceService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("resource:delete")
    public Result<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequirePermission("resource:update")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        resourceService.changeStatus(id, status);
        return Result.success();
    }

    @PostMapping("/{id}/view")
    @RequirePermission("resource:view")
    public Result<Void> view(@PathVariable Long id) {
        learningRecordService.start(id);
        return Result.success();
    }
}
