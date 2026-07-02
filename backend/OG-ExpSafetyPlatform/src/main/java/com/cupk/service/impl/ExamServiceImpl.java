package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.*;
import com.cupk.pojo.*;
import com.cupk.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 考试引擎服务实现（核心：含自动评分引擎）
 */
@Service
public class ExamServiceImpl implements ExamService {

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private ExamPaperQuestionMapper examPaperQuestionMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private ExamAnswerMapper examAnswerMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public Page<Map<String, Object>> getAvailableExams(int pageNum, int pageSize, Long courseId) {
        // 查询已发布的试卷
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaper::getStatus, "PUBLISHED")
               .eq(courseId != null, ExamPaper::getCourseId, courseId)
               .orderByDesc(ExamPaper::getCreateTime);

        Page<ExamPaper> paperPage = examPaperMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 转换为Map，过滤已考过的试卷（TODO: 需要当前用户ID）
        Page<Map<String, Object>> result = new Page<>(pageNum, pageSize, paperPage.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (ExamPaper paper : paperPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", paper.getId());
            item.put("title", paper.getTitle());
            item.put("description", paper.getDescription());
            item.put("totalScore", paper.getTotalScore());
            item.put("passScore", paper.getPassScore());
            item.put("duration", paper.getDuration());
            item.put("startTime", paper.getStartTime());
            item.put("endTime", paper.getEndTime());
            records.add(item);
        }
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> startExam(Long paperId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null || !"PUBLISHED".equals(paper.getStatus())) {
            throw new RuntimeException("试卷不可用");
        }

        // 创建考试记录
        ExamRecord record = new ExamRecord();
        record.setStudentId(1L); // TODO: 从LoginUserHolder获取
        record.setPaperId(paperId);
        record.setStatus("IN_PROGRESS");
        record.setStartTime(new Date());
        examRecordMapper.insert(record);

        // 查询试卷关联的题目
        LambdaQueryWrapper<ExamPaperQuestion> eqWrapper = new LambdaQueryWrapper<>();
        eqWrapper.eq(ExamPaperQuestion::getPaperId, paperId)
                 .orderByAsc(ExamPaperQuestion::getOrderNum);
        List<ExamPaperQuestion> eqList = examPaperQuestionMapper.selectList(eqWrapper);

        // 组装题目列表（不含答案）
        List<Map<String, Object>> questions = new ArrayList<>();
        for (ExamPaperQuestion eq : eqList) {
            Question q = questionMapper.selectById(eq.getQuestionId());
            if (q != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", q.getId());
                item.put("type", q.getType());
                item.put("content", q.getContent());
                item.put("options", q.getOptions());
                item.put("score", eq.getScore());
                item.put("orderNum", eq.getOrderNum());
                questions.add(item);
            }
        }

        // 计算结束时间
        long endTimeMillis = System.currentTimeMillis() + paper.getDuration() * 60 * 1000L;

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getId());
        result.put("questions", questions);
        result.put("startTime", record.getStartTime());
        result.put("duration", paper.getDuration());
        result.put("endTime", new Date(endTimeMillis));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitExam(Long recordId, List<Map<String, Object>> answers) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || !"IN_PROGRESS".equals(record.getStatus())) {
            throw new RuntimeException("考试状态异常");
        }

        ExamPaper paper = examPaperMapper.selectById(record.getPaperId());

        // 校验是否超时
        long now = System.currentTimeMillis();
        long examEndTime = record.getStartTime().getTime() + paper.getDuration() * 60 * 1000L;
        if (now > examEndTime) {
            record.setStatus("SUBMITTED");
            record.setSubmitTime(new Date());
            examRecordMapper.updateById(record);
            throw new RuntimeException("考试已超时，无法提交");
        }

        int objectiveScore = 0;
        int correctCount = 0;
        List<Map<String, Object>> answerDetails = new ArrayList<>();

        for (Map<String, Object> ans : answers) {
            Long questionId = Long.valueOf(ans.get("questionId").toString());
            String studentAnswer = (String) ans.get("answer");

            Question question = questionMapper.selectById(questionId);
            ExamAnswer examAnswer = new ExamAnswer();
            examAnswer.setRecordId(recordId);
            examAnswer.setQuestionId(questionId);
            examAnswer.setStudentAnswer(studentAnswer);

            boolean correct = false;
            switch (question.getType()) {
                case "SINGLE":
                case "JUDGE":
                    correct = studentAnswer != null && studentAnswer.equalsIgnoreCase(question.getAnswer());
                    break;
                case "MULTIPLE":
                    Set<String> studentSet = parseAnswerSet(studentAnswer);
                    Set<String> correctSet = parseAnswerSet(question.getAnswer());
                    correct = studentSet.equals(correctSet);
                    break;
                case "SHORT_ANSWER":
                    examAnswer.setIsCorrect(null); // 待批改
                    examAnswer.setScore(0);
                    examAnswerMapper.insert(examAnswer);
                    continue; // 简答题暂不参与评分
            }

            if (correct) {
                objectiveScore += getQuestionScore(record.getPaperId(), questionId);
                correctCount++;
                examAnswer.setIsCorrect(1);
                examAnswer.setScore(getQuestionScore(record.getPaperId(), questionId));
            } else {
                examAnswer.setIsCorrect(0);
                examAnswer.setScore(0);
            }
            examAnswerMapper.insert(examAnswer);

            // 组装答题详情
            Map<String, Object> detail = new HashMap<>();
            detail.put("questionId", questionId);
            detail.put("studentAnswer", studentAnswer);
            detail.put("correctAnswer", question.getAnswer());
            detail.put("isCorrect", correct);
            detail.put("analysis", question.getAnalysis());
            answerDetails.add(detail);
        }

        // 更新考试记录
        record.setObjectiveScore(objectiveScore);
        record.setTotalScore(objectiveScore); // 简答题暂不计入
        record.setPassed(objectiveScore >= paper.getPassScore() ? 1 : 0);
        record.setStatus("SUBMITTED");
        record.setSubmitTime(new Date());
        examRecordMapper.updateById(record);

        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", record.getTotalScore());
        result.put("objectiveScore", objectiveScore);
        result.put("passed", record.getPassed() == 1);
        result.put("correctCount", correctCount);
        result.put("totalCount", answers.size());
        result.put("answerDetails", answerDetails);
        return result;
    }

    @Override
    public Page<ExamRecord> getMyRecords(int pageNum, int pageSize, String status) {
        Page<ExamRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getStudentId, 1L) // TODO: 从LoginUserHolder获取
               .eq(status != null && !status.isEmpty(), ExamRecord::getStatus, status)
               .orderByDesc(ExamRecord::getCreateTime);
        return examRecordMapper.selectPage(page, wrapper);
    }

    @Override
    public Map<String, Object> getRecordDetail(Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);

        LambdaQueryWrapper<ExamAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamAnswer::getRecordId, recordId);
        List<ExamAnswer> answers = examAnswerMapper.selectList(wrapper);

        List<Map<String, Object>> answerList = new ArrayList<>();
        for (ExamAnswer ans : answers) {
            Question q = questionMapper.selectById(ans.getQuestionId());
            Map<String, Object> item = new HashMap<>();
            item.put("question", q);
            item.put("studentAnswer", ans.getStudentAnswer());
            item.put("correctAnswer", q != null ? q.getAnswer() : null);
            item.put("isCorrect", ans.getIsCorrect());
            item.put("analysis", q != null ? q.getAnalysis() : null);
            item.put("score", ans.getScore());
            answerList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("record", record);
        result.put("answers", answerList);
        return result;
    }

    @Override
    public Page<Map<String, Object>> getWrongQuestions(int pageNum, int pageSize, String type, Long courseId) {
        // 查询当前学生答错的题目
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getStudentId, 1L); // TODO
        List<ExamRecord> records = examRecordMapper.selectList(recordWrapper);
        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());

        if (recordIds.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        LambdaQueryWrapper<ExamAnswer> answerWrapper = new LambdaQueryWrapper<>();
        answerWrapper.in(ExamAnswer::getRecordId, recordIds)
                      .eq(ExamAnswer::getIsCorrect, 0);
        List<ExamAnswer> wrongAnswers = examAnswerMapper.selectList(answerWrapper);

        // 组装错题信息
        List<Map<String, Object>> wrongList = new ArrayList<>();
        for (ExamAnswer ans : wrongAnswers) {
            Question q = questionMapper.selectById(ans.getQuestionId());
            if (q != null && (type == null || type.equals(q.getType()))
                    && (courseId == null || courseId.equals(q.getCourseId()))) {
                Map<String, Object> item = new HashMap<>();
                item.put("question", q);
                item.put("wrongAnswer", ans.getStudentAnswer());
                item.put("correctAnswer", q.getAnswer());
                item.put("analysis", q.getAnalysis());
                wrongList.add(item);
            }
        }

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize, wrongList.size());
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, wrongList.size());
        page.setRecords(wrongList.subList(start, end));
        return page;
    }

    @Override
    public List<Map<String, Object>> getWrongQuestionStats() {
        // TODO: 按知识点统计错题
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> getStatisticsOverview(Long paperId) {
        // TODO
        return new HashMap<>();
    }

    @Override
    public List<Map<String, Object>> getScoreDistribution(Long paperId) {
        // TODO
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getQuestionAnalysis(Long paperId) {
        // TODO
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getKnowledgeAnalysis(Long courseId) {
        // TODO
        return new ArrayList<>();
    }

    // ===== 私有工具方法 =====

    private Set<String> parseAnswerSet(String answer) {
        if (answer == null || answer.isEmpty()) return Collections.emptySet();
        return Arrays.stream(answer.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private int getQuestionScore(Long paperId, Long questionId) {
        LambdaQueryWrapper<ExamPaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaperQuestion::getPaperId, paperId)
               .eq(ExamPaperQuestion::getQuestionId, questionId);
        ExamPaperQuestion eq = examPaperQuestionMapper.selectOne(wrapper);
        return eq != null ? eq.getScore() : 0;
    }
}
