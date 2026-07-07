package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cupk.exception.BusinessException;
import com.cupk.mapper.ExperimentAdmissionMapper;
import com.cupk.mapper.ExamRecordMapper;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.LearningTaskMapper;
import com.cupk.mapper.LearningTaskRecordMapper;
import com.cupk.pojo.CourseStudent;
import com.cupk.pojo.ExamPaper;
import com.cupk.pojo.ExamRecord;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.ExperimentAdmission;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.LearningTask;
import com.cupk.pojo.LearningTaskRecord;
import com.cupk.service.AdmissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdmissionServiceImpl implements AdmissionService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ExperimentAdmissionMapper admissionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final ExperimentMapper experimentMapper;
    private final LabCourseMapper courseMapper;
    private final LearningTaskMapper learningTaskMapper;
    private final LearningTaskRecordMapper taskRecordMapper;

    public AdmissionServiceImpl(ExperimentAdmissionMapper admissionMapper,
                                ExamRecordMapper examRecordMapper,
                                CourseStudentMapper courseStudentMapper,
                                ExperimentMapper experimentMapper,
                                LabCourseMapper courseMapper,
                                LearningTaskMapper learningTaskMapper,
                                LearningTaskRecordMapper taskRecordMapper) {
        this.admissionMapper = admissionMapper;
        this.examRecordMapper = examRecordMapper;
        this.courseStudentMapper = courseStudentMapper;
        this.experimentMapper = experimentMapper;
        this.courseMapper = courseMapper;
        this.learningTaskMapper = learningTaskMapper;
        this.taskRecordMapper = taskRecordMapper;
    }

    @Override
    @Transactional
    public ExperimentAdmission issueOnPassedExam(ExamRecord record, ExamPaper paper) {
        if (record == null || paper == null || record.getExperimentId() == null
                || record.getPassed() == null || record.getPassed() != 1) {
            return null;
        }
        Date now = new Date();
        Date validUntil = validityEnd(now, paper.getAdmissionValidityDays());
        admissionMapper.update(null, new LambdaUpdateWrapper<ExperimentAdmission>()
                .eq(ExperimentAdmission::getStudentId, record.getStudentId())
                .eq(ExperimentAdmission::getExperimentId, record.getExperimentId())
                .eq(ExperimentAdmission::getStatus, "VALID")
                .eq(ExperimentAdmission::getDeleted, 0)
                .set(ExperimentAdmission::getStatus, "REPLACED"));

        ExperimentAdmission admission = new ExperimentAdmission();
        admission.setStudentId(record.getStudentId());
        admission.setExperimentId(record.getExperimentId());
        admission.setPaperId(record.getPaperId());
        admission.setRecordId(record.getId());
        admission.setStatus("VALID");
        admission.setIssuedTime(now);
        admission.setValidUntil(validUntil);
        admission.setCreateTime(now);
        admission.setDeleted(0);
        admissionMapper.insert(admission);
        return admission;
    }

    @Override
    @Transactional
    public void revokeByExamRecord(Long recordId, String reason) {
        if (recordId == null) {
            return;
        }
        Date now = new Date();
        admissionMapper.update(null, new LambdaUpdateWrapper<ExperimentAdmission>()
                .eq(ExperimentAdmission::getRecordId, recordId)
                .eq(ExperimentAdmission::getStatus, "VALID")
                .eq(ExperimentAdmission::getDeleted, 0)
                .set(ExperimentAdmission::getStatus, "REVOKED")
                .set(ExperimentAdmission::getRevokeTime, now)
                .set(ExperimentAdmission::getRevokeReason, reason));
    }

    @Override
    public Map<String, Object> getAdmissionStatus(Long studentId, Long experimentId) {
        Experiment experiment = experimentMapper.selectById(experimentId);
        LabCourse course = experiment == null ? null : courseMapper.selectById(experiment.getCourseId());
        boolean studentInCourse = experiment != null && studentInCourse(studentId, experiment.getCourseId());
        List<Map<String, Object>> missingTasks = missingRequiredTasks(studentId, experimentId);
        boolean tasksCompleted = missingTasks.isEmpty();
        ExperimentAdmission admission = latestAdmission(studentId, experimentId);
        Date now = new Date();
        boolean examPassed = admission != null;
        boolean notRevoked = admission != null && "VALID".equals(admission.getStatus());
        boolean notExpired = admission != null && (admission.getValidUntil() == null || !admission.getValidUntil().before(now));
        boolean experimentOpen = experiment != null && Integer.valueOf(1).equals(experiment.getStatus());
        boolean courseOpen = course != null && !Integer.valueOf(2).equals(course.getStatus());
        boolean qualified = studentInCourse && tasksCompleted && examPassed && notRevoked && notExpired && experimentOpen && courseOpen;

        Map<String, Object> result = new HashMap<>();
        result.put("qualified", qualified);
        result.put("studentInCourse", studentInCourse);
        result.put("tasksCompleted", tasksCompleted);
        result.put("examPassed", examPassed);
        result.put("notExpired", notExpired);
        result.put("notRevoked", notRevoked);
        result.put("experimentOpen", experimentOpen);
        result.put("courseOpen", courseOpen);
        result.put("missingTasks", missingTasks);
        result.put("status", admission == null ? "NONE" : admission.getStatus());
        result.put("validUntil", admission == null ? null : admission.getValidUntil());
        result.put("admissionId", admission == null ? null : admission.getId());
        if (!qualified) {
            result.put("reason", reason(studentInCourse, tasksCompleted, examPassed, notExpired, notRevoked,
                    experimentOpen, courseOpen, missingTasks.size()));
        }
        return result;
    }

    @Override
    public void assertReservable(Long studentId, Long experimentId) {
        if (experimentId == null) {
            throw new BusinessException(400, "预约缺少实验项目");
        }
        Map<String, Object> status = getAdmissionStatus(studentId, experimentId);
        if (!Boolean.TRUE.equals(status.get("qualified"))) {
            throw new BusinessException(403, "暂未获得实验准入资格：" + status.get("reason"));
        }
    }

    private List<Map<String, Object>> missingRequiredTasks(Long studentId, Long experimentId) {
        List<LearningTask> tasks = learningTaskMapper.selectList(new LambdaQueryWrapper<LearningTask>()
                .eq(LearningTask::getExperimentId, experimentId)
                .eq(LearningTask::getRequiredFlag, 1)
                .eq(LearningTask::getStatus, 1));
        List<Map<String, Object>> missing = new java.util.ArrayList<>();
        for (LearningTask task : tasks) {
            if ("EXAM".equals(task.getTaskType()) && examTaskPassed(studentId, task)) {
                continue;
            }
            Long count = taskRecordMapper.selectCount(new LambdaQueryWrapper<LearningTaskRecord>()
                    .eq(LearningTaskRecord::getTaskId, task.getId())
                    .eq(LearningTaskRecord::getStudentId, studentId)
                    .eq(LearningTaskRecord::getStatus, "COMPLETED"));
            if (count == null || count == 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("taskId", task.getId());
                item.put("taskName", task.getTaskName());
                missing.add(item);
            }
        }
        return missing;
    }

    private boolean examTaskPassed(Long studentId, LearningTask task) {
        Long count = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStudentId, studentId)
                .eq(task.getTargetPaperId() != null, ExamRecord::getPaperId, task.getTargetPaperId())
                .eq(task.getTargetPaperId() == null, ExamRecord::getExperimentId, task.getExperimentId())
                .eq(ExamRecord::getPassed, 1)
                .in(ExamRecord::getStatus, "GRADED", "SUBMITTED", "REVIEWED"));
        return count != null && count > 0;
    }

    private boolean studentInCourse(Long studentId, Long courseId) {
        Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getStudentId, studentId)
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStatus, 1));
        return count != null && count > 0;
    }

    private ExperimentAdmission latestAdmission(Long studentId, Long experimentId) {
        return admissionMapper.selectOne(new LambdaQueryWrapper<ExperimentAdmission>()
                .eq(ExperimentAdmission::getStudentId, studentId)
                .eq(ExperimentAdmission::getExperimentId, experimentId)
                .eq(ExperimentAdmission::getDeleted, 0)
                .orderByDesc(ExperimentAdmission::getIssuedTime)
                .last("LIMIT 1"));
    }

    private Date validityEnd(Date issuedTime, Integer validityDays) {
        int days = validityDays == null || validityDays <= 0 ? 180 : validityDays;
        LocalDateTime end = LocalDateTime.ofInstant(issuedTime.toInstant(), ZONE).plusDays(days);
        return Date.from(end.atZone(ZONE).toInstant());
    }

    private String reason(boolean studentInCourse, boolean tasksCompleted, boolean examPassed,
                          boolean notExpired, boolean notRevoked, boolean experimentOpen,
                          boolean courseOpen, int missingCount) {
        if (!studentInCourse) return "学生未加入该实验所属课程";
        if (!courseOpen) return "课程已归档，不能新增预约";
        if (!experimentOpen) return "实验未开放或已关闭";
        if (!tasksCompleted) return "仍有 " + missingCount + " 项必做学习任务未完成";
        if (!examPassed) return "正式安全考试未通过";
        if (!notExpired) return "准入资格已过期";
        if (!notRevoked) return "准入资格已撤销";
        return "未满足准入条件";
    }
}
