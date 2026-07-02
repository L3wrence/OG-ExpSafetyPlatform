package com.cupk.controller;

import com.cupk.pojo.ExamPaper;
import com.cupk.service.ExamPaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 试卷管理接口（教师端）
 * 路径：/api/exams/papers
 */
@RestController
@RequestMapping("/api/exams/papers")
public class ExamPaperController {

    @Autowired
    private ExamPaperService examPaperService;

    /** 分页查询试卷 */
    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long courseId,
                          @RequestParam(required = false) String status) {
        return Result.success(examPaperService.pagePapers(pageNum, pageSize, keyword, courseId, status));
    }

    /** 试卷详情（含题目列表） */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(examPaperService.getPaperDetail(id));
    }

    /** 创建试卷 */
    @PostMapping
    public Result<Map<String, Long>> create(@RequestBody ExamPaper paper) {
        return Result.success(Map.of("id", examPaperService.createPaper(paper)));
    }

    /** 编辑试卷信息 */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody ExamPaper paper) {
        examPaperService.updatePaper(id, paper);
        return Result.success();
    }

    /** 逻辑删除试卷 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        examPaperService.deletePaper(id);
        return Result.success();
    }

    /** 发布/关闭试卷 */
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        examPaperService.updateStatus(id, body.get("status"));
        return Result.success();
    }

    /** 向试卷添加题目 */
    @PostMapping("/{id}/questions")
    public Result<?> addQuestions(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> questionIds = ((List<Integer>) body.get("questionIds")).stream()
                .map(Integer::longValue).toList();
        @SuppressWarnings("unchecked")
        List<Integer> scores = (List<Integer>) body.get("scores");
        examPaperService.addQuestions(id, questionIds, scores);
        return Result.success();
    }

    /** 从试卷移除题目 */
    @DeleteMapping("/{id}/questions/{qid}")
    public Result<?> removeQuestion(@PathVariable Long id, @PathVariable Long qid) {
        examPaperService.removeQuestion(id, qid);
        return Result.success();
    }

    /** 调整题目排序 */
    @PutMapping("/{id}/questions/order")
    public Result<?> updateOrder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Integer>> orders = (List<Map<String, Integer>>) body.get("orders");
        examPaperService.updateQuestionOrder(id, orders);
        return Result.success();
    }
}
