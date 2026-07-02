package com.cupk.service;

import java.util.List;
import java.util.Map;

/**
 * 推荐算法服务接口
 */
public interface RecommendService {

    /** 获取推荐资源（含推荐分+理由） */
    Map<String, Object> getRecommendedResources(Long experimentId);

    /** 计算推荐总分（算法核心） */
    double calculateScore(Long studentId, Long resourceId, Long experimentId);
}
