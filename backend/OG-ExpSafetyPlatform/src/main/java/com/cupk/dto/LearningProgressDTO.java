package com.cupk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LearningProgressDTO {
    @NotNull(message = "资源ID不能为空")
    private Long resourceId;
    @NotNull(message = "学习进度不能为空")
    @Min(0) @Max(100)
    private BigDecimal progress;
    @Min(0)
    private Integer durationSeconds = 0;
    private Integer finishFlag;
}
