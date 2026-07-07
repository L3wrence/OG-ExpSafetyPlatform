package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.AiChatRecordMapper;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.HseWrongQuestionMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.QuestionMapper;
import com.cupk.mapper.SafetyKnowledgeMapper;
import com.cupk.mapper.TeachingResourceMapper;
import com.cupk.pojo.AiChatRecord;
import com.cupk.pojo.CourseStudent;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.HseWrongQuestion;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.Question;
import com.cupk.pojo.SafetyKnowledge;
import com.cupk.pojo.TeachingResource;
import com.cupk.service.AiChatService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {
    private static final String TOOL_NAME = "LocalCourseKB+Template";
    private static final String DISCLAIMER = "AI 辅助内容仅供学习参考，正式安全要求以实验规程和教师要求为准。";
    private static final List<String> SUPPORTED_SCENES = List.of("SAFETY_QA", "ERROR_EXPLAIN", "REPORT_SUGGEST");
    private static final Map<String, String> SCENE_NAMES = Map.of(
            "SAFETY_QA", "安全问题辅助解释",
            "ERROR_EXPLAIN", "错题原因解释",
            "REPORT_SUGGEST", "实验报告改进建议"
    );

    private final AiChatRecordMapper aiChatRecordMapper;
    private final QuestionMapper questionMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final ExperimentMapper experimentMapper;
    private final LabCourseMapper courseMapper;
    private final SafetyKnowledgeMapper safetyKnowledgeMapper;
    private final TeachingResourceMapper resourceMapper;
    private final HseWrongQuestionMapper wrongQuestionMapper;

    public AiChatServiceImpl(AiChatRecordMapper aiChatRecordMapper,
                             QuestionMapper questionMapper,
                             CourseStudentMapper courseStudentMapper,
                             ExperimentMapper experimentMapper,
                             LabCourseMapper courseMapper,
                             SafetyKnowledgeMapper safetyKnowledgeMapper,
                             TeachingResourceMapper resourceMapper,
                             HseWrongQuestionMapper wrongQuestionMapper) {
        this.aiChatRecordMapper = aiChatRecordMapper;
        this.questionMapper = questionMapper;
        this.courseStudentMapper = courseStudentMapper;
        this.experimentMapper = experimentMapper;
        this.courseMapper = courseMapper;
        this.safetyKnowledgeMapper = safetyKnowledgeMapper;
        this.resourceMapper = resourceMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
    }

    @Override
    public Map<String, Object> ask(String scene, String question, Long experimentId) {
        String normalizedScene = normalizeScene(scene);
        if (!StringUtils.hasText(question)) {
            throw new BusinessException(400, "问题不能为空");
        }
        AiScope scope = resolveScope(experimentId);
        List<SafetyKnowledge> matchedKnowledge = searchSafetyKnowledge(question, scope);
        List<TeachingResource> matchedResources = searchResources(question, scope);
        List<HseWrongQuestion> wrongQuestions = "ERROR_EXPLAIN".equals(normalizedScene)
                ? searchOwnWrongQuestions(question, scope)
                : List.of();
        List<String> relatedKnowledge = matchedKnowledge.stream()
                .map(SafetyKnowledge::getKnowledgePoint)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(5)
                .toList();

        String answer = buildAnswer(normalizedScene, question, matchedKnowledge, matchedResources, wrongQuestions);

        AiChatRecord record = new AiChatRecord();
        record.setUserId(UserContext.userId());
        record.setScene(normalizedScene);
        record.setQuestion(question);
        record.setAnswer(answer);
        record.setToolName(TOOL_NAME);
        record.setExperimentId(experimentId);
        record.setCreateTime(new Date());
        aiChatRecordMapper.insert(record);

        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("answer", answer);
        result.put("relatedKnowledge", relatedKnowledge);
        result.put("knowledgeBaseMatchCount", matchedKnowledge.size() + matchedResources.size());
        result.put("scene", normalizedScene);
        result.put("disclaimer", DISCLAIMER);
        return result;
    }

    @Override
    public Page<AiChatRecord> getRecords(int pageNum, int pageSize, String scene) {
        Page<AiChatRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatRecord::getUserId, UserContext.userId())
                .eq(StringUtils.hasText(scene), AiChatRecord::getScene, scene)
                .orderByDesc(AiChatRecord::getCreateTime);
        return aiChatRecordMapper.selectPage(page, wrapper);
    }

    @Override
    public void updateFeedback(Long id, String manualRevision) {
        AiChatRecord existing = aiChatRecordMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "AI问答记录不存在");
        }
        if (!UserContext.userId().equals(existing.getUserId())) {
            throw new BusinessException(403, "不能修改他人的AI问答记录");
        }
        AiChatRecord record = new AiChatRecord();
        record.setId(id);
        record.setManualRevision(manualRevision);
        aiChatRecordMapper.updateById(record);
    }

    private List<SafetyKnowledge> searchSafetyKnowledge(String query, AiScope scope) {
        if (!StringUtils.hasText(query) || scope.experimentIds().isEmpty()) {
            return List.of();
        }
        Map<Long, SafetyKnowledge> result = new LinkedHashMap<>();
        for (String keyword : keywords(query)) {
            LambdaQueryWrapper<SafetyKnowledge> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(SafetyKnowledge::getExperimentId, scope.experimentIds())
                    .eq(SafetyKnowledge::getStatus, 1)
                    .and(w -> w.like(SafetyKnowledge::getContent, keyword)
                            .or().like(SafetyKnowledge::getKnowledgePoint, keyword)
                            .or().like(SafetyKnowledge::getRiskType, keyword))
                    .last("LIMIT 10");
            safetyKnowledgeMapper.selectList(wrapper).forEach(item -> result.putIfAbsent(item.getId(), item));
        }
        return new ArrayList<>(result.values());
    }

    private List<TeachingResource> searchResources(String query, AiScope scope) {
        if (!StringUtils.hasText(query) || scope.courseIds().isEmpty()) {
            return List.of();
        }
        Map<Long, TeachingResource> result = new LinkedHashMap<>();
        for (String keyword : keywords(query)) {
            LambdaQueryWrapper<TeachingResource> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(TeachingResource::getCourseId, scope.courseIds())
                    .eq(TeachingResource::getStatus, 1)
                    .eq(TeachingResource::getInvalidFlag, 0)
                    .and(w -> w.like(TeachingResource::getTitle, keyword)
                            .or().like(TeachingResource::getDescription, keyword)
                            .or().like(TeachingResource::getKnowledgePoint, keyword)
                            .or().like(TeachingResource::getTags, keyword))
                    .last("LIMIT 10");
            resourceMapper.selectList(wrapper).forEach(item -> result.putIfAbsent(item.getId(), item));
        }
        return new ArrayList<>(result.values());
    }

    private List<HseWrongQuestion> searchOwnWrongQuestions(String query, AiScope scope) {
        if (!UserContext.isStudent() || !StringUtils.hasText(query) || scope.experimentIds().isEmpty()) {
            return List.of();
        }
        Map<Long, HseWrongQuestion> result = new LinkedHashMap<>();
        for (String keyword : keywords(query)) {
            List<Question> scopedQuestions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                    .in(Question::getExperimentId, scope.experimentIds())
                    .and(w -> w.like(Question::getContent, keyword)
                            .or().like(Question::getKnowledgePoint, keyword)
                            .or().like(Question::getAnalysis, keyword))
                    .last("LIMIT 20"));
            List<Long> questionIds = scopedQuestions.stream().map(Question::getId).toList();
            if (questionIds.isEmpty()) {
                continue;
            }
            wrongQuestionMapper.selectList(new LambdaQueryWrapper<HseWrongQuestion>()
                            .eq(HseWrongQuestion::getStudentId, UserContext.userId())
                            .in(HseWrongQuestion::getQuestionId, questionIds)
                            .last("LIMIT 10"))
                    .forEach(item -> result.putIfAbsent(item.getId(), item));
        }
        return new ArrayList<>(result.values());
    }

    private Set<String> keywords(String query) {
        return List.of(query.split("[,，。；;、\\s]+")).stream()
                .map(String::trim)
                .filter(keyword -> keyword.length() >= 2)
                .limit(8)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private String buildAnswer(String scene,
                               String question,
                               List<SafetyKnowledge> matchedKnowledge,
                               List<TeachingResource> matchedResources,
                               List<HseWrongQuestion> wrongQuestions) {
        StringBuilder answer = new StringBuilder();
        answer.append(DISCLAIMER).append("\n");
        answer.append("场景：").append(SCENE_NAMES.getOrDefault(scene, "通用实验安全辅助")).append("\n\n");
        if (containsDecisionRequest(question)) {
            answer.append("我不能替代教师进行正式评分、预约审核或实验准入判断。请在系统对应流程中提交材料，由教师或管理员按规程处理。").append("\n\n");
        }
        switch (scene) {
            case "ERROR_EXPLAIN" -> appendErrorExplanation(answer, question, wrongQuestions, matchedKnowledge);
            case "REPORT_SUGGEST" -> appendReportSuggestion(answer, question, matchedKnowledge, matchedResources);
            default -> appendSafetyAnswer(answer, question, matchedKnowledge, matchedResources);
        }
        answer.append("\n边界说明：本助手不提供正在使用的正式题库标准答案，不自动写完整实验报告，也不修改成绩、预约或准入状态。");
        return answer.toString();
    }

    private void appendSafetyAnswer(StringBuilder answer,
                                    String question,
                                    List<SafetyKnowledge> matchedKnowledge,
                                    List<TeachingResource> matchedResources) {
        answer.append("针对问题：").append(question).append("\n");
        if (matchedKnowledge.isEmpty() && matchedResources.isEmpty()) {
            answer.append("建议先核对实验指导书、安全知识库和现场教师要求，再进行实验操作。");
            return;
        }
        answer.append("可参考以下安全知识点：").append("\n");
        for (int i = 0; i < Math.min(3, matchedKnowledge.size()); i++) {
            SafetyKnowledge item = matchedKnowledge.get(i);
            answer.append(i + 1).append(". ")
                    .append(valueOrDefault(item.getKnowledgePoint(), item.getContent()))
                    .append("\n");
            if (StringUtils.hasText(item.getContent())) {
                answer.append("   说明：").append(limitText(item.getContent(), 180)).append("\n");
            }
        }
        appendResourceHints(answer, matchedResources);
    }

    private void appendErrorExplanation(StringBuilder answer,
                                        String question,
                                        List<HseWrongQuestion> wrongQuestions,
                                        List<SafetyKnowledge> matchedKnowledge) {
        answer.append("错题/疑问：").append(question).append("\n");
        HseWrongQuestion bestMatch = wrongQuestions.stream().filter(Objects::nonNull).findFirst().orElse(null);
        if (bestMatch == null) {
            answer.append("没有找到与你本人已提交错题直接匹配的记录。建议先在错题练习中定位题目，再回到对应知识点复习；我不会直接给出正式题库标准答案。");
            return;
        }
        answer.append("关联知识点：").append(valueOrDefault(bestMatch.getKnowledgePoint(), "未标注")).append("\n");
        answer.append("复习方向：先确认该知识点的适用条件、风险后果和实验规程要求，再对照自己的作答过程找出误判环节。").append("\n");
        matchedKnowledge.stream()
                .filter(item -> Objects.equals(item.getId(), bestMatch.getKnowledgeId())
                        || Objects.equals(item.getKnowledgePoint(), bestMatch.getKnowledgePoint()))
                .findFirst()
                .ifPresent(item -> answer.append("知识提示：").append(limitText(item.getContent(), 220)).append("\n"));
    }

    private void appendReportSuggestion(StringBuilder answer,
                                        String question,
                                        List<SafetyKnowledge> matchedKnowledge,
                                        List<TeachingResource> matchedResources) {
        answer.append("报告改进方向：").append(question).append("\n");
        answer.append("1. 检查是否缺少实验目的、原理、关键风险和必要 PPE 说明。").append("\n");
        answer.append("2. 对照教师模板补齐数据记录、计算过程、误差来源和安全反思。").append("\n");
        answer.append("3. 对异常数据只给分析方向和证据要求，不编造实验结果。").append("\n");
        answer.append("4. 结论应回应实验目标，并说明结果可靠性和改进空间。");
        if (!matchedKnowledge.isEmpty()) {
            answer.append("\n可补充的安全知识点：")
                    .append(matchedKnowledge.stream()
                            .map(SafetyKnowledge::getKnowledgePoint)
                            .filter(StringUtils::hasText)
                            .distinct()
                            .limit(3)
                            .collect(Collectors.joining("、")));
        }
        appendResourceHints(answer, matchedResources);
    }

    private void appendResourceHints(StringBuilder answer, List<TeachingResource> matchedResources) {
        if (matchedResources.isEmpty()) {
            return;
        }
        answer.append("\n建议回看资源：")
                .append(matchedResources.stream()
                        .map(TeachingResource::getTitle)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .limit(3)
                        .collect(Collectors.joining("、")));
    }

    private AiScope resolveScope(Long experimentId) {
        if (UserContext.isAdmin()) {
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<LabCourse>().select(LabCourse::getId))
                    .stream().map(LabCourse::getId).toList();
            return scopeFromCourseIds(courseIds, experimentId);
        }
        if (UserContext.isTeacher()) {
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<LabCourse>()
                            .select(LabCourse::getId)
                            .eq(LabCourse::getTeacherId, UserContext.userId()))
                    .stream().map(LabCourse::getId).toList();
            return scopeFromCourseIds(courseIds, experimentId);
        }
        if (UserContext.isStudent()) {
            List<Long> courseIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                            .select(CourseStudent::getCourseId)
                            .eq(CourseStudent::getStudentId, UserContext.userId())
                            .eq(CourseStudent::getStatus, 1)
                            .eq(CourseStudent::getDeleted, 0))
                    .stream().map(CourseStudent::getCourseId).toList();
            return scopeFromCourseIds(courseIds, experimentId);
        }
        throw new BusinessException(403, "无权使用实验知识助手");
    }

    private AiScope scopeFromCourseIds(List<Long> courseIds, Long experimentId) {
        if (courseIds == null || courseIds.isEmpty()) {
            throw new BusinessException(403, "没有可访问的课程内容");
        }
        if (experimentId != null) {
            Experiment experiment = experimentMapper.selectById(experimentId);
            if (experiment == null) {
                throw new BusinessException(404, "实验不存在");
            }
            if (!courseIds.contains(experiment.getCourseId())) {
                throw new BusinessException(403, "不能访问未授权课程的实验内容");
            }
            return new AiScope(List.of(experiment.getCourseId()), List.of(experimentId));
        }
        List<Long> experimentIds = experimentMapper.selectList(new LambdaQueryWrapper<Experiment>()
                        .select(Experiment::getId)
                        .in(Experiment::getCourseId, courseIds))
                .stream().map(Experiment::getId).toList();
        return new AiScope(courseIds, experimentIds);
    }

    private String normalizeScene(String scene) {
        String value = StringUtils.hasText(scene) ? scene : "SAFETY_QA";
        if (!SUPPORTED_SCENES.contains(value)) {
            throw new BusinessException(400, "不支持的AI辅助场景");
        }
        return value;
    }

    private boolean containsDecisionRequest(String question) {
        String value = question == null ? "" : question;
        return List.of("给我打分", "评分", "成绩", "通过准入", "准入资格", "帮我预约", "审核预约", "批准预约", "自动生成报告", "写完整报告")
                .stream()
                .anyMatch(value::contains);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private record AiScope(List<Long> courseIds, List<Long> experimentIds) {
    }
}
