package com.cupk.vo;

import lombok.Data;

import java.util.Date;

/**
 * 试卷展示VO
 */
@Data
public class ExamPaperVO {
    private Long id;
    private String title;
    private String description;
    private Long courseId;
    private Integer totalScore;
    private Integer passScore;
    private Integer duration;
    private Long teacherId;
    private String status;
    private Date startTime;
    private Date endTime;
    private Date createTime;
    /** 题目数量 */
    private Integer questionCount;
}
