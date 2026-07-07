package com.cupk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExperimentCreateDTO {
    @NotNull(message = "所属课程不能为空")
    private Long courseId;
    @NotBlank(message = "实验名称不能为空")
    private String expName;
    @NotBlank(message = "实验编号不能为空")
    private String expCode;
    private String direction;
    private String coverUrl;
    private String scenarioIntro;
    private String visualTheme;
    private String description;
    private String objective;
    private String principle;
    private String equipment;
    private String materials;
    private String location;
    private String applicableClasses;
    @NotBlank(message = "风险等级不能为空")
    private String riskLevel;
    private String hazardSources;
    private String riskTypes;
    private String ppeRequirements;
    private String prerequisiteKnowledge;
    private String safetyRequirement;
    private Integer examRequired = 1;
    @Min(value = 1, message = "实验时长必须大于0")
    private Integer durationMinutes;
    @Min(0) @Max(100)
    private Integer safetyPassScore = 60;
    private String dataRecordRequirement;
    private String abnormalHandling;
    private String emergencyProcedure;
    private String reportTemplateUrl;
    private String gradingCriteria;
    private Integer reservationEnabled = 1;
    private Integer status = 0;
    private Integer sort = 0;
}
