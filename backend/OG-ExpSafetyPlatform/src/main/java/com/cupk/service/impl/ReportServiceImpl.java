package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.ReportMapper;
import com.cupk.mapper.ReportScoreMapper;
import com.cupk.pojo.Report;
import com.cupk.pojo.ReportScore;
import com.cupk.service.ReportService;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 瀹為獙鎶ュ憡鏈嶅姟瀹炵幇
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
        Report existing = requireReport(id);
        requireOwner(existing, "不能修改他人的报告");
        if (!"DRAFT".equals(existing.getStatus()) && !"RETURNED".equals(existing.getStatus())) {
            throw new BusinessException(400, "报告状态不允许修改");
        }
        report.setId(id);
        report.setStudentId(existing.getStudentId());
        report.setStatus("RETURNED".equals(existing.getStatus()) ? "DRAFT" : existing.getStatus());
        report.setUpdateTime(new Date());
        reportMapper.updateById(report);
    }

    @Override
    public void submitReport(Long id) {
        Report report = requireReport(id);
        requireOwner(report, "不能提交他人的报告");
        if (!"DRAFT".equals(report.getStatus()) && !"RETURNED".equals(report.getStatus())) {
            throw new BusinessException(400, "报告状态不允许提交");
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
        Report report = requireReport(id);
        if (UserContext.isStudent() && !UserContext.getUserId().equals(report.getStudentId())) {
            throw new BusinessException(403, "不能查看他人的报告");
        }

        // 鏌ヨ鏈€鏂拌瘎鍒?
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
        Report target = requireReport(reportId);
        if (!"SUBMITTED".equals(target.getStatus())) {
            throw new BusinessException(400, "只有已提交的报告可以评分");
        }
        // 灏嗘棫璇勫垎鏍囪涓洪潪鏈€鏂?
        LambdaQueryWrapper<ReportScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportScore::getReportId, reportId)
               .eq(ReportScore::getIsLatest, 1);
        ReportScore oldScore = new ReportScore();
        oldScore.setIsLatest(0);
        reportScoreMapper.update(oldScore, wrapper);

        // 鍒涘缓鏂拌瘎鍒?
        ReportScore newScore = new ReportScore();
        newScore.setReportId(reportId);
        newScore.setTeacherId(UserContext.getUserId());
        newScore.setScore(score);
        newScore.setComment(comment);
        newScore.setIsLatest(1);
        newScore.setCreateTime(new Date());
        newScore.setGradeTime(new Date());
        reportScoreMapper.insert(newScore);

        // 鏇存柊鎶ュ憡鐘舵€?
        Report report = new Report();
        report.setId(reportId);
        report.setStatus("GRADED");
        reportMapper.updateById(report);
    }

    @Override
    @Transactional
    public void returnReport(Long reportId, String comment) {
        Report target = requireReport(reportId);
        if (!"SUBMITTED".equals(target.getStatus())) {
            throw new BusinessException(400, "只有已提交的报告可以退回");
        }
        // 灏嗘棫璇勫垎鏍囪涓洪潪鏈€鏂?
        LambdaQueryWrapper<ReportScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportScore::getReportId, reportId)
               .eq(ReportScore::getIsLatest, 1);
        ReportScore oldScore = new ReportScore();
        oldScore.setIsLatest(0);
        reportScoreMapper.update(oldScore, wrapper);

        // 鍒涘缓閫€鍥炶褰?
        ReportScore returnRecord = new ReportScore();
        returnRecord.setReportId(reportId);
        returnRecord.setTeacherId(UserContext.getUserId());
        returnRecord.setScore(0);
        returnRecord.setComment(comment);
        returnRecord.setIsLatest(1);
        returnRecord.setCreateTime(new Date());
        returnRecord.setGradeTime(new Date());
        reportScoreMapper.insert(returnRecord);

        // 鏇存柊鐘舵€?
        Report report = new Report();
        report.setId(reportId);
        report.setStatus("RETURNED");
        reportMapper.updateById(report);
    }

    private Report requireReport(Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(404, "报告不存在");
        }
        return report;
    }

    private void requireOwner(Report report, String message) {
        if (!UserContext.getUserId().equals(report.getStudentId())) {
            throw new BusinessException(403, message);
        }
    }
}
