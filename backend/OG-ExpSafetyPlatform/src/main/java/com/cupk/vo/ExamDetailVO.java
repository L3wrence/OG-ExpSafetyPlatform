package com.cupk.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 考试详情VO（含每题答题）
 */
@Data
public class ExamDetailVO {
    private Long recordId;
    private String paperTitle;
    private Integer totalScore;
    private Integer passScore;
    private Boolean passed;
    private Date startTime;
    private Date submitTime;
    /** 每题答题明细 */
    private List<AnswerDetailVO> answers;
}
