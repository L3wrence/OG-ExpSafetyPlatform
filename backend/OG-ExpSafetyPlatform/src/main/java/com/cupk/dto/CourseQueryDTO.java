package com.cupk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CourseQueryDTO {
    @Min(value = 1, message = "pageNum最小为1")
    private Long pageNum = 1L;
    @Min(value = 1, message = "pageSize最小为1")
    @Max(value = 100, message = "pageSize最大为100")
    private Long pageSize = 10L;
    private String keyword;     //关键字（课程名称、课程编号）
    private String direction;
    private Long teacherId;
    private String semester;
    private Integer status;
}
