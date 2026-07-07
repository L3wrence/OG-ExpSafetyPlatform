package com.cupk.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReportRubricScoreItemDTO {
    @NotNull(message = "评分项ID不能为空")
    private Long rubricItemId;
    @NotNull(message = "评分不能为空")
    @DecimalMin(value = "0.0", message = "评分不能为负数")
    private BigDecimal score;
    @Size(max = 500, message = "单项评语不能超过500个字符")
    private String comment;
}
