package com.cupk.controller;

import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.LearningProgressDTO;
import com.cupk.interceptor.UserContext;
import com.cupk.pojo.LearningRecord;
import com.cupk.service.LearningRecordService;
import com.cupk.vo.LearningProgressVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning-records")
public class LearningRecordController {
    private final LearningRecordService learningRecordService;

    public LearningRecordController(LearningRecordService learningRecordService) {
        this.learningRecordService = learningRecordService;
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

    @GetMapping("/experiments/{experimentId}/progress")
    @RequirePermission("learning:update:self")
    public Result<LearningProgressVO> experimentProgress(@PathVariable Long experimentId) {
        return Result.success(learningRecordService.experimentProgress(experimentId, UserContext.userId()));
    }
}
