package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 预约记录表 t_reservation
 * 记录学生的实验预约信息
 */
@Data
@TableName("t_reservation")
public class Reservation {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 学生ID（引用 t_user.id） */
    private Long studentId;

    /** 时间段ID（引用 t_lab_time_slot.id） */
    private Long timeSlotId;

    /** 实验室ID（冗余，便于查询） */
    private Long labId;

    /** 实验项目ID（引用 t_experiment.id） */
    private Long experimentId;

    /** 实验目的/内容说明 */
    private String purpose;

    /** 状态：PENDING / APPROVED / REJECTED / CANCELLED */
    private String status;

    /** 审核教师ID */
    private Long teacherId;

    /** 审核意见 */
    private String reviewComment;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date reviewTime;

    /** 申请时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date updateTime;
}
