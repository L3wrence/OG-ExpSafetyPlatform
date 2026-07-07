package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 考题表 t_question
 * 存储所有安全考题，独立于试卷存在，可被多份试卷复用
 */
@Data
@TableName("t_question")
public class Question {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 题型：SINGLE / MULTIPLE / JUDGE / SHORT_ANSWER */
    private String type;

    /** 题目内容 */
    private String content;

    /** 选项JSON，格式：[{"key":"A","label":"..."}]，简答题为NULL */
    private String options;

    /** 正确答案。单选/判断：单字符如"A"；多选："A,C"；简答：参考答案文本 */
    private String answer;

    /** 默认分值（组卷时可覆盖） */
    private Integer score;

    /** 答案解析 */
    private String analysis;

    /** 关联的安全知识点名称 */
    private String knowledgePoint;
    private Long knowledgeId;
    private Long experimentId;
    private String riskType;
    private Long relatedResourceId;

    /** 难度：EASY / MEDIUM / HARD */
    private String difficulty;

    /** 关联课程ID（引用 t_lab_course.id） */
    private Long courseId;

    /** 创建者用户ID */
    private Long createBy;

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
