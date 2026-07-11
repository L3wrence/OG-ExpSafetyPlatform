package com.cupk.vo.ai;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiWrongAnswerExplainVO {
    private Long recordId;
    private Long answerId;
    private Long questionId;
    private String misconception;
    private String whyWrong;
    private String riskConsequence;
    private List<String> correctReasoning = new ArrayList<>();
    private List<String> reviewPlan = new ArrayList<>();
    private List<AiSourceVO> sources = new ArrayList<>();
    private Boolean fallback;
    private String model;
    private String disclaimer;
}
