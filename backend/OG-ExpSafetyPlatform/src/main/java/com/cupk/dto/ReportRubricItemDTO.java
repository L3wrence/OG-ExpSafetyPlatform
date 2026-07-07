package com.cupk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportRubricItemDTO {
    private Long id;
    @NotBlank(message = "评分项名称不能为空")
    @Size(max = 100, message = "评分项名称不能超过100个字符")
    private String itemName;
    @Size(max = 500, message = "评分说明不能超过500个字符")
    private String description;
    @NotNull(message = "评分项满分不能为空")
    @Min(value = 1, message = "评分项满分必须大于0")
    @Max(value = 100, message = "单项满分不能超过100")
    private Integer maxScore;
    private Integer orderNo;
}
