package com.cupk.vo;

import lombok.Data;

import java.util.Date;

/**
 * 考试记录展示VO
 */
@Data
public class ExamRecordVO {
    private Long id;
    private Long studentId;
    private Long paperId;
    private String paperTitle;
    private Integer totalScore;
    private Integer objectiveScore;
    private Integer subjectiveScore;
    private String status;
    private Boolean passed;
    private Date startTime;
    private Date submitTime;
}
