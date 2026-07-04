package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.AiChatRecordMapper;
import com.cupk.mapper.QuestionMapper;
import com.cupk.pojo.AiChatRecord;
import com.cupk.pojo.Question;
import com.cupk.service.AiChatService;
import com.cupk.common.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI问答服务实现
 * 基于本地安全知识库（t_question 表）匹配相关知识点，辅助生成回答
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    @Autowired
    private AiChatRecordMapper aiChatRecordMapper;

    @Autowired
    private QuestionMapper questionMapper;

    /** 场景对应的系统提示词（附录B） */
    private static final Map<String, String> SCENE_PROMPTS = Map.of(
        "SAFETY_QA", "你是油气工程实验安全专家，请基于安全知识库回答以下问题：",
        "ERROR_EXPLAIN", "你是实验教学辅导老师，请用通俗易懂的方式解释以下错题的正确思路：",
        "REPORT_SUGGEST", "你是实验报告评审助手，请分析实验报告并给出改进建议："
    );

    @Override
    public Map<String, Object> ask(String scene, String question, Long experimentId) {
        Long userId = UserContext.getUserId();

        // 1. 从本地知识库匹配相关安全知识
        List<Question> relevantKnowledge = searchKnowledgeBase(question);

        // 2. 构建上下文
        String context = buildContext(relevantKnowledge);
        String systemPrompt = SCENE_PROMPTS.getOrDefault(scene, "请回答以下问题：");

        // 3. 生成回答（TODO: 接入真实AI模型，当前使用知识库匹配+模板）
        String answer = generateAnswer(scene, question, relevantKnowledge, context, systemPrompt);

        // 4. 提取关联知识点
        List<String> relatedKnowledge = relevantKnowledge.stream()
                .map(Question::getKnowledgePoint)
                .filter(Objects::nonNull)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        // 5. 保存AI问答记录
        AiChatRecord record = new AiChatRecord();
        record.setUserId(userId);
        record.setScene(scene);
        record.setQuestion(question);
        record.setAnswer(answer);
        record.setToolName("LocalKB+Template");
        record.setExperimentId(experimentId);
        record.setCreateTime(new Date());
        aiChatRecordMapper.insert(record);

        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("answer", answer);
        result.put("relatedKnowledge", relatedKnowledge);
        result.put("knowledgeBaseMatchCount", relevantKnowledge.size());
        result.put("scene", scene);
        return result;
    }

    @Override
    public Page<AiChatRecord> getRecords(int pageNum, int pageSize, String scene) {
        Page<AiChatRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatRecord::getUserId, UserContext.getUserId())
               .eq(scene != null && !scene.isEmpty(), AiChatRecord::getScene, scene)
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

    // ==================== 知识库检索 ====================

    /**
     * 基于关键词匹配搜索本地知识库
     */
    private List<Question> searchKnowledgeBase(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 提取查询关键词
        String[] keywords = query.split("[，,。.！!？?\\s]+");
        List<Question> allMatched = new ArrayList<>();

        for (String keyword : keywords) {
            if (keyword.length() < 2) continue; // 跳过太短的词

            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(Question::getContent, keyword)
                   .or()
                   .like(Question::getKnowledgePoint, keyword)
                   .or()
                   .like(Question::getAnalysis, keyword)
                   .last("LIMIT 10");
            allMatched.addAll(questionMapper.selectList(wrapper));
        }

        // 去重，按匹配次数排序
        Map<Long, Question> uniqueMap = new LinkedHashMap<>();
        for (Question q : allMatched) {
            uniqueMap.putIfAbsent(q.getId(), q);
        }
        return new ArrayList<>(uniqueMap.values());
    }

    /**
     * 构建知识库上下文
     */
    private String buildContext(List<Question> knowledgeList) {
        if (knowledgeList.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("【相关安全知识点】\n");
        Set<String> seen = new HashSet<>();
        for (Question q : knowledgeList) {
            if (q.getKnowledgePoint() != null && seen.add(q.getKnowledgePoint())) {
                sb.append("• ").append(q.getKnowledgePoint());
                if (q.getAnalysis() != null && !q.getAnalysis().isEmpty()) {
                    sb.append("：").append(q.getAnalysis());
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 生成回答（模板驱动，待接入AI模型后替换）
     */
    private String generateAnswer(String scene, String query,
                                   List<Question> knowledgeList,
                                   String context, String systemPrompt) {
        StringBuilder answer = new StringBuilder();
        answer.append("【AI辅助生成，仅供参考】\n\n");

        switch (scene) {
            case "SAFETY_QA":
                answer.append(generateSafetyQA(query, knowledgeList, context));
                break;
            case "ERROR_EXPLAIN":
                answer.append(generateErrorExplanation(query, knowledgeList));
                break;
            case "REPORT_SUGGEST":
                answer.append(generateReportSuggestion(query));
                break;
            default:
                answer.append("关于「").append(query).append("」的解答：\n\n");
                if (!context.isEmpty()) {
                    answer.append(context).append("\n");
                }
                answer.append("如需更详细的解答，请提供更多上下文信息。");
        }

        answer.append("\n\n---\n");
        answer.append("> ⚠ 本回答由本地知识库辅助生成，已记录AI使用情况。");
        answer.append("如需人工帮助，请联系实验指导教师。");
        return answer.toString();
    }

    private String generateSafetyQA(String query, List<Question> knowledgeList, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("针对您的安全咨询「").append(query).append("」：\n\n");

        if (!context.isEmpty()) {
            sb.append(context).append("\n");
        }

        if (!knowledgeList.isEmpty()) {
            sb.append("【相关考题参考】\n");
            for (int i = 0; i < Math.min(3, knowledgeList.size()); i++) {
                Question q = knowledgeList.get(i);
                sb.append(i + 1).append(". ").append(q.getContent()).append("\n");
                if (q.getAnswer() != null) {
                    sb.append("   参考答案：").append(q.getAnswer()).append("\n");
                }
                if (q.getAnalysis() != null) {
                    sb.append("   解析：").append(q.getAnalysis()).append("\n");
                }
            }
        } else {
            sb.append("建议查阅安全手册中关于该操作的注意事项。\n");
            sb.append("实验前请确保：佩戴个人防护装备、熟悉紧急停机和应急处理流程。");
        }

        return sb.toString();
    }

    private String generateErrorExplanation(String query, List<Question> knowledgeList) {
        StringBuilder sb = new StringBuilder();
        sb.append("关于您的错题疑问「").append(query).append("」：\n\n");

        if (!knowledgeList.isEmpty()) {
            sb.append("该题目涉及以下知识点：\n");
            Set<String> seen = new HashSet<>();
            for (Question q : knowledgeList) {
                if (q.getKnowledgePoint() != null && seen.add(q.getKnowledgePoint())) {
                    sb.append("• ").append(q.getKnowledgePoint()).append("\n");
                }
                if (q.getAnalysis() != null) {
                    sb.append("  解析思路：").append(q.getAnalysis()).append("\n\n");
                    break; // 只取最相关的一个
                }
            }
        } else {
            sb.append("建议回顾相关安全知识章节，重点关注操作规范和安全要求。\n");
        }

        return sb.toString();
    }

    private String generateReportSuggestion(String query) {
        StringBuilder sb = new StringBuilder();
        sb.append("针对您的报告问题「").append(query).append("」：\n\n");
        sb.append("一份完整的实验报告通常应包含以下部分：\n");
        sb.append("1. **实验目的**：明确本次实验要验证的原理或达到的目标\n");
        sb.append("2. **实验原理**：简述实验所依据的理论基础\n");
        sb.append("3. **实验步骤**：详细记录操作过程，含关键参数\n");
        sb.append("4. **数据记录与分析**：原始数据+处理过程+图表\n");
        sb.append("5. **结果讨论**：分析误差来源、提出改进建议\n");
        sb.append("6. **安全注意事项**：记录实验中采取的安全措施\n\n");
        sb.append("请对照以上结构检查您的报告是否完整。");
        return sb.toString();
    }
}
