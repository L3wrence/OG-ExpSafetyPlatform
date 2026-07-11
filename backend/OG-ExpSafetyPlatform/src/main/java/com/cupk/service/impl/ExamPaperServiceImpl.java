package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.ExamPaperMapper;
import com.cupk.mapper.ExamPaperQuestionMapper;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.QuestionMapper;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.pojo.ExamPaper;
import com.cupk.pojo.ExamPaperQuestion;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.Question;
import com.cupk.service.ExamPaperService;
import com.cupk.util.AccessUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 试卷管理服务实现
 */
@Service
public class ExamPaperServiceImpl implements ExamPaperService {

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private ExamPaperQuestionMapper examPaperQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private LabCourseMapper labCourseMapper;

    @Autowired
    private ExperimentMapper experimentMapper;

    @Override
    public Page<ExamPaper> pagePapers(int pageNum, int pageSize, String keyword,
                                       Long courseId, String status) {
        Page<ExamPaper> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(courseId != null, ExamPaper::getCourseId, courseId)
               .eq(StringUtils.hasText(status), ExamPaper::getStatus, status)
               .like(StringUtils.hasText(keyword), ExamPaper::getTitle, keyword)
               .orderByDesc(ExamPaper::getCreateTime);
        if (UserContext.isTeacher()) {
            List<Long> courseIds = teacherCourseIds();
            if (courseIds.isEmpty()) {
                page.setRecords(List.of());
                page.setTotal(0);
                return page;
            }
            wrapper.in(ExamPaper::getCourseId, courseIds);
        }

        return examPaperMapper.selectPage(page, wrapper);
    }

    @Override
    public Map<String, Object> getPaperDetail(Long id) {
        ExamPaper paper = examPaperMapper.selectById(id);
        if (paper == null) {
            return null;
        }
        assertCourseReadable(paper.getCourseId());
        // 查询关联的题目列表（按排序号排序）
        LambdaQueryWrapper<ExamPaperQuestion> eqWrapper = new LambdaQueryWrapper<>();
        eqWrapper.eq(ExamPaperQuestion::getPaperId, id)
                 .orderByAsc(ExamPaperQuestion::getOrderNum);
        List<ExamPaperQuestion> eqList = examPaperQuestionMapper.selectList(eqWrapper);

        // 组装题目详情
        List<Map<String, Object>> questions = new ArrayList<>();
        for (ExamPaperQuestion eq : eqList) {
            Question q = questionMapper.selectById(eq.getQuestionId());
            if (q != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("question", q);
                item.put("score", eq.getScore());
                item.put("orderNum", eq.getOrderNum());
                questions.add(item);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("paper", paper);
        result.put("questions", questions);
        return result;
    }

    @Override
    public Long createPaper(ExamPaper paper) {
        normalizePaperScope(paper);
        assertCourseWritable(paper.getCourseId());
        assertCourseNotArchived(paper.getCourseId());
        applyDefaults(paper);
        paper.setStatus("DRAFT");
        examPaperMapper.insert(paper);
        return paper.getId();
    }

    @Override
    public void updatePaper(Long id, ExamPaper paper) {
        ExamPaper current = examPaperMapper.selectById(id);
        if (current == null) {
            throw new BusinessException(404, "试卷不存在");
        }
        assertCourseWritable(current.getCourseId());
        normalizePaperScope(paper);
        assertCourseWritable(paper.getCourseId());
        assertCourseNotArchived(paper.getCourseId());
        applyDefaults(paper);
        Map<String, Integer> summary = paperScoreSummary(id);
        if (summary.get("objective") > paper.getObjectiveScore()) {
            throw new BusinessException(400, "已选客观题分值总和不能超过设定的客观题分数");
        }
        if (summary.get("subjective") > paper.getSubjectiveScore()) {
            throw new BusinessException(400, "主观题分值总和不能超过设定的主观题分数");
        }
        paper.setId(id);
        paper.setTeacherId(current.getTeacherId());
        examPaperMapper.updateById(paper);
    }

    @Override
    public void deletePaper(Long id) {
        ExamPaper current = requirePaper(id);
        assertCourseWritable(current.getCourseId());
        examPaperMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        if (!Set.of("DRAFT", "PUBLISHED", "CLOSED").contains(status)) {
            throw new BusinessException(400, "不支持的试卷状态");
        }
        ExamPaper existing = requirePaper(id);
        assertCourseWritable(existing.getCourseId());
        if ("PUBLISHED".equals(status)) {
            Map<String, Integer> summary = paperScoreSummary(id);
            int objectiveTarget = existing.getObjectiveScore() == null ? 0 : existing.getObjectiveScore();
            int subjectiveTarget = existing.getSubjectiveScore() == null ? 0 : existing.getSubjectiveScore();
            if (summary.get("total") == 0) {
                throw new BusinessException(400, "发布试卷前必须配置题目");
            }
            if (summary.get("objective") != objectiveTarget || summary.get("subjective") != subjectiveTarget
                    || summary.get("total") != existing.getTotalScore()) {
                throw new BusinessException(400, "题目分值合计必须与客观题、主观题和试卷总分一致");
            }
        }
        ExamPaper paper = new ExamPaper();
        paper.setId(id);
        paper.setStatus(status);
        examPaperMapper.updateById(paper);
    }

    @Override
    @Transactional
    public void addQuestions(Long paperId, List<Long> questionIds, List<Integer> scores) {
        ExamPaper paper = requirePaper(paperId);
        assertCourseWritable(paper.getCourseId());
        if (questionIds == null || questionIds.isEmpty()) {
            throw new BusinessException(400, "请选择试题");
        }
        Set<Long> existingQuestionIds = examPaperQuestionMapper.selectList(new LambdaQueryWrapper<ExamPaperQuestion>()
                        .eq(ExamPaperQuestion::getPaperId, paperId))
                .stream().map(ExamPaperQuestion::getQuestionId).collect(java.util.stream.Collectors.toSet());
        List<Question> questionsToAdd = new ArrayList<>();
        Set<Long> batchQuestionIds = new HashSet<>();
        int addedScore = 0;
        for (Long questionId : questionIds) {
            if (existingQuestionIds.contains(questionId) || !batchQuestionIds.add(questionId)) {
                continue;
            }
            Question question = questionMapper.selectById(questionId);
            if (question == null) {
                throw new BusinessException(404, "题目不存在：" + questionId);
            }
            if (!paper.getCourseId().equals(question.getCourseId())) {
                throw new BusinessException(403, "不能向试卷加入其他课程的题目：" + questionId);
            }
            if (!Set.of("SINGLE", "MULTIPLE", "JUDGE").contains(question.getType())) {
                throw new BusinessException(400, "客观题只能从题库中的单选题、多选题或判断题选择");
            }
            if (question.getScore() == null || question.getScore() <= 0) {
                throw new BusinessException(400, "题库题目必须设置有效的默认分值：" + questionId);
            }
            questionsToAdd.add(question);
            addedScore += question.getScore();
        }
        int objectiveTarget = paper.getObjectiveScore() == null ? 0 : paper.getObjectiveScore();
        if (paperScoreSummary(paperId).get("objective") + addedScore > objectiveTarget) {
            throw new BusinessException(400, "所选客观题分值总和超过设定的客观题分数");
        }

        int nextOrder = nextQuestionOrder(paperId);
        int added = 0;
        for (Question question : questionsToAdd) {
            ExamPaperQuestion eq = new ExamPaperQuestion();
            eq.setPaperId(paperId);
            eq.setQuestionId(question.getId());
            eq.setScore(question.getScore());
            eq.setOrderNum(nextOrder + added);
            examPaperQuestionMapper.insert(eq);
            added++;
        }
    }

    @Override
    @Transactional
    public void removeQuestion(Long paperId, Long questionId) {
        ExamPaper paper = requirePaper(paperId);
        assertCourseWritable(paper.getCourseId());
        LambdaQueryWrapper<ExamPaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaperQuestion::getPaperId, paperId)
               .eq(ExamPaperQuestion::getQuestionId, questionId);
        examPaperQuestionMapper.delete(wrapper);
    }

    @Override
    @Transactional
    public void updateQuestionScore(Long paperId, Long questionId, Integer score) {
        ExamPaper paper = requirePaper(paperId);
        assertCourseWritable(paper.getCourseId());
        if (score == null || score < 0) {
            throw new BusinessException(400, "题目分值不能小于0");
        }
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(404, "题目不存在");
        }
        boolean subjective = "SHORT_ANSWER".equals(question.getType());
        if (!subjective && !Objects.equals(score, question.getScore())) {
            throw new BusinessException(400, "客观题必须使用题库默认分值，不能在试卷中修改");
        }
        ExamPaperQuestion current = examPaperQuestionMapper.selectOne(new LambdaQueryWrapper<ExamPaperQuestion>()
                .eq(ExamPaperQuestion::getPaperId, paperId)
                .eq(ExamPaperQuestion::getQuestionId, questionId));
        if (current == null) {
            throw new BusinessException(404, "试卷中不存在该题目");
        }
        Map<String, Integer> summary = paperScoreSummary(paperId);
        int updatedCategoryScore = summary.get(subjective ? "subjective" : "objective")
                - (current.getScore() == null ? 0 : current.getScore()) + score;
        int target = subjective
                ? (paper.getSubjectiveScore() == null ? 0 : paper.getSubjectiveScore())
                : (paper.getObjectiveScore() == null ? 0 : paper.getObjectiveScore());
        if (updatedCategoryScore > target) {
            throw new BusinessException(400, subjective
                    ? "主观题分值总和不能超过设定的主观题分数"
                    : "客观题分值总和不能超过设定的客观题分数");
        }
        ExamPaperQuestion eq = new ExamPaperQuestion();
        eq.setScore(score);
        int updated = examPaperQuestionMapper.update(eq, new LambdaQueryWrapper<ExamPaperQuestion>()
                .eq(ExamPaperQuestion::getPaperId, paperId)
                .eq(ExamPaperQuestion::getQuestionId, questionId));
        if (updated == 0) {
            throw new BusinessException(404, "试卷中不存在该题目");
        }
    }

    @Override
    @Transactional
    public void addSubjectiveQuestion(Long paperId, Map<String, Object> payload) {
        ExamPaper paper = requirePaper(paperId);
        assertCourseWritable(paper.getCourseId());
        String content = stringRule(payload, "content");
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(400, "主观题题干不能为空");
        }
        int score = intRule(payload, "score", 0);
        if (score <= 0) {
            throw new BusinessException(400, "主观题分值必须大于0");
        }
        int subjectiveTarget = paper.getSubjectiveScore() == null ? 0 : paper.getSubjectiveScore();
        if (paperScoreSummary(paperId).get("subjective") + score > subjectiveTarget) {
            throw new BusinessException(400, "主观题分值总和不能超过设定的主观题分数");
        }
        Question question = new Question();
        question.setType("SHORT_ANSWER");
        question.setContent(content);
        String answer = stringRule(payload, "answer");
        question.setAnswer(answer == null ? "" : answer);
        question.setAnalysis(stringRule(payload, "analysis"));
        question.setScore(score);
        question.setKnowledgePoint(StringUtils.hasText(stringRule(payload, "knowledgePoint"))
                ? stringRule(payload, "knowledgePoint") : "主观题");
        question.setDifficulty(StringUtils.hasText(stringRule(payload, "difficulty"))
                ? stringRule(payload, "difficulty") : "MEDIUM");
        question.setCourseId(paper.getCourseId());
        question.setExperimentId(paper.getExperimentId());
        question.setCreateBy(UserContext.getUserId());
        questionMapper.insert(question);
        ExamPaperQuestion eq = new ExamPaperQuestion();
        eq.setPaperId(paperId);
        eq.setQuestionId(question.getId());
        eq.setScore(score);
        eq.setOrderNum(nextQuestionOrder(paperId));
        examPaperQuestionMapper.insert(eq);
    }

    @Override
    @Transactional
    public void updateQuestionOrder(Long paperId, List<Map<String, Integer>> orders) {
        ExamPaper paper = requirePaper(paperId);
        assertCourseWritable(paper.getCourseId());
        for (Map<String, Integer> order : orders) {
            Long questionId = order.get("questionId").longValue();
            Integer orderNum = order.get("orderNum");

            LambdaQueryWrapper<ExamPaperQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExamPaperQuestion::getPaperId, paperId)
                   .eq(ExamPaperQuestion::getQuestionId, questionId);

            ExamPaperQuestion eq = new ExamPaperQuestion();
            eq.setOrderNum(orderNum);
            examPaperQuestionMapper.update(eq, wrapper);
        }
    }

    private Map<String, Integer> paperScoreSummary(Long paperId) {
        LambdaQueryWrapper<ExamPaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaperQuestion::getPaperId, paperId);
        List<ExamPaperQuestion> eqList = examPaperQuestionMapper.selectList(wrapper);
        int objective = 0;
        int subjective = 0;
        for (ExamPaperQuestion eq : eqList) {
            Question question = questionMapper.selectById(eq.getQuestionId());
            int score = eq.getScore() == null ? 0 : eq.getScore();
            if (question != null && "SHORT_ANSWER".equals(question.getType())) {
                subjective += score;
            } else {
                objective += score;
            }
        }
        Map<String, Integer> result = new HashMap<>();
        result.put("objective", objective);
        result.put("subjective", subjective);
        result.put("total", objective + subjective);
        return result;
    }

    private int nextQuestionOrder(Long paperId) {
        ExamPaperQuestion last = examPaperQuestionMapper.selectOne(new LambdaQueryWrapper<ExamPaperQuestion>()
                .eq(ExamPaperQuestion::getPaperId, paperId)
                .orderByDesc(ExamPaperQuestion::getOrderNum)
                .last("LIMIT 1"));
        return last == null ? 1 : last.getOrderNum() + 1;
    }

    private void assertCourseNotArchived(Long courseId) {
        if (courseId == null) {
            return;
        }
        LabCourse course = labCourseMapper.selectById(courseId);
        if (course != null && course.getStatus() != null && course.getStatus() == 2) {
            throw new BusinessException(409, "课程已归档，不能新增或修改考试");
        }
    }

    @Override
    @Transactional
    public Map<String, Object> smartAssemble(Long paperId, Map<String, Object> rule) {
        ExamPaper paper = requirePaper(paperId);
        assertCourseWritable(paper.getCourseId());
        int count = intRule(rule, "count", 10);
        Integer targetScore = stringRule(rule, "targetScore") == null ? null : intRule(rule, "targetScore", 0);
        if (targetScore == null && (count <= 0 || count > 200)) {
            throw new BusinessException(400, "抽题数量必须在1到200之间");
        }
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getCourseId, paper.getCourseId())
                .eq(paper.getExperimentId() != null, Question::getExperimentId, paper.getExperimentId())
                .eq(stringRule(rule, "experimentId") != null, Question::getExperimentId, longRule(rule, "experimentId"))
                .eq(StringUtils.hasText(stringRule(rule, "type")), Question::getType, stringRule(rule, "type"))
                .eq(StringUtils.hasText(stringRule(rule, "difficulty")), Question::getDifficulty, stringRule(rule, "difficulty"))
                .eq(StringUtils.hasText(stringRule(rule, "knowledgePoint")), Question::getKnowledgePoint, stringRule(rule, "knowledgePoint"))
                .in(Question::getType, List.of("SINGLE", "MULTIPLE", "JUDGE"));
        List<Question> candidates = questionMapper.selectList(wrapper);
        Set<Long> existingQuestionIds = examPaperQuestionMapper.selectList(new LambdaQueryWrapper<ExamPaperQuestion>()
                        .eq(ExamPaperQuestion::getPaperId, paperId))
                .stream().map(ExamPaperQuestion::getQuestionId).collect(java.util.stream.Collectors.toSet());
        candidates.removeIf(question -> existingQuestionIds.contains(question.getId()));
        Collections.shuffle(candidates);
        if (candidates.isEmpty()) {
            throw new BusinessException(400, "当前条件下没有可用于组卷的题目");
        }
        List<Question> picked;
        if (targetScore != null) {
            if (targetScore <= 0) {
                throw new BusinessException(400, "随机抽题目标分值必须大于0");
            }
            int remainingScore = (paper.getObjectiveScore() == null ? 0 : paper.getObjectiveScore())
                    - paperScoreSummary(paperId).get("objective");
            if (targetScore > remainingScore) {
                throw new BusinessException(400, "随机抽题目标分值超过客观题剩余分数");
            }
            picked = pickExactScore(candidates, targetScore);
            if (picked.isEmpty()) {
                throw new BusinessException(400, "当前题库默认分值无法组合出目标客观题分数");
            }
        } else {
            int actual = Math.min(count, candidates.size());
            picked = candidates.stream().limit(actual).toList();
        }
        List<Long> questionIds = picked.stream().map(Question::getId).toList();
        List<Integer> scores = picked.stream().map(q -> q.getScore() == null ? 0 : q.getScore()).toList();
        addQuestions(paperId, questionIds, scores);
        Map<String, Object> result = new HashMap<>();
        result.put("requestedCount", count);
        result.put("actualCount", picked.size());
        result.put("actualScore", scores.stream().mapToInt(Integer::intValue).sum());
        result.put("candidateCount", candidates.size());
        return result;
    }

    private List<Question> pickExactScore(List<Question> candidates, int targetScore) {
        List<Question> valid = candidates.stream()
                .filter(q -> q.getScore() != null && q.getScore() > 0 && q.getScore() <= targetScore)
                .limit(120)
                .toList();
        return pickExactScore(valid, targetScore, 0, new ArrayList<>());
    }

    private List<Question> pickExactScore(List<Question> candidates, int remaining, int index, List<Question> current) {
        if (remaining == 0) {
            return new ArrayList<>(current);
        }
        if (remaining < 0 || index >= candidates.size()) {
            return List.of();
        }
        for (int i = index; i < candidates.size(); i++) {
            Question question = candidates.get(i);
            current.add(question);
            List<Question> result = pickExactScore(candidates, remaining - question.getScore(), i + 1, current);
            if (!result.isEmpty()) {
                return result;
            }
            current.remove(current.size() - 1);
        }
        return List.of();
    }

    private ExamPaper requirePaper(Long id) {
        ExamPaper paper = examPaperMapper.selectById(id);
        if (paper == null) {
            throw new BusinessException(404, "试卷不存在");
        }
        return paper;
    }

    private void normalizePaperScope(ExamPaper paper) {
        if (paper.getExperimentId() != null) {
            Experiment experiment = experimentMapper.selectById(paper.getExperimentId());
            if (experiment == null) {
                throw new BusinessException(404, "实验项目不存在");
            }
            if (paper.getCourseId() == null) {
                paper.setCourseId(experiment.getCourseId());
            } else if (!paper.getCourseId().equals(experiment.getCourseId())) {
                throw new BusinessException(400, "试卷课程与实验所属课程不一致");
            }
        }
        if (paper.getCourseId() == null) {
            throw new BusinessException(400, "试卷必须关联课程");
        }
    }

    private void assertCourseReadable(Long courseId) {
        if (UserContext.isAdmin()) {
            return;
        }
        if (UserContext.isTeacher()) {
            assertCourseWritable(courseId);
            return;
        }
        throw new BusinessException(403, "无权查看该试卷");
    }

    private void assertCourseWritable(Long courseId) {
        LabCourse course = labCourseMapper.selectById(courseId);
        AccessUtil.assertCourseWritable(course);
    }

    private List<Long> teacherCourseIds() {
        return labCourseMapper.selectList(new LambdaQueryWrapper<LabCourse>()
                        .select(LabCourse::getId)
                        .eq(LabCourse::getTeacherId, UserContext.getUserId()))
                .stream().map(LabCourse::getId).toList();
    }

    private void applyDefaults(ExamPaper paper) {
        paper.setTotalScore(paper.getTotalScore() == null ? 100 : paper.getTotalScore());
        paper.setObjectiveScore(paper.getObjectiveScore() == null ? paper.getTotalScore() : paper.getObjectiveScore());
        paper.setSubjectiveScore(paper.getSubjectiveScore() == null ? Math.max(0, paper.getTotalScore() - paper.getObjectiveScore()) : paper.getSubjectiveScore());
        if (paper.getObjectiveScore() + paper.getSubjectiveScore() != paper.getTotalScore()) {
            throw new BusinessException(400, "客观题分数和主观题分数之和必须等于总分");
        }
        paper.setPassScore(paper.getPassScore() == null ? 60 : paper.getPassScore());
        paper.setDuration(paper.getDuration() == null || paper.getDuration() <= 0 ? 30 : paper.getDuration());
        paper.setAttemptLimit(paper.getAttemptLimit() == null || paper.getAttemptLimit() <= 0 ? 1 : paper.getAttemptLimit());
        paper.setShowAnswerAfterSubmit(paper.getShowAnswerAfterSubmit() == null ? 1 : paper.getShowAnswerAfterSubmit());
        paper.setAdmissionValidityDays(paper.getAdmissionValidityDays() == null || paper.getAdmissionValidityDays() <= 0 ? 180 : paper.getAdmissionValidityDays());
        paper.setMultipleScorePolicy(StringUtils.hasText(paper.getMultipleScorePolicy()) ? paper.getMultipleScorePolicy() : "ALL_OR_NOTHING");
        paper.setRandomEnabled(paper.getRandomEnabled() == null ? 0 : paper.getRandomEnabled());
        paper.setRandomCount(paper.getRandomCount() == null ? 0 : paper.getRandomCount());
    }

    private String stringRule(Map<String, Object> rule, String key) {
        if (rule == null || rule.get(key) == null) {
            return null;
        }
        String value = String.valueOf(rule.get(key)).trim();
        return value.isEmpty() ? null : value;
    }

    private Long longRule(Map<String, Object> rule, String key) {
        String value = stringRule(rule, key);
        return value == null ? null : Long.valueOf(value);
    }

    private int intRule(Map<String, Object> rule, String key, int fallback) {
        String value = stringRule(rule, key);
        return value == null ? fallback : Integer.parseInt(value);
    }
}
