package com.cupk.controller;

import com.cupk.pojo.Report;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.common.UserContext;
import com.cupk.dto.report.ReportCreateDTO;
import com.cupk.dto.report.GradeDTO;
import com.cupk.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 实验报告接口
 * 路径：/api/reports
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    // ===== 学生端 =====

    /** 创建/保存草稿 */
    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody ReportCreateDTO dto) {
        Report report = new Report();
        BeanUtils.copyProperties(dto, report);
        report.setStudentId(UserContext.getUserId());
        return Result.success(Map.of("id", reportService.createReport(report)));
    }

    /** 修改报告 */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody ReportCreateDTO dto) {
        Report report = new Report();
        BeanUtils.copyProperties(dto, report);
        report.setStudentId(UserContext.getUserId());
        reportService.updateReport(id, report);
        return Result.success();
    }

    /** 正式提交报告 */
    @PutMapping("/{id}/submit")
    public Result<?> submit(@PathVariable Long id) {
        reportService.submitReport(id);
        return Result.success();
    }

    /** 我的报告列表 */
    @GetMapping("/my")
    public Result<?> myReports(@RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) String status) {
        return Result.success(reportService.getMyReports(pageNum, pageSize, status));
    }

    /** 报告详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(reportService.getReportDetail(id));
    }

    // ===== 教师端 =====

    /** 待批改报告列表 */
    @RequirePermission("report:review")
    @GetMapping("/pending")
    public Result<?> pending(@RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize,
                              @RequestParam(required = false) Long experimentId) {
        return Result.success(reportService.getPendingReports(pageNum, pageSize, experimentId));
    }

    /** 评分+评语 */
    @RequirePermission("report:review")
    @PutMapping("/{id}/grade")
    public Result<?> grade(@PathVariable Long id, @Valid @RequestBody GradeDTO dto) {
        reportService.gradeReport(id, dto.getScore(), dto.getComment());
        return Result.success();
    }

    /** 退回修改 */
    @RequirePermission("report:review")
    @PutMapping("/{id}/return")
    public Result<?> returnReport(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reportService.returnReport(id, body.get("comment"));
        return Result.success();
    }
}
