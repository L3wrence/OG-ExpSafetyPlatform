package com.cupk.service.impl;

import com.cupk.mapper.RecommendRecordMapper;
import com.cupk.service.RecommendService;
import com.cupk.common.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import com.cupk.common.UserContext;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 推荐算法服务实现
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private RecommendRecordMapper recommendRecordMapper;

    @Override
    public Map<String, Object> getRecommendedResources(Long experimentId) {
        Long studentId = UserContext.getUserId();
        List<Map<String, Object>> resources = new ArrayList<>();

        // TODO: 调用成员B的资源数据，计算推荐分
        // 按总分降序排列，取Top 10

        Map<String, Object> result = new HashMap<>();
        result.put("resources", resources);
        result.put("generatedAt", new Date());
        return result;
    }

    @Override
    public double calculateScore(Long studentId, Long resourceId, Long experimentId) {
        // 推荐总分 = 0.35 × 知识点匹配 + 0.25 × 错误相关 + 0.20 × 未学程度 + 0.10 × 热度 + 0.10 × 难度匹配
        // TODO: 调用成员B的数据计算各因子
        double knowledgeMatch = 0.35 * 0;   // TODO
        double errorRelevance = 0.25 * 0;   // TODO
        double newness = 0.20 * 0;          // TODO
        double popularity = 0.10 * 0;       // TODO
        double difficultyMatch = 0.10 * 0;  // TODO

        return knowledgeMatch + errorRelevance + newness + popularity + difficultyMatch;
    }

    /** 构建推荐理由 */
    private String buildReason(Long studentId, Long resourceId, double totalScore) {
        // TODO: 根据错题分布生成可解释的推荐理由
        return "根据您的学习情况自动推荐";
    }
}
