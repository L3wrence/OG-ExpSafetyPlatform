package com.cupk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.pojo.ExamRecord;
import com.cupk.vo.ExamSessionVO;

import java.util.List;
import java.util.Map;

/**
 * 考试引擎服务接口（核心：含自动评分）
 */
public interface ExamService {

    /** 学生可参加的考试列表（仅返回PUBLISHED且未考过的） */
    Page<Map<String, Object>> getAvailableExams(int pageNum, int pageSize, Long courseId);

    /** 开始考试，返回题目列表+计时信息 */
    ExamSessionVO startExam(Long paperId);

    /** 查询当前学生进行中的考试 */
    ExamSessionVO getInProgressExam(Long paperId);

    /** 提交答案 + 自动评分 */
    Map<String, Object> submitExam(Long recordId, List<Map<String, Object>> answers);

    /** 自动保存答案，幂等覆盖当前草稿 */
    Map<String, Object> saveAnswers(Long recordId, List<Map<String, Object>> answers);

    /** 自动/手动提交答案 + 自动评分 */
    Map<String, Object> submitExam(Long recordId, List<Map<String, Object>> answers, boolean autoSubmit);

    /** 我的考试记录列表 */
    Page<ExamRecord> getMyRecords(int pageNum, int pageSize, String status, Long courseId);

    /** 考试详情（含每题答题） */
    Map<String, Object> getRecordDetail(Long recordId);

    /** 我的错题本 */
    Page<Map<String, Object>> getWrongQuestions(int pageNum, int pageSize, String type, Long courseId);

    /** 错题知识点统计 */
    List<Map<String, Object>> getWrongQuestionStats();

    // ===== 考试统计（教师端） =====

    /** 考试总览 */
    Map<String, Object> getStatisticsOverview(Long paperId);

    /** 分数段分布 */
    List<Map<String, Object>> getScoreDistribution(Long paperId);

    /** 每题正确率 */
    List<Map<String, Object>> getQuestionAnalysis(Long paperId);

    /** 知识点薄弱分析 */
    List<Map<String, Object>> getKnowledgeAnalysis(Long courseId);

    // ===== 简答题批改（教师端） =====

    /** 待批改简答题的考试记录列表 */
    Page<Map<String, Object>> getPendingGradingRecords(int pageNum, int pageSize, Long paperId);

    /** 指定试卷的学生提交记录 */
    Page<Map<String, Object>> getPaperSubmissionRecords(int pageNum, int pageSize, Long paperId);

    /** 教师查看学生完整答卷及批改状态 */
    Map<String, Object> getGradingRecordDetail(Long recordId);

    /** 逐题批改简答题，批完后重算总分和通过状态 */
    Map<String, Object> gradeShortAnswer(Long recordId, List<Map<String, Object>> grades);
}
