package com.cupk.controller;

import com.cupk.common.PageResult;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.SafetyKnowledgeCreateDTO;
import com.cupk.dto.SafetyKnowledgeQueryDTO;
import com.cupk.dto.SafetyKnowledgeUpdateDTO;
import com.cupk.pojo.SafetyKnowledge;
import com.cupk.service.SafetyKnowledgeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/safety-knowledge")
public class SafetyKnowledgeController {
    private final SafetyKnowledgeService safetyKnowledgeService;

    public SafetyKnowledgeController(SafetyKnowledgeService safetyKnowledgeService) {
        this.safetyKnowledgeService = safetyKnowledgeService;
    }

    @GetMapping
    @RequirePermission("safety:view")
    public Result<PageResult<SafetyKnowledge>> page(@Valid SafetyKnowledgeQueryDTO dto) {
        return Result.success(safetyKnowledgeService.page(dto));
    }

    @PostMapping
    @RequirePermission("safety:create")
    public Result<Long> create(@Valid @RequestBody SafetyKnowledgeCreateDTO dto) {
        return Result.success(safetyKnowledgeService.create(dto));
    }

    @PutMapping("/{id}")
    @RequirePermission("safety:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SafetyKnowledgeUpdateDTO dto) {
        safetyKnowledgeService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("safety:delete")
    public Result<Void> delete(@PathVariable Long id) {
        safetyKnowledgeService.delete(id);
        return Result.success();
    }
}
