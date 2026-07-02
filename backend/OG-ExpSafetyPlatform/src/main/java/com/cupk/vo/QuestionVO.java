package com.cupk.vo;

import lombok.Data;

import java.util.Date;

/**
 * 题目展示VO
 */
@Data
public class QuestionVO {
    private Long id;
    private String type;
    private String content;
    private String options;
    private String answer;
    private Integer score;
    private String analysis;
    private String knowledgePoint;
    private String difficulty;
    private Long courseId;
    private Long createBy;
    private Date createTime;
    private Date updateTime;
}
