package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseCompletionVO {
    private Long courseId;
    private String courseName;
    private Long studentCount;
    private Long requiredResourceCount;
    private Long finishedCount;
    private BigDecimal completionRate;
}
