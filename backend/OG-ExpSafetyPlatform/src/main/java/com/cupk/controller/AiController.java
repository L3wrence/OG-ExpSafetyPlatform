package com.cupk.controller;

import com.cupk.common.Result;
import com.cupk.dto.ai.AiAskDTO;
import com.cupk.service.AiChatService;
import jakarta.validation.Valid;
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
    public Result<?> ask(@Valid @RequestBody AiAskDTO dto) {
        return Result.success(aiChatService.ask(dto.getScene(), dto.getQuestion(), dto.getExperimentId()));
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
