package com.cupk.vo;

import com.cupk.pojo.SafetyKnowledge;
import com.cupk.pojo.TeachingResource;
import lombok.Data;

import java.util.List;

@Data
public class HseQuestionVO {
    private Long id;
    private String type;
    private String content;
    private String options;
    private Integer score;
    private String difficulty;
    private String knowledgePoint;
    private Long knowledgeId;
    private Long experimentId;
    private String riskType;
    private String analysis;
    private String answer;
    private SafetyKnowledge knowledge;
    private List<TeachingResource> resources;
    private Boolean favorite;
    private Integer wrongCount;
    private Integer correctStreak;
    private String masteryStatus;
    private Boolean correct;
    private String correctAnswer;
    private String studentAnswer;
}
