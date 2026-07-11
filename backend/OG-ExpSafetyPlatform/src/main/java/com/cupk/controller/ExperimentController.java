package com.cupk.controller;

import com.cupk.common.PageResult;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.ExperimentCreateDTO;
import com.cupk.dto.ExperimentQueryDTO;
import com.cupk.dto.ExperimentStepDTO;
import com.cupk.dto.ExperimentUpdateDTO;
import com.cupk.pojo.Experiment;
import com.cupk.service.ExperimentService;
import com.cupk.vo.ExperimentDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.http.MediaType;
import java.util.HashMap;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {
    private final ExperimentService experimentService;

    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @GetMapping
    @RequirePermission("experiment:view")
    public Result<PageResult<Experiment>> page(@Valid ExperimentQueryDTO dto) {
        return Result.success(experimentService.page(dto));
    }

    @GetMapping("/{id}")
    @RequirePermission("experiment:view")
    public Result<ExperimentDetailVO> detail(@PathVariable Long id) {
        return Result.success(experimentService.detail(id));
    }

    @GetMapping("/{id}/overview")
    @RequirePermission("experiment:view")
    public Result<ExperimentDetailVO> overview(@PathVariable Long id) {
        return Result.success(experimentService.detail(id));
    }

    @PostMapping
    @RequirePermission("experiment:create")
    public Result<Long> create(@Valid @RequestBody ExperimentCreateDTO dto) {
        return Result.success(experimentService.create(dto));
    }

    @PutMapping("/{id}")
    @RequirePermission("experiment:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ExperimentUpdateDTO dto) {
        experimentService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("experiment:delete")
    public Result<Void> delete(@PathVariable Long id) {
        experimentService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequirePermission("experiment:update")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        experimentService.changeStatus(id, status);
        return Result.success();
    }

    @PostMapping(value = "/{id}/steps", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("experiment:update")
    public Result<Void> saveSteps(@PathVariable Long id, @Valid @RequestPart("metadata") List<ExperimentStepDTO> steps,
                                  MultipartHttpServletRequest request) {
        Map<Integer, MultipartFile> files = new HashMap<>();
        request.getFileMap().forEach((name, file) -> {
            if (name.startsWith("file_")) files.put(Integer.parseInt(name.substring(5)), file);
        });
        experimentService.saveSteps(id, steps, files);
        return Result.success();
    }
}
