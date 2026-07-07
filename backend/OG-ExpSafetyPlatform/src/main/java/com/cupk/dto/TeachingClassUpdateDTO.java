package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeachingClassUpdateDTO {
    @NotBlank(message = "教学班名称不能为空")
    private String className;
    @NotNull(message = "任课教师不能为空")
    private Long teacherId;
    private Long assistantId;
    private String adminClass;
    private String semester;
    private Integer status;
}
