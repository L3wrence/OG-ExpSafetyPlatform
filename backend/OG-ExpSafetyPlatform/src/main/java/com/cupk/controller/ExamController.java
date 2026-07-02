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
    public Map<String, Object> available(@RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          @RequestParam(required = false) Long courseId) {
        // TODO
        return null;
    }

    /** 开始考试 */
    @PostMapping("/{paperId}/start")
    public Map<String, Object> start(@PathVariable Long paperId) {
        // TODO
        return null;
    }

    /** 提交答案 */
    @PostMapping("/{recordId}/submit")
    public Map<String, Object> submit(@PathVariable Long recordId,
                                       @RequestBody Map<String, Object> body) {
        // TODO
        return null;
    }

    /** 我的考试记录列表 */
    @GetMapping("/records")
    public Map<String, Object> myRecords(@RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          @RequestParam(required = false) String status) {
        // TODO
        return null;
    }

    /** 考试详情（含每题答题） */
    @GetMapping("/records/{id}")
    public Map<String, Object> recordDetail(@PathVariable Long id) {
        // TODO
        return null;
    }

    /** 我的错题本 */
    @GetMapping("/wrong-questions")
    public Map<String, Object> wrongQuestions(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) String type,
                                               @RequestParam(required = false) Long courseId) {
        // TODO
        return null;
    }

    /** 错题知识点统计 */
    @GetMapping("/wrong-questions/stats")
    public List<Map<String, Object>> wrongQuestionStats() {
        // TODO
        return null;
    }
}
