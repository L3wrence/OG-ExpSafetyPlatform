package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 试卷表 t_exam_paper
 * 存储试卷基本信息，试卷与题目通过中间表 t_exam_paper_question 建立多对多关系
 */
@Data
@TableName("t_exam_paper")
public class ExamPaper {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 试卷标题 */
    private String title;

    /** 试卷说明/考试须知 */
    private String description;

    /** 关联课程ID（引用 t_lab_course.id） */
    private Long courseId;
    private Long experimentId;

    /** 试卷总分 */
    private Integer totalScore;
    private Integer objectiveScore;
    private Integer subjectiveScore;

    /** 及格分数线 */
    private Integer passScore;

    /** 考试时长（分钟） */
    private Integer duration;
    private Integer attemptLimit;
    private Integer showAnswerAfterSubmit;
    private Integer admissionValidityDays;
    private String multipleScorePolicy;
    private Integer randomEnabled;
    private Integer randomCount;

    /** 出卷教师ID（引用 t_user.id） */
    private Long teacherId;

    /** 状态：DRAFT / PUBLISHED / CLOSED */
    private String status;

    /** 考试开放时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date startTime;

    /** 考试截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date endTime;

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
