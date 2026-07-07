package com.cupk.vo;

import lombok.Data;

@Data
public class HseWeakPointVO {
    private String knowledgePoint;
    private String riskType;
    private Integer wrongCount;
    private Integer answerCount;
    private Integer weakWeight;
}
