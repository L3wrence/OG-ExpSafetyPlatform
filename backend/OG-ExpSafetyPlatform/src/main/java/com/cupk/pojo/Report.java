package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 实验报告表 t_report
 * 支持退回修改和多次评分记录
 */
@Data
@TableName("t_report")
public class Report {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 学生ID */
    private Long studentId;

    /** 实验项目ID（引用 t_experiment.id） */
    private Long experimentId;

    /** 报告标题 */
    private String title;

    /** 报告正文 */
    private String content;

    /** 附件文件路径 */
    private String fileUrl;

    /** 状态：DRAFT / SUBMITTED / GRADED / RETURNED */
    private String status;

    /** 首次提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date submitTime;

    /** 最近一次提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date latestSubmitTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;
}
