package com.cupk.controller;

import com.cupk.pojo.Question;
import com.cupk.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 题目管理接口（教师端）
 * 路径：/api/questions
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    /** 分页查询题库 */
    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String type,
                          @RequestParam(required = false) String difficulty,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long courseId) {
        return Result.success(questionService.pageQuestions(pageNum, pageSize, type, difficulty, keyword, courseId));
    }

    /** 查看题目详情 */
    @GetMapping("/{id}")
    public Result<Question> detail(@PathVariable Long id) {
        return Result.success(questionService.getQuestionById(id));
    }

    /** 新增单个题目 */
    @PostMapping
    public Result<Map<String, Long>> add(@RequestBody Question question) {
        return Result.success(Map.of("id", questionService.addQuestion(question)));
    }

    /** 批量导入题目 */
    @PostMapping("/batch")
    public Result<Map<String, Integer>> batchAdd(@RequestBody List<Question> questions) {
        return Result.success(questionService.batchAddQuestions(questions));
    }

    /** 编辑题目 */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Question question) {
        questionService.updateQuestion(id, question);
        return Result.success();
    }

    /** 逻辑删除题目 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return Result.success();
    }
}
