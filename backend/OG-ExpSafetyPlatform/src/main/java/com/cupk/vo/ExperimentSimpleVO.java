package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExperimentSimpleVO {
    private Long id;
    private String expName;
    private String expCode;
    private String direction;
    private String coverUrl;
    private String scenarioIntro;
    private String visualTheme;
    private String riskLevel;
    private Integer durationMinutes;
    private Integer status;
    private BigDecimal learningProgress;
}
