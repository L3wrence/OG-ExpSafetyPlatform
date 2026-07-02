package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.ReportMapper;
import com.cupk.mapper.ReportScoreMapper;
import com.cupk.pojo.Report;
import com.cupk.pojo.ReportScore;
import com.cupk.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 实验报告服务实现
 */
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private ReportScoreMapper reportScoreMapper;

    @Override
    public Long createReport(Report report) {
        report.setStudentId(UserContext.getUserId());
        report.setStatus("DRAFT");
        report.setCreateTime(new Date());
        reportMapper.insert(report);
        return report.getId();
    }

    @Override
    public void updateReport(Long id, Report report) {
        report.setId(id);
        report.setUpdateTime(new Date());
        reportMapper.updateById(report);
    }

    @Override
    public void submitReport(Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null || !"DRAFT".equals(report.getStatus())) {
            throw new RuntimeException("报告状态不允许提交");
        }
        report.setStatus("SUBMITTED");
        Date now = new Date();
        report.setSubmitTime(report.getSubmitTime() == null ? now : report.getSubmitTime());
        report.setLatestSubmitTime(now);
        reportMapper.updateById(report);
    }

    @Override
    public Page<Report> getMyReports(int pageNum, int pageSize, String status) {
        Page<Report> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getStudentId, UserContext.getUserId())
               .eq(status != null && !status.isEmpty(), Report::getStatus, status)
               .orderByDesc(Report::getCreateTime);
        return reportMapper.selectPage(page, wrapper);
    }

    @Override
    public Map<String, Object> getReportDetail(Long id) {
        Report report = reportMapper.selectById(id);

        // 查询最新评分
        LambdaQueryWrapper<ReportScore> scoreWrapper = new LambdaQueryWrapper<>();
        scoreWrapper.eq(ReportScore::getReportId, id)
                     .eq(ReportScore::getIsLatest, 1)
                     .last("LIMIT 1");
        ReportScore latestScore = reportScoreMapper.selectOne(scoreWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("report", report);
        result.put("latestScore", latestScore);
        return result;
    }

    @Override
    public Page<Report> getPendingReports(int pageNum, int pageSize, Long experimentId) {
        Page<Report> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getStatus, "SUBMITTED")
               .eq(experimentId != null, Report::getExperimentId, experimentId)
               .orderByAsc(Report::getLatestSubmitTime);
        return reportMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public void gradeReport(Long reportId, Integer score, String comment) {
        // 将旧评分标记为非最新
        LambdaQueryWrapper<ReportScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportScore::getReportId, reportId)
               .eq(ReportScore::getIsLatest, 1);
        ReportScore oldScore = new ReportScore();
        oldScore.setIsLatest(0);
        reportScoreMapper.update(oldScore, wrapper);

        // 创建新评分
        ReportScore newScore = new ReportScore();
        newScore.setReportId(reportId);
        newScore.setTeacherId(UserContext.getUserId());
        newScore.setScore(score);
        newScore.setComment(comment);
        newScore.setIsLatest(1);
        newScore.setCreateTime(new Date());
        reportScoreMapper.insert(newScore);

        // 更新报告状态
        Report report = new Report();
        report.setId(reportId);
        report.setStatus("GRADED");
        reportMapper.updateById(report);
    }

    @Override
    @Transactional
    public void returnReport(Long reportId, String comment) {
        // 将旧评分标记为非最新
        LambdaQueryWrapper<ReportScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportScore::getReportId, reportId)
               .eq(ReportScore::getIsLatest, 1);
        ReportScore oldScore = new ReportScore();
        oldScore.setIsLatest(0);
        reportScoreMapper.update(oldScore, wrapper);

        // 创建退回记录
        ReportScore returnRecord = new ReportScore();
        returnRecord.setReportId(reportId);
        returnRecord.setTeacherId(UserContext.getUserId());
        returnRecord.setComment(comment);
        returnRecord.setIsLatest(1);
        returnRecord.setCreateTime(new Date());
        reportScoreMapper.insert(returnRecord);

        // 更新状态
        Report report = new Report();
        report.setId(reportId);
        report.setStatus("RETURNED");
        reportMapper.updateById(report);
    }
}
