package com.cupk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.pojo.Report;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 实验报告服务接口
 */
public interface ReportService {

    /** 创建/保存草稿 */
    Long createReport(Report report);

    /** 修改报告 */
    void updateReport(Long id, Report report);

    /** 正式提交报告 */
    void submitReport(Long id);

    Map<String, Object> uploadReportFile(Long experimentId, MultipartFile file);

    /** 我的报告列表 */
    Page<Report> getMyReports(int pageNum, int pageSize, String status);

    /** 报告详情（含最新评分） */
    Map<String, Object> getReportDetail(Long id);

    // ===== 教师端 =====

    /** 待批改报告列表 */
    Page<Report> getPendingReports(int pageNum, int pageSize, Long experimentId);

    /** 评分+评语 */
    void gradeReport(Long reportId, Integer score, String comment);

    /** 退回修改 */
    void returnReport(Long reportId, String comment);
}
