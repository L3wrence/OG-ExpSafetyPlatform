package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.AiChatRecordMapper;
import com.cupk.mapper.QuestionMapper;
import com.cupk.pojo.AiChatRecord;
import com.cupk.pojo.Question;
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
    private static final String TOOL_NAME = "LocalKB+Template";
    private static final Map<String, String> SCENE_NAMES = Map.of(
            "SAFETY_QA", "安全问题辅助解释",
            "ERROR_EXPLAIN", "错题原因解释",
            "REPORT_SUGGEST", "实验报告改进建议"
    );

    private final AiChatRecordMapper aiChatRecordMapper;
    private final QuestionMapper questionMapper;

    public AiChatServiceImpl(AiChatRecordMapper aiChatRecordMapper, QuestionMapper questionMapper) {
        this.aiChatRecordMapper = aiChatRecordMapper;
        this.questionMapper = questionMapper;
    }

    @Override
    public Map<String, Object> ask(String scene, String question, Long experimentId) {
        Long userId = UserContext.getUserId();
        String normalizedScene = StringUtils.hasText(scene) ? scene : "SAFETY_QA";
        List<Question> matchedQuestions = searchKnowledgeBase(question);
        List<String> relatedKnowledge = matchedQuestions.stream()
                .map(Question::getKnowledgePoint)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(5)
                .toList();

        String answer = buildAnswer(normalizedScene, question, matchedQuestions);

        AiChatRecord record = new AiChatRecord();
        record.setUserId(userId);
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
        result.put("knowledgeBaseMatchCount", matchedQuestions.size());
        result.put("scene", normalizedScene);
        return result;
    }

    @Override
    public Page<AiChatRecord> getRecords(int pageNum, int pageSize, String scene) {
        Page<AiChatRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatRecord::getUserId, UserContext.getUserId())
                .eq(StringUtils.hasText(scene), AiChatRecord::getScene, scene)
                .orderByDesc(AiChatRecord::getCreateTime);
        return aiChatRecordMapper.selectPage(page, wrapper);
    }

    @Override
    public void updateFeedback(Long id, String manualRevision) {
        AiChatRecord record = new AiChatRecord();
        record.setId(id);
        record.setManualRevision(manualRevision);
        aiChatRecordMapper.updateById(record);
    }

    private List<Question> searchKnowledgeBase(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        Map<Long, Question> result = new LinkedHashMap<>();
        for (String keyword : keywords(query)) {
            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(Question::getContent, keyword)
                    .or()
                    .like(Question::getKnowledgePoint, keyword)
                    .or()
                    .like(Question::getAnalysis, keyword)
                    .last("LIMIT 10");
            questionMapper.selectList(wrapper).forEach(question -> result.putIfAbsent(question.getId(), question));
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

    private String buildAnswer(String scene, String question, List<Question> matchedQuestions) {
        StringBuilder answer = new StringBuilder();
        answer.append("AI辅助生成，仅供参考。").append("\n");
        answer.append("场景：").append(SCENE_NAMES.getOrDefault(scene, "通用实验安全辅助")).append("\n\n");

        switch (scene) {
            case "ERROR_EXPLAIN" -> appendErrorExplanation(answer, question, matchedQuestions);
            case "REPORT_SUGGEST" -> appendReportSuggestion(answer, question, matchedQuestions);
            default -> appendSafetyAnswer(answer, question, matchedQuestions);
        }

        answer.append("\n使用说明：AI回答不会替代教师评分、实验预约审核或正式安全规范。");
        return answer.toString();
    }

    private void appendSafetyAnswer(StringBuilder answer, String question, List<Question> matchedQuestions) {
        answer.append("针对问题：").append(question).append("\n");
        if (matchedQuestions.isEmpty()) {
            answer.append("建议先核对实验指导书、安全知识库和现场教师要求，再进行实验操作。");
            return;
        }
        answer.append("可参考以下安全知识点：").append("\n");
        for (int i = 0; i < Math.min(3, matchedQuestions.size()); i++) {
            Question item = matchedQuestions.get(i);
            answer.append(i + 1).append(". ");
            answer.append(valueOrDefault(item.getKnowledgePoint(), item.getContent())).append("\n");
            if (StringUtils.hasText(item.getAnalysis())) {
                answer.append("   说明：").append(item.getAnalysis()).append("\n");
            }
        }
    }

    private void appendErrorExplanation(StringBuilder answer, String question, List<Question> matchedQuestions) {
        answer.append("错题/疑问：").append(question).append("\n");
        Question bestMatch = matchedQuestions.stream().filter(Objects::nonNull).findFirst().orElse(null);
        if (bestMatch == null) {
            answer.append("建议回到对应知识点，先区分概念、适用条件和安全后果，再重新作答。");
            return;
        }
        answer.append("关联知识点：").append(valueOrDefault(bestMatch.getKnowledgePoint(), "未标注")).append("\n");
        answer.append("正确答案：").append(valueOrDefault(bestMatch.getAnswer(), "需教师确认")).append("\n");
        if (StringUtils.hasText(bestMatch.getAnalysis())) {
            answer.append("解析：").append(bestMatch.getAnalysis());
        }
    }

    private void appendReportSuggestion(StringBuilder answer, String question, List<Question> matchedQuestions) {
        answer.append("报告改进方向：").append(question).append("\n");
        answer.append("1. 补充实验目的、实验原理和关键安全风险。").append("\n");
        answer.append("2. 将实验步骤、原始数据、计算过程和结果分析分开书写。").append("\n");
        answer.append("3. 对异常数据说明可能原因，并给出改进措施。").append("\n");
        answer.append("4. 在结论中回应实验目标，避免只罗列现象。");
        if (!matchedQuestions.isEmpty()) {
            answer.append("\n可补充的安全知识点：")
                    .append(matchedQuestions.stream()
                            .map(Question::getKnowledgePoint)
                            .filter(StringUtils::hasText)
                            .distinct()
                            .limit(3)
                            .collect(Collectors.joining("、")));
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
