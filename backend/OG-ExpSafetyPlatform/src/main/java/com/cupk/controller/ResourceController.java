package com.cupk.controller;

import com.cupk.common.PageResult;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.ResourceCreateDTO;
import com.cupk.dto.ResourceInteractionDTO;
import com.cupk.dto.ResourceQueryDTO;
import com.cupk.dto.ResourceTimelineNoteDTO;
import com.cupk.dto.ResourceUpdateDTO;
import com.cupk.pojo.TeachingResource;
import com.cupk.service.LearningRecordService;
import com.cupk.service.ResourceService;
import com.cupk.service.ResourceTimelineNoteService;
import com.cupk.vo.ResourcePreviewVO;
import com.cupk.vo.ResourceStatsVO;
import com.cupk.vo.ResourceTimelineNoteVO;
import com.cupk.vo.ResourceTimelineStatsVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {
    private final ResourceService resourceService;
    private final LearningRecordService learningRecordService;
    private final ResourceTimelineNoteService timelineNoteService;

    public ResourceController(ResourceService resourceService, LearningRecordService learningRecordService,
                              ResourceTimelineNoteService timelineNoteService) {
        this.resourceService = resourceService;
        this.learningRecordService = learningRecordService;
        this.timelineNoteService = timelineNoteService;
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

    @GetMapping("/{id}/timeline-notes")
    @RequirePermission("resource:view")
    public Result<List<ResourceTimelineNoteVO>> timelineNotes(@PathVariable Long id,
                                                             @RequestParam(required = false) Boolean mineOnly) {
        return Result.success(timelineNoteService.listByResource(id, mineOnly));
    }

    @PostMapping("/{id}/timeline-notes")
    @RequirePermission("resource:view")
    public Result<Long> createTimelineNote(@PathVariable Long id, @Valid @RequestBody ResourceTimelineNoteDTO dto) {
        dto.setResourceId(id);
        return Result.success(timelineNoteService.create(dto));
    }

    @DeleteMapping("/timeline-notes/{noteId}")
    @RequirePermission("resource:view")
    public Result<Void> deleteTimelineNote(@PathVariable Long noteId) {
        timelineNoteService.delete(noteId);
        return Result.success();
    }

    @GetMapping("/experiments/{experimentId}/timeline-hotspots")
    @RequirePermission("experiment:view")
    public Result<List<ResourceTimelineStatsVO>> timelineHotspots(@PathVariable Long experimentId) {
        return Result.success(timelineNoteService.hotspots(experimentId));
    }
}
