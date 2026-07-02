package com.cupk.dto.exam;

import lombok.Data;

/**
 * 单题答案DTO
 */
@Data
public class AnswerItemDTO {
    /** 题目ID */
    private Long questionId;

    /** 学生答案。单选/判断："A"；多选："A,C"；简答：文本 */
    private String answer;
}
