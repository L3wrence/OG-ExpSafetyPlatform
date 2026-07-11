package com.cupk.controller;

import com.cupk.common.PageResult;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.ResourceSubmissionDTO;
import com.cupk.dto.ReviewDTO;
import com.cupk.service.ResourceSubmissionService;
import com.cupk.vo.ResourcePreviewVO;
import com.cupk.vo.ResourceSubmissionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@RestController
public class ResourceSubmissionController {
    private final ResourceSubmissionService submissionService;

    public ResourceSubmissionController(ResourceSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping(value = "/api/resource-submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("resource-submission:create")
    public Result<Long> submit(@Valid @RequestPart("metadata") ResourceSubmissionDTO dto,
                               @RequestPart("file") MultipartFile file) {
        return Result.success(submissionService.submit(dto, file));
    }

    @GetMapping("/api/resource-submissions/my")
    @RequirePermission("resource-submission:create")
    public Result<PageResult<ResourceSubmissionVO>> my(@RequestParam(defaultValue = "1") Long pageNum,
                                                       @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(submissionService.my(pageNum, pageSize));
    }

    @GetMapping("/api/admin/resource-submissions")
    @RequirePermission("resource-submission:review")
    public Result<PageResult<ResourceSubmissionVO>> adminPage(@RequestParam(required = false) String status,
                                                              @RequestParam(defaultValue = "1") Long pageNum,
                                                              @RequestParam(defaultValue = "10") Long pageSize,
                                                              @RequestParam(required = false) String keyword) {
        return Result.success(submissionService.page(status, pageNum, pageSize, keyword));
    }

    @PutMapping("/api/admin/resource-submissions/{id}/approve")
    @RequirePermission("resource-submission:review")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) ReviewDTO dto) {
        submissionService.approve(id, dto == null ? new ReviewDTO() : dto);
        return Result.success();
    }

    @PutMapping("/api/admin/resource-submissions/{id}/reject")
    @RequirePermission("resource-submission:review")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) ReviewDTO dto) {
        submissionService.reject(id, dto == null ? new ReviewDTO() : dto);
        return Result.success();
    }

    @GetMapping("/api/public/resources")
    @RequirePermission("resource:view")
    public Result<PageResult<ResourceSubmissionVO>> publicResources(@RequestParam(defaultValue = "1") Long pageNum,
                                                                    @RequestParam(defaultValue = "10") Long pageSize,
                                                                    @RequestParam(required = false) String keyword) {
        return Result.success(submissionService.page("APPROVED", pageNum, pageSize, keyword));
    }

    @GetMapping("/api/public/resources/{id}/preview")
    @RequirePermission("resource:view")
    public Result<ResourcePreviewVO> preview(@PathVariable Long id) {
        return Result.success(submissionService.preview(id));
    }
}
