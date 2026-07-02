package com.cupk.controller;

import com.cupk.pojo.Report;
import com.cupk.service.ReportService;
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
    public Map<String, Long> create(@RequestBody Report report) {
        // TODO
        return null;
    }

    /** 修改报告 */
    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @RequestBody Report report) {
        // TODO
    }

    /** 正式提交报告 */
    @PutMapping("/{id}/submit")
    public void submit(@PathVariable Long id) {
        // TODO
    }

    /** 我的报告列表 */
    @GetMapping("/my")
    public Map<String, Object> myReports(@RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          @RequestParam(required = false) String status) {
        // TODO
        return null;
    }

    /** 报告详情 */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        // TODO
        return null;
    }

    // ===== 教师端 =====

    /** 待批改报告列表 */
    @GetMapping("/pending")
    public Map<String, Object> pending(@RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "10") int pageSize,
                                        @RequestParam(required = false) Long experimentId) {
        // TODO
        return null;
    }

    /** 评分+评语 */
    @PutMapping("/{id}/grade")
    public void grade(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        // TODO
    }

    /** 退回修改 */
    @PutMapping("/{id}/return")
    public void returnReport(@PathVariable Long id, @RequestBody Map<String, String> body) {
        // TODO
    }
}
