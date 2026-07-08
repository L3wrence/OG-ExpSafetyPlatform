package com.cupk.vo;

import lombok.Data;

@Data
public class CourseJoinVO {
    private Long courseId;
    private Long teachingClassId;
    private String courseName;
    private String className;
}
