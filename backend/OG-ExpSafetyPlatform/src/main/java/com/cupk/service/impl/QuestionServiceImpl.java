package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.QuestionMapper;
import com.cupk.pojo.Question;
import com.cupk.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 题目管理服务实现
 */
@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public Page<Question> pageQuestions(int pageNum, int pageSize, String type,
                                         String difficulty, String keyword, Long courseId) {
        Page<Question> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(StringUtils.hasText(type), Question::getType, type)
               .eq(StringUtils.hasText(difficulty), Question::getDifficulty, difficulty)
               .eq(courseId != null, Question::getCourseId, courseId)
               .like(StringUtils.hasText(keyword), Question::getContent, keyword)
               .orderByDesc(Question::getCreateTime);

        return questionMapper.selectPage(page, wrapper);
    }

    @Override
    public Question getQuestionById(Long id) {
        return questionMapper.selectById(id);
    }

    @Override
    public Long addQuestion(Question question) {
        questionMapper.insert(question);
        return question.getId();
    }

    @Override
    public Map<String, Integer> batchAddQuestions(List<Question> questions) {
        int successCount = 0;
        int failCount = 0;
        for (Question q : questions) {
            try {
                questionMapper.insert(q);
                successCount++;
            } catch (Exception e) {
                failCount++;
            }
        }
        Map<String, Integer> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        return result;
    }

    @Override
    public void updateQuestion(Long id, Question question) {
        question.setId(id);
        questionMapper.updateById(question);
    }

    @Override
    public void deleteQuestion(Long id) {
        questionMapper.deleteById(id);  // MyBatis-Plus @TableLogic 自动实现逻辑删除
    }
}
