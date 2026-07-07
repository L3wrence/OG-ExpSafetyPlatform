package com.cupk.vo;

import com.cupk.pojo.LabCourse;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

//课程明细视图
@Data
public class CourseDetailVO {
    private LabCourse course;       //实验课程
    private String teacherName;     //负责教师姓名
    private Long experimentCount;   //项目数量
    private Long resourceCount;     //资源数量
    private BigDecimal averageProgress;     //平均进度
    private List<ExperimentSimpleVO> experiments;   //项目简介
    private String learningRequirement;     //学习要求
    private Integer studentCount;           //学生数量
    private Integer teachingClassCount;     //教学班数量
    private List<TeachingClassVO> teachingClasses;
    private List<CourseStudentVO> students;
    private List<String> announcements;
}
