package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearningTaskUpdateDTO {
    @NotNull(message = "实验ID不能为空")
    private Long experimentId;
    @NotBlank(message = "任务名称不能为空")
    private String taskName;
    @NotBlank(message = "任务类型不能为空")
    private String taskType;
    private Long targetResourceId;
    private Long targetKnowledgeId;
    private Long targetPaperId;
    private Long prerequisiteTaskId;
    private Integer requiredFlag = 1;
    private Integer sort = 0;
    private LocalDateTime openTime;
    private LocalDateTime deadline;
    private String completionRule = "AUTO";
    private Integer status = 1;
}
