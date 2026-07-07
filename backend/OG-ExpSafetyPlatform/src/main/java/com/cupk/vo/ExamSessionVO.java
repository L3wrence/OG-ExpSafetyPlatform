package com.cupk.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class ExamSessionVO {
    private Long recordId;
    private Long paperId;
    private String paperTitle;
    private List<ExamSessionQuestionVO> questions;
    private Map<Long, String> answers;
    private Date startTime;
    private Date endTime;
    private Long remainingSeconds;
    private Boolean resumed;
}
