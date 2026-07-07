package com.cupk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class HsePracticeQueryDTO {
    private String mode = "RANDOM";
    private Long knowledgeId;
    private String knowledgePoint;
    private Long experimentId;
    private String riskType;
    private String difficulty;
    @Min(1)
    @Max(100)
    private Integer count = 10;
}
