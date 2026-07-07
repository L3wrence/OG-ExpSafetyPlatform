package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseStudentImportItemDTO {
    @NotBlank(message = "学号不能为空")
    private String username;
    private String realName;
    private String major;
    private String className;
    private String phone;
    private String groupName;
}
