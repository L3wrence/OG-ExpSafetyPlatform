package com.cupk.vo;

import lombok.Data;

@Data
public class ResourceTimelineStatsVO {
    private Long resourceId;
    private String resourceTitle;
    private Long experimentId;
    private Integer noteCount;
    private Integer questionCount;
    private Integer riskCount;
    private String latestQuestion;
}
