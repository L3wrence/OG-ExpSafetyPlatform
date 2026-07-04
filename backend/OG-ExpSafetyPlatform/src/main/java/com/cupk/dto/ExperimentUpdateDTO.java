package com.cupk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExperimentUpdateDTO {
    @NotNull(message = "所属课程不能为空")
    private Long courseId;
    @NotBlank(message = "实验名称不能为空")
    private String expName;
    @NotBlank(message = "实验编号不能为空")
    private String expCode;
    private String objective;
    private String principle;
    private String equipment;
    @NotBlank(message = "风险等级不能为空")
    private String riskLevel;
    @Min(value = 1, message = "实验时长必须大于0")
    private Integer durationMinutes;
    @Min(0) @Max(100)
    private Integer safetyPassScore;
    private Integer reservationEnabled;
    private Integer status;
    private Integer sort;
}
