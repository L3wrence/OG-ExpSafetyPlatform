package com.cupk.controller;

import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.ai.RecommendQueryDTO;
import com.cupk.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 推荐接口
 * 路径：/api/recommendations
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    /** 获取推荐资源（支持 GET 参数和 POST body 两种方式） */
    @RequirePermission("recommend:view")
    @GetMapping("/resources")
    public Result<?> getResources(@RequestParam(required = false) Long experimentId) {
        return Result.success(recommendService.getRecommendedResources(experimentId));
    }

    /** 获取推荐资源（POST 方式，支持更多查询条件） */
    @RequirePermission("recommend:view")
    @PostMapping("/resources")
    public Result<?> queryResources(@RequestBody RecommendQueryDTO dto) {
        return Result.success(recommendService.getRecommendedResources(
                dto != null ? dto.getExperimentId() : null));
    }
}
