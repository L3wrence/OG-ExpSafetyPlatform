package com.cupk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.pojo.ExamPaper;

import java.util.List;
import java.util.Map;

/**
 * 试卷管理服务接口
 */
public interface ExamPaperService {

    /** 分页查询试卷 */
    Page<ExamPaper> pagePapers(int pageNum, int pageSize, String keyword, Long courseId, String status);

    /** 试卷详情（含题目列表） */
    Map<String, Object> getPaperDetail(Long id);

    /** 创建试卷 */
    Long createPaper(ExamPaper paper);

    /** 编辑试卷信息 */
    void updatePaper(Long id, ExamPaper paper);

    /** 逻辑删除试卷 */
    void deletePaper(Long id);

    /** 发布/关闭试卷 */
    void updateStatus(Long id, String status);

    /** 向试卷添加题目 */
    void addQuestions(Long paperId, List<Long> questionIds, List<Integer> scores);

    /** 从试卷移除题目 */
    void removeQuestion(Long paperId, Long questionId);

    /** 调整题目排序 */
    void updateQuestionOrder(Long paperId, List<Map<String, Integer>> orders);

    /** 按筛选条件自动抽题组卷 */
    Map<String, Object> smartAssemble(Long paperId, Map<String, Object> rule);
}
