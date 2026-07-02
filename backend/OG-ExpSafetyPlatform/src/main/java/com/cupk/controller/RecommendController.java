package com.cupk.controller;

import com.cupk.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 推荐接口
 * 路径：/api/recommendations
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    /** 获取推荐资源 */
    @GetMapping("/resources")
    public Map<String, Object> getResources(@RequestParam(required = false) Long experimentId) {
        // TODO
        return null;
    }
}
