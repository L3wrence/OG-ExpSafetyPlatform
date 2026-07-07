package com.cupk.controller;

import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.HsePracticeQueryDTO;
import com.cupk.dto.HsePracticeSubmitDTO;
import com.cupk.pojo.HseWrongQuestion;
import com.cupk.service.HseTrainingService;
import com.cupk.vo.HsePracticeResultVO;
import com.cupk.vo.HseQuestionVO;
import com.cupk.vo.HseWeakPointVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hse-training")
public class HseTrainingController {
    private final HseTrainingService hseTrainingService;

    public HseTrainingController(HseTrainingService hseTrainingService) {
        this.hseTrainingService = hseTrainingService;
    }

    @GetMapping("/practice")
    @RequirePermission("safety:view")
    public Result<List<HseQuestionVO>> practice(@Valid HsePracticeQueryDTO dto) {
        return Result.success(hseTrainingService.practice(dto));
    }

    @PostMapping("/practice/submit")
    @RequirePermission("safety:view")
    public Result<HsePracticeResultVO> submit(@Valid @RequestBody HsePracticeSubmitDTO dto) {
        return Result.success(hseTrainingService.submit(dto));
    }

    @GetMapping("/wrong-book")
    @RequirePermission("safety:view")
    public Result<List<HseWrongQuestion>> wrongBook() {
        return Result.success(hseTrainingService.wrongBook());
    }

    @GetMapping("/weak-points/my")
    @RequirePermission("safety:view")
    public Result<List<HseWeakPointVO>> myWeakPoints() {
        return Result.success(hseTrainingService.myWeakPoints());
    }

    @GetMapping("/weak-points/class")
    @RequirePermission("safety:view")
    public Result<List<HseWeakPointVO>> classWeakPoints(@RequestParam Long courseId) {
        return Result.success(hseTrainingService.classWeakPoints(courseId));
    }

    @PostMapping("/questions/{questionId}/favorite")
    @RequirePermission("safety:view")
    public Result<Void> favorite(@PathVariable Long questionId) {
        hseTrainingService.favorite(questionId);
        return Result.success();
    }

    @DeleteMapping("/questions/{questionId}/favorite")
    @RequirePermission("safety:view")
    public Result<Void> unfavorite(@PathVariable Long questionId) {
        hseTrainingService.unfavorite(questionId);
        return Result.success();
    }
}
