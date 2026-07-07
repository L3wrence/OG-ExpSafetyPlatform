package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.enums.ExamRecordStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cupk.mapper.*;
import com.cupk.pojo.*;
import com.cupk.service.AdmissionService;
import com.cupk.service.ExamService;
import com.cupk.service.LearningTaskService;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.vo.ExamSessionQuestionVO;
import com.cupk.vo.ExamSessionVO;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Map<String, Object>>> SNAPSHOT_TYPE = new TypeReference<>() {};

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

    @Autowired
    private AdmissionService admissionService;

    @Autowired
    private LearningTaskService learningTaskService;

    @Autowired
    private CourseStudentMapper courseStudentMapper;

    @Autowired
    private LabCourseMapper courseMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Page<Map<String, Object>> getAvailableExams(int pageNum, int pageSize, Long courseId) {
        // 鏌ヨ宸插彂甯冪殑璇曞嵎
        Long studentId = UserContext.getUserId();
        List<Long> enrolledCourseIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                        .select(CourseStudent::getCourseId)
                        .eq(CourseStudent::getStudentId, studentId)
                        .eq(CourseStudent::getStatus, 1))
                .stream().map(CourseStudent::getCourseId).toList();
        if (enrolledCourseIds.isEmpty() || (courseId != null && !enrolledCourseIds.contains(courseId))) {
            Page<Map<String, Object>> empty = new Page<>(pageNum, pageSize, 0);
            empty.setRecords(Collections.emptyList());
            return empty;
        }
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        Date now = new Date();
        wrapper.eq(ExamPaper::getStatus, "PUBLISHED")
               .in(ExamPaper::getCourseId, enrolledCourseIds)
               .eq(courseId != null, ExamPaper::getCourseId, courseId)
               .and(w -> w.isNull(ExamPaper::getStartTime).or().le(ExamPaper::getStartTime, now))
               .and(w -> w.isNull(ExamPaper::getEndTime).or().ge(ExamPaper::getEndTime, now))
               .orderByDesc(ExamPaper::getCreateTime);

        Page<ExamPaper> paperPage = examPaperMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 转换为前端需要的考试列表结构。
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
            int usedAttempts = usedAttempts(studentId, paper.getId());
            int limit = attemptLimit(paper);
            item.put("endTime", paper.getEndTime());
            item.put("attemptLimit", limit);
            item.put("usedAttempts", usedAttempts);
            item.put("remainingAttempts", Math.max(limit - usedAttempts, 0));
            if (usedAttempts < limit) {
                records.add(item);
            }
        }
        result.setRecords(records);
        result.setTotal(records.size());
        return result;
    }

    @Override
    @Transactional
    public ExamSessionVO startExam(Long paperId) {
        if (!UserContext.isStudent()) {
            throw new BusinessException(403, "只有学生可以参加考试");
        }
        ExamPaper paper = requireAvailablePaper(paperId);
        assertStudentInCourse(UserContext.getUserId(), paper.getCourseId());

        Long studentId = UserContext.getUserId();
        ExamRecord inProgress = findInProgressRecord(studentId, paperId);
        if (inProgress != null) {
            if (timedOut(inProgress)) {
                submitExam(inProgress.getId(), Collections.emptyList(), true);
                return null;
            }
            return toSessionVO(inProgress, paper, true);
        }
        if (usedAttempts(studentId, paperId) >= attemptLimit(paper)) {
            throw new BusinessException(409, "已达到该试卷考试次数上限");
        }

        List<Map<String, Object>> snapshots = buildQuestionSnapshot(paper);
        if (snapshots.isEmpty()) {
            throw new BusinessException(400, "试卷未配置题目");
        }

        Date startTime = new Date();
        Date examEndTime = new Date(startTime.getTime() + durationMinutes(paper) * 60 * 1000L);
        if (paper.getEndTime() != null && paper.getEndTime().before(examEndTime)) {
            examEndTime = paper.getEndTime();
        }
        ExamRecord record = new ExamRecord();
        record.setStudentId(studentId);
        record.setPaperId(paperId);
        record.setExperimentId(paper.getExperimentId());
        record.setStatus(ExamRecordStatus.IN_PROGRESS.name());
        record.setStartTime(startTime);
        record.setEndTime(examEndTime);
        record.setQuestionSnapshotJson(writeJson(snapshots));
        examRecordMapper.insert(record);
        return toSessionVO(record, paper, false);
    }

    @Override
    @Transactional
    public ExamSessionVO getInProgressExam(Long paperId) {
        if (!UserContext.isStudent()) {
            throw new BusinessException(403, "只有学生可以查询自己的考试");
        }
        ExamRecord record = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStudentId, UserContext.getUserId())
                .eq(paperId != null, ExamRecord::getPaperId, paperId)
                .eq(ExamRecord::getStatus, ExamRecordStatus.IN_PROGRESS.name())
                .orderByDesc(ExamRecord::getStartTime)
                .last("LIMIT 1"));
        if (record == null) {
            return null;
        }
        if (timedOut(record)) {
            submitExam(record.getId(), Collections.emptyList(), true);
            return null;
        }
        ExamPaper paper = examPaperMapper.selectById(record.getPaperId());
        return toSessionVO(record, paper, true);
    }

    @Override
    @Transactional
    public Map<String, Object> submitExam(Long recordId, List<Map<String, Object>> answers) {
        return submitExam(recordId, answers, false);
    }

    @Override
    @Transactional
    public Map<String, Object> saveAnswers(Long recordId, List<Map<String, Object>> answers) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || !ExamRecordStatus.IN_PROGRESS.name().equals(record.getStatus())) {
            throw new BusinessException(400, "考试状态异常");
        }
        if (!UserContext.getUserId().equals(record.getStudentId())) {
            throw new BusinessException(403, "不能保存他人的考试记录");
        }
        if (timedOut(record)) {
            submitExam(recordId, answers, true);
            throw new BusinessException(400, "考试已超时，系统已自动交卷");
        }
        List<Map<String, Object>> snapshots = loadSnapshot(record);
        Map<Long, Map<String, Object>> snapshotMap = snapshotByQuestionId(snapshots);
        Map<Long, String> normalized = normalizeAnswers(answers, snapshotMap);
        upsertAnswers(recordId, normalized, snapshotMap, false);
        Date savedAt = new Date();
        record.setLastSaveTime(savedAt);
        examRecordMapper.updateById(record);
        Map<String, Object> result = new HashMap<>();
        result.put("savedAt", savedAt);
        result.put("answerCount", normalized.size());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitExam(Long recordId, List<Map<String, Object>> answers, boolean autoSubmit) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "考试记录不存在");
        }
        if (!UserContext.getUserId().equals(record.getStudentId())) {
            throw new BusinessException(403, "不能提交他人的考试记录");
        }
        canonicalizeLegacyStatus(record);
        if (!ExamRecordStatus.IN_PROGRESS.name().equals(record.getStatus())) {
            return buildSubmitResult(record, examPaperMapper.selectById(record.getPaperId()), null, autoSubmit);
        }
        ExamPaper paper = examPaperMapper.selectById(record.getPaperId());
        if (paper == null) {
            throw new BusinessException(404, "试卷不存在");
        }
        List<Map<String, Object>> snapshots = loadSnapshot(record);
        Map<Long, Map<String, Object>> snapshotMap = snapshotByQuestionId(snapshots);
        Map<Long, String> submittedAnswers = normalizeAnswers(answers, snapshotMap);
        upsertAnswers(recordId, submittedAnswers, snapshotMap, false);

        int objectiveScore = 0;
        int correctCount = 0;
        List<Map<String, Object>> answerDetails = new ArrayList<>();
        boolean showAnswers = Integer.valueOf(1).equals(paper.getShowAnswerAfterSubmit());
        boolean hasShortAnswer = false;

        for (Map<String, Object> snapshot : snapshots) {
            Long questionId = numberValue(snapshot.get("id")).longValue();
            String type = stringValue(snapshot.get("type"));
            String studentAnswer = submittedAnswers.get(questionId);
            if (!submittedAnswers.containsKey(questionId)) {
                ExamAnswer existing = firstAnswer(recordId, questionId);
                studentAnswer = existing == null ? null : existing.getStudentAnswer();
            }
            studentAnswer = normalizeAnswer(type, studentAnswer);
            int questionScore = intValue(snapshot.get("score"));
            ExamAnswer examAnswer = new ExamAnswer();
            examAnswer.setRecordId(recordId);
            examAnswer.setQuestionId(questionId);
            Number knowledgeId = numberValue(snapshot.get("knowledgeId"));
            examAnswer.setKnowledgeId(knowledgeId == null ? null : knowledgeId.longValue());
            examAnswer.setStudentAnswer(studentAnswer);

            boolean correct = false;
            switch (type) {
                case "SINGLE":
                case "JUDGE":
                    correct = Objects.equals(studentAnswer, normalizeAnswer(type, stringValue(snapshot.get("answer"))));
                    break;
                case "MULTIPLE":
                    correct = Objects.equals(studentAnswer, normalizeAnswer(type, stringValue(snapshot.get("answer"))));
                    break;
                case "SHORT_ANSWER":
                    hasShortAnswer = true;
                    examAnswer.setIsCorrect(null); // 寰呮壒鏀?
                    examAnswer.setCorrectFlag(null);
                    examAnswer.setScore(0);
                    upsertScoredAnswer(examAnswer);
                    continue; // 绠€绛旈鏆備笉鍙備笌璇勫垎
            }

            if (correct) {
                objectiveScore += questionScore;
                correctCount++;
                examAnswer.setIsCorrect(1);
                examAnswer.setCorrectFlag(1);
                examAnswer.setScore(questionScore);
            } else {
                examAnswer.setIsCorrect(0);
                examAnswer.setCorrectFlag(0);
                examAnswer.setScore(0);
            }
            upsertScoredAnswer(examAnswer);

            // 缁勮绛旈璇︽儏
            Map<String, Object> detail = new HashMap<>();
            detail.put("questionId", questionId);
            detail.put("studentAnswer", studentAnswer);
            detail.put("correctAnswer", showAnswers ? snapshot.get("answer") : null);
            detail.put("isCorrect", correct);
            detail.put("analysis", showAnswers ? snapshot.get("analysis") : null);
            answerDetails.add(detail);
        }

        // 鏇存柊鑰冭瘯璁板綍
        record.setObjectiveScore(objectiveScore);
        record.setTotalScore(objectiveScore);
        record.setPassed(hasShortAnswer ? null : (objectiveScore >= paper.getPassScore() ? 1 : 0));
        record.setStatus(autoSubmit || timedOut(record)
                ? ExamRecordStatus.EXPIRED.name()
                : (hasShortAnswer ? ExamRecordStatus.PENDING_REVIEW.name() : ExamRecordStatus.GRADED.name()));
        record.setSubmitTime(new Date());
        record.setFinalGradeTime(hasShortAnswer ? null : record.getSubmitTime());
        record.setAutoSubmitFlag(autoSubmit ? 1 : timedOut(record) ? 1 : 0);
        examRecordMapper.updateById(record);
        ExperimentAdmission admission = null;
        if (ExamRecordStatus.GRADED.name().equals(record.getStatus()) && Integer.valueOf(1).equals(record.getPassed())) {
            admission = admissionService.issueOnPassedExam(record, paper);
        } else {
            admissionService.revokeByExamRecord(record.getId(), "考试未最终通过");
        }
        if (admission != null) {
            record.setAdmissionId(admission.getId());
            examRecordMapper.updateById(record);
        }
        learningTaskService.syncExamCompleted(record.getStudentId(), record.getPaperId(), record.getExperimentId());

        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", record.getTotalScore());
        result.put("objectiveScore", objectiveScore);
        result.put("status", record.getStatus());
        result.put("passed", record.getPassed() == null ? null : record.getPassed() == 1);
        result.put("correctCount", correctCount);
        result.put("totalCount", snapshots.size());
        result.put("answerDetails", answerDetails);
        result.put("showAnswers", showAnswers);
        result.put("admission", admission);
        result.put("autoSubmit", autoSubmit || timedOut(record));
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
        if (record == null) {
            throw new BusinessException(404, "考试记录不存在");
        }
        if (!UserContext.getUserId().equals(record.getStudentId())) {
            throw new BusinessException(403, "不能查看他人的考试记录");
        }
        canonicalizeLegacyStatus(record);

        LambdaQueryWrapper<ExamAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamAnswer::getRecordId, recordId);
        List<ExamAnswer> answers = examAnswerMapper.selectList(wrapper);
        ExamPaper paper = examPaperMapper.selectById(record.getPaperId());
        boolean showAnswers = paper == null || Integer.valueOf(1).equals(paper.getShowAnswerAfterSubmit());
        Map<Long, Map<String, Object>> snapshotMap = snapshotByQuestionId(loadSnapshot(record));

        List<Map<String, Object>> answerList = new ArrayList<>();
        for (ExamAnswer ans : answers) {
            Map<String, Object> snapshot = snapshotMap.get(ans.getQuestionId());
            Map<String, Object> item = new HashMap<>();
            item.put("question", snapshot == null ? null : studentQuestionView(snapshot));
            item.put("studentAnswer", ans.getStudentAnswer());
            item.put("correctAnswer", showAnswers && snapshot != null ? snapshot.get("answer") : null);
            item.put("isCorrect", ans.getIsCorrect());
            item.put("analysis", showAnswers && snapshot != null ? snapshot.get("analysis") : null);
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
        assertPaperTeacherOrAdmin(examPaperMapper.selectById(paperId));
        // 鏌ヨ璇ヨ瘯鍗锋墍鏈夊凡鎻愪氦鐨勮€冭瘯璁板綍
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getPaperId, paperId)
               .in(ExamRecord::getStatus, finalRecordStatuses());
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
        assertPaperTeacherOrAdmin(examPaperMapper.selectById(paperId));
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getPaperId, paperId)
               .in(ExamRecord::getStatus, finalRecordStatuses());
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
        assertPaperTeacherOrAdmin(examPaperMapper.selectById(paperId));
        // 1. 鑾峰彇璇曞嵎-棰樼洰鍏宠仈锛堟寜鎺掑簭鍙峰崌搴忥級
        LambdaQueryWrapper<ExamPaperQuestion> eqWrapper = new LambdaQueryWrapper<>();
        eqWrapper.eq(ExamPaperQuestion::getPaperId, paperId)
                 .orderByAsc(ExamPaperQuestion::getOrderNum);
        List<ExamPaperQuestion> eqList = examPaperQuestionMapper.selectList(eqWrapper);

        // 2. 鑾峰彇璇ヨ瘯鍗锋墍鏈夋彁浜ょ殑鑰冭瘯璁板綍
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getPaperId, paperId)
                     .in(ExamRecord::getStatus, finalRecordStatuses());
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
        List<Long> teacherCourseIds = null;
        if (courseId != null) {
            assertCourseTeacherOrAdmin(courseId);
        } else if (UserContext.isTeacher()) {
            teacherCourseIds = courseMapper.selectList(new LambdaQueryWrapper<LabCourse>()
                            .select(LabCourse::getId)
                            .eq(LabCourse::getTeacherId, UserContext.getUserId()))
                    .stream().map(LabCourse::getId).toList();
            if (teacherCourseIds.isEmpty()) {
                return new ArrayList<>();
            }
        }
        // 1. 鏌ヨ璇ヨ绋嬩笅鎵€鏈夐鐩?
        LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<>();
        questionWrapper.eq(courseId != null, Question::getCourseId, courseId)
                       .in(teacherCourseIds != null, Question::getCourseId, teacherCourseIds)
                       .isNotNull(Question::getKnowledgePoint);
        List<Question> questions = questionMapper.selectList(questionWrapper);
        List<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());

        if (questionIds.isEmpty()) return new ArrayList<>();

        // 2. 鑾峰彇鎵€鏈夊凡鎻愪氦鐨勮€冭瘯璁板綍ID
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.in(ExamRecord::getStatus, finalRecordStatuses());
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
               .eq(ExamRecord::getStatus, ExamRecordStatus.PENDING_REVIEW.name())
               .eq(paperId != null, ExamRecord::getPaperId, paperId)
               .orderByAsc(ExamRecord::getSubmitTime);
        if (paperId != null) {
            assertPaperTeacherOrAdmin(examPaperMapper.selectById(paperId));
        } else if (UserContext.isTeacher()) {
            List<Long> paperIds = teacherPaperIds();
            if (paperIds.isEmpty()) {
                page.setRecords(Collections.emptyList());
                page.setTotal(0);
                return convertToMapPage(page);
            }
            wrapper.in(ExamRecord::getPaperId, paperIds);
        }
        Page<ExamRecord> recordPage = examRecordMapper.selectPage(page, wrapper);

        Page<Map<String, Object>> result = new Page<>(pageNum, pageSize, recordPage.getTotal());
        List<Map<String, Object>> items = new ArrayList<>();
        for (ExamRecord r : recordPage.getRecords()) {
            ExamPaper paper = examPaperMapper.selectById(r.getPaperId());
            User student = userMapper.selectById(r.getStudentId());
            Map<Long, Map<String, Object>> snapshotMap = snapshotByQuestionId(loadSnapshot(r));
            List<ExamAnswer> pendingAnswers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                    .eq(ExamAnswer::getRecordId, r.getId())
                    .isNull(ExamAnswer::getIsCorrect));
            Map<String, Object> item = new HashMap<>();
            item.put("recordId", r.getId());
            item.put("studentId", r.getStudentId());
            item.put("studentName", student == null ? null : student.getRealName());
            item.put("studentUsername", student == null ? null : student.getUsername());
            item.put("paperId", r.getPaperId());
            item.put("paperTitle", paper == null ? null : paper.getTitle());
            item.put("objectiveScore", r.getObjectiveScore());
            item.put("totalScore", r.getTotalScore());
            item.put("submitTime", r.getSubmitTime());
            item.put("pendingCount", pendingAnswers.size());
            item.put("answers", pendingAnswers.stream()
                    .map(answer -> buildPendingAnswerItem(answer, snapshotMap))
                    .toList());
            items.add(item);
        }
        result.setRecords(items);
        return result;
    }

    private Map<String, Object> buildPendingAnswerItem(ExamAnswer answer, Map<Long, Map<String, Object>> snapshotMap) {
        Map<String, Object> snapshot = snapshotMap.get(answer.getQuestionId());
        Question question = snapshot == null || snapshot.get("content") == null
                ? questionMapper.selectById(answer.getQuestionId())
                : null;
        Map<String, Object> item = new HashMap<>();
        item.put("answerId", answer.getId());
        item.put("questionId", answer.getQuestionId());
        item.put("type", snapshotValue(snapshot, question, "type"));
        item.put("content", snapshotValue(snapshot, question, "content"));
        item.put("studentAnswer", answer.getStudentAnswer());
        item.put("referenceAnswer", snapshotValue(snapshot, question, "answer"));
        item.put("analysis", snapshotValue(snapshot, question, "analysis"));
        item.put("maxScore", maxScore(snapshot, question));
        item.put("score", answer.getScore());
        item.put("orderNum", snapshot == null ? 0 : snapshot.getOrDefault("orderNum", 0));
        return item;
    }

    private Object snapshotValue(Map<String, Object> snapshot, Question question, String key) {
        if (snapshot != null && snapshot.get(key) != null) {
            return snapshot.get(key);
        }
        if (question == null) {
            return null;
        }
        return switch (key) {
            case "type" -> question.getType();
            case "content" -> question.getContent();
            case "answer" -> question.getAnswer();
            case "analysis" -> question.getAnalysis();
            default -> null;
        };
    }

    private int maxScore(Map<String, Object> snapshot, Question question) {
        Number score = snapshot == null ? null : numberValue(snapshot.get("score"));
        if (score != null) {
            return score.intValue();
        }
        return question == null || question.getScore() == null ? 0 : question.getScore();
    }

    @Override
    @Transactional
    public Map<String, Object> gradeShortAnswer(Long recordId, List<Map<String, Object>> grades) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || !ExamRecordStatus.PENDING_REVIEW.name().equals(record.getStatus())) {
            throw new BusinessException(400, "考试记录状态异常，无法批改");
        }

        ExamPaper paper = examPaperMapper.selectById(record.getPaperId());
        assertPaperTeacherOrAdmin(paper);

        for (Map<String, Object> g : grades) {
            Long answerId = Long.valueOf(g.get("answerId").toString());
            int score = Integer.parseInt(g.get("score").toString());

            ExamAnswer answer = examAnswerMapper.selectById(answerId);
            if (answer == null || !answer.getRecordId().equals(recordId)) {
                throw new BusinessException(400, "答题记录不存在：" + answerId);
            }
            Map<String, Object> snapshot = snapshotByQuestionId(loadSnapshot(record)).get(answer.getQuestionId());
            int maxScore = maxScore(snapshot, questionMapper.selectById(answer.getQuestionId()));
            if (score < 0 || (maxScore > 0 && score > maxScore)) {
                throw new BusinessException(400, "简答题得分不能小于0或超过题目分值");
            }

            answer.setIsCorrect(score > 0 ? 1 : 0);
            answer.setCorrectFlag(score > 0 ? 1 : 0);
            answer.setScore(score);
            examAnswerMapper.updateById(answer);
        }

        Long pending = examAnswerMapper.selectCount(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId)
                .isNull(ExamAnswer::getIsCorrect));
        if (pending != null && pending > 0) {
            Map<String, Object> result = new HashMap<>();
            result.put("recordId", recordId);
            result.put("pendingCount", pending);
            result.put("status", record.getStatus());
            return result;
        }

        // 閲嶇畻鎬诲垎鍜岄€氳繃鐘舵€?
        int subjectiveScore = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                        .eq(ExamAnswer::getRecordId, recordId))
                .stream()
                .filter(a -> a.getIsCorrect() != null)
                .filter(a -> "SHORT_ANSWER".equals(snapshotByQuestionId(loadSnapshot(record))
                        .getOrDefault(a.getQuestionId(), Collections.emptyMap()).get("type")))
                .mapToInt(a -> a.getScore() == null ? 0 : a.getScore())
                .sum();
        int totalScore = (record.getObjectiveScore() != null ? record.getObjectiveScore() : 0) + subjectiveScore;
        record.setSubjectiveScore(subjectiveScore);
        record.setTotalScore(totalScore);
        record.setPassed(totalScore >= paper.getPassScore() ? 1 : 0);
        record.setStatus(ExamRecordStatus.GRADED.name());
        record.setFinalGradeTime(new Date());
        examRecordMapper.updateById(record);
        ExperimentAdmission admission = null;
        if (Integer.valueOf(1).equals(record.getPassed())) {
            admission = admissionService.issueOnPassedExam(record, paper);
            if (admission != null) {
                record.setAdmissionId(admission.getId());
                examRecordMapper.updateById(record);
            }
        } else {
            admissionService.revokeByExamRecord(record.getId(), "主观题批改后未通过");
        }
        learningTaskService.syncExamCompleted(record.getStudentId(), record.getPaperId(), record.getExperimentId());

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", recordId);
        result.put("subjectiveScore", subjectiveScore);
        result.put("totalScore", totalScore);
        result.put("passed", record.getPassed() == 1);
        result.put("status", record.getStatus());
        result.put("admission", admission);
        return result;
    }

    private Page<Map<String, Object>> convertToMapPage(Page<ExamRecord> source) {
        Page<Map<String, Object>> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        // simplified - just return empty
        return target;
    }

    // ===== 绉佹湁宸ュ叿鏂规硶 =====

    private List<Map<String, Object>> buildQuestionSnapshot(ExamPaper paper) {
        if (Integer.valueOf(1).equals(paper.getRandomEnabled()) && paper.getRandomCount() != null && paper.getRandomCount() > 0) {
            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(paper.getCourseId() != null, Question::getCourseId, paper.getCourseId())
                    .eq(paper.getExperimentId() != null, Question::getExperimentId, paper.getExperimentId())
                    .in(Question::getType, List.of("SINGLE", "MULTIPLE", "JUDGE", "SHORT_ANSWER"));
            List<Question> candidates = questionMapper.selectList(wrapper);
            Collections.shuffle(candidates);
            List<Map<String, Object>> snapshots = new ArrayList<>();
            int count = Math.min(paper.getRandomCount(), candidates.size());
            int defaultScore = count == 0 ? 0 : Math.max(1, paper.getTotalScore() == null ? 1 : paper.getTotalScore() / count);
            for (int i = 0; i < count; i++) {
                Question question = candidates.get(i);
                snapshots.add(toSnapshot(question, question.getScore() == null || question.getScore() <= 0 ? defaultScore : question.getScore(), i + 1));
            }
            return snapshots;
        }

        List<ExamPaperQuestion> eqList = examPaperQuestionMapper.selectList(new LambdaQueryWrapper<ExamPaperQuestion>()
                .eq(ExamPaperQuestion::getPaperId, paper.getId())
                .orderByAsc(ExamPaperQuestion::getOrderNum));
        List<Map<String, Object>> snapshots = new ArrayList<>();
        for (ExamPaperQuestion eq : eqList) {
            Question question = questionMapper.selectById(eq.getQuestionId());
            if (question != null) {
                snapshots.add(toSnapshot(question, eq.getScore(), eq.getOrderNum()));
            }
        }
        return snapshots;
    }

    private Map<String, Object> toSnapshot(Question question, Integer score, Integer orderNum) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", question.getId());
        item.put("type", question.getType());
        item.put("content", question.getContent());
        item.put("options", question.getOptions());
        item.put("answer", question.getAnswer());
        item.put("analysis", question.getAnalysis());
        item.put("knowledgePoint", question.getKnowledgePoint());
        item.put("knowledgeId", question.getKnowledgeId());
        item.put("riskType", question.getRiskType());
        item.put("difficulty", question.getDifficulty());
        item.put("relatedResourceId", question.getRelatedResourceId());
        item.put("score", score == null ? 0 : score);
        item.put("orderNum", orderNum == null ? 0 : orderNum);
        return item;
    }

    private Map<String, Object> studentQuestionView(Map<String, Object> snapshot) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", snapshot.get("id"));
        item.put("type", snapshot.get("type"));
        item.put("content", snapshot.get("content"));
        item.put("options", snapshot.get("options"));
        item.put("score", snapshot.get("score"));
        item.put("orderNum", snapshot.get("orderNum"));
        return item;
    }

    private String writeJson(List<Map<String, Object>> snapshots) {
        try {
            return OBJECT_MAPPER.writeValueAsString(snapshots);
        } catch (Exception e) {
            throw new BusinessException(500, "生成试卷快照失败");
        }
    }

    private List<Map<String, Object>> loadSnapshot(ExamRecord record) {
        if (record.getQuestionSnapshotJson() == null || record.getQuestionSnapshotJson().isBlank()) {
            ExamPaper paper = examPaperMapper.selectById(record.getPaperId());
            return paper == null ? List.of() : buildQuestionSnapshot(paper);
        }
        try {
            return OBJECT_MAPPER.readValue(record.getQuestionSnapshotJson(), SNAPSHOT_TYPE);
        } catch (Exception e) {
            throw new BusinessException(500, "读取试卷快照失败");
        }
    }

    private Map<Long, Map<String, Object>> snapshotByQuestionId(List<Map<String, Object>> snapshots) {
        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> snapshot : snapshots) {
            Number id = numberValue(snapshot.get("id"));
            if (id != null) {
                result.put(id.longValue(), snapshot);
            }
        }
        return result;
    }

    private Map<Long, String> normalizeAnswers(List<Map<String, Object>> answers, Map<Long, Map<String, Object>> snapshotMap) {
        Map<Long, String> result = new LinkedHashMap<>();
        if (answers == null) {
            return result;
        }
        for (Map<String, Object> ans : answers) {
            if (ans == null || ans.get("questionId") == null) {
                continue;
            }
            Long questionId = Long.valueOf(ans.get("questionId").toString());
            if (result.containsKey(questionId)) {
                throw new BusinessException(400, "同一题目不能重复提交：" + questionId);
            }
            Map<String, Object> snapshot = snapshotMap.get(questionId);
            if (snapshot == null) {
                throw new BusinessException(400, "提交答案包含非本次考试题目：" + questionId);
            }
            Object answer = ans.get("answer");
            result.put(questionId, normalizeAnswer(stringValue(snapshot.get("type")), answer == null ? null : answer.toString()));
        }
        return result;
    }

    private String normalizeAnswer(String questionType, String answer) {
        if (answer == null) {
            return null;
        }
        String type = questionType == null ? "" : questionType.trim().toUpperCase(Locale.ROOT);
        if ("SHORT_ANSWER".equals(type)) {
            return answer.trim();
        }
        if ("MULTIPLE".equals(type)) {
            return Arrays.stream(answer.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .map(item -> item.toUpperCase(Locale.ROOT))
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(","));
        }
        if ("JUDGE".equals(type)) {
            String normalized = answer.trim().toUpperCase(Locale.ROOT);
            if (Set.of("TRUE", "T", "YES", "Y", "1", "A", "正确", "对").contains(normalized)) {
                return "TRUE";
            }
            if (Set.of("FALSE", "F", "NO", "N", "0", "B", "错误", "错").contains(normalized)) {
                return "FALSE";
            }
            return normalized;
        }
        return answer.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private void upsertAnswers(Long recordId, Map<Long, String> normalized, Map<Long, Map<String, Object>> snapshotMap, boolean scored) {
        for (Map.Entry<Long, String> entry : normalized.entrySet()) {
            ExamAnswer answer = firstAnswer(recordId, entry.getKey());
            if (answer == null) {
                answer = new ExamAnswer();
                answer.setRecordId(recordId);
                answer.setQuestionId(entry.getKey());
            }
            answer.setStudentAnswer(entry.getValue());
            Number knowledgeId = numberValue(snapshotMap.get(entry.getKey()).get("knowledgeId"));
            answer.setKnowledgeId(knowledgeId == null ? null : knowledgeId.longValue());
            if (!scored && answer.getScore() == null) {
                answer.setScore(0);
            }
            if (answer.getId() == null) {
                examAnswerMapper.insert(answer);
            } else {
                examAnswerMapper.updateById(answer);
            }
        }
    }

    private void upsertScoredAnswer(ExamAnswer answer) {
        ExamAnswer current = firstAnswer(answer.getRecordId(), answer.getQuestionId());
        if (current == null) {
            examAnswerMapper.insert(answer);
            return;
        }
        answer.setId(current.getId());
        examAnswerMapper.updateById(answer);
    }

    private ExamAnswer firstAnswer(Long recordId, Long questionId) {
        List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId)
                .eq(ExamAnswer::getQuestionId, questionId)
                .orderByAsc(ExamAnswer::getId));
        return answers.isEmpty() ? null : answers.get(0);
    }

    private ExamPaper requireAvailablePaper(Long paperId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null || !"PUBLISHED".equals(paper.getStatus())) {
            throw new BusinessException(400, "试卷不可用");
        }
        Date now = new Date();
        if (paper.getStartTime() != null && paper.getStartTime().after(now)) {
            throw new BusinessException(400, "考试尚未开始");
        }
        if (paper.getEndTime() != null && paper.getEndTime().before(now)) {
            throw new BusinessException(400, "考试已结束");
        }
        return paper;
    }

    private void assertStudentInCourse(Long studentId, Long courseId) {
        if (courseId == null) {
            throw new BusinessException(400, "试卷未关联课程");
        }
        Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getStudentId, studentId)
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStatus, 1));
        if (count == null || count == 0) {
            throw new BusinessException(403, "学生未加入试卷所属课程");
        }
    }

    private void assertPaperTeacherOrAdmin(ExamPaper paper) {
        if (paper == null) {
            throw new BusinessException(404, "试卷不存在");
        }
        if (UserContext.isAdmin()) {
            return;
        }
        if (!UserContext.isTeacher()) {
            throw new BusinessException(403, "只有试卷所属课程教师或管理员可以批改");
        }
        LabCourse course = courseMapper.selectById(paper.getCourseId());
        if (course == null || !UserContext.getUserId().equals(course.getTeacherId())) {
            throw new BusinessException(403, "不能批改非本人课程的考试");
        }
    }

    private void assertCourseTeacherOrAdmin(Long courseId) {
        if (UserContext.isAdmin()) {
            return;
        }
        if (!UserContext.isTeacher()) {
            throw new BusinessException(403, "无权查看课程考试数据");
        }
        LabCourse course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!UserContext.getUserId().equals(course.getTeacherId())) {
            throw new BusinessException(403, "不能查看非本人课程的考试数据");
        }
    }

    private List<Long> teacherPaperIds() {
        if (!UserContext.isTeacher()) {
            return List.of();
        }
        List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<LabCourse>()
                        .select(LabCourse::getId)
                        .eq(LabCourse::getTeacherId, UserContext.getUserId()))
                .stream().map(LabCourse::getId).toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return examPaperMapper.selectList(new LambdaQueryWrapper<ExamPaper>()
                        .select(ExamPaper::getId)
                        .in(ExamPaper::getCourseId, courseIds))
                .stream().map(ExamPaper::getId).toList();
    }

    private ExamRecord findInProgressRecord(Long studentId, Long paperId) {
        return examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStudentId, studentId)
                .eq(ExamRecord::getPaperId, paperId)
                .eq(ExamRecord::getStatus, ExamRecordStatus.IN_PROGRESS.name())
                .orderByDesc(ExamRecord::getStartTime)
                .last("LIMIT 1"));
    }

    private ExamSessionVO toSessionVO(ExamRecord record, ExamPaper paper, boolean resumed) {
        List<Map<String, Object>> snapshots = loadSnapshot(record);
        Map<Long, String> savedAnswers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                        .eq(ExamAnswer::getRecordId, record.getId())
                        .orderByAsc(ExamAnswer::getId))
                .stream()
                .collect(Collectors.toMap(ExamAnswer::getQuestionId, ExamAnswer::getStudentAnswer, (left, right) -> left, LinkedHashMap::new));
        ExamSessionVO vo = new ExamSessionVO();
        vo.setRecordId(record.getId());
        vo.setPaperId(record.getPaperId());
        vo.setPaperTitle(paper == null ? null : paper.getTitle());
        vo.setQuestions(snapshots.stream().map(this::studentQuestionVO).toList());
        vo.setAnswers(savedAnswers);
        vo.setStartTime(record.getStartTime());
        vo.setEndTime(record.getEndTime());
        vo.setRemainingSeconds(record.getEndTime() == null ? null : Math.max(0L, (record.getEndTime().getTime() - System.currentTimeMillis()) / 1000L));
        vo.setResumed(resumed);
        return vo;
    }

    private ExamSessionQuestionVO studentQuestionVO(Map<String, Object> snapshot) {
        ExamSessionQuestionVO vo = new ExamSessionQuestionVO();
        Number id = numberValue(snapshot.get("id"));
        vo.setId(id == null ? null : id.longValue());
        vo.setType(stringValue(snapshot.get("type")));
        vo.setContent(stringValue(snapshot.get("content")));
        vo.setOptions(stringValue(snapshot.get("options")));
        vo.setScore(intValue(snapshot.get("score")));
        vo.setOrderNo(intValue(snapshot.get("orderNum")));
        return vo;
    }

    private void canonicalizeLegacyStatus(ExamRecord record) {
        if (record == null || record.getStatus() == null) {
            return;
        }
        if ("REVIEWED".equals(record.getStatus())) {
            record.setStatus(ExamRecordStatus.GRADED.name());
            if (record.getFinalGradeTime() == null) {
                record.setFinalGradeTime(record.getSubmitTime());
            }
            examRecordMapper.updateById(record);
            return;
        }
        if ("SUBMITTED".equals(record.getStatus())) {
            Long pending = examAnswerMapper.selectCount(new LambdaQueryWrapper<ExamAnswer>()
                    .eq(ExamAnswer::getRecordId, record.getId())
                    .isNull(ExamAnswer::getIsCorrect));
            record.setStatus(pending != null && pending > 0
                    ? ExamRecordStatus.PENDING_REVIEW.name()
                    : ExamRecordStatus.GRADED.name());
            if (ExamRecordStatus.GRADED.name().equals(record.getStatus()) && record.getFinalGradeTime() == null) {
                record.setFinalGradeTime(record.getSubmitTime());
            }
            examRecordMapper.updateById(record);
        }
    }

    private Map<String, Object> buildSubmitResult(ExamRecord record, ExamPaper paper, ExperimentAdmission admission, boolean autoSubmit) {
        canonicalizeLegacyStatus(record);
        List<Map<String, Object>> snapshots = loadSnapshot(record);
        Map<Long, Map<String, Object>> snapshotMap = snapshotByQuestionId(snapshots);
        List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, record.getId()));
        List<Map<String, Object>> answerDetails = new ArrayList<>();
        int correctCount = 0;
        boolean showAnswers = paper == null || Integer.valueOf(1).equals(paper.getShowAnswerAfterSubmit());
        for (ExamAnswer answer : answers) {
            Map<String, Object> snapshot = snapshotMap.get(answer.getQuestionId());
            if (Integer.valueOf(1).equals(answer.getIsCorrect())) {
                correctCount++;
            }
            Map<String, Object> detail = new HashMap<>();
            detail.put("questionId", answer.getQuestionId());
            detail.put("studentAnswer", answer.getStudentAnswer());
            detail.put("correctAnswer", showAnswers && snapshot != null ? snapshot.get("answer") : null);
            detail.put("isCorrect", answer.getIsCorrect() == null ? null : answer.getIsCorrect() == 1);
            detail.put("analysis", showAnswers && snapshot != null ? snapshot.get("analysis") : null);
            answerDetails.add(detail);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", record.getTotalScore());
        result.put("objectiveScore", record.getObjectiveScore());
        result.put("subjectiveScore", record.getSubjectiveScore());
        result.put("status", record.getStatus());
        result.put("passed", record.getPassed() == null ? null : record.getPassed() == 1);
        result.put("correctCount", correctCount);
        result.put("totalCount", snapshots.size());
        result.put("answerDetails", answerDetails);
        result.put("showAnswers", showAnswers);
        result.put("admission", admission);
        result.put("autoSubmit", autoSubmit || Integer.valueOf(1).equals(record.getAutoSubmitFlag()));
        return result;
    }

    private List<String> finalRecordStatuses() {
        return List.of(ExamRecordStatus.GRADED.name(), ExamRecordStatus.EXPIRED.name(), "SUBMITTED", "REVIEWED");
    }

    private boolean timedOut(ExamRecord record) {
        return record.getEndTime() != null && new Date().after(record.getEndTime());
    }

    private int attemptLimit(ExamPaper paper) {
        return paper.getAttemptLimit() == null || paper.getAttemptLimit() <= 0 ? 1 : paper.getAttemptLimit();
    }

    private int durationMinutes(ExamPaper paper) {
        return paper.getDuration() == null || paper.getDuration() <= 0 ? 30 : paper.getDuration();
    }

    private int usedAttempts(Long studentId, Long paperId) {
        Long count = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStudentId, studentId)
                .eq(ExamRecord::getPaperId, paperId)
                .in(ExamRecord::getStatus, "IN_PROGRESS", "PENDING_REVIEW", "GRADED", "EXPIRED", "SUBMITTED", "REVIEWED"));
        return count == null ? 0 : count.intValue();
    }

    private Number numberValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number;
        return Long.valueOf(value.toString());
    }

    private int intValue(Object value) {
        Number number = numberValue(value);
        return number == null ? 0 : number.intValue();
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

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
