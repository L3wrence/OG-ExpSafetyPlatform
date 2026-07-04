package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExamPassRateVO {
    private Long experimentId;
    private String expName;
    private Long studentCount;
    private Long passedCount;
    private BigDecimal passRate;
}
