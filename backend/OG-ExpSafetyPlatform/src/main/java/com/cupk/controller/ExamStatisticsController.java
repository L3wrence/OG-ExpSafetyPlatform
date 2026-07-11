package com.cupk.controller;

import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
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
    @RequirePermission("exam:statistics")
    @GetMapping("/overview")
    public Result<?> overview(@RequestParam Long paperId) {
        return Result.success(examService.getStatisticsOverview(paperId));
    }

    /** 分数段分布 */
    @RequirePermission("exam:statistics")
    @GetMapping("/score-distribution")
    public Result<?> scoreDistribution(@RequestParam Long paperId) {
        return Result.success(examService.getScoreDistribution(paperId));
    }

    /** 每题正确率 */
    @RequirePermission("exam:statistics")
    @GetMapping("/question-analysis")
    public Result<?> questionAnalysis(@RequestParam Long paperId) {
        return Result.success(examService.getQuestionAnalysis(paperId));
    }

    /** 知识点薄弱分析 */
    @RequirePermission("exam:statistics")
    @GetMapping("/knowledge-analysis")
    public Result<?> knowledgeAnalysis(@RequestParam Long courseId) {
        return Result.success(examService.getKnowledgeAnalysis(courseId));
    }

    // ===== 简答题批改 =====

    /** 待批改简答题的考试记录列表 */
    @RequirePermission("exam:statistics")
    @GetMapping("/pending-grading")
    public Result<?> pendingGrading(@RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(required = false) Long paperId) {
        return Result.success(examService.getPendingGradingRecords(pageNum, pageSize, paperId));
    }

    /** 指定试卷的全部学生提交记录 */
    @RequirePermission("exam:statistics")
    @GetMapping("/submissions")
    public Result<?> submissions(@RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "10") int pageSize,
                                 @RequestParam Long paperId) {
        return Result.success(examService.getPaperSubmissionRecords(pageNum, pageSize, paperId));
    }

    /** 教师查看完整学生答卷 */
    @RequirePermission("exam:statistics")
    @GetMapping("/grading-records/{recordId}")
    public Result<?> gradingRecord(@PathVariable Long recordId) {
        return Result.success(examService.getGradingRecordDetail(recordId));
    }

    /** 批改简答题 */
    @RequirePermission("exam:statistics")
    @PutMapping("/grade-short-answer")
    public Result<?> gradeShortAnswer(@RequestParam Long recordId,
                                       @RequestBody List<Map<String, Object>> grades) {
        return Result.success(examService.gradeShortAnswer(recordId, grades));
    }
}
