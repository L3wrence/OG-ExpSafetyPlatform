package com.cupk.vo;

import lombok.Data;

import java.util.List;

@Data
public class HsePracticeResultVO {
    private Integer totalCount;
    private Integer correctCount;
    private Integer score;
    private Integer accuracy;
    private String notice;
    private List<HseQuestionVO> details;
}
