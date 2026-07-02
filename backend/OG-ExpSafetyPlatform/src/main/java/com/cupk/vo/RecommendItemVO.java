package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 单项推荐VO
 */
@Data
public class RecommendItemVO {
    private Long resourceId;
    private String resourceTitle;
    private BigDecimal totalScore;
    private String reason;
}
