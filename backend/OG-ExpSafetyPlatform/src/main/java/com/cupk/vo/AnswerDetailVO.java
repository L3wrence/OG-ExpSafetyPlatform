package com.cupk.vo;

import lombok.Data;

/**
 * 答题明细VO
 */
@Data
public class AnswerDetailVO {
    private Long questionId;
    private String type;
    private String content;
    private String options;
    private String studentAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private String analysis;
    private Integer score;
}
