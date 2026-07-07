package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LearningPathVO {
    private Long experimentId;
    private BigDecimal progress;
    private Integer totalCount;
    private Integer completedCount;
    private LearningTaskVO nextTask;
    private List<LearningTaskVO> tasks;
    private List<LearningTaskVO> reminders;
}
