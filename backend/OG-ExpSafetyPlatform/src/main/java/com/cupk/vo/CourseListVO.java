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
    private String coverUrl;        //课程封面
    private String tagline;         //课程短标语
    private String highlightTags;   //课程亮点标签
    private String visualTheme;     //视觉主题
    private String semester;        //开课学期
    private Integer status;         //课程状态
    private Integer experimentCount;    //项目数量
    private Integer resourceCount;      //资源数量
    private BigDecimal averageProgress; //平均进度
    private BigDecimal credit;          //学分
    private Integer hours;              //学时
    private String assessmentMethod;    //考核方式
    private Integer teachingClassCount; //教学班数量
    private Integer studentCount;       //学生数量
}
