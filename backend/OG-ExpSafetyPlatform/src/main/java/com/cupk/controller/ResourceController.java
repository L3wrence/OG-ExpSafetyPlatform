package com.cupk.controller;

import com.cupk.common.PageResult;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.ResourceCreateDTO;
import com.cupk.dto.ResourceInteractionDTO;
import com.cupk.dto.ResourceQueryDTO;
import com.cupk.dto.ResourceUpdateDTO;
import com.cupk.pojo.TeachingResource;
import com.cupk.service.LearningRecordService;
import com.cupk.service.ResourceService;
import com.cupk.vo.ResourcePreviewVO;
import com.cupk.vo.ResourceStatsVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/{id}")
    @RequirePermission("resource:view")
    public Result<TeachingResource> detail(@PathVariable Long id) {
        return Result.success(resourceService.detail(id));
    }

    @PostMapping("/upload")
    @RequirePermission("resource:create")
    public Result<TeachingResource> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(resourceService.upload(file));
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

    @PostMapping("/{id}/download")
    @RequirePermission("resource:view")
    public Result<Void> download(@PathVariable Long id) {
        resourceService.markDownload(id);
        return Result.success();
    }

    @PostMapping("/{id}/interaction")
    @RequirePermission("resource:view")
    public Result<Void> interact(@PathVariable Long id, @Valid @RequestBody ResourceInteractionDTO dto) {
        resourceService.interact(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/invalid")
    @RequirePermission("resource:update")
    public Result<Void> markInvalid(@PathVariable Long id, @RequestParam Integer invalidFlag) {
        resourceService.markInvalid(id, invalidFlag);
        return Result.success();
    }

    @GetMapping("/{id}/stats")
    @RequirePermission("resource:view")
    public Result<ResourceStatsVO> stats(@PathVariable Long id) {
        return Result.success(resourceService.stats(id));
    }

    @GetMapping("/{id}/preview")
    @RequirePermission("resource:view")
    public Result<ResourcePreviewVO> preview(@PathVariable Long id) {
        return Result.success(resourceService.preview(id));
    }
}
