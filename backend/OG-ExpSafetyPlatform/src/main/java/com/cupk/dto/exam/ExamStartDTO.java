package com.cupk.dto.exam;

import lombok.Data;

/**
 * 开始考试响应DTO
 */
@Data
public class ExamStartDTO {
    private Long recordId;
    private Date startTime;
    private Integer duration;
    private Date endTime;
}
