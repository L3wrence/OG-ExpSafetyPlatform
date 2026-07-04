package com.cupk.vo;

import lombok.Data;

@Data
public class ExperimentSimpleVO {
    private Long id;
    private String expName;
    private String expCode;
    private String riskLevel;
    private Integer durationMinutes;
    private Integer status;
}
