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
    public Map<String, Object> ask(@RequestBody Map<String, Object> body) {
        // TODO
        return null;
    }

    /** 我的AI问答历史 */
    @GetMapping("/records")
    public Map<String, Object> records(@RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "10") int pageSize,
                                        @RequestParam(required = false) String scene) {
        // TODO
        return null;
    }

    /** 记录人工修改 */
    @PutMapping("/records/{id}/feedback")
    public void feedback(@PathVariable Long id, @RequestBody Map<String, String> body) {
        // TODO
    }
}
