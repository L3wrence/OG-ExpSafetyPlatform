package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResourceCompletionVO {
    private Long resourceId;
    private String title;
    private Long studentCount;
    private Long finishedStudentCount;
    private BigDecimal completionRate;
}
