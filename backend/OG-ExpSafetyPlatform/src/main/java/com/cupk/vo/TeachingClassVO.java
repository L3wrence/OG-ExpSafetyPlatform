package com.cupk.vo;

import lombok.Data;

@Data
public class TeachingClassVO {
    private Long id;
    private Long courseId;
    private String className;
    private Long teacherId;
    private String teacherName;
    private Long assistantId;
    private String assistantName;
    private String adminClass;
    private String semester;
    private Integer status;
    private Integer studentCount;
}
