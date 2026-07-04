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
    private String objective;   //实验目的
    private String principle;   //实验原理
    private String equipment;   //仪器设备
    private String riskLevel;   //风险等级
    private Integer durationMinutes;   //实验时长
    private Integer safetyPassScore;   //安全考试及格分
    private Integer reservationEnabled; //是否开放预约
    private Integer status;     //开放状态
    private Integer sort;       //排序
}
