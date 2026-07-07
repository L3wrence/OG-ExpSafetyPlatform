package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

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
    private String tagline;
    private String highlightTags;
    private String visualTheme;
    private String description;
    private String semester;
    private Integer status = 0;
    private Integer sort = 0;
    private BigDecimal credit;
    private Integer hours;
    private String assessmentMethod;
    private String learningRequirement;
    private Integer allowEmptyPublish = 0;
}
