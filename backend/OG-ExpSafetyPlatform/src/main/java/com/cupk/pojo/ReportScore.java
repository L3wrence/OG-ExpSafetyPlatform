package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 评分记录表 t_report_score
 * 支持退回修改和多次评分记录，is_latest标记最新评分
 */
@Data
@TableName("t_report_score")
public class ReportScore {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 报告ID（引用 t_report.id） */
    private Long reportId;

    /** 评分教师ID */
    private Long teacherId;

    /** 分数（0-100） */
    private Integer score;

    /** 教师评语 */
    private String comment;

    /** 是否为最新评分（退回重评时标记历史记录） */
    private Integer isLatest;

    /** 评分时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date gradeTime;
}
