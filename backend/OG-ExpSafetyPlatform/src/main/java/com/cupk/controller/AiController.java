package com.cupk.controller;

import com.cupk.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI辅助接口
 * 路径：/api/ai
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiChatService aiChatService;

    /** AI问答 */
    @PostMapping("/ask")
    public Result<?> ask(@RequestBody Map<String, Object> body) {
        String scene = (String) body.get("scene");
        String question = (String) body.get("question");
        Long experimentId = body.get("experimentId") != null
                ? Long.valueOf(body.get("experimentId").toString()) : null;
        return Result.success(aiChatService.ask(scene, question, experimentId));
    }

    /** 我的AI问答历史 */
    @GetMapping("/records")
    public Result<?> records(@RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize,
                              @RequestParam(required = false) String scene) {
        return Result.success(aiChatService.getRecords(pageNum, pageSize, scene));
    }

    /** 记录人工修改 */
    @PutMapping("/records/{id}/feedback")
    public Result<?> feedback(@PathVariable Long id, @RequestBody Map<String, String> body) {
        aiChatService.updateFeedback(id, body.get("manualRevision"));
        return Result.success();
    }
}
