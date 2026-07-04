package com.cupk.vo;

import lombok.Data;

import java.util.Date;

/**
 * 报告展示VO
 */
@Data
public class ReportVO {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long experimentId;
    private String title;
    private String content;
    private String fileUrl;
    private String status;
    private Date submitTime;
    private Date latestSubmitTime;
    private Integer latestScore;
    private String latestComment;
}
