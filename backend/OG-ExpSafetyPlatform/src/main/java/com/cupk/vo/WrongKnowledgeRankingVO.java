package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WrongKnowledgeRankingVO {
    private Long knowledgeId;
    private String knowledgePoint;
    private Long wrongCount;
    private Long answerCount;
    private BigDecimal wrongRate;
}
