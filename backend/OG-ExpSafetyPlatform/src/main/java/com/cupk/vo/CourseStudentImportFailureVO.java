package com.cupk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CourseStudentImportFailureVO {
    private Integer rowIndex;
    private String username;
    private String reason;
}
