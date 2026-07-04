package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseCreateDTO {
    @NotBlank(message = "课程名称不能为空")
    private String courseName;
    @NotBlank(message = "课程编号不能为空")
    private String courseCode;
    private String direction;
    @NotNull(message = "负责教师不能为空")
    private Long teacherId;
    private String coverUrl;
    private String description;
    private String semester;
    private Integer status = 1;
    private Integer sort = 0;
}
