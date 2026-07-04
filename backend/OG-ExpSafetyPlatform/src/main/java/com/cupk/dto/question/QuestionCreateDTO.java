package com.cupk.dto.question;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 题目创建/编辑请求DTO
 */
@Data
public class QuestionCreateDTO {
    @NotBlank(message = "题型不能为空")
    private String type;          // SINGLE / MULTIPLE / JUDGE / SHORT_ANSWER

    @NotBlank(message = "题目内容不能为空")
    private String content;

    private String options;       // 选项JSON，简答题为null
    private String answer;        // 正确答案
    private Integer score;        // 默认分值
    private String analysis;      // 答案解析
    private String knowledgePoint;
    private String difficulty;    // EASY / MEDIUM / HARD
    private Long courseId;
}
