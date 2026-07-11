package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

//实验项目
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_experiment")
public class Experiment extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;      //实验所属课程ID
    private String expName;     //实验名称
    private String expCode;     //实验编号
    private String direction;   //专业方向
    private String coverUrl;    //实验封面地址
    private String scenarioIntro; //实验情境导入
    private String visualTheme; //视觉主题
    private String description; //实验简介
    private String objective;   //实验目的
    private String principle;   //实验原理
    private String equipment;   //仪器设备
    private String materials;   //材料
    private String location;    //实验地点
    private String applicableClasses; //适用班级
    private String riskLevel;   //风险等级
    private String hazardSources; //危险源
    private String riskTypes;   //风险类型
    private String ppeRequirements; //PPE要求
    private String prerequisiteKnowledge; //前置知识
    private String safetyRequirement; //安全要求
    private Integer examRequired; //是否要求准入考试
    private Long admissionPaperId; //预约准入绑定试卷ID
    private Integer durationMinutes;   //实验时长
    private Integer safetyPassScore;   //安全考试及格分
    private String dataRecordRequirement; //数据记录要求
    private String abnormalHandling; //异常处理方法
    private String emergencyProcedure; //应急处置流程
    private String reportTemplateUrl; //报告模板
    private String gradingCriteria; //评分标准
    private Integer reservationEnabled; //是否开放预约
    private Integer status;     //开放状态
    private Integer sort;       //排序
}
