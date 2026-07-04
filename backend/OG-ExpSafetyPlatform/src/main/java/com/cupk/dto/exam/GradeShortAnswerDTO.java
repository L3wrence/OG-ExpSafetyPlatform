package com.cupk.dto.exam;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 教师批改简答题请求DTO
 */
@Data
public class GradeShortAnswerDTO {
    @NotNull(message = "答案ID不能为空")
    private Long answerId;    // t_exam_answer.id

    @NotNull(message = "得分不能为空")
    private Integer score;    // 该题得分

    private String comment;   // 批注（可选）
}
