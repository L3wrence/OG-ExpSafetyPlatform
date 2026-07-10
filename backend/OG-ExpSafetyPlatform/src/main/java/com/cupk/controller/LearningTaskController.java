package com.cupk.controller;

import com.cupk.common.PageResult;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.LearningTaskCreateDTO;
import com.cupk.dto.LearningTaskQueryDTO;
import com.cupk.dto.LearningTaskUpdateDTO;
import com.cupk.pojo.LearningTask;
import com.cupk.service.LearningTaskService;
import com.cupk.vo.LearningPathVO;
import com.cupk.vo.LearningTaskDistributionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning-tasks")
public class LearningTaskController {
    private final LearningTaskService learningTaskService;

    public LearningTaskController(LearningTaskService learningTaskService) {
        this.learningTaskService = learningTaskService;
    }

    @GetMapping
    @RequirePermission("experiment:view")
    public Result<PageResult<LearningTask>> page(@Valid LearningTaskQueryDTO dto) {
        return Result.success(learningTaskService.page(dto));
    }

    @PostMapping
    @RequirePermission("experiment:update")
    public Result<Long> create(@Valid @RequestBody LearningTaskCreateDTO dto) {
        return Result.success(learningTaskService.create(dto));
    }

    @PutMapping("/{id}")
    @RequirePermission("experiment:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody LearningTaskUpdateDTO dto) {
        learningTaskService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("experiment:update")
    public Result<Void> disable(@PathVariable Long id) {
        learningTaskService.disable(id);
        return Result.success();
    }

    @GetMapping("/experiments/{experimentId}/path")
    @RequirePermission("course:view")
    public Result<LearningPathVO> path(@PathVariable Long experimentId) {
        return Result.success(learningTaskService.path(experimentId));
    }

    @PostMapping("/{id}/confirm-checklist")
    @RequirePermission("course:view")
    public Result<Void> confirmChecklist(@PathVariable Long id) {
        learningTaskService.confirmChecklist(id);
        return Result.success();
    }

    @GetMapping("/experiments/{experimentId}/distribution")
    @RequirePermission("experiment:view")
    public Result<List<LearningTaskDistributionVO>> distribution(@PathVariable Long experimentId) {
        return Result.success(learningTaskService.distribution(experimentId));
    }
}
