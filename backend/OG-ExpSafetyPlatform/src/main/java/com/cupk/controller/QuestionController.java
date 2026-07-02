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
    public Map<String, Object> list(@RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize,
                                     @RequestParam(required = false) String type,
                                     @RequestParam(required = false) String difficulty,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Long courseId) {
        // TODO
        return null;
    }

    /** 查看题目详情 */
    @GetMapping("/{id}")
    public Question detail(@PathVariable Long id) {
        // TODO
        return null;
    }

    /** 新增单个题目 */
    @PostMapping
    public Map<String, Long> add(@RequestBody Question question) {
        // TODO
        return null;
    }

    /** 批量导入题目 */
    @PostMapping("/batch")
    public Map<String, Integer> batchAdd(@RequestBody List<Question> questions) {
        // TODO
        return null;
    }

    /** 编辑题目 */
    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @RequestBody Question question) {
        // TODO
    }

    /** 逻辑删除题目 */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        // TODO
    }
}
