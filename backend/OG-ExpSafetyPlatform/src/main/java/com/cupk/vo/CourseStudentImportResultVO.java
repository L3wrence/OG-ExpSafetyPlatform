package com.cupk.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseStudentImportResultVO {
    private Integer successCount = 0;
    private Integer failCount = 0;
    private List<CourseStudentImportFailureVO> failures = new ArrayList<>();
}
