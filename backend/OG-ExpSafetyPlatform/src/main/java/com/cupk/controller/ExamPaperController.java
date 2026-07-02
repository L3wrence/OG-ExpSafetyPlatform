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
    public Map<String, Object> list(@RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Long courseId,
                                     @RequestParam(required = false) String status) {
        // TODO
        return null;
    }

    /** 试卷详情（含题目列表） */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        // TODO
        return null;
    }

    /** 创建试卷 */
    @PostMapping
    public Map<String, Long> create(@RequestBody ExamPaper paper) {
        // TODO
        return null;
    }

    /** 编辑试卷信息 */
    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @RequestBody ExamPaper paper) {
        // TODO
    }

    /** 逻辑删除试卷 */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        // TODO
    }

    /** 发布/关闭试卷 */
    @PutMapping("/{id}/status")
    public void updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        // TODO
    }

    /** 向试卷添加题目 */
    @PostMapping("/{id}/questions")
    public void addQuestions(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        // TODO
    }

    /** 从试卷移除题目 */
    @DeleteMapping("/{id}/questions/{qid}")
    public void removeQuestion(@PathVariable Long id, @PathVariable Long qid) {
        // TODO
    }

    /** 调整题目排序 */
    @PutMapping("/{id}/questions/order")
    public void updateOrder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        // TODO
    }
}
