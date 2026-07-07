package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.dto.HsePracticeQueryDTO;
import com.cupk.dto.HsePracticeSubmitDTO;
import com.cupk.dto.HsePracticeSubmitItemDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.*;
import com.cupk.pojo.*;
import com.cupk.service.HseTrainingService;
import com.cupk.util.AccessUtil;
import com.cupk.vo.HsePracticeResultVO;
import com.cupk.vo.HseQuestionVO;
import com.cupk.vo.HseWeakPointVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HseTrainingServiceImpl implements HseTrainingService {
    private final QuestionMapper questionMapper;
    private final SafetyKnowledgeMapper knowledgeMapper;
    private final TeachingResourceMapper resourceMapper;
    private final HsePracticeAnswerMapper practiceAnswerMapper;
    private final HseWrongQuestionMapper wrongQuestionMapper;
    private final HseQuestionFavoriteMapper favoriteMapper;
    private final LabCourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;

    public HseTrainingServiceImpl(QuestionMapper questionMapper, SafetyKnowledgeMapper knowledgeMapper,
                                  TeachingResourceMapper resourceMapper, HsePracticeAnswerMapper practiceAnswerMapper,
                                  HseWrongQuestionMapper wrongQuestionMapper, HseQuestionFavoriteMapper favoriteMapper,
                                  LabCourseMapper courseMapper, CourseStudentMapper courseStudentMapper) {
        this.questionMapper = questionMapper;
        this.knowledgeMapper = knowledgeMapper;
        this.resourceMapper = resourceMapper;
        this.practiceAnswerMapper = practiceAnswerMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.favoriteMapper = favoriteMapper;
        this.courseMapper = courseMapper;
        this.courseStudentMapper = courseStudentMapper;
    }

    @Override
    public List<HseQuestionVO> practice(HsePracticeQueryDTO dto) {
        if ("DAILY".equals(dto.getMode())) {
            long seed = LocalDate.now().toEpochDay();
            return questionMapper.selectList(questionWrapper(dto)
                            .last("ORDER BY RAND(" + seed + ") LIMIT 1"))
                    .stream().map(this::toQuestionVO).toList();
        }
        LambdaQueryWrapper<Question> wrapper = questionWrapper(dto)
                .last("ORDER BY RAND() LIMIT " + safeCount(dto.getCount()));
        return questionMapper.selectList(wrapper).stream().map(this::toQuestionVO).toList();
    }

    @Override
    @Transactional
    public HsePracticeResultVO submit(HsePracticeSubmitDTO dto) {
        Long studentId = UserContext.userId();
        int total = 0;
        int correct = 0;
        int score = 0;
        List<HseQuestionVO> details = new ArrayList<>();
        for (HsePracticeSubmitItemDTO item : dto.getAnswers()) {
            Question question = questionMapper.selectById(item.getQuestionId());
            if (question == null) continue;
            total++;
            boolean right = isCorrect(question, item.getAnswer());
            if (right) {
                correct++;
                score += question.getScore() == null ? 0 : question.getScore();
            }
            saveAnswer(studentId, question, item.getAnswer(), right, dto.getPracticeType());
            updateWrongBook(studentId, question, right);
            HseQuestionVO vo = toQuestionVO(question);
            vo.setCorrect(right);
            vo.setCorrectAnswer(question.getAnswer());
            vo.setStudentAnswer(item.getAnswer());
            details.add(vo);
        }
        HsePracticeResultVO result = new HsePracticeResultVO();
        result.setTotalCount(total);
        result.setCorrectCount(correct);
        result.setScore(score);
        result.setAccuracy(total == 0 ? 0 : Math.round(correct * 100f / total));
        result.setNotice("练习成绩用于薄弱点强化，不作为实验准入依据；准入只以正式安全考试结果为准。");
        result.setDetails(details);
        return result;
    }

    @Override
    public List<HseWrongQuestion> wrongBook() {
        return wrongQuestionMapper.selectList(new LambdaQueryWrapper<HseWrongQuestion>()
                .eq(HseWrongQuestion::getStudentId, UserContext.userId())
                .gt(HseWrongQuestion::getWrongCount, 0)
                .orderByDesc(HseWrongQuestion::getLastWrongTime));
    }

    @Override
    public List<HseWeakPointVO> myWeakPoints() {
        return weakPoints(List.of(UserContext.userId()));
    }

    @Override
    public List<HseWeakPointVO> classWeakPoints(Long courseId) {
        AccessUtil.requireTeacherOrAdmin();
        LabCourse course = courseMapper.selectById(courseId);
        AccessUtil.assertCourseWritable(course);
        List<Long> studentIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                        .select(CourseStudent::getStudentId)
                        .eq(CourseStudent::getCourseId, courseId)
                        .eq(CourseStudent::getStatus, 1))
                .stream().map(CourseStudent::getStudentId).toList();
        return weakPoints(studentIds);
    }

    @Override
    @Transactional
    public void favorite(Long questionId) {
        if (questionMapper.selectById(questionId) == null) {
            throw new BusinessException(404, "题目不存在");
        }
        Long studentId = UserContext.userId();
        Long exists = favoriteMapper.selectCount(new LambdaQueryWrapper<HseQuestionFavorite>()
                .eq(HseQuestionFavorite::getStudentId, studentId)
                .eq(HseQuestionFavorite::getQuestionId, questionId));
        if (exists == null || exists == 0) {
            HseQuestionFavorite favorite = new HseQuestionFavorite();
            favorite.setStudentId(studentId);
            favorite.setQuestionId(questionId);
            favoriteMapper.insert(favorite);
        }
    }

    @Override
    @Transactional
    public void unfavorite(Long questionId) {
        favoriteMapper.delete(new LambdaQueryWrapper<HseQuestionFavorite>()
                .eq(HseQuestionFavorite::getStudentId, UserContext.userId())
                .eq(HseQuestionFavorite::getQuestionId, questionId));
    }

    private LambdaQueryWrapper<Question> questionWrapper(HsePracticeQueryDTO dto) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dto.getKnowledgeId() != null, Question::getKnowledgeId, dto.getKnowledgeId());
        wrapper.eq(dto.getExperimentId() != null, Question::getExperimentId, dto.getExperimentId());
        wrapper.eq(StringUtils.hasText(dto.getRiskType()), Question::getRiskType, dto.getRiskType());
        wrapper.eq(StringUtils.hasText(dto.getDifficulty()), Question::getDifficulty, dto.getDifficulty());
        wrapper.eq(StringUtils.hasText(dto.getKnowledgePoint()), Question::getKnowledgePoint, dto.getKnowledgePoint());
        if ("WEAK".equals(dto.getMode())) {
            List<Long> weakQuestionIds = wrongBook().stream()
                    .filter(item -> !"MASTERED".equals(item.getMasteryStatus()))
                    .map(HseWrongQuestion::getQuestionId)
                    .toList();
            if (weakQuestionIds.isEmpty()) {
                wrapper.eq(Question::getId, -1L);
            } else {
                wrapper.in(Question::getId, weakQuestionIds);
            }
        }
        return wrapper;
    }

    private HseQuestionVO toQuestionVO(Question question) {
        HseQuestionVO vo = new HseQuestionVO();
        vo.setId(question.getId());
        vo.setType(question.getType());
        vo.setContent(question.getContent());
        vo.setOptions(question.getOptions());
        vo.setScore(question.getScore());
        vo.setDifficulty(question.getDifficulty());
        vo.setKnowledgePoint(question.getKnowledgePoint());
        vo.setKnowledgeId(question.getKnowledgeId());
        vo.setExperimentId(question.getExperimentId());
        vo.setRiskType(question.getRiskType());
        vo.setAnalysis(buildAnalysis(question));
        vo.setKnowledge(resolveKnowledge(question));
        vo.setResources(resolveResources(question, vo.getKnowledge()));
        vo.setFavorite(isFavorite(question.getId()));
        return vo;
    }

    private String buildAnalysis(Question question) {
        String base = StringUtils.hasText(question.getAnalysis()) ? question.getAnalysis() : "暂无解析。";
        String prefix = question.getKnowledgePoint() == null ? "" : "关联知识点：" + question.getKnowledgePoint() + "。";
        return prefix + base;
    }

    private SafetyKnowledge resolveKnowledge(Question question) {
        if (question.getKnowledgeId() != null) return knowledgeMapper.selectById(question.getKnowledgeId());
        if (!StringUtils.hasText(question.getKnowledgePoint())) return null;
        return knowledgeMapper.selectOne(new LambdaQueryWrapper<SafetyKnowledge>()
                .eq(SafetyKnowledge::getKnowledgePoint, question.getKnowledgePoint())
                .last("LIMIT 1"));
    }

    private List<TeachingResource> resolveResources(Question question, SafetyKnowledge knowledge) {
        List<Long> ids = new ArrayList<>();
        if (question.getRelatedResourceId() != null) ids.add(question.getRelatedResourceId());
        if (knowledge != null && knowledge.getReferenceResourceId() != null) ids.add(knowledge.getReferenceResourceId());
        if (ids.isEmpty()) {
            return resourceMapper.selectList(new LambdaQueryWrapper<TeachingResource>()
                    .eq(question.getExperimentId() != null, TeachingResource::getExperimentId, question.getExperimentId())
                    .like(StringUtils.hasText(question.getKnowledgePoint()), TeachingResource::getTags, question.getKnowledgePoint())
                    .eq(TeachingResource::getStatus, 1)
                    .last("LIMIT 3"));
        }
        return resourceMapper.selectBatchIds(ids.stream().distinct().toList());
    }

    private boolean isFavorite(Long questionId) {
        return favoriteMapper.selectCount(new LambdaQueryWrapper<HseQuestionFavorite>()
                .eq(HseQuestionFavorite::getStudentId, UserContext.userId())
                .eq(HseQuestionFavorite::getQuestionId, questionId)) > 0;
    }

    private void saveAnswer(Long studentId, Question question, String answer, boolean right, String practiceType) {
        HsePracticeAnswer entity = new HsePracticeAnswer();
        entity.setStudentId(studentId);
        entity.setQuestionId(question.getId());
        entity.setKnowledgeId(question.getKnowledgeId());
        entity.setExperimentId(question.getExperimentId());
        entity.setRiskType(question.getRiskType());
        entity.setPracticeType(practiceType);
        entity.setStudentAnswer(answer);
        entity.setCorrectFlag(right ? 1 : 0);
        entity.setScore(right ? (question.getScore() == null ? 0 : question.getScore()) : 0);
        practiceAnswerMapper.insert(entity);
    }

    private void updateWrongBook(Long studentId, Question question, boolean right) {
        HseWrongQuestion wrong = wrongQuestionMapper.selectOne(new LambdaQueryWrapper<HseWrongQuestion>()
                .eq(HseWrongQuestion::getStudentId, studentId)
                .eq(HseWrongQuestion::getQuestionId, question.getId()));
        if (wrong == null) {
            wrong = new HseWrongQuestion();
            wrong.setStudentId(studentId);
            wrong.setQuestionId(question.getId());
            wrong.setKnowledgeId(question.getKnowledgeId());
            wrong.setKnowledgePoint(question.getKnowledgePoint());
            wrong.setRiskType(question.getRiskType());
            wrong.setWrongCount(0);
            wrong.setCorrectStreak(0);
        }
        if (right) {
            wrong.setCorrectStreak((wrong.getCorrectStreak() == null ? 0 : wrong.getCorrectStreak()) + 1);
        } else {
            wrong.setWrongCount((wrong.getWrongCount() == null ? 0 : wrong.getWrongCount()) + 1);
            wrong.setCorrectStreak(0);
            wrong.setLastWrongTime(LocalDateTime.now());
        }
        wrong.setMasteryStatus(resolveMastery(wrong));
        if (wrong.getId() == null) wrongQuestionMapper.insert(wrong);
        else wrongQuestionMapper.updateById(wrong);
    }

    private String resolveMastery(HseWrongQuestion wrong) {
        if (wrong.getCorrectStreak() != null && wrong.getCorrectStreak() >= 3) return "MASTERED";
        if (wrong.getWrongCount() != null && wrong.getWrongCount() >= 3) return "WEAK";
        return "LEARNING";
    }

    private boolean isCorrect(Question question, String answer) {
        String correct = question.getAnswer();
        if (!StringUtils.hasText(correct)) return false;
        if ("MULTIPLE".equals(question.getType())) {
            return splitAnswer(correct).equals(splitAnswer(answer));
        }
        return correct.trim().equalsIgnoreCase(String.valueOf(answer).trim());
    }

    private Set<String> splitAnswer(String value) {
        if (!StringUtils.hasText(value)) return Set.of();
        return Arrays.stream(value.split(",")).map(String::trim).map(String::toUpperCase).collect(Collectors.toSet());
    }

    private List<HseWeakPointVO> weakPoints(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return List.of();
        List<HseWrongQuestion> wrongs = wrongQuestionMapper.selectList(new LambdaQueryWrapper<HseWrongQuestion>()
                .in(HseWrongQuestion::getStudentId, studentIds)
                .gt(HseWrongQuestion::getWrongCount, 0));
        Map<String, List<HseWrongQuestion>> groups = wrongs.stream()
                .collect(Collectors.groupingBy(item -> String.valueOf(item.getKnowledgePoint())));
        return groups.entrySet().stream().map(entry -> {
            HseWeakPointVO vo = new HseWeakPointVO();
            vo.setKnowledgePoint(entry.getKey());
            vo.setRiskType(entry.getValue().stream().map(HseWrongQuestion::getRiskType).filter(Objects::nonNull).findFirst().orElse(null));
            int wrongCount = entry.getValue().stream().mapToInt(item -> item.getWrongCount() == null ? 0 : item.getWrongCount()).sum();
            int streak = entry.getValue().stream().mapToInt(item -> item.getCorrectStreak() == null ? 0 : item.getCorrectStreak()).sum();
            vo.setWrongCount(wrongCount);
            vo.setAnswerCount(wrongCount + streak);
            vo.setWeakWeight(Math.max(0, wrongCount * 10 - streak * 3));
            return vo;
        }).sorted(Comparator.comparing(HseWeakPointVO::getWeakWeight).reversed()).limit(10).toList();
    }

    private int safeCount(Integer count) {
        return Math.max(1, Math.min(100, count == null ? 10 : count));
    }

}
