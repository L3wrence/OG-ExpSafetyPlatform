package com.cupk.dto.report;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评分请求DTO
 */
@Data
public class GradeDTO {
    @NotNull(message = "分数不能为空")
    @Min(value = 0, message = "分数不能小于0")
    @Max(value = 100, message = "分数不能大于100")
    private Integer score;

    private String comment;
}
