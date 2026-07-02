package com.cupk.controller;

import com.cupk.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 考试统计接口（教师端，用于ECharts可视化）
 * 路径：/api/exams/statistics
 */
@RestController
@RequestMapping("/api/exams/statistics")
public class ExamStatisticsController {

    @Autowired
    private ExamService examService;

    /** 考试总览：平均分/通过率/最高分/最低分 */
    @GetMapping("/overview")
    public Map<String, Object> overview(@RequestParam Long paperId) {
        // TODO
        return null;
    }

    /** 分数段分布 */
    @GetMapping("/score-distribution")
    public List<Map<String, Object>> scoreDistribution(@RequestParam Long paperId) {
        // TODO
        return null;
    }

    /** 每题正确率 */
    @GetMapping("/question-analysis")
    public List<Map<String, Object>> questionAnalysis(@RequestParam Long paperId) {
        // TODO
        return null;
    }

    /** 知识点薄弱分析 */
    @GetMapping("/knowledge-analysis")
    public List<Map<String, Object>> knowledgeAnalysis(@RequestParam Long courseId) {
        // TODO
        return null;
    }
}
