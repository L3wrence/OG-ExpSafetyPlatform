package com.cupk.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExperimentStepDTO {
    @NotNull(message = "步骤序号不能为空")
    @Min(value = 1, message = "步骤序号最小为1")
    private Integer stepNo;
    @NotBlank(message = "步骤标题不能为空")
    private String title;
    @NotBlank(message = "步骤内容不能为空")
    private String content;
    private String safetyTip;
    private String mediaType;
    private String flowchartData;
    private Integer requiredFlag = 1;
    private Integer estimatedMinutes = 0;
}
