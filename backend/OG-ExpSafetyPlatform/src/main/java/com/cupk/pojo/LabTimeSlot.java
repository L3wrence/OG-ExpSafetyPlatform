package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 实验时间段表 t_lab_time_slot
 * 管理实验室的可用时间段
 */
@Data
@TableName("t_lab_time_slot")
public class LabTimeSlot {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 实验室ID（引用 t_lab.id） */
    private Long labId;
    private Long experimentId;

    /** 日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date date;

    /** 开始时间（如 08:00） */
    @JsonFormat(pattern = "HH:mm", timezone = "Asia/Shanghai")
    private Date startTime;

    /** 结束时间（如 10:00） */
    @JsonFormat(pattern = "HH:mm", timezone = "Asia/Shanghai")
    private Date endTime;

    /** 该时段容量上限 */
    private Integer capacity;

    /** 已预约人数 */
    private Integer bookedCount;

    /** 状态：AVAILABLE / FULL / CLOSED */
    private String status;

    /** 创建者ID */
    private Long createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date updateTime;
}
