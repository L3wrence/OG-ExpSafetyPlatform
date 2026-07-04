package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

//实验课程
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_lab_course")
public class LabCourse extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String courseName;  //课程名
    private String courseCode;  //课程码
    private String direction;   //专业方向
    private Long teacherId;     //负责教师ID
    private String coverUrl;    //课程封面地址
    private String description; //课程描述
    private String semester;    //开课学期
    private Integer status;     //课程状态
    private Integer sort;       //显示顺序
}

