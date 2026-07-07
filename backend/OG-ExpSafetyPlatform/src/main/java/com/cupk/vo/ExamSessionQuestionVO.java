package com.cupk.vo;

import lombok.Data;

@Data
public class ExamSessionQuestionVO {
    private Long id;
    private String type;
    private String content;
    private String options;
    private Integer score;
    private Integer orderNo;
}
