package com.cupk.controller;

import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.LearningProgressDTO;
import com.cupk.interceptor.UserContext;
import com.cupk.pojo.LearningRecord;
import com.cupk.service.LearningRecordService;
import com.cupk.service.StepLearningRecordService;
import com.cupk.vo.LearningProgressVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning-records")
public class LearningRecordController {
    private final LearningRecordService learningRecordService;
    private final StepLearningRecordService stepLearningRecordService;

    public LearningRecordController(LearningRecordService learningRecordService,
                                    StepLearningRecordService stepLearningRecordService) {
        this.learningRecordService = learningRecordService;
        this.stepLearningRecordService = stepLearningRecordService;
    }

    @PutMapping("/progress")
    @RequirePermission("learning:update:self")
    public Result<Void> updateProgress(@Valid @RequestBody LearningProgressDTO dto) {
        learningRecordService.updateProgress(dto);
        return Result.success();
    }

    @GetMapping("/my")
    @RequirePermission("learning:update:self")
    public Result<List<LearningRecord>> myRecords() {
        return Result.success(learningRecordService.myRecords());
    }

    @GetMapping("/steps/my")
    @RequirePermission("learning:update:self")
    public Result<List<com.cupk.pojo.StepLearningRecord>> myStepRecords() {
        return Result.success(stepLearningRecordService.myRecords());
    }

    @PutMapping("/steps/{stepId}/complete")
    @RequirePermission("learning:update:self")
    public Result<Void> completeStep(@PathVariable Long stepId) {
        stepLearningRecordService.complete(stepId);
        return Result.success();
    }

    @GetMapping("/experiments/{experimentId}/progress")
    @RequirePermission("learning:update:self")
    public Result<LearningProgressVO> experimentProgress(@PathVariable Long experimentId) {
        return Result.success(learningRecordService.experimentProgress(experimentId, UserContext.userId()));
    }
}
