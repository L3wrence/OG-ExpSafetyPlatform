package com.cupk.controller;

import com.cupk.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 在线考试接口（学生端）
 * 路径：/api/exams
 */
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    /** 学生可参加的考试列表 */
    @GetMapping("/available")
    public Result<?> available(@RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) Long courseId) {
        return Result.success(examService.getAvailableExams(pageNum, pageSize, courseId));
    }

    /** 开始考试 */
    @PostMapping("/{paperId}/start")
    public Result<?> start(@PathVariable Long paperId) {
        return Result.success(examService.startExam(paperId));
    }

    /** 提交答案 */
    @PostMapping("/{recordId}/submit")
    public Result<?> submit(@PathVariable Long recordId,
                             @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        return Result.success(examService.submitExam(recordId, answers));
    }

    /** 我的考试记录列表 */
    @GetMapping("/records")
    public Result<?> myRecords(@RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) String status) {
        return Result.success(examService.getMyRecords(pageNum, pageSize, status));
    }

    /** 考试详情（含每题答题） */
    @GetMapping("/records/{id}")
    public Result<?> recordDetail(@PathVariable Long id) {
        return Result.success(examService.getRecordDetail(id));
    }

    /** 我的错题本 */
    @GetMapping("/wrong-questions")
    public Result<?> wrongQuestions(@RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize,
                                     @RequestParam(required = false) String type,
                                     @RequestParam(required = false) Long courseId) {
        return Result.success(examService.getWrongQuestions(pageNum, pageSize, type, courseId));
    }

    /** 错题知识点统计 */
    @GetMapping("/wrong-questions/stats")
    public Result<?> wrongQuestionStats() {
        return Result.success(examService.getWrongQuestionStats());
    }
}
