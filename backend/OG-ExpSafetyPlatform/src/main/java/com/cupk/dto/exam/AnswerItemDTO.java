package com.cupk.dto.exam;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 单题答案DTO
 */
@Data
public class AnswerItemDTO {
    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    private String answer; // 单选/判断："A"；多选："A,C"；简答：文本
}
