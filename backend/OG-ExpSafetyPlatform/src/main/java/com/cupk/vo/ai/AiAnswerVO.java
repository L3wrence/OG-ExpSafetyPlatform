package com.cupk.vo.ai;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiAnswerVO {
    private Long id;
    private String scene;
    private String answer;
    private String riskLevel;
    private List<String> keyPoints = new ArrayList<>();
    private List<String> prohibitedActions = new ArrayList<>();
    private List<String> followUpQuestions = new ArrayList<>();
    private List<AiSourceVO> sources = new ArrayList<>();
    private List<String> relatedKnowledge = new ArrayList<>();
    private Integer knowledgeBaseMatchCount;
    private Boolean fallback;
    private String model;
    private String disclaimer;
}
