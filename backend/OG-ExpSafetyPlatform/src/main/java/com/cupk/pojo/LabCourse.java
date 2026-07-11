package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String coverFilePath;
    private String coverOriginalFilename;
    private String coverContentType;
    private Long coverFileSize;
    @TableField(exist = false)
    private String coverUrl;
    private String tagline;     //课程短标语
    private String highlightTags; //课程亮点标签
    private String visualTheme;  //视觉主题
    private String description; //课程描述
    private String semester;    //开课学期
    private Integer status;     //课程状态
    private Integer sort;       //显示顺序
    private BigDecimal credit;  //学分
    private Integer hours;      //学时
    private String assessmentMethod;   //考核方式
    private String learningRequirement; //学习要求
    private Integer allowEmptyPublish; //是否允许空课程发布
    private LocalDateTime archiveTime; //归档时间
}

