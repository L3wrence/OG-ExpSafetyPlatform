package com.cupk.controller;

import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.exam.ExamSaveDTO;
import com.cupk.dto.exam.ExamSubmitDTO;
import com.cupk.interceptor.UserContext;
import com.cupk.service.AdmissionService;
import com.cupk.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    @Autowired
    private AdmissionService admissionService;

    /** 学生可参加的考试列表 */
    @RequirePermission("exam:take")
    @GetMapping("/available")
    public Result<?> available(@RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) Long courseId) {
        return Result.success(examService.getAvailableExams(pageNum, pageSize, courseId));
    }

    /** 开始考试 */
    @RequirePermission("exam:take")
    @PostMapping("/{paperId}/start")
    public Result<?> start(@PathVariable Long paperId) {
        return Result.success(examService.startExam(paperId));
    }

    /** 查询当前学生进行中的考试 */
    @RequirePermission("exam:take")
    @GetMapping("/in-progress")
    public Result<?> inProgress(@RequestParam(required = false) Long paperId) {
        return Result.success(examService.getInProgressExam(paperId));
    }

    /** 提交答案 */
    @RequirePermission("exam:take")
    @PostMapping("/{recordId}/submit")
    public Result<?> submit(@PathVariable Long recordId,
                             @Valid @RequestBody ExamSubmitDTO dto) {
        // 将 DTO 转为 ExamServiceImpl 所需的 List<Map> 格式
        List<Map<String, Object>> answers = dto.getAnswers().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", a.getQuestionId());
            m.put("answer", a.getAnswer());
            return m;
        }).toList();
        return Result.success(examService.submitExam(recordId, answers, Boolean.TRUE.equals(dto.getAutoSubmit())));
    }

    /** 自动保存答案 */
    @RequirePermission("exam:take")
    @PutMapping("/{recordId}/answers")
    public Result<?> saveAnswers(@PathVariable Long recordId,
                                 @RequestBody ExamSaveDTO dto) {
        List<Map<String, Object>> answers = dto.getAnswers().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", a.getQuestionId());
            m.put("answer", a.getAnswer());
            return m;
        }).toList();
        return Result.success(examService.saveAnswers(recordId, answers));
    }

    /** 当前学生实验准入状态 */
    @RequirePermission("exam:take")
    @GetMapping("/admissions/{experimentId}")
    public Result<?> admission(@PathVariable Long experimentId) {
        return Result.success(admissionService.getAdmissionStatus(UserContext.getUserId(), experimentId));
    }

    /** 我的考试记录列表 */
    @RequirePermission("exam:take")
    @GetMapping("/records")
    public Result<?> myRecords(@RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) String status) {
        return Result.success(examService.getMyRecords(pageNum, pageSize, status));
    }

    /** 考试详情（含每题答题） */
    @RequirePermission("exam:take")
    @GetMapping("/records/{id}")
    public Result<?> recordDetail(@PathVariable Long id) {
        return Result.success(examService.getRecordDetail(id));
    }

    /** 我的错题本 */
    @RequirePermission("exam:take")
    @GetMapping("/wrong-questions")
    public Result<?> wrongQuestions(@RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize,
                                     @RequestParam(required = false) String type,
                                     @RequestParam(required = false) Long courseId) {
        return Result.success(examService.getWrongQuestions(pageNum, pageSize, type, courseId));
    }

    /** 错题知识点统计 */
    @RequirePermission("exam:take")
    @GetMapping("/wrong-questions/stats")
    public Result<?> wrongQuestionStats() {
        return Result.success(examService.getWrongQuestionStats());
    }
}
