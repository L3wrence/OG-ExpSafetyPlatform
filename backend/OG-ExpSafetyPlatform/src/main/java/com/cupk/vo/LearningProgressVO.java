package com.cupk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class LearningProgressVO {
    private Long experimentId;
    private Integer requiredResourceCount;
    private Integer finishedResourceCount;
    private BigDecimal progress;
}
