package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResourceStatsVO {
    private Long resourceId;
    private Integer viewCount;
    private Integer downloadCount;
    private Integer favoriteCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer ratingCount;
    private BigDecimal ratingAvg;
    private Integer learnerCount;
    private Integer completedCount;
    private BigDecimal completionRate;
}
