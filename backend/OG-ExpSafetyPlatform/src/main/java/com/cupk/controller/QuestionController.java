package com.cupk.controller;

import com.cupk.pojo.Question;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.common.UserContext;
import com.cupk.dto.question.QuestionCreateDTO;
import com.cupk.dto.question.QuestionQueryDTO;
import com.cupk.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
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
    public Result<?> list(@Valid QuestionQueryDTO query) {
        return Result.success(questionService.pageQuestions(
                query.getPageNum(), query.getPageSize(),
                query.getType(), query.getDifficulty(),
                query.getKeyword(), query.getCourseId()));
    }

    /** 查看题目详情 */
    @GetMapping("/{id}")
    public Result<Question> detail(@PathVariable Long id) {
        return Result.success(questionService.getQuestionById(id));
    }

    /** 新增单个题目 */
    @RequirePermission("question:create")
    @PostMapping
    public Result<Map<String, Long>> add(@Valid @RequestBody QuestionCreateDTO dto) {
        Question question = new Question();
        BeanUtils.copyProperties(dto, question);
        question.setCreateBy(UserContext.getUserId());
        return Result.success(Map.of("id", questionService.addQuestion(question)));
    }

    /** 批量导入题目 */
    @RequirePermission("question:create")
    @PostMapping("/batch")
    public Result<Map<String, Integer>> batchAdd(@RequestBody List<QuestionCreateDTO> dtos) {
        List<Question> questions = dtos.stream().map(dto -> {
            Question q = new Question();
            BeanUtils.copyProperties(dto, q);
            q.setCreateBy(UserContext.getUserId());
            return q;
        }).toList();
        return Result.success(questionService.batchAddQuestions(questions));
    }

    /** 编辑题目 */
    @RequirePermission("question:update")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody QuestionCreateDTO dto) {
        Question question = new Question();
        BeanUtils.copyProperties(dto, question);
        questionService.updateQuestion(id, question);
        return Result.success();
    }

    /** 逻辑删除题目 */
    @RequirePermission("question:delete")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return Result.success();
    }
}
