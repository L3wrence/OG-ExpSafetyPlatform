package com.cupk.dto.question;

import lombok.Data;

/**
 * 题目分页查询DTO
 */
@Data
public class QuestionQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String type;
    private String difficulty;
    private String keyword;
    private Long courseId;
}
