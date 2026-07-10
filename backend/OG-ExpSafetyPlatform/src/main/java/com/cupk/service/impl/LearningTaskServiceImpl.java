package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.PageResult;
import com.cupk.dto.LearningTaskCreateDTO;
import com.cupk.dto.LearningTaskQueryDTO;
import com.cupk.dto.LearningTaskUpdateDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.*;
import com.cupk.pojo.*;
import com.cupk.service.LearningTaskService;
import com.cupk.util.AccessUtil;
import com.cupk.vo.LearningPathVO;
import com.cupk.vo.LearningTaskDistributionVO;
import com.cupk.vo.LearningTaskVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LearningTaskServiceImpl implements LearningTaskService {
    private static final Set<String> TASK_TYPES = Set.of(
            "READ_RESOURCE", "WATCH_VIDEO", "PRACTICE", "EXAM", "CHECKLIST"
    );

    private final LearningTaskMapper taskMapper;
    private final LearningTaskRecordMapper recordMapper;
    private final ExperimentMapper experimentMapper;
    private final LabCourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final ExamRecordMapper examRecordMapper;
    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;

    public LearningTaskServiceImpl(LearningTaskMapper taskMapper, LearningTaskRecordMapper recordMapper,
                                   ExperimentMapper experimentMapper, LabCourseMapper courseMapper,
                                   CourseStudentMapper courseStudentMapper, LearningRecordMapper learningRecordMapper,
                                   ExamRecordMapper examRecordMapper, OperationLogMapper operationLogMapper,
                                   UserMapper userMapper) {
        this.taskMapper = taskMapper;
        this.recordMapper = recordMapper;
        this.experimentMapper = experimentMapper;
        this.courseMapper = courseMapper;
        this.courseStudentMapper = courseStudentMapper;
        this.learningRecordMapper = learningRecordMapper;
        this.examRecordMapper = examRecordMapper;
        this.operationLogMapper = operationLogMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<LearningTask> page(LearningTaskQueryDTO dto) {
        LambdaQueryWrapper<LearningTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dto.getCourseId() != null, LearningTask::getCourseId, dto.getCourseId());
        wrapper.eq(dto.getExperimentId() != null, LearningTask::getExperimentId, dto.getExperimentId());
        wrapper.eq(dto.getStatus() != null, LearningTask::getStatus, dto.getStatus());
        wrapper.eq(dto.getTaskType() != null && !dto.getTaskType().isBlank(), LearningTask::getTaskType, dto.getTaskType());
        if (UserContext.isTeacher()) {
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<LabCourse>()
                            .select(LabCourse::getId)
                            .eq(LabCourse::getTeacherId, UserContext.userId()))
                    .stream().map(LabCourse::getId).toList();
            if (courseIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, dto.getPageNum(), dto.getPageSize());
            }
            wrapper.in(LearningTask::getCourseId, courseIds);
        }
        wrapper.orderByAsc(LearningTask::getSort).orderByAsc(LearningTask::getId);
        Page<LearningTask> page = taskMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional
    public Long create(LearningTaskCreateDTO dto) {
        Experiment experiment = requireExperiment(dto.getExperimentId());
        LabCourse course = requireCourse(experiment.getCourseId());
        AccessUtil.assertCourseWritable(course);
        validate(dto.getTaskType(), dto.getPrerequisiteTaskId(), dto.getExperimentId());
        LearningTask task = new LearningTask();
        BeanUtils.copyProperties(dto, task);
        task.setCourseId(course.getId());
        applyDefaults(task);
        taskMapper.insert(task);
        log("CREATE", "创建学习任务：" + task.getTaskName(), "SUCCESS");
        return task.getId();
    }

    @Override
    @Transactional
    public void update(Long id, LearningTaskUpdateDTO dto) {
        LearningTask current = requireTask(id);
        AccessUtil.assertCourseWritable(requireCourse(current.getCourseId()));
        Experiment experiment = requireExperiment(dto.getExperimentId());
        LabCourse course = requireCourse(experiment.getCourseId());
        AccessUtil.assertCourseWritable(course);
        validate(dto.getTaskType(), dto.getPrerequisiteTaskId(), dto.getExperimentId());
        BeanUtils.copyProperties(dto, current);
        current.setCourseId(course.getId());
        applyDefaults(current);
        taskMapper.updateById(current);
        log("UPDATE", "更新学习任务：" + current.getTaskName(), "SUCCESS");
    }

    @Override
    @Transactional
    public void disable(Long id) {
        LearningTask task = requireTask(id);
        AccessUtil.assertCourseWritable(requireCourse(task.getCourseId()));
        task.setStatus(0);
        taskMapper.updateById(task);
        log("DISABLE", "停用学习任务：" + task.getTaskName(), "SUCCESS");
    }

    @Override
    public LearningPathVO path(Long experimentId) {
        AccessUtil.requireStudent();
        Experiment experiment = requireExperiment(experimentId);
        assertStudentInCourse(experiment.getCourseId());
        List<LearningTask> tasks = listActiveTasks(experimentId);
        List<LearningTaskVO> items = tasks.stream().map(task -> toStudentVO(task, UserContext.userId(), tasks)).toList();
        int total = (int) items.stream().filter(item -> flag(item.getTask().getRequiredFlag()) == 1).count();
        int completed = (int) items.stream()
                .filter(item -> flag(item.getTask().getRequiredFlag()) == 1 && Boolean.TRUE.equals(item.getCompleted()))
                .count();
        LearningPathVO vo = new LearningPathVO();
        vo.setExperimentId(experimentId);
        vo.setTotalCount(total);
        vo.setCompletedCount(completed);
        vo.setProgress(total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        vo.setTasks(items);
        vo.setNextTask(items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getOpened()) && !Boolean.TRUE.equals(item.getLocked()) && !Boolean.TRUE.equals(item.getCompleted()))
                .findFirst().orElse(null));
        vo.setReminders(items.stream().filter(item -> "DUE_SOON".equals(item.getState()) || "OVERDUE".equals(item.getState())).toList());
        return vo;
    }

    @Override
    @Transactional
    public void confirmChecklist(Long id) {
        AccessUtil.requireStudent();
        LearningTask task = requireTask(id);
        if (!"CHECKLIST".equals(task.getTaskType())) {
            throw new BusinessException(400, "只有准备清单任务允许确认");
        }
        assertStudentInCourse(task.getCourseId());
        List<LearningTask> tasks = listActiveTasks(task.getExperimentId());
        LearningTaskVO vo = toStudentVO(task, UserContext.userId(), tasks);
        if (!Boolean.TRUE.equals(vo.getOpened()) || Boolean.TRUE.equals(vo.getLocked())) {
            throw new BusinessException(403, "任务未开放或前置任务未完成");
        }
        completeRecord(task.getId(), UserContext.userId(), "CHECKLIST");
        log("CONFIRM", "确认准备清单：" + task.getTaskName(), "SUCCESS");
    }

    @Override
    @Transactional
    public void syncResourceCompleted(Long studentId, Long resourceId) {
        if (studentId == null || resourceId == null) {
            return;
        }
        List<LearningTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<LearningTask>()
                .eq(LearningTask::getTargetResourceId, resourceId)
                .in(LearningTask::getTaskType, "READ_RESOURCE", "WATCH_VIDEO")
                .eq(LearningTask::getStatus, 1));
        for (LearningTask task : tasks) {
            completeRecord(task.getId(), studentId, task.getTaskType());
        }
    }

    @Override
    @Transactional
    public void syncExamCompleted(Long studentId, Long paperId, Long experimentId) {
        if (studentId == null && paperId == null && experimentId == null) {
            return;
        }
        LambdaQueryWrapper<LearningTask> wrapper = new LambdaQueryWrapper<LearningTask>()
                .in(LearningTask::getTaskType, "PRACTICE", "EXAM")
                .eq(LearningTask::getStatus, 1);
        if (paperId != null && experimentId != null) {
            wrapper.and(w -> w.eq(LearningTask::getTargetPaperId, paperId)
                    .or()
                    .eq(LearningTask::getExperimentId, experimentId));
        } else if (paperId != null) {
            wrapper.eq(LearningTask::getTargetPaperId, paperId);
        } else {
            wrapper.eq(LearningTask::getExperimentId, experimentId);
        }
        List<LearningTask> tasks = taskMapper.selectList(wrapper);
        for (LearningTask task : tasks) {
            if (isCompleted(task, studentId)) {
                completeRecord(task.getId(), studentId, task.getTaskType());
            }
        }
    }

    @Override
    public List<LearningTaskDistributionVO> distribution(Long experimentId) {
        Experiment experiment = requireExperiment(experimentId);
        AccessUtil.assertCourseWritable(requireCourse(experiment.getCourseId()));
        List<Long> studentIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                        .select(CourseStudent::getStudentId)
                        .eq(CourseStudent::getCourseId, experiment.getCourseId())
                        .eq(CourseStudent::getStatus, 1))
                .stream().map(CourseStudent::getStudentId).toList();
        List<LearningTask> tasks = listActiveTasks(experimentId);
        List<LearningTaskDistributionVO> result = new ArrayList<>();
        for (LearningTask task : tasks) {
            LearningTaskDistributionVO vo = new LearningTaskDistributionVO();
            vo.setTask(task);
            vo.setStudentCount(studentIds.size());
            int notStarted = 0;
            int inProgress = 0;
            int completed = 0;
            int dueSoon = 0;
            int overdue = 0;
            for (Long studentId : studentIds) {
                LearningTaskVO item = toStudentVO(task, studentId, tasks);
                if (Boolean.TRUE.equals(item.getCompleted())) completed++;
                else if ("IN_PROGRESS".equals(item.getState())) inProgress++;
                else notStarted++;
                if ("DUE_SOON".equals(item.getState())) dueSoon++;
                if ("OVERDUE".equals(item.getState())) overdue++;
            }
            vo.setNotStartedCount(notStarted);
            vo.setInProgressCount(inProgress);
            vo.setCompletedCount(completed);
            vo.setDueSoonCount(dueSoon);
            vo.setOverdueCount(overdue);
            result.add(vo);
        }
        return result;
    }

    private LearningTaskVO toStudentVO(LearningTask task, Long studentId, List<LearningTask> allTasks) {
        boolean completed = isCompleted(task, studentId);
        if (completed) {
            syncCompletedRecord(task, studentId);
        }
        LocalDateTime now = LocalDateTime.now();
        boolean opened = task.getOpenTime() == null || !task.getOpenTime().isAfter(now);
        LearningTask prerequisite = task.getPrerequisiteTaskId() == null ? null : allTasks.stream()
                .filter(item -> task.getPrerequisiteTaskId().equals(item.getId()))
                .findFirst().orElse(null);
        boolean locked = prerequisite != null && !isCompleted(prerequisite, studentId);
        String state = resolveState(task, studentId, opened, locked, completed, now);
        LearningTaskVO vo = new LearningTaskVO();
        vo.setTask(task);
        vo.setOpened(opened);
        vo.setLocked(locked);
        vo.setCompleted(completed);
        vo.setState(state);
        vo.setActionPath(actionPath(task));
        vo.setPrerequisiteTaskName(prerequisite == null ? null : prerequisite.getTaskName());
        if (!opened) {
            vo.setLockedReason("任务尚未开放");
        } else if (locked) {
            vo.setLockedReason("请先完成前置任务：" + prerequisite.getTaskName());
        }
        LearningTaskRecord record = findRecord(task.getId(), studentId);
        vo.setCompleteTime(record == null ? null : record.getCompleteTime());
        return vo;
    }

    private String resolveState(LearningTask task, Long studentId, boolean opened, boolean locked, boolean completed, LocalDateTime now) {
        if (completed) return "COMPLETED";
        if (!opened) return "NOT_OPEN";
        if (locked) return "LOCKED";
        if (task.getDeadline() != null && task.getDeadline().isBefore(now)) return "OVERDUE";
        if (task.getDeadline() != null && !task.getDeadline().isAfter(now.plusHours(48))) return "DUE_SOON";
        if (hasStarted(task, studentId)) return "IN_PROGRESS";
        return "NOT_STARTED";
    }

    private boolean isCompleted(LearningTask task, Long studentId) {
        if ("READ_RESOURCE".equals(task.getTaskType()) || "WATCH_VIDEO".equals(task.getTaskType())) {
            if (task.getTargetResourceId() == null) return false;
            return learningRecordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                    .eq(LearningRecord::getStudentId, studentId)
                    .eq(LearningRecord::getResourceId, task.getTargetResourceId())
                    .eq(LearningRecord::getFinishFlag, 1)) > 0;
        }
        if ("PRACTICE".equals(task.getTaskType())) {
            return examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getStudentId, studentId)
                    .eq(task.getTargetPaperId() != null, ExamRecord::getPaperId, task.getTargetPaperId())
                    .eq(task.getTargetPaperId() == null, ExamRecord::getExperimentId, task.getExperimentId())
                    .in(ExamRecord::getStatus, List.of("PENDING_REVIEW", "GRADED", "EXPIRED", "SUBMITTED", "REVIEWED"))) > 0;
        }
        if ("EXAM".equals(task.getTaskType())) {
            return examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getStudentId, studentId)
                    .eq(task.getTargetPaperId() != null, ExamRecord::getPaperId, task.getTargetPaperId())
                    .eq(task.getTargetPaperId() == null, ExamRecord::getExperimentId, task.getExperimentId())
                    .eq(ExamRecord::getPassed, 1)
                    .in(ExamRecord::getStatus, List.of("GRADED", "SUBMITTED", "REVIEWED"))) > 0;
        }
        LearningTaskRecord record = findRecord(task.getId(), studentId);
        return record != null && "COMPLETED".equals(record.getStatus());
    }

    private boolean hasStarted(LearningTask task, Long studentId) {
        if (task.getTargetResourceId() != null && learningRecordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getStudentId, studentId)
                .eq(LearningRecord::getResourceId, task.getTargetResourceId())) > 0) {
            return true;
        }
        if (task.getTargetPaperId() != null && examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStudentId, studentId)
                .eq(ExamRecord::getPaperId, task.getTargetPaperId())) > 0) {
            return true;
        }
        return findRecord(task.getId(), studentId) != null;
    }

    private void syncCompletedRecord(LearningTask task, Long studentId) {
        if ("CHECKLIST".equals(task.getTaskType())) return;
        completeRecord(task.getId(), studentId, task.getTaskType());
    }

    private void completeRecord(Long taskId, Long studentId, String sourceType) {
        LearningTaskRecord record = findRecord(taskId, studentId);
        LocalDateTime now = LocalDateTime.now();
        if (record == null) {
            record = new LearningTaskRecord();
            record.setTaskId(taskId);
            record.setStudentId(studentId);
            record.setStartTime(now);
        }
        record.setStatus("COMPLETED");
        record.setCompleteTime(now);
        record.setSourceType(sourceType);
        if (record.getId() == null) recordMapper.insert(record);
        else recordMapper.updateById(record);
    }

    private LearningTaskRecord findRecord(Long taskId, Long studentId) {
        return recordMapper.selectOne(new LambdaQueryWrapper<LearningTaskRecord>()
                .eq(LearningTaskRecord::getTaskId, taskId)
                .eq(LearningTaskRecord::getStudentId, studentId));
    }

    private String actionPath(LearningTask task) {
        if (task.getTargetResourceId() != null) return "/student/resources";
        if ("PRACTICE".equals(task.getTaskType()) || "EXAM".equals(task.getTaskType())) return "/student/exams";
        return null;
    }

    private List<LearningTask> listActiveTasks(Long experimentId) {
        return taskMapper.selectList(new LambdaQueryWrapper<LearningTask>()
                .eq(LearningTask::getExperimentId, experimentId)
                .eq(LearningTask::getStatus, 1)
                .orderByAsc(LearningTask::getSort)
                .orderByAsc(LearningTask::getId));
    }

    private void validate(String taskType, Long prerequisiteTaskId, Long experimentId) {
        if (!TASK_TYPES.contains(taskType)) {
            throw new BusinessException(400, "不支持的任务类型");
        }
        if (prerequisiteTaskId != null) {
            LearningTask prerequisite = requireTask(prerequisiteTaskId);
            if (!experimentId.equals(prerequisite.getExperimentId())) {
                throw new BusinessException(400, "前置任务必须属于同一实验");
            }
        }
    }

    private void applyDefaults(LearningTask task) {
        task.setRequiredFlag(flag(task.getRequiredFlag()));
        task.setSort(task.getSort() == null ? 0 : task.getSort());
        task.setStatus(task.getStatus() == null ? 1 : task.getStatus());
        task.setCompletionRule(task.getCompletionRule() == null || task.getCompletionRule().isBlank() ? "AUTO" : task.getCompletionRule());
    }

    private LearningTask requireTask(Long id) {
        LearningTask task = taskMapper.selectById(id);
        if (task == null) throw new BusinessException(404, "学习任务不存在");
        return task;
    }

    private Experiment requireExperiment(Long id) {
        Experiment experiment = experimentMapper.selectById(id);
        if (experiment == null) throw new BusinessException(404, "实验项目不存在");
        return experiment;
    }

    private LabCourse requireCourse(Long id) {
        LabCourse course = courseMapper.selectById(id);
        if (course == null) throw new BusinessException(404, "课程不存在");
        return course;
    }

    private void assertStudentInCourse(Long courseId) {
        Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, UserContext.userId())
                .eq(CourseStudent::getStatus, 1));
        if (count == null || count == 0) throw new BusinessException(403, "无权访问该课程任务");
    }

    private int flag(Integer value) {
        return value == null ? 0 : value;
    }

    private void log(String action, String content, String result) {
        OperationLog log = new OperationLog();
        log.setUserId(UserContext.userId());
        User user = userMapper.selectById(UserContext.userId());
        log.setUsername(user == null ? null : user.getUsername());
        log.setModule("LEARNING_TASK");
        log.setAction(action);
        log.setContent(content);
        log.setResult(result);
        operationLogMapper.insert(log);
    }
}
