package com.cupk.controller;

import com.cupk.common.PageResult;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.DiscussionReplyCreateDTO;
import com.cupk.dto.DiscussionTopicCreateDTO;
import com.cupk.service.DiscussionService;
import com.cupk.vo.DiscussionTopicVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/discussions")
public class DiscussionController {
    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping
    @RequirePermission("course:view")
    public Result<PageResult<DiscussionTopicVO>> page(@RequestParam(required = false) Long courseId,
                                                       @RequestParam(required = false) Long experimentId,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(discussionService.page(courseId, experimentId, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @RequirePermission("course:view")
    public Result<DiscussionTopicVO> detail(@PathVariable Long id) {
        return Result.success(discussionService.detail(id));
    }

    @PostMapping
    @RequirePermission("course:view")
    public Result<Map<String, Long>> create(@Valid @RequestBody DiscussionTopicCreateDTO dto) {
        return Result.success(Map.of("id", discussionService.create(dto)));
    }

    @PostMapping("/{id}/replies")
    @RequirePermission("course:view")
    public Result<Map<String, Long>> reply(@PathVariable Long id, @Valid @RequestBody DiscussionReplyCreateDTO dto) {
        return Result.success(Map.of("id", discussionService.reply(id, dto)));
    }

    @PutMapping("/{id}/status")
    @RequirePermission("course:update")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        discussionService.updateStatus(id, status);
        return Result.success();
    }

    @PutMapping("/{id}/featured")
    @RequirePermission("course:update")
    public Result<Void> updateFeatured(@PathVariable Long id, @RequestParam Integer featured) {
        discussionService.updateFeatured(id, featured);
        return Result.success();
    }
}
