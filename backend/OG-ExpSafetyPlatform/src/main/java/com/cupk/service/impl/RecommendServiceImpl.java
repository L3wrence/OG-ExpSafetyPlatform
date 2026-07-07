package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.mapper.*;
import com.cupk.pojo.*;
import com.cupk.service.RecommendService;
import com.cupk.interceptor.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 鎺ㄨ崘绠楁硶鏈嶅姟瀹炵幇
 * 浜斿洜瀛愭墦鍒嗘ā鍨嬶紙闄勫綍A锛夛細
 *   鎬诲垎 = 0.35脳鐭ヨ瘑鐐瑰尮閰?+ 0.25脳閿欒鐩稿叧 + 0.20脳鏈绋嬪害 + 0.10脳鐑害 + 0.10脳闅惧害鍖归厤
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

    @Autowired
    private CourseStudentMapper courseStudentMapper;

    private static final int    TOP_N          = 10;

    @Override
    public Map<String, Object> getRecommendedResources(Long experimentId) {
        Long studentId = UserContext.getUserId();
        List<Long> courseIds = enrolledCourseIds(studentId);
        if (courseIds.isEmpty()) {
            return Map.of("resources", List.of(), "generatedAt", new Date(), "totalEvaluated", 0);
        }
        if (experimentId != null) {
            Experiment experiment = experimentMapper.selectById(experimentId);
            if (experiment == null || !courseIds.contains(experiment.getCourseId())) {
                return Map.of("resources", List.of(), "generatedAt", new Date(), "totalEvaluated", 0);
            }
        }

        Set<String> weakPoints = getStudentWeakKnowledgePoints(studentId);
        LambdaQueryWrapper<TeachingResource> resourceWrapper = new LambdaQueryWrapper<TeachingResource>()
                .eq(TeachingResource::getStatus, 1);
        if (experimentId != null) {
            resourceWrapper.and(w -> w.in(TeachingResource::getCourseId, courseIds)
                    .or()
                    .eq(TeachingResource::getExperimentId, experimentId));
        } else {
            resourceWrapper.in(TeachingResource::getCourseId, courseIds);
        }
        List<TeachingResource> candidates = teachingResourceMapper.selectList(resourceWrapper);
        List<ScoreRecord> scored = new ArrayList<>();

        for (TeachingResource resource : candidates) {
            double totalScore = priorityScore(studentId, resource, experimentId, weakPoints);
            String reason = priorityReason(studentId, resource, experimentId, weakPoints);

            ScoreRecord sr = new ScoreRecord();
            sr.resourceId = resource.getId();
            sr.totalScore = totalScore;
            sr.reason = reason;
            scored.add(sr);
        }

        List<Map<String, Object>> resources = scored.stream()
                .sorted((a, b) -> Double.compare(b.totalScore, a.totalScore))
                .limit(TOP_N)
                .map(sr -> {
                    saveRecommendRecord(studentId, sr.resourceId, experimentId, sr.totalScore, sr.reason);
                    Map<String, Object> item = new HashMap<>();
                    item.put("resourceId", sr.resourceId);
                    item.put("score", Math.round(sr.totalScore * 100.0) / 100.0);
                    item.put("reason", sr.reason);
                    TeachingResource res = teachingResourceMapper.selectById(sr.resourceId);
                    item.put("resourceName", res != null ? res.getTitle() : "资源" + sr.resourceId);
                    item.put("title", res != null ? res.getTitle() : "资源" + sr.resourceId);
                    item.put("resourceType", res != null ? res.getResourceType() : null);
                    item.put("experimentId", res != null ? res.getExperimentId() : null);
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
        TeachingResource resource = teachingResourceMapper.selectById(resourceId);
        return resource == null ? 0 : priorityScore(studentId, resource, experimentId, getStudentWeakKnowledgePoints(studentId));
    }

    private double priorityScore(Long studentId, TeachingResource resource, Long experimentId, Set<String> weakPoints) {
        double score = 0;
        if (experimentId != null && experimentId.equals(resource.getExperimentId())
                && Integer.valueOf(1).equals(resource.getRequiredFlag()) && !resourceFinished(studentId, resource.getId())) {
            score += 1000;
        }
        if (resource.getKnowledgePoint() != null && weakPoints.contains(resource.getKnowledgePoint())) {
            score += 600;
        }
        if (Integer.valueOf(1).equals(resource.getRequiredFlag()) || "REQUIRED".equals(resource.getCategory())) {
            score += 300;
        }
        if (!resourceFinished(studentId, resource.getId())) {
            score += 200;
        }
        score += Math.min(resource.getViewCount() == null ? 0 : resource.getViewCount(), 100) / 10.0;
        return score;
    }

    private String priorityReason(Long studentId, TeachingResource resource, Long experimentId, Set<String> weakPoints) {
        List<String> reasons = new ArrayList<>();
        if (experimentId != null && experimentId.equals(resource.getExperimentId())
                && Integer.valueOf(1).equals(resource.getRequiredFlag()) && !resourceFinished(studentId, resource.getId())) {
            reasons.add("当前实验仍未完成的必学资源");
        }
        if (resource.getKnowledgePoint() != null && weakPoints.contains(resource.getKnowledgePoint())) {
            reasons.add("关联你的错题知识点：" + resource.getKnowledgePoint());
        }
        if (Integer.valueOf(1).equals(resource.getRequiredFlag()) || "REQUIRED".equals(resource.getCategory())) {
            reasons.add("教师配置的必学资源");
        }
        if (!resourceFinished(studentId, resource.getId())) {
            reasons.add("尚未完成学习");
        }
        return reasons.isEmpty() ? "根据当前课程学习进度推荐。" : String.join("；", reasons) + "。";
    }

    private boolean resourceFinished(Long studentId, Long resourceId) {
        Long count = learningRecordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getStudentId, studentId)
                .eq(LearningRecord::getResourceId, resourceId)
                .eq(LearningRecord::getFinishFlag, 1));
        return count != null && count > 0;
    }

    private List<Long> enrolledCourseIds(Long studentId) {
        return courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                        .select(CourseStudent::getCourseId)
                        .eq(CourseStudent::getStudentId, studentId)
                        .eq(CourseStudent::getStatus, 1))
                .stream().map(CourseStudent::getCourseId).toList();
    }

    // ==================== 浜斿洜瀛愯绠?====================

    /**
     * 鍥犲瓙1锛氱煡璇嗙偣鍖归厤搴︼紙35%锛?
     * 璧勬簮鎵€灞炲疄楠?vs 鐩爣瀹為獙鐨勮绋嬪叧鑱斿害
     * 鏁版嵁鏉ユ簮锛歵_resource.experiment_id + t_experiment.course_id
     */
    private double calcKnowledgeMatch(Long resourceId, Long experimentId) {
        TeachingResource resource = teachingResourceMapper.selectById(resourceId);
        if (resource == null || resource.getExperimentId() == null) return 50.0;

        Experiment targetExp = experimentMapper.selectById(experimentId);
        Experiment resourceExp = experimentMapper.selectById(resource.getExperimentId());

        if (targetExp == null || resourceExp == null) return 50.0;

        // 鍚屼竴瀹為獙婊″垎锛屽悓涓€璇剧▼80鍒嗭紝涓嶅悓璇剧▼30鍒?
        if (resource.getExperimentId().equals(experimentId)) return 100.0;
        if (targetExp.getCourseId() != null && targetExp.getCourseId().equals(resourceExp.getCourseId())) return 80.0;
        return 30.0;
    }

    /**
     * 鍥犲瓙2锛氶敊璇浉鍏冲害锛?5%锛?
     * 璧勬簮鎵€灞炶绋嬭鐩栧鐢熼敊棰樿绋嬬殑绋嬪害
     * 鏁版嵁鏉ユ簮锛歵_exam_answer锛坕s_correct=0锛? t_question.course_id + t_resource.experiment_id 鈫?t_experiment.course_id
     */
    private double calcErrorRelevance(Long studentId, Long resourceId) {
        // 1. 鑾峰彇璧勬簮鎵€灞炵殑璇剧▼
        TeachingResource resource = teachingResourceMapper.selectById(resourceId);
        if (resource == null || resource.getExperimentId() == null) return 0;
        Experiment resourceExp = experimentMapper.selectById(resource.getExperimentId());
        if (resourceExp == null || resourceExp.getCourseId() == null) return 0;
        Long resourceCourseId = resourceExp.getCourseId();

        // 2. 鏌ュ嚭瀛︾敓鎵€鏈夎€冭瘯璁板綍
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getStudentId, studentId);
        List<ExamRecord> records = examRecordMapper.selectList(recordWrapper);
        if (records.isEmpty()) return 0;

        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());

        // 3. 鏌ュ嚭鎵€鏈夐敊棰?
        LambdaQueryWrapper<ExamAnswer> answerWrapper = new LambdaQueryWrapper<>();
        answerWrapper.in(ExamAnswer::getRecordId, recordIds)
                     .eq(ExamAnswer::getIsCorrect, 0);
        List<ExamAnswer> wrongAnswers = examAnswerMapper.selectList(answerWrapper);
        if (wrongAnswers.isEmpty()) return 0;

        // 4. 缁熻閿欓娑夊強鐨勮绋?
        Set<Long> wrongCourseIds = new HashSet<>();
        for (ExamAnswer ans : wrongAnswers) {
            Question q = questionMapper.selectById(ans.getQuestionId());
            if (q != null && q.getCourseId() != null) {
                wrongCourseIds.add(q.getCourseId());
            }
        }
        if (wrongCourseIds.isEmpty()) return 0;

        // 5. 濡傛灉璧勬簮鎵€灞炶绋嬪湪瀛︾敓閿欓璇剧▼涓?鈫?楂樼浉鍏?
        if (wrongCourseIds.contains(resourceCourseId)) {
            // 閿欓闆嗕腑鍦ㄨ璇剧▼鐨勭▼搴﹁秺楂橈紝鍒嗘暟瓒婇珮
            long wrongInThisCourse = wrongAnswers.stream()
                .filter(a -> {
                    Question q = questionMapper.selectById(a.getQuestionId());
                    return q != null && resourceCourseId.equals(q.getCourseId());
                })
                .count();
            return Math.min(100.0, (double) wrongInThisCourse / wrongAnswers.size() * 100.0 * 1.5);
        }
        return 10.0; // 涓嶇洿鎺ョ浉鍏充絾浠嶆湁寰急鍏宠仈
    }

    /**
     * 鍥犲瓙3锛氭湭瀛︿範绋嬪害锛?0%锛?
     * 璇ヨ祫婧愯璇ュ鐢熷杩囩殑娆℃暟锛?=鏈锛屽垎鏁版渶楂?00锛?
     * 鏁版嵁鏉ユ簮锛歵_learning_record锛堝鐢烮D + 璧勬簮ID锛?
     */
    private double calcNewness(Long studentId, Long resourceId) {
        LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningRecord::getStudentId, studentId)
               .eq(LearningRecord::getResourceId, resourceId);
        long learnCount = learningRecordMapper.selectCount(wrapper);

        // 浠庢湭瀛﹁繃寰楁弧鍒嗭紝瀛﹁繃3娆′互涓婂緱0鍒?
        if (learnCount == 0) return 100.0;
        if (learnCount >= 3) return 0;
        return (3 - learnCount) / 3.0 * 100.0;
    }

    /**
     * 鍥犲瓙4锛氳祫婧愮儹搴︼紙10%锛?
     * 璇ヨ祫婧愯鍏ㄤ綋瀛︾敓瀛︿範鐨勬€绘鏁帮紙褰掍竴鍖栧埌0-100锛?
     * 鏁版嵁鏉ユ簮锛歵_learning_record锛圕OUNT鑱氬悎锛?
     */
    private double calcPopularity(Long resourceId) {
        LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningRecord::getResourceId, resourceId);
        long totalCount = learningRecordMapper.selectCount(wrapper);

        // 鎬诲涔犺褰曟暟浣滀负鍒嗘瘝
        long maxCount = learningRecordMapper.selectCount(null);
        if (maxCount == 0) return 0;

        return (double) totalCount / maxCount * 100.0;
    }

    /**
     * 鍥犲瓙5锛氶毦搴﹀尮閰嶅害锛?0%锛?
     * 璧勬簮鎵€灞炲疄楠岄闄╃瓑绾?vs 鐩爣瀹為獙椋庨櫓绛夌骇
     * 瀹屽叏鍖归厤100鍒嗭紝宸?绾?0鍒嗭紝宸?绾т互涓?鍒?
     */
    private double calcDifficultyMatch(Long resourceId, Long experimentId) {
        // 鑾峰彇璧勬簮鎵€灞炲疄楠岀殑椋庨櫓绛夌骇
        TeachingResource resource = teachingResourceMapper.selectById(resourceId);
        String resourceDiff = null;
        if (resource != null && resource.getExperimentId() != null) {
            Experiment resourceExp = experimentMapper.selectById(resource.getExperimentId());
            resourceDiff = resourceExp != null ? resourceExp.getRiskLevel() : null;
        }

        // 鑾峰彇鐩爣瀹為獙鐨勯闄╃瓑绾?
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

    // ==================== 鎺ㄨ崘鐞嗙敱鐢熸垚 ====================

    /**
     * 鏋勫缓鍙В閲婄殑鎺ㄨ崘鐞嗙敱
     */
    private String buildReason(Long studentId, Long resourceId, double totalScore, Long experimentId) {
        StringBuilder reason = new StringBuilder();

        // 鍩轰簬閿欒鐩稿叧搴︾敓鎴愮悊鐢?
        double errorScore = calcErrorRelevance(studentId, resourceId);
        if (errorScore > 50) {
            // 鎵惧嚭鍏蜂綋閿欓鐭ヨ瘑鐐?
            Set<String> weakPoints = getStudentWeakKnowledgePoints(studentId);
            if (!weakPoints.isEmpty()) {
                reason.append("因为您在 ")
                      .append(String.join("、", weakPoints.stream().limit(3).collect(Collectors.toList())))
                      .append(" 知识点存在错题，建议优先学习。");
            }
        }

        // 鍩轰簬鏈涔犵▼搴︾敓鎴愮悊鐢?
        double newness = calcNewness(studentId, resourceId);
        if (newness > 80) {
            reason.append("该资源尚未学习，适合作为新的补充材料。");
        }

        // 鍩轰簬鐭ヨ瘑鐐瑰尮閰?
        double knowledge = calcKnowledgeMatch(resourceId, experimentId);
        if (knowledge > 60) {
            reason.append("与当前实验内容匹配度较高。");
        }

        if (reason.length() == 0) {
            reason.append("根据您的学习情况自动推荐。");
        } else {
            reason.append("建议优先学习。");
        }

        return reason.toString();
    }

    /**
     * 鑾峰彇瀛︾敓钖勫急鐭ヨ瘑鐐归泦鍚?
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
     * 淇濆瓨鎺ㄨ崘璁板綍鍒版暟鎹簱
     */
    private void saveRecommendRecord(Long studentId, Long resourceId, Long experimentId,
                                      double totalScore, String reason) {
        RecommendRecord record = new RecommendRecord();
        record.setStudentId(studentId);
        record.setResourceId(resourceId);
        record.setExperimentId(experimentId);
        record.setTotalScore(new java.math.BigDecimal(String.format("%.2f", totalScore)));
        String breakdown = String.format("{\"priorityScore\":%.1f}", totalScore);
        record.setScoreBreakdown(breakdown);
        record.setReason(reason);
        record.setClicked(0);
        recommendRecordMapper.insert(record);
    }

    /**
     * 鑾峰彇鍙敤璧勬簮ID鍒楄〃锛堟煡璇?t_resource 琛級
     */
    private List<Long> getAvailableResourceIds() {
        LambdaQueryWrapper<TeachingResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachingResource::getStatus, 1)
               .select(TeachingResource::getId);
        return teachingResourceMapper.selectList(wrapper).stream()
                .map(TeachingResource::getId)
                .collect(Collectors.toList());
    }

    // ==================== 鍐呴儴绫?====================

    private static class ScoreRecord {
        Long resourceId;
        double totalScore;
        String reason;
    }
}
