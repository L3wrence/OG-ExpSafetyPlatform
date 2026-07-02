package com.cupk.vo;

import lombok.Data;

import java.util.Date;

/**
 * 错题VO
 */
@Data
public class WrongQuestionVO {
    private Long questionId;
    private String type;
    private String content;
    private String options;
    private String wrongAnswer;
    private String correctAnswer;
    private String analysis;
    private String knowledgePoint;
    private Date examDate;
}
