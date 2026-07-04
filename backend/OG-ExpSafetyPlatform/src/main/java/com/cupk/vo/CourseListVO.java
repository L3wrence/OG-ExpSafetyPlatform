package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;

//课程表
@Data
public class CourseListVO {
    private Long id;
    private String courseName;      //课程名称
    private String courseCode;      //课程代码
    private String direction;       //专业方向
    private Long teacherId;         //负责教师ID
    private String teacherName;     //负责教师姓名
    private String semester;        //开课学期
    private Integer status;         //课程状态
    private Integer experimentCount;    //项目数量
    private Integer resourceCount;      //资源数量
    private BigDecimal averageProgress; //平均进度
}
