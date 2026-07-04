package com.cupk.dto.ai;

import lombok.Data;

/**
 * 推荐查询DTO
 */
@Data
public class RecommendQueryDTO {
    /** 关联实验ID（可选，传了则按实验维度推荐） */
    private Long experimentId;
}
