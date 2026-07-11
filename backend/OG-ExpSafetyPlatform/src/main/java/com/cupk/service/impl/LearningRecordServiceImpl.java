package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.dto.LearningProgressDTO;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.TeachingClassMapper;
import com.cupk.pojo.CourseStudent;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.LearningRecord;
import com.cupk.pojo.TeachingClass;
import com.cupk.pojo.TeachingResource;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.LearningRecordMapper;
import com.cupk.mapper.TeachingResourceMapper;
import com.cupk.service.LearningRecordService;
import com.cupk.service.LearningTaskService;
import com.cupk.service.ResourceAccessService;
import com.cupk.util.AccessUtil;
import com.cupk.vo.LearningProgressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LearningRecordServiceImpl implements LearningRecordService {
    private final LearningRecordMapper recordMapper;
    private final TeachingResourceMapper resourceMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final ExperimentMapper experimentMapper;
    private final LabCourseMapper courseMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final LearningTaskService learningTaskService;
    private final ResourceAccessService resourceAccessService;

    public LearningRecordServiceImpl(LearningRecordMapper recordMapper, TeachingResourceMapper resourceMapper,
                                     CourseStudentMapper courseStudentMapper,
                                     ExperimentMapper experimentMapper,
                                     LabCourseMapper courseMapper,
                                     TeachingClassMapper teachingClassMapper,
                                     LearningTaskService learningTaskService, ResourceAccessService resourceAccessService) {
        this.recordMapper = recordMapper;
        this.resourceMapper = resourceMapper;
        this.courseStudentMapper = courseStudentMapper;
        this.experimentMapper = experimentMapper;
        this.courseMapper = courseMapper;
        this.teachingClassMapper = teachingClassMapper;
        this.learningTaskService = learningTaskService;
        this.resourceAccessService = resourceAccessService;
    }

    @Override
    @Transactional
    public void start(Long resourceId) {
        AccessUtil.requireStudent();
        TeachingResource resource = requireOpenResource(resourceId);
        resourceMapper.incrementViewCount(resourceId);
        Long studentId = UserContext.userId();
        LearningRecord record = find(studentId, resourceId);
        LocalDateTime now = LocalDateTime.now();
        if (record == null) {
            record = new LearningRecord();
            record.setStudentId(studentId);
            record.setResourceId(resourceId);
            record.setExperimentId(resource.getExperimentId());
            record.setProgress(BigDecimal.ZERO);
            record.setDurationSeconds(0);
            record.setLastPositionSeconds(0);
            record.setFinishFlag(0);
            record.setFirstTime(now);
            record.setLastTime(now);
            recordMapper.insert(record);
        } else {
            record.setExperimentId(resource.getExperimentId());
            record.setLastTime(now);
            recordMapper.updateById(record);
        }
    }

    @Override
    @Transactional
    public void updateProgress(LearningProgressDTO dto) {
        AccessUtil.requireStudent();
        TeachingResource resource = requireOpenResource(dto.getResourceId());
        Long studentId = UserContext.userId();
        LearningRecord record = find(studentId, dto.getResourceId());
        if (record == null) {
            start(dto.getResourceId());
            record = find(studentId, dto.getResourceId());
        }
        boolean wasFinished = Integer.valueOf(1).equals(record.getFinishFlag());
        record.setProgress(dto.getProgress());
        record.setExperimentId(resource.getExperimentId());
        int currentDuration = record.getDurationSeconds() == null ? 0 : record.getDurationSeconds();
        int durationDelta = dto.getDurationSeconds() == null ? 0 : dto.getDurationSeconds();
        record.setDurationSeconds(currentDuration + durationDelta);
        if (dto.getLastPositionSeconds() != null) {
            record.setLastPositionSeconds(dto.getLastPositionSeconds());
        }
        if (dto.getNote() != null) {
            record.setNote(dto.getNote());
        }
        record.setFinishFlag(resolveFinishFlag(dto, resource, record.getDurationSeconds()));
        record.setLastTime(LocalDateTime.now());
        recordMapper.updateById(record);
        if (!wasFinished && Integer.valueOf(1).equals(record.getFinishFlag())) {
            learningTaskService.syncResourceCompleted(studentId, dto.getResourceId());
        }
    }

    @Override
    public LearningProgressVO experimentProgress(Long experimentId, Long studentId) {
        List<Long> resourceIds = resourceMapper.selectList(new LambdaQueryWrapper<TeachingResource>()
                .select(TeachingResource::getId)
                .eq(TeachingResource::getExperimentId, experimentId)
                .eq(TeachingResource::getRequiredFlag, 1)
                .eq(TeachingResource::getStatus, 1))
                .stream().map(TeachingResource::getId).toList();
        int requiredCount = resourceIds.size();
        if (requiredCount == 0) {
            return new LearningProgressVO(experimentId, 0, 0, BigDecimal.ZERO);
        }
        long finished = recordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getStudentId, studentId)
                .in(LearningRecord::getResourceId, resourceIds)
                .eq(LearningRecord::getFinishFlag, 1));
        BigDecimal progress = BigDecimal.valueOf(finished)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(requiredCount), 2, RoundingMode.HALF_UP);
        return new LearningProgressVO(experimentId, requiredCount, Math.toIntExact(finished), progress);
    }

    @Override
    public List<LearningRecord> myRecords() {
        AccessUtil.requireStudent();
        return recordMapper.selectList(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getStudentId, UserContext.userId())
                .orderByDesc(LearningRecord::getLastTime));
    }

    private LearningRecord find(Long studentId, Long resourceId) {
        return recordMapper.selectOne(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getStudentId, studentId)
                .eq(LearningRecord::getResourceId, resourceId));
    }

    private TeachingResource requireOpenResource(Long id) {
        TeachingResource resource = resourceMapper.selectById(id);
        resourceAccessService.assertReadable(resource);
        return resource;
    }

    private boolean isPublicResource(TeachingResource resource) {
        return "PUBLIC".equalsIgnoreCase(resource.getOpenScope());
    }

    private boolean canManageResource(TeachingResource resource) {
        if (!UserContext.isTeacher()) {
            return false;
        }
        Experiment experiment = experimentMapper.selectById(resource.getExperimentId());
        if (experiment == null) {
            return false;
        }
        LabCourse course = courseMapper.selectById(experiment.getCourseId());
        if (course == null) {
            return false;
        }
        if (UserContext.userId().equals(course.getTeacherId())) {
            return true;
        }
        return teachingClassMapper.selectCount(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getCourseId, course.getId())
                .and(w -> w.eq(TeachingClass::getTeacherId, UserContext.userId())
                        .or().eq(TeachingClass::getAssistantId, UserContext.userId()))) > 0;
    }

    @Override
    public LearningRecord resourceRecord(Long resourceId) {
        AccessUtil.requireStudent();
        requireOpenResource(resourceId);
        return find(UserContext.userId(), resourceId);
    }

    private Integer resolveFinishFlag(LearningProgressDTO dto, TeachingResource resource, Integer durationSeconds) {
        if (dto.getFinishFlag() != null && dto.getFinishFlag() == 0) {
            return 0;
        }
        int minProgress = resource.getMinProgress() == null ? 100 : resource.getMinProgress();
        int minSeconds = resource.getMinStudySeconds() == null ? 0 : resource.getMinStudySeconds();
        String rule = resource.getCompletionRule() == null ? "CONFIRM" : resource.getCompletionRule();
        boolean progressEnough = dto.getProgress().compareTo(BigDecimal.valueOf(minProgress)) >= 0;
        boolean timeEnough = durationSeconds != null && durationSeconds >= minSeconds;
        if ("TIME".equals(rule)) {
            return timeEnough ? 1 : 0;
        }
        if ("PROGRESS".equals(rule)) {
            return progressEnough ? 1 : 0;
        }
        if ("PROGRESS_TIME".equals(rule)) {
            return progressEnough && timeEnough ? 1 : 0;
        }
        return dto.getFinishFlag() != null && dto.getFinishFlag() == 1 ? 1 : (progressEnough && timeEnough ? 1 : 0);
    }
}
