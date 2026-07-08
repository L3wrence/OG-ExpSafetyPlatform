package com.cupk.controller;

import com.cupk.common.PageResult;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.ReviewDTO;
import com.cupk.dto.TeacherCertificationApplyDTO;
import com.cupk.service.TeacherCertificationService;
import com.cupk.vo.TeacherCertificationVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class TeacherCertificationController {
    private final TeacherCertificationService certificationService;

    public TeacherCertificationController(TeacherCertificationService certificationService) {
        this.certificationService = certificationService;
    }

    @GetMapping("/api/teacher-certifications/my")
    @RequirePermission("teacher-certification:apply")
    public Result<TeacherCertificationVO> my() {
        return Result.success(certificationService.my());
    }

    @PostMapping("/api/teacher-certifications")
    @RequirePermission("teacher-certification:apply")
    public Result<Long> apply(@Valid @RequestBody TeacherCertificationApplyDTO dto) {
        return Result.success(certificationService.apply(dto));
    }

    @GetMapping("/api/admin/teacher-certifications")
    @RequirePermission("teacher-certification:review")
    public Result<PageResult<TeacherCertificationVO>> page(@RequestParam(required = false) String status,
                                                           @RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(certificationService.page(status, pageNum, pageSize));
    }

    @PutMapping("/api/admin/teacher-certifications/{id}/approve")
    @RequirePermission("teacher-certification:review")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) ReviewDTO dto) {
        certificationService.approve(id, dto == null ? new ReviewDTO() : dto);
        return Result.success();
    }

    @PutMapping("/api/admin/teacher-certifications/{id}/reject")
    @RequirePermission("teacher-certification:review")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) ReviewDTO dto) {
        certificationService.reject(id, dto == null ? new ReviewDTO() : dto);
        return Result.success();
    }
}
