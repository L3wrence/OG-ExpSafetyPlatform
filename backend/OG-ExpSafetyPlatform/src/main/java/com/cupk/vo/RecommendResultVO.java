package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 推荐结果VO
 */
@Data
public class RecommendResultVO {
    /** 生成时间 */
    private Date generatedAt;
    /** 推荐资源列表 */
    private List<RecommendItemVO> resources;
}
