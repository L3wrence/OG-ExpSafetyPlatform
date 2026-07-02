package com.cupk.dto.report;

import lombok.Data;

/**
 * 评分请求DTO
 */
@Data
public class GradeDTO {
    /** 分数 0-100 */
    private Integer score;
    /** 评语 */
    private String comment;
}
