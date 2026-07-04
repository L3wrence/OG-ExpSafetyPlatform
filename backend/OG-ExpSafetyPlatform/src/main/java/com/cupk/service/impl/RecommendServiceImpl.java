package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.mapper.*;
import com.cupk.pojo.*;
import com.cupk.service.RecommendService;
import com.cupk.common.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐算法服务实现
 * 五因子打分模型（附录A）：
 *   总分 = 0.35×知识点匹配 + 0.25×错误相关 + 0.20×未学程度 + 0.10×热度 + 0.10×难度匹配
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private RecommendRecordMapper recommendRecordMapper;

    @Autowired
    private ExamAnswerMapper examAnswerMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private TeachingResourceMapper teachingResourceMapper;

    @Autowired
    private ExperimentMapper experimentMapper;

    @Autowired
    private LearningRecordMapper learningRecordMapper;

    // ===== 五因子权重（来自工作提纲附录A） =====
    private static final double W_KNOWLEDGE    = 0.35;
    private static final double W_ERROR        = 0.25;
    private static final double W_NEWNESS      = 0.20;
    private static final double W_POPULARITY   = 0.10;
    private static final double W_DIFFICULTY   = 0.10;
    private static final int    TOP_N          = 10;

    @Override
    public Map<String, Object> getRecommendedResources(Long experimentId) {
        Long studentId = UserContext.getUserId();
        List<ScoreRecord> scored = new ArrayList<>();

        // 从 t_resource 表获取所有可用资源ID
        List<Long> allResourceIds = getAvailableResourceIds();

        // 逐资源打分
        for (Long resourceId : allResourceIds) {
            double totalScore = calculateScore(studentId, resourceId, experimentId);
            String reason = buildReason(studentId, resourceId, totalScore, experimentId);

            ScoreRecord sr = new ScoreRecord();
            sr.resourceId = resourceId;
            sr.totalScore = totalScore;
            sr.reason = reason;
            scored.add(sr);

            // 保存推荐记录
            saveRecommendRecord(studentId, resourceId, experimentId, totalScore, reason);
        }

        // 按总分降序，取 Top N
        List<Map<String, Object>> resources = scored.stream()
                .sorted((a, b) -> Double.compare(b.totalScore, a.totalScore))
                .limit(TOP_N)
                .map(sr -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("resourceId", sr.resourceId);
                    item.put("score", Math.round(sr.totalScore * 100.0) / 100.0);
                    item.put("reason", sr.reason);
                    // 填充资源详情
                    TeachingResource res = teachingResourceMapper.selectById(sr.resourceId);
                    item.put("resourceName", res != null ? res.getTitle() : "资源" + sr.resourceId);
                    item.put("resourceType", res != null ? res.getResourceType() : null);
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("resources", resources);
        result.put("generatedAt", new Date());
        result.put("totalEvaluated", scored.size());
        return result;
    }

    @Override
    public double calculateScore(Long studentId, Long resourceId, Long experimentId) {
        double knowledgeMatch  = calcKnowledgeMatch(resourceId, experimentId);
        double errorRelevance  = calcErrorRelevance(studentId, resourceId);
        double newness         = calcNewness(studentId, resourceId);
        double popularity      = calcPopularity(resourceId);
        double difficultyMatch = calcDifficultyMatch(resourceId, experimentId);

        double total = W_KNOWLEDGE  * knowledgeMatch
                     + W_ERROR      * errorRelevance
                     + W_NEWNESS    * newness
                     + W_POPULARITY * popularity
                     + W_DIFFICULTY * difficultyMatch;

        // 归一化到 0-100
        return Math.min(100, Math.max(0, total));
    }

    // ==================== 五因子计算 ====================

    /**
     * 因子1：知识点匹配度（35%）
     * 资源所属实验 vs 目标实验的课程关联度
     * 数据来源：t_resource.experiment_id + t_experiment.course_id
     */
    private double calcKnowledgeMatch(Long resourceId, Long experimentId) {
        TeachingResource resource = teachingResourceMapper.selectById(resourceId);
        if (resource == null || resource.getExperimentId() == null) return 50.0;

        Experiment targetExp = experimentMapper.selectById(experimentId);
        Experiment resourceExp = experimentMapper.selectById(resource.getExperimentId());

        if (targetExp == null || resourceExp == null) return 50.0;

        // 同一实验满分，同一课程80分，不同课程30分
        if (resource.getExperimentId().equals(experimentId)) return 100.0;
        if (targetExp.getCourseId() != null && targetExp.getCourseId().equals(resourceExp.getCourseId())) return 80.0;
        return 30.0;
    }

    /**
     * 因子2：错误相关度（25%）
     * 资源所属课程覆盖学生错题课程的程度
     * 数据来源：t_exam_answer（is_correct=0）+ t_question.course_id + t_resource.experiment_id → t_experiment.course_id
     */
    private double calcErrorRelevance(Long studentId, Long resourceId) {
        // 1. 获取资源所属的课程
        TeachingResource resource = teachingResourceMapper.selectById(resourceId);
        if (resource == null || resource.getExperimentId() == null) return 0;
        Experiment resourceExp = experimentMapper.selectById(resource.getExperimentId());
        if (resourceExp == null || resourceExp.getCourseId() == null) return 0;
        Long resourceCourseId = resourceExp.getCourseId();

        // 2. 查出学生所有考试记录
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getStudentId, studentId);
        List<ExamRecord> records = examRecordMapper.selectList(recordWrapper);
        if (records.isEmpty()) return 0;

        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());

        // 3. 查出所有错题
        LambdaQueryWrapper<ExamAnswer> answerWrapper = new LambdaQueryWrapper<>();
        answerWrapper.in(ExamAnswer::getRecordId, recordIds)
                     .eq(ExamAnswer::getIsCorrect, 0);
        List<ExamAnswer> wrongAnswers = examAnswerMapper.selectList(answerWrapper);
        if (wrongAnswers.isEmpty()) return 0;

        // 4. 统计错题涉及的课程
        Set<Long> wrongCourseIds = new HashSet<>();
        for (ExamAnswer ans : wrongAnswers) {
            Question q = questionMapper.selectById(ans.getQuestionId());
            if (q != null && q.getCourseId() != null) {
                wrongCourseIds.add(q.getCourseId());
            }
        }
        if (wrongCourseIds.isEmpty()) return 0;

        // 5. 如果资源所属课程在学生错题课程中 → 高相关
        if (wrongCourseIds.contains(resourceCourseId)) {
            // 错题集中在该课程的程度越高，分数越高
            long wrongInThisCourse = wrongAnswers.stream()
                .filter(a -> {
                    Question q = questionMapper.selectById(a.getQuestionId());
                    return q != null && resourceCourseId.equals(q.getCourseId());
                })
                .count();
            return Math.min(100.0, (double) wrongInThisCourse / wrongAnswers.size() * 100.0 * 1.5);
        }
        return 10.0; // 不直接相关但仍有微弱关联
    }

    /**
     * 因子3：未学习程度（20%）
     * 该资源被该学生学过的次数（0=未学，分数最高100）
     * 数据来源：t_learning_record（学生ID + 资源ID）
     */
    private double calcNewness(Long studentId, Long resourceId) {
        LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningRecord::getStudentId, studentId)
               .eq(LearningRecord::getResourceId, resourceId);
        long learnCount = learningRecordMapper.selectCount(wrapper);

        // 从未学过得满分，学过3次以上得0分
        if (learnCount == 0) return 100.0;
        if (learnCount >= 3) return 0;
        return (3 - learnCount) / 3.0 * 100.0;
    }

    /**
     * 因子4：资源热度（10%）
     * 该资源被全体学生学习的总次数（归一化到0-100）
     * 数据来源：t_learning_record（COUNT聚合）
     */
    private double calcPopularity(Long resourceId) {
        LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningRecord::getResourceId, resourceId);
        long totalCount = learningRecordMapper.selectCount(wrapper);

        // 总学习记录数作为分母
        long maxCount = learningRecordMapper.selectCount(null);
        if (maxCount == 0) return 0;

        return (double) totalCount / maxCount * 100.0;
    }

    /**
     * 因子5：难度匹配度（10%）
     * 资源所属实验风险等级 vs 目标实验风险等级
     * 完全匹配100分，差1级50分，差2级以上0分
     */
    private double calcDifficultyMatch(Long resourceId, Long experimentId) {
        // 获取资源所属实验的风险等级
        TeachingResource resource = teachingResourceMapper.selectById(resourceId);
        String resourceDiff = null;
        if (resource != null && resource.getExperimentId() != null) {
            Experiment resourceExp = experimentMapper.selectById(resource.getExperimentId());
            resourceDiff = resourceExp != null ? resourceExp.getRiskLevel() : null;
        }

        // 获取目标实验的风险等级
        Experiment targetExp = experimentMapper.selectById(experimentId);
        String experimentDiff = targetExp != null ? targetExp.getRiskLevel() : null;

        if (resourceDiff == null || experimentDiff == null) return 50.0;

        List<String> levels = List.of("EASY", "MEDIUM", "HARD");
        int idx1 = levels.indexOf(resourceDiff.toUpperCase());
        int idx2 = levels.indexOf(experimentDiff.toUpperCase());
        if (idx1 < 0 || idx2 < 0) return 50.0;

        int diff = Math.abs(idx1 - idx2);
        if (diff == 0) return 100.0;
        if (diff == 1) return 50.0;
        return 0;
    }

    // ==================== 推荐理由生成 ====================

    /**
     * 构建可解释的推荐理由
     */
    private String buildReason(Long studentId, Long resourceId, double totalScore, Long experimentId) {
        StringBuilder reason = new StringBuilder();

        // 基于错误相关度生成理由
        double errorScore = calcErrorRelevance(studentId, resourceId);
        if (errorScore > 50) {
            // 找出具体错题知识点
            Set<String> weakPoints = getStudentWeakKnowledgePoints(studentId);
            if (!weakPoints.isEmpty()) {
                reason.append("因为您在")
                      .append(String.join("、", weakPoints.stream().limit(3).collect(Collectors.toList())))
                      .append("知识点有错题，");
            }
        }

        // 基于未学习程度生成理由
        double newness = calcNewness(studentId, resourceId);
        if (newness > 80) {
            reason.append("该资源您尚未学习，");
        }

        // 基于知识点匹配
        double knowledge = calcKnowledgeMatch(resourceId, experimentId);
        if (knowledge > 60) {
            reason.append("与当前实验内容高度匹配，");
        }

        if (reason.length() == 0) {
            reason.append("根据您的学习情况自动推荐");
        } else {
            reason.append("建议优先学习");
        }

        return reason.toString();
    }

    /**
     * 获取学生薄弱知识点集合
     */
    private Set<String> getStudentWeakKnowledgePoints(Long studentId) {
        Set<String> weakPoints = new LinkedHashSet<>();
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getStudentId, studentId);
        List<Long> recordIds = examRecordMapper.selectList(recordWrapper)
                .stream().map(ExamRecord::getId).collect(Collectors.toList());
        if (recordIds.isEmpty()) return weakPoints;

        LambdaQueryWrapper<ExamAnswer> answerWrapper = new LambdaQueryWrapper<>();
        answerWrapper.in(ExamAnswer::getRecordId, recordIds)
                     .eq(ExamAnswer::getIsCorrect, 0);
        List<ExamAnswer> wrongAnswers = examAnswerMapper.selectList(answerWrapper);

        for (ExamAnswer ans : wrongAnswers) {
            Question q = questionMapper.selectById(ans.getQuestionId());
            if (q != null && q.getKnowledgePoint() != null) {
                weakPoints.add(q.getKnowledgePoint());
            }
        }
        return weakPoints;
    }

    /**
     * 保存推荐记录到数据库
     */
    private void saveRecommendRecord(Long studentId, Long resourceId, Long experimentId,
                                      double totalScore, String reason) {
        RecommendRecord record = new RecommendRecord();
        record.setStudentId(studentId);
        record.setResourceId(resourceId);
        record.setExperimentId(experimentId);
        record.setTotalScore(new java.math.BigDecimal(String.format("%.2f", totalScore)));
        // score_breakdown 存储为JSON
        String breakdown = String.format(
            "{\"knowledgeMatch\":%.1f,\"errorRelevance\":%.1f,\"newness\":%.1f,\"popularity\":%.1f,\"difficultyMatch\":%.1f}",
            calcKnowledgeMatch(resourceId, experimentId),
            calcErrorRelevance(studentId, resourceId),
            calcNewness(studentId, resourceId),
            calcPopularity(resourceId),
            calcDifficultyMatch(resourceId, experimentId)
        );
        record.setScoreBreakdown(breakdown);
        record.setReason(reason);
        record.setClicked(0);
        recommendRecordMapper.insert(record);
    }

    /**
     * 获取可用资源ID列表（查询 t_resource 表）
     */
    private List<Long> getAvailableResourceIds() {
        LambdaQueryWrapper<TeachingResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachingResource::getStatus, 1)
               .select(TeachingResource::getId);
        return teachingResourceMapper.selectList(wrapper).stream()
                .map(TeachingResource::getId)
                .collect(Collectors.toList());
    }

    // ==================== 内部类 ====================

    private static class ScoreRecord {
        Long resourceId;
        double totalScore;
        String reason;
    }
}
