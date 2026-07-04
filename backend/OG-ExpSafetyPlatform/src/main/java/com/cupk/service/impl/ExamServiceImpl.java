package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.*;
import com.cupk.pojo.*;
import com.cupk.service.ExamService;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 鑰冭瘯寮曟搸鏈嶅姟瀹炵幇锛堟牳蹇冿細鍚嚜鍔ㄨ瘎鍒嗗紩鎿庯級
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
        // 鏌ヨ宸插彂甯冪殑璇曞嵎
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaper::getStatus, "PUBLISHED")
               .eq(courseId != null, ExamPaper::getCourseId, courseId)
               .orderByDesc(ExamPaper::getCreateTime);

        Page<ExamPaper> paperPage = examPaperMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 杞崲涓篗ap锛岃繃婊ゅ凡鑰冭繃鐨勮瘯鍗凤紙TODO: 闇€瑕佸綋鍓嶇敤鎴稩D锛?
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
            throw new BusinessException(400, "?????");
        }

        // 鍒涘缓鑰冭瘯璁板綍
        ExamRecord record = new ExamRecord();
        record.setStudentId(UserContext.getUserId());
        record.setPaperId(paperId);
        record.setExperimentId(paper.getExperimentId());
        record.setStatus("IN_PROGRESS");
        record.setStartTime(new Date());
        examRecordMapper.insert(record);

        // 鏌ヨ璇曞嵎鍏宠仈鐨勯鐩?
        LambdaQueryWrapper<ExamPaperQuestion> eqWrapper = new LambdaQueryWrapper<>();
        eqWrapper.eq(ExamPaperQuestion::getPaperId, paperId)
                 .orderByAsc(ExamPaperQuestion::getOrderNum);
        List<ExamPaperQuestion> eqList = examPaperQuestionMapper.selectList(eqWrapper);

        // 缁勮棰樼洰鍒楄〃锛堜笉鍚瓟妗堬級
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

        // 璁＄畻缁撴潫鏃堕棿
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
            throw new BusinessException(400, "??????");
        }

        ExamPaper paper = examPaperMapper.selectById(record.getPaperId());

        // 鏍￠獙鏄惁瓒呮椂
        long now = System.currentTimeMillis();
        long examEndTime = record.getStartTime().getTime() + paper.getDuration() * 60 * 1000L;
        if (now > examEndTime) {
            record.setStatus("SUBMITTED");
            record.setSubmitTime(new Date());
            examRecordMapper.updateById(record);
            throw new BusinessException(400, "鑰冭瘯宸茶秴鏃讹紝鏃犳硶鎻愪氦");
        }

        int objectiveScore = 0;
        int correctCount = 0;
        List<Map<String, Object>> answerDetails = new ArrayList<>();

        for (Map<String, Object> ans : answers) {
            Long questionId = Long.valueOf(ans.get("questionId").toString());
            String studentAnswer = (String) ans.get("answer");

            Question question = questionMapper.selectById(questionId);
            if (question == null) continue; // 璺宠繃涓嶅瓨鍦ㄧ殑棰樼洰
            ExamAnswer examAnswer = new ExamAnswer();
            examAnswer.setRecordId(recordId);
            examAnswer.setQuestionId(questionId);
            examAnswer.setKnowledgeId(questionId);
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
                    examAnswer.setIsCorrect(null); // 寰呮壒鏀?
                    examAnswer.setCorrectFlag(null);
                    examAnswer.setScore(0);
                    examAnswerMapper.insert(examAnswer);
                    continue; // 绠€绛旈鏆備笉鍙備笌璇勫垎
            }

            if (correct) {
                objectiveScore += getQuestionScore(record.getPaperId(), questionId);
                correctCount++;
                examAnswer.setIsCorrect(1);
                examAnswer.setCorrectFlag(1);
                examAnswer.setScore(getQuestionScore(record.getPaperId(), questionId));
            } else {
                examAnswer.setIsCorrect(0);
                examAnswer.setCorrectFlag(0);
                examAnswer.setScore(0);
            }
            examAnswerMapper.insert(examAnswer);

            // 缁勮绛旈璇︽儏
            Map<String, Object> detail = new HashMap<>();
            detail.put("questionId", questionId);
            detail.put("studentAnswer", studentAnswer);
            detail.put("correctAnswer", question.getAnswer());
            detail.put("isCorrect", correct);
            detail.put("analysis", question.getAnalysis());
            answerDetails.add(detail);
        }

        // 鏇存柊鑰冭瘯璁板綍
        record.setObjectiveScore(objectiveScore);
        record.setTotalScore(objectiveScore); // 绠€绛旈鏆備笉璁″叆
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
        // 鏌ヨ褰撳墠瀛︾敓绛旈敊鐨勯鐩?
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

        // 缁勮閿欓淇℃伅
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
        // 1. 鏌ュ嚭褰撳墠瀛︾敓鎵€鏈夎€冭瘯璁板綍
        Long studentId = UserContext.getUserId();
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getStudentId, studentId);
        List<ExamRecord> records = examRecordMapper.selectList(recordWrapper);
        if (records.isEmpty()) return new ArrayList<>();

        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());

        // 2. 鏌ュ嚭鎵€鏈夐敊棰?
        LambdaQueryWrapper<ExamAnswer> answerWrapper = new LambdaQueryWrapper<>();
        answerWrapper.in(ExamAnswer::getRecordId, recordIds)
                      .eq(ExamAnswer::getIsCorrect, 0);
        List<ExamAnswer> wrongAnswers = examAnswerMapper.selectList(answerWrapper);
        if (wrongAnswers.isEmpty()) return new ArrayList<>();

        // 3. 鎸夌煡璇嗙偣鍒嗙粍缁熻
        Map<String, Integer> knowledgeCount = new LinkedHashMap<>();
        for (ExamAnswer ans : wrongAnswers) {
            Question q = questionMapper.selectById(ans.getQuestionId());
            if (q != null && q.getKnowledgePoint() != null) {
                knowledgeCount.merge(q.getKnowledgePoint(), 1, Integer::sum);
            }
        }

        // 4. 鎸夋暟閲忛檷搴忔帓鍒?
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
        // 鏌ヨ璇ヨ瘯鍗锋墍鏈夊凡鎻愪氦鐨勮€冭瘯璁板綍
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

        // 鑾峰彇璇曞嵎淇℃伅
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

        // 瀹氫箟鍒嗘暟娈?
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
        // 1. 鑾峰彇璇曞嵎-棰樼洰鍏宠仈锛堟寜鎺掑簭鍙峰崌搴忥級
        LambdaQueryWrapper<ExamPaperQuestion> eqWrapper = new LambdaQueryWrapper<>();
        eqWrapper.eq(ExamPaperQuestion::getPaperId, paperId)
                 .orderByAsc(ExamPaperQuestion::getOrderNum);
        List<ExamPaperQuestion> eqList = examPaperQuestionMapper.selectList(eqWrapper);

        // 2. 鑾峰彇璇ヨ瘯鍗锋墍鏈夋彁浜ょ殑鑰冭瘯璁板綍
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getPaperId, paperId)
                     .eq(ExamRecord::getStatus, "SUBMITTED");
        List<ExamRecord> records = examRecordMapper.selectList(recordWrapper);
        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamPaperQuestion eq : eqList) {
            Question q = questionMapper.selectById(eq.getQuestionId());
            if (q == null) continue;

            // 缁熻璇ラ姝ｇ‘鐜?
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
        // 1. 鏌ヨ璇ヨ绋嬩笅鎵€鏈夐鐩?
        LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<>();
        questionWrapper.eq(courseId != null, Question::getCourseId, courseId)
                       .isNotNull(Question::getKnowledgePoint);
        List<Question> questions = questionMapper.selectList(questionWrapper);
        List<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());

        if (questionIds.isEmpty()) return new ArrayList<>();

        // 2. 鑾峰彇鎵€鏈夊凡鎻愪氦鐨勮€冭瘯璁板綍ID
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getStatus, "SUBMITTED");
        List<ExamRecord> records = examRecordMapper.selectList(recordWrapper);
        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());

        // 3. 缁熻姣忎釜鐭ヨ瘑鐐圭殑姝ｇ‘鐜?
        // 鍏堟寜鐭ヨ瘑鐐瑰垎缁勯鐩?
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
            double weakLevel = total > 0 ? 100 - correctRate : 0; // 钖勫急绋嬪害锛岃秺楂樿秺钖勫急

            Map<String, Object> item = new HashMap<>();
            item.put("knowledgePoint", knowledge);
            item.put("totalAnswers", total);
            item.put("correctAnswers", correct);
            item.put("correctRate", Math.round(correctRate * 10.0) / 10.0);
            item.put("weakLevel", Math.round(weakLevel * 10.0) / 10.0);
            item.put("questionCount", qIds.size());
            result.add(item);
        }

        // 鎸夎杽寮辩▼搴﹂檷搴忔帓鍒?
        result.sort((a, b) -> Double.compare(
                (double) b.get("weakLevel"), (double) a.get("weakLevel")));
        return result;
    }

    // ===== 绠€绛旈鎵规敼锛堟暀甯堢锛?=====

    @Override
    public Page<Map<String, Object>> getPendingGradingRecords(int pageNum, int pageSize, Long paperId) {
        // 鏌ユ壘鍚湁绠€绛旈锛坕sCorrect IS NULL锛夌殑宸叉彁浜よ€冭瘯璁板綍
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
            // 缁熻寰呮壒鏀规暟閲?
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
            throw new BusinessException(400, "鑰冭瘯璁板綍鐘舵€佸紓甯革紝鏃犳硶鎵规敼");
        }

        ExamPaper paper = examPaperMapper.selectById(record.getPaperId());
        int subjectiveScore = 0;

        for (Map<String, Object> g : grades) {
            Long answerId = Long.valueOf(g.get("answerId").toString());
            int score = Integer.parseInt(g.get("score").toString());

            ExamAnswer answer = examAnswerMapper.selectById(answerId);
            if (answer == null || !answer.getRecordId().equals(recordId)) {
                throw new BusinessException(400, "绛旈璁板綍涓嶅瓨鍦? " + answerId);
            }

            answer.setIsCorrect(score > 0 ? 1 : 0);
            answer.setCorrectFlag(score > 0 ? 1 : 0);
            answer.setScore(score);
            examAnswerMapper.updateById(answer);
            subjectiveScore += score;
        }

        // 閲嶇畻鎬诲垎鍜岄€氳繃鐘舵€?
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

    // ===== 绉佹湁宸ュ叿鏂规硶 =====

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
