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
            boolean randomReady = Integer.valueOf(1).equals(existing.getRandomEnabled())
                    && existing.getRandomCount() != null && existing.getRandomCount() > 0;
            Long questionCount = examPaperQuestionMapper.selectCount(new LambdaQueryWrapper<ExamPaperQuestion>()
                    .eq(ExamPaperQuestion::getPaperId, id));
            if (!randomReady && (questionCount == null || questionCount == 0)) {
                throw new BusinessException(400, "发布试卷前必须配置题目或启用随机抽题");
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
        // 先获取当前最大排序号
        LambdaQueryWrapper<ExamPaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaperQuestion::getPaperId, paperId)
               .orderByDesc(ExamPaperQuestion::getOrderNum)
               .last("LIMIT 1");
        ExamPaperQuestion last = examPaperQuestionMapper.selectOne(wrapper);
        int nextOrder = (last != null) ? last.getOrderNum() + 1 : 1;

        for (int i = 0; i < questionIds.size(); i++) {
            Question question = questionMapper.selectById(questionIds.get(i));
            if (question == null) {
                throw new BusinessException(404, "题目不存在：" + questionIds.get(i));
            }
            if (!paper.getCourseId().equals(question.getCourseId())) {
                throw new BusinessException(403, "不能向试卷加入其他课程的题目：" + questionIds.get(i));
            }
            ExamPaperQuestion eq = new ExamPaperQuestion();
            eq.setPaperId(paperId);
            eq.setQuestionId(questionIds.get(i));
            eq.setScore(scores != null && i < scores.size() ? scores.get(i) : 0);
            eq.setOrderNum(nextOrder + i);
            examPaperQuestionMapper.insert(eq);
        }

        // 重新计算试卷总分
        recalcTotalScore(paperId);
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
        recalcTotalScore(paperId);
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

    /** 重新计算试卷总分 */
    private void recalcTotalScore(Long paperId) {
        LambdaQueryWrapper<ExamPaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaperQuestion::getPaperId, paperId);
        List<ExamPaperQuestion> eqList = examPaperQuestionMapper.selectList(wrapper);

        int total = eqList.stream().mapToInt(ExamPaperQuestion::getScore).sum();
        ExamPaper paper = new ExamPaper();
        paper.setId(paperId);
        paper.setTotalScore(total);
        examPaperMapper.updateById(paper);
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
        int score = intRule(rule, "score", 5);
        if (count <= 0 || count > 200) {
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
        Collections.shuffle(candidates);
        if (candidates.isEmpty()) {
            throw new BusinessException(400, "当前条件下没有可用于组卷的题目");
        }
        int actual = Math.min(count, candidates.size());
        List<Long> questionIds = candidates.stream().limit(actual).map(Question::getId).toList();
        List<Integer> scores = questionIds.stream().map(id -> score).toList();
        addQuestions(paperId, questionIds, scores);
        Map<String, Object> result = new HashMap<>();
        result.put("requestedCount", count);
        result.put("actualCount", actual);
        result.put("candidateCount", candidates.size());
        return result;
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
