package com.cupk.controller;

import com.cupk.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public Result<?> overview(@RequestParam Long paperId) {
        return Result.success(examService.getStatisticsOverview(paperId));
    }

    /** 分数段分布 */
    @GetMapping("/score-distribution")
    public Result<?> scoreDistribution(@RequestParam Long paperId) {
        return Result.success(examService.getScoreDistribution(paperId));
    }

    /** 每题正确率 */
    @GetMapping("/question-analysis")
    public Result<?> questionAnalysis(@RequestParam Long paperId) {
        return Result.success(examService.getQuestionAnalysis(paperId));
    }

    /** 知识点薄弱分析 */
    @GetMapping("/knowledge-analysis")
    public Result<?> knowledgeAnalysis(@RequestParam Long courseId) {
        return Result.success(examService.getKnowledgeAnalysis(courseId));
    }
}
