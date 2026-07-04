package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.*;
import com.cupk.pojo.*;
import com.cupk.service.ExamService;
import com.cupk.exception.BusinessException;
import com.cupk.common.UserContext;
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
            throw new BusinessException(400, "试卷不可用");
        }

        // 创建考试记录
        ExamRecord record = new ExamRecord();
        record.setStudentId(UserContext.getUserId());
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
            throw new BusinessException(400, "考试状态异常");
        }

        ExamPaper paper = examPaperMapper.selectById(record.getPaperId());

        // 校验是否超时
        long now = System.currentTimeMillis();
        long examEndTime = record.getStartTime().getTime() + paper.getDuration() * 60 * 1000L;
        if (now > examEndTime) {
            record.setStatus("SUBMITTED");
            record.setSubmitTime(new Date());
            examRecordMapper.updateById(record);
            throw new BusinessException(400, "考试已超时，无法提交");
        }

        int objectiveScore = 0;
        int correctCount = 0;
        List<Map<String, Object>> answerDetails = new ArrayList<>();

        for (Map<String, Object> ans : answers) {
            Long questionId = Long.valueOf(ans.get("questionId").toString());
            String studentAnswer = (String) ans.get("answer");

            Question question = questionMapper.selectById(questionId);
            if (question == null) continue; // 跳过不存在的题目
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
        wrapper.eq(ExamRecord::getStudentId, UserContext.getUserId())
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
        recordWrapper.eq(ExamRecord::getStudentId, UserContext.getUserId());
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
        // 1. 查出当前学生所有考试记录
        Long studentId = UserContext.getUserId();
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getStudentId, studentId);
        List<ExamRecord> records = examRecordMapper.selectList(recordWrapper);
        if (records.isEmpty()) return new ArrayList<>();

        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());

        // 2. 查出所有错题
        LambdaQueryWrapper<ExamAnswer> answerWrapper = new LambdaQueryWrapper<>();
        answerWrapper.in(ExamAnswer::getRecordId, recordIds)
                      .eq(ExamAnswer::getIsCorrect, 0);
        List<ExamAnswer> wrongAnswers = examAnswerMapper.selectList(answerWrapper);
        if (wrongAnswers.isEmpty()) return new ArrayList<>();

        // 3. 按知识点分组统计
        Map<String, Integer> knowledgeCount = new LinkedHashMap<>();
        for (ExamAnswer ans : wrongAnswers) {
            Question q = questionMapper.selectById(ans.getQuestionId());
            if (q != null && q.getKnowledgePoint() != null) {
                knowledgeCount.merge(q.getKnowledgePoint(), 1, Integer::sum);
            }
        }

        // 4. 按数量降序排列
        return knowledgeCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("knowledgePoint", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getStatisticsOverview(Long paperId) {
        // 查询该试卷所有已提交的考试记录
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getPaperId, paperId)
               .eq(ExamRecord::getStatus, "SUBMITTED");
        List<ExamRecord> records = examRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("totalCount", 0);
            empty.put("avgScore", 0);
            empty.put("passRate", 0);
            empty.put("maxScore", 0);
            empty.put("minScore", 0);
            return empty;
        }

        int totalCount = records.size();
        double avgScore = records.stream().mapToInt(ExamRecord::getTotalScore).average().orElse(0);
        int maxScore = records.stream().mapToInt(ExamRecord::getTotalScore).max().orElse(0);
        int minScore = records.stream().mapToInt(ExamRecord::getTotalScore).min().orElse(0);
        long passCount = records.stream().filter(r -> r.getPassed() == 1).count();
        double passRate = (double) passCount / totalCount * 100;

        // 获取试卷信息
        ExamPaper paper = examPaperMapper.selectById(paperId);

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", totalCount);
        result.put("avgScore", Math.round(avgScore * 10.0) / 10.0);
        result.put("passRate", Math.round(passRate * 10.0) / 10.0);
        result.put("maxScore", maxScore);
        result.put("minScore", minScore);
        result.put("passScore", paper != null ? paper.getPassScore() : 0);
        result.put("totalScore", paper != null ? paper.getTotalScore() : 0);
        return result;
    }

    @Override
    public List<Map<String, Object>> getScoreDistribution(Long paperId) {
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getPaperId, paperId)
               .eq(ExamRecord::getStatus, "SUBMITTED");
        List<ExamRecord> records = examRecordMapper.selectList(wrapper);

        // 定义分数段
        int[][] ranges = {{0, 59}, {60, 69}, {70, 79}, {80, 89}, {90, 100}};
        String[] labels = {"0-59", "60-69", "70-79", "80-89", "90-100"};

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < ranges.length; i++) {
            int low = ranges[i][0], high = ranges[i][1];
            long count = records.stream()
                    .filter(r -> r.getTotalScore() != null && r.getTotalScore() >= low && r.getTotalScore() <= high)
                    .count();

            Map<String, Object> item = new HashMap<>();
            item.put("range", labels[i]);
            item.put("count", (int) count);
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getQuestionAnalysis(Long paperId) {
        // 1. 获取试卷-题目关联（按排序号升序）
        LambdaQueryWrapper<ExamPaperQuestion> eqWrapper = new LambdaQueryWrapper<>();
        eqWrapper.eq(ExamPaperQuestion::getPaperId, paperId)
                 .orderByAsc(ExamPaperQuestion::getOrderNum);
        List<ExamPaperQuestion> eqList = examPaperQuestionMapper.selectList(eqWrapper);

        // 2. 获取该试卷所有提交的考试记录
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getPaperId, paperId)
                     .eq(ExamRecord::getStatus, "SUBMITTED");
        List<ExamRecord> records = examRecordMapper.selectList(recordWrapper);
        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamPaperQuestion eq : eqList) {
            Question q = questionMapper.selectById(eq.getQuestionId());
            if (q == null) continue;

            // 统计该题正确率
            int totalAnswers = 0;
            int correctAnswers = 0;
            if (!recordIds.isEmpty()) {
                LambdaQueryWrapper<ExamAnswer> answerWrapper = new LambdaQueryWrapper<>();
                answerWrapper.eq(ExamAnswer::getQuestionId, eq.getQuestionId())
                             .in(ExamAnswer::getRecordId, recordIds);
                List<ExamAnswer> answers = examAnswerMapper.selectList(answerWrapper);
                totalAnswers = answers.size();
                correctAnswers = (int) answers.stream().filter(a -> a.getIsCorrect() != null && a.getIsCorrect() == 1).count();
            }

            double correctRate = totalAnswers > 0 ? (double) correctAnswers / totalAnswers * 100 : 0;

            Map<String, Object> item = new HashMap<>();
            item.put("questionId", q.getId());
            item.put("content", q.getContent());
            item.put("type", q.getType());
            item.put("difficulty", q.getDifficulty());
            item.put("knowledgePoint", q.getKnowledgePoint());
            item.put("totalAnswers", totalAnswers);
            item.put("correctAnswers", correctAnswers);
            item.put("correctRate", Math.round(correctRate * 10.0) / 10.0);
            item.put("orderNum", eq.getOrderNum());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getKnowledgeAnalysis(Long courseId) {
        // 1. 查询该课程下所有题目
        LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<>();
        questionWrapper.eq(courseId != null, Question::getCourseId, courseId)
                       .isNotNull(Question::getKnowledgePoint);
        List<Question> questions = questionMapper.selectList(questionWrapper);
        List<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());

        if (questionIds.isEmpty()) return new ArrayList<>();

        // 2. 获取所有已提交的考试记录ID
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getStatus, "SUBMITTED");
        List<ExamRecord> records = examRecordMapper.selectList(recordWrapper);
        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());

        // 3. 统计每个知识点的正确率
        // 先按知识点分组题目
        Map<String, List<Long>> knowledgeQuestionMap = new LinkedHashMap<>();
        for (Question q : questions) {
            if (q.getKnowledgePoint() != null) {
                knowledgeQuestionMap.computeIfAbsent(q.getKnowledgePoint(), k -> new ArrayList<>()).add(q.getId());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<String, List<Long>> entry : knowledgeQuestionMap.entrySet()) {
            String knowledge = entry.getKey();
            List<Long> qIds = entry.getValue();

            LambdaQueryWrapper<ExamAnswer> answerWrapper = new LambdaQueryWrapper<>();
            answerWrapper.in(ExamAnswer::getQuestionId, qIds);
            if (!recordIds.isEmpty()) {
                answerWrapper.in(ExamAnswer::getRecordId, recordIds);
            }
            List<ExamAnswer> answers = examAnswerMapper.selectList(answerWrapper);

            int total = answers.size();
            int correct = (int) answers.stream().filter(a -> a.getIsCorrect() != null && a.getIsCorrect() == 1).count();

            double correctRate = total > 0 ? (double) correct / total * 100 : 0;
            double weakLevel = total > 0 ? 100 - correctRate : 0; // 薄弱程度，越高越薄弱

            Map<String, Object> item = new HashMap<>();
            item.put("knowledgePoint", knowledge);
            item.put("totalAnswers", total);
            item.put("correctAnswers", correct);
            item.put("correctRate", Math.round(correctRate * 10.0) / 10.0);
            item.put("weakLevel", Math.round(weakLevel * 10.0) / 10.0);
            item.put("questionCount", qIds.size());
            result.add(item);
        }

        // 按薄弱程度降序排列
        result.sort((a, b) -> Double.compare(
                (double) b.get("weakLevel"), (double) a.get("weakLevel")));
        return result;
    }

    // ===== 简答题批改（教师端） =====

    @Override
    public Page<Map<String, Object>> getPendingGradingRecords(int pageNum, int pageSize, Long paperId) {
        // 查找含有简答题（isCorrect IS NULL）的已提交考试记录
        LambdaQueryWrapper<ExamAnswer> answerWrapper = new LambdaQueryWrapper<>();
        answerWrapper.isNull(ExamAnswer::getIsCorrect)
                     .select(ExamAnswer::getRecordId);
        List<Long> recordIds = examAnswerMapper.selectList(answerWrapper).stream()
                .map(ExamAnswer::getRecordId)
                .distinct()
                .collect(Collectors.toList());

        Page<ExamRecord> page = new Page<>(pageNum, pageSize);
        if (recordIds.isEmpty()) {
            page.setTotal(0);
            page.setRecords(Collections.emptyList());
            return convertToMapPage(page);
        }

        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ExamRecord::getId, recordIds)
               .eq(ExamRecord::getStatus, "SUBMITTED")
               .eq(paperId != null, ExamRecord::getPaperId, paperId)
               .orderByAsc(ExamRecord::getSubmitTime);
        Page<ExamRecord> recordPage = examRecordMapper.selectPage(page, wrapper);

        Page<Map<String, Object>> result = new Page<>(pageNum, pageSize, recordPage.getTotal());
        List<Map<String, Object>> items = new ArrayList<>();
        for (ExamRecord r : recordPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("recordId", r.getId());
            item.put("studentId", r.getStudentId());
            item.put("paperId", r.getPaperId());
            item.put("objectiveScore", r.getObjectiveScore());
            item.put("totalScore", r.getTotalScore());
            item.put("submitTime", r.getSubmitTime());
            // 统计待批改数量
            LambdaQueryWrapper<ExamAnswer> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(ExamAnswer::getRecordId, r.getId())
                       .isNull(ExamAnswer::getIsCorrect);
            item.put("pendingCount", examAnswerMapper.selectCount(countWrapper));
            items.add(item);
        }
        result.setRecords(items);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> gradeShortAnswer(Long recordId, List<Map<String, Object>> grades) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || !"SUBMITTED".equals(record.getStatus())) {
            throw new BusinessException(400, "考试记录状态异常，无法批改");
        }

        ExamPaper paper = examPaperMapper.selectById(record.getPaperId());
        int subjectiveScore = 0;

        for (Map<String, Object> g : grades) {
            Long answerId = Long.valueOf(g.get("answerId").toString());
            int score = Integer.parseInt(g.get("score").toString());

            ExamAnswer answer = examAnswerMapper.selectById(answerId);
            if (answer == null || !answer.getRecordId().equals(recordId)) {
                throw new BusinessException(400, "答题记录不存在: " + answerId);
            }

            answer.setIsCorrect(score > 0 ? 1 : 0);
            answer.setScore(score);
            examAnswerMapper.updateById(answer);
            subjectiveScore += score;
        }

        // 重算总分和通过状态
        int totalScore = (record.getObjectiveScore() != null ? record.getObjectiveScore() : 0) + subjectiveScore;
        record.setSubjectiveScore(subjectiveScore);
        record.setTotalScore(totalScore);
        record.setPassed(totalScore >= paper.getPassScore() ? 1 : 0);
        examRecordMapper.updateById(record);

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", recordId);
        result.put("subjectiveScore", subjectiveScore);
        result.put("totalScore", totalScore);
        result.put("passed", record.getPassed() == 1);
        return result;
    }

    private Page<Map<String, Object>> convertToMapPage(Page<ExamRecord> source) {
        Page<Map<String, Object>> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        // simplified - just return empty
        return target;
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
