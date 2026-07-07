package com.cupk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Min(0)
    private Integer lastPositionSeconds;
    @Size(max = 1000, message = "学习笔记不能超过1000个字符")
    private String note;
    private Integer finishFlag;
}
