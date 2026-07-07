package com.cupk.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CourseStudentRemoveDTO {
    @NotEmpty(message = "请选择要移出的学生")
    private List<Long> studentIds;
}
