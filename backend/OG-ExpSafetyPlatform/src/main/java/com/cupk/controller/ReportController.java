package com.cupk.controller;

import com.cupk.pojo.Report;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.interceptor.UserContext;
import com.cupk.dto.report.ReportCreateDTO;
import com.cupk.dto.report.GradeDTO;
import com.cupk.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 瀹為獙鎶ュ憡鎺ュ彛
 * 璺緞锛?api/reports
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    // ===== 瀛︾敓绔?=====

    /** 鍒涘缓/淇濆瓨鑽夌 */
    @RequirePermission("report:submit")
    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody ReportCreateDTO dto) {
        Report report = new Report();
        BeanUtils.copyProperties(dto, report);
        report.setStudentId(UserContext.getUserId());
        return Result.success(Map.of("id", reportService.createReport(report)));
    }

    /** 淇敼鎶ュ憡 */
    @RequirePermission("report:submit")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody ReportCreateDTO dto) {
        Report report = new Report();
        BeanUtils.copyProperties(dto, report);
        report.setStudentId(UserContext.getUserId());
        reportService.updateReport(id, report);
        return Result.success();
    }

    /** 姝ｅ紡鎻愪氦鎶ュ憡 */
    @RequirePermission("report:submit")
    @PutMapping("/{id}/submit")
    public Result<?> submit(@PathVariable Long id) {
        reportService.submitReport(id);
        return Result.success();
    }

    /** 鎴戠殑鎶ュ憡鍒楄〃 */
    @RequirePermission("report:view")
    @GetMapping("/my")
    public Result<?> myReports(@RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) String status) {
        return Result.success(reportService.getMyReports(pageNum, pageSize, status));
    }

    /** 鎶ュ憡璇︽儏 */
    @RequirePermission("report:view")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(reportService.getReportDetail(id));
    }

    // ===== 鏁欏笀绔?=====

    /** 寰呮壒鏀规姤鍛婂垪琛?*/
    @RequirePermission("report:review")
    @GetMapping("/pending")
    public Result<?> pending(@RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize,
                              @RequestParam(required = false) Long experimentId) {
        return Result.success(reportService.getPendingReports(pageNum, pageSize, experimentId));
    }

    /** 璇勫垎+璇勮 */
    @RequirePermission("report:grade")
    @PutMapping("/{id}/grade")
    public Result<?> grade(@PathVariable Long id, @Valid @RequestBody GradeDTO dto) {
        reportService.gradeReport(id, dto.getScore(), dto.getComment());
        return Result.success();
    }

    /** 閫€鍥炰慨鏀?*/
    @RequirePermission("report:review")
    @PutMapping("/{id}/return")
    public Result<?> returnReport(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reportService.returnReport(id, body.get("comment"));
        return Result.success();
    }
}
