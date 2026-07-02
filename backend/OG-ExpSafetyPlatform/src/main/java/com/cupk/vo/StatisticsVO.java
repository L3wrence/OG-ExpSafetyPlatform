package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 考试统计VO
 */
@Data
public class StatisticsVO {

    /** 考试总览 */
    @Data
    public static class Overview {
        private BigDecimal avgScore;
        private BigDecimal passRate;
        private Integer totalStudents;
        private Integer passedStudents;
        private Integer maxScore;
        private Integer minScore;
    }

    /** 分数段分布 */
    @Data
    public static class ScoreDistribution {
        private String range;  // "0-59", "60-69", ...
        private Integer count;
    }

    /** 题目正确率分析 */
    @Data
    public static class QuestionAnalysis {
        private Long questionId;
        private String content;
        private BigDecimal correctRate;
        private Integer wrongCount;
        private String type;
    }

    /** 知识点薄弱分析 */
    @Data
    public static class KnowledgeAnalysis {
        private String knowledgePoint;
        private BigDecimal wrongRate;
        private Integer questionCount;
    }
}
