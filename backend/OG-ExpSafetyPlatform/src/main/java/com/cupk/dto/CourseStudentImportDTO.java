package com.cupk.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CourseStudentImportDTO {
    private Long teachingClassId;
    private String defaultGroupName;
    @Valid
    @NotEmpty(message = "学生名单不能为空")
    private List<CourseStudentImportItemDTO> students;
}
