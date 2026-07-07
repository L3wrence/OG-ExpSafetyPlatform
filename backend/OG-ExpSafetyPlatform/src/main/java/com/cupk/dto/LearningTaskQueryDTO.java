package com.cupk.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class LearningTaskQueryDTO {
    private Long courseId;
    private Long experimentId;
    private String taskType;
    private Integer status;
    @Min(value = 1, message = "页码必须大于0")
    private long pageNum = 1;
    @Min(value = 1, message = "每页条数必须大于0")
    private long pageSize = 10;
}
