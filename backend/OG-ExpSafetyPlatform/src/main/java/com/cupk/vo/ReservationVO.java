package com.cupk.vo;

import lombok.Data;

import java.util.Date;

/**
 * 预约展示VO
 */
@Data
public class ReservationVO {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long timeSlotId;
    private Long labId;
    private String labName;
    private String location;
    private Date date;
    private String timeRange;
    private Long experimentId;
    private String experimentName;
    private String purpose;
    private String status;
    private Long teacherId;
    private String reviewComment;
    private Date reviewTime;
    private Date createTime;
}
