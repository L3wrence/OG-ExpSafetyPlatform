package com.cupk.dto.exam;

import lombok.Data;

import java.util.Date;

/**
 * 试卷创建/编辑请求DTO
 */
@Data
public class PaperCreateDTO {
    private String title;
    private String description;
    private Long courseId;
    private Integer totalScore;
    private Integer passScore;
    private Integer duration;
    private Date startTime;
    private Date endTime;
}
