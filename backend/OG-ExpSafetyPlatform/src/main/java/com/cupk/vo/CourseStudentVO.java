package com.cupk.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseStudentVO {
    private Long id;
    private Long courseId;
    private Long teachingClassId;
    private String teachingClassName;
    private Long studentId;
    private String username;
    private String realName;
    private String major;
    private String className;
    private String phone;
    private String groupName;
    private String semester;
    private Integer status;
    private LocalDateTime joinTime;
}
