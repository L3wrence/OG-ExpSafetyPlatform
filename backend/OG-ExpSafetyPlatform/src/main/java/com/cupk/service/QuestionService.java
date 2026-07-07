package com.cupk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.pojo.Question;

import java.util.List;
import java.util.Map;

/**
 * 题目管理服务接口
 */
public interface QuestionService {

    /** 分页查询题库 */
    Page<Question> pageQuestions(int pageNum, int pageSize, String type, String difficulty, String keyword,
                                 Long courseId, Long experimentId, Long knowledgeId,
                                 String knowledgePoint, String riskType);

    /** 查看题目详情 */
    Question getQuestionById(Long id);

    /** 新增单个题目 */
    Long addQuestion(Question question);

    /** 批量导入题目，返回 {successCount, failCount} */
    Map<String, Integer> batchAddQuestions(List<Question> questions);

    /** 编辑题目 */
    void updateQuestion(Long id, Question question);

    /** 逻辑删除题目 */
    void deleteQuestion(Long id);
}
