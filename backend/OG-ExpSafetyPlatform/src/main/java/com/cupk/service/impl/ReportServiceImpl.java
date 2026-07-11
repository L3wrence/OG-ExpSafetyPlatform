package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.ReportMapper;
import com.cupk.mapper.ReportScoreMapper;
import com.cupk.mapper.ReservationMapper;
import com.cupk.pojo.CourseStudent;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.Report;
import com.cupk.pojo.ReportScore;
import com.cupk.pojo.Reservation;
import com.cupk.service.PortalMessageService;
import com.cupk.service.ReportService;
import com.cupk.service.ReportRubricService;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 瀹為獙鎶ュ憡鏈嶅姟瀹炵幇
 */
@Service
public class ReportServiceImpl implements ReportService {
    private static final long MAX_REPORT_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> REPORT_FILE_EXTENSIONS = Set.of("pdf", "doc", "docx", "xls", "xlsx");

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private ReportScoreMapper reportScoreMapper;

    @Autowired
    private ExperimentMapper experimentMapper;

    @Autowired
    private LabCourseMapper labCourseMapper;

    @Autowired
    private CourseStudentMapper courseStudentMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private ReportRubricService reportRubricService;

    @Autowired
    private PortalMessageService portalMessageService;

    @Override
    @Transactional
    public Long createReport(Report report) {
        report.setStudentId(UserContext.getUserId());
        assertCanCreate(report);
        report.setStatus("DRAFT");
        report.setCreateTime(new Date());
        reportMapper.insert(report);
        return report.getId();
    }

    @Override
    @Transactional
    public void updateReport(Long id, Report report) {
        Report existing = requireReport(id);
        requireOwner(existing, "不能修改他人的报告");
        if (!"DRAFT".equals(existing.getStatus()) && !"RETURNED".equals(existing.getStatus())) {
            throw new BusinessException(400, "报告状态不允许修改");
        }
        if (report.getExperimentId() != null && !report.getExperimentId().equals(existing.getExperimentId())) {
            throw new BusinessException(400, "报告所属实验不允许修改");
        }
        if (report.getTitle() == null || report.getTitle().trim().isEmpty()) {
            throw new BusinessException(400, "报告标题不能为空");
        }
        assertExperimentWritableByStudent(existing.getStudentId(), existing.getExperimentId());
        report.setId(id);
        report.setStudentId(existing.getStudentId());
        report.setExperimentId(existing.getExperimentId());
        report.setStatus(existing.getStatus());
        report.setUpdateTime(new Date());
        reportMapper.updateById(report);
    }

    @Override
    @Transactional
    public void submitReport(Long id) {
        Report report = requireReport(id);
        requireOwner(report, "不能提交他人的报告");
        if ("SUBMITTED".equals(report.getStatus())) {
            return;
        }
        if (!"DRAFT".equals(report.getStatus()) && !"RETURNED".equals(report.getStatus())) {
            throw new BusinessException(400, "报告状态不允许提交");
        }
        assertExperimentWritableByStudent(report.getStudentId(), report.getExperimentId());
        if (report.getContent() == null || report.getContent().trim().isEmpty()) {
            throw new BusinessException(400, "报告正文不能为空");
        }
        report.setStatus("SUBMITTED");
        Date now = new Date();
        report.setSubmitTime(report.getSubmitTime() == null ? now : report.getSubmitTime());
        report.setLatestSubmitTime(now);
        reportMapper.updateById(report);
    }

    @Override
    public Map<String, Object> uploadReportFile(Long experimentId, MultipartFile file) {
        if (experimentId == null) {
            throw new BusinessException(400, "请先选择实验");
        }
        assertExperimentWritableByStudent(UserContext.getUserId(), experimentId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        if (file.getSize() > MAX_REPORT_FILE_SIZE) {
            throw new BusinessException(400, "报告文件大小不能超过20MB");
        }
        String originalFilename = Path.of(file.getOriginalFilename() == null ? "report" : file.getOriginalFilename())
                .getFileName().toString().replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
        int dot = originalFilename.lastIndexOf('.');
        String extension = dot < 0 ? "" : originalFilename.substring(dot + 1).toLowerCase();
        if (!REPORT_FILE_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "仅支持 PDF、Word 或 Excel 报告文件");
        }
        String storedName = UUID.randomUUID() + "-" + originalFilename;
        Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "reports", UserContext.getUserId().toString())
                .toAbsolutePath().normalize();
        Path target = uploadDir.resolve(storedName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new BusinessException(400, "非法文件路径");
        }
        try {
            Files.createDirectories(uploadDir);
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(500, "报告文件保存失败");
        }
        return Map.of(
                "fileUrl", "/uploads/reports/" + UserContext.getUserId() + "/" + storedName,
                "originalFilename", originalFilename,
                "fileSize", file.getSize());
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
        assertReadable(report);

        // 鏌ヨ鏈€鏂拌瘎鍒?
        LambdaQueryWrapper<ReportScore> scoreWrapper = new LambdaQueryWrapper<>();
        scoreWrapper.eq(ReportScore::getReportId, id)
                     .eq(ReportScore::getIsLatest, 1)
                     .last("LIMIT 1");
        ReportScore latestScore = reportScoreMapper.selectOne(scoreWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("report", report);
        result.put("latestScore", latestScore);
        result.put("template", reportRubricService.template(report.getExperimentId()));
        result.put("rubric", reportRubricService.rubric(report.getExperimentId()));
        result.put("scoreItems", latestScore == null ? List.of() : reportRubricService.scoreItems(latestScore.getId()).get("items"));
        return result;
    }

    @Override
    public Page<Report> getPendingReports(int pageNum, int pageSize, Long experimentId) {
        Page<Report> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getStatus, "SUBMITTED")
               .eq(experimentId != null, Report::getExperimentId, experimentId)
               .orderByAsc(Report::getLatestSubmitTime);
        if (UserContext.isTeacher()) {
            List<Long> experimentIds = teacherExperimentIds();
            if (experimentIds.isEmpty()) {
                page.setRecords(new ArrayList<>());
                page.setTotal(0);
                return page;
            }
            wrapper.in(Report::getExperimentId, experimentIds);
        }
        return reportMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public void gradeReport(Long reportId, Integer score, String comment) {
        Report target = requireReport(reportId);
        assertWritable(target);
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
        portalMessageService.send(target.getStudentId(), "报告评分完成", target.getTitle(),
                "REPORT_GRADED", target.getId(), "/student/grades");
    }

    @Override
    @Transactional
    public void returnReport(Long reportId, String comment) {
        Report target = requireReport(reportId);
        assertWritable(target);
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
        portalMessageService.send(target.getStudentId(), "报告被退回", target.getTitle(),
                "REPORT_RETURNED", target.getId(), "/student/grades");
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

    private void assertReadable(Report report) {
        if (UserContext.isTeacher()) {
            assertTeacherOwnsExperiment(report.getExperimentId());
            return;
        }
        if (UserContext.isLearner()) {
            requireOwner(report, "不能查看他人的报告");
            return;
        }
    }

    private void assertWritable(Report report) {
        if (UserContext.isTeacher()) {
            assertTeacherOwnsExperiment(report.getExperimentId());
            return;
        }
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "无权批改或退回报告");
        }
    }

    private void assertCanCreate(Report report) {
        if (!UserContext.isLearner()) {
            throw new BusinessException(403, "只有课堂成员可以创建实验报告");
        }
        if (report.getTitle() == null || report.getTitle().trim().isEmpty()) {
            throw new BusinessException(400, "报告标题不能为空");
        }
        assertExperimentWritableByStudent(report.getStudentId(), report.getExperimentId());
        Long duplicated = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getStudentId, report.getStudentId())
                .eq(Report::getExperimentId, report.getExperimentId())
                .in(Report::getStatus, "DRAFT", "SUBMITTED", "RETURNED", "GRADED"));
        if (duplicated != null && duplicated > 0) {
            throw new BusinessException(409, "同一实验只能创建一份有效报告");
        }
    }

    private void assertExperimentWritableByStudent(Long studentId, Long experimentId) {
        Experiment experiment = experimentMapper.selectById(experimentId);
        if (experiment == null) {
            throw new BusinessException(404, "实验项目不存在");
        }
        LabCourse course = labCourseMapper.selectById(experiment.getCourseId());
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (course.getStatus() != null && course.getStatus() == 2) {
            throw new BusinessException(409, "课程已归档，不能创建或提交报告");
        }
        if (experiment.getStatus() == null || experiment.getStatus() != 1) {
            throw new BusinessException(409, "实验未开放，不能创建或提交报告");
        }
        Long enrolled = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, experiment.getCourseId())
                .eq(CourseStudent::getStudentId, studentId)
                .eq(CourseStudent::getStatus, 1));
        if (enrolled == null || enrolled == 0) {
            throw new BusinessException(403, "学生未加入该实验所属课程");
        }
        if (Integer.valueOf(1).equals(experiment.getReservationEnabled())) {
            Long approved = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                    .eq(Reservation::getStudentId, studentId)
                    .eq(Reservation::getExperimentId, experimentId)
                    .in(Reservation::getStatus, "APPROVED", "COMPLETED", "CHECKED_IN"));
            if (approved == null || approved == 0) {
                throw new BusinessException(403, "提交该实验报告前需要存在已通过或已完成的预约");
            }
        }
    }

    private void assertTeacherOwnsExperiment(Long experimentId) {
        Experiment experiment = experimentMapper.selectById(experimentId);
        if (experiment == null) {
            throw new BusinessException(404, "实验项目不存在");
        }
        LabCourse course = labCourseMapper.selectById(experiment.getCourseId());
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!UserContext.getUserId().equals(course.getTeacherId())) {
            throw new BusinessException(403, "不能处理非本人负责课程的报告");
        }
    }

    private List<Long> teacherExperimentIds() {
        List<Long> courseIds = labCourseMapper.selectList(new LambdaQueryWrapper<LabCourse>()
                        .select(LabCourse::getId)
                        .eq(LabCourse::getTeacherId, UserContext.getUserId()))
                .stream()
                .map(LabCourse::getId)
                .toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return experimentMapper.selectList(new LambdaQueryWrapper<Experiment>()
                        .select(Experiment::getId)
                        .in(Experiment::getCourseId, courseIds))
                .stream()
                .map(Experiment::getId)
                .toList();
    }
}
