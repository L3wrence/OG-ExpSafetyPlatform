package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.dto.LearningProgressDTO;
import com.cupk.pojo.LearningRecord;
import com.cupk.pojo.TeachingResource;
import com.cupk.exception.BusinessException;
import com.cupk.common.UserContext;
import com.cupk.mapper.LearningRecordMapper;
import com.cupk.mapper.TeachingResourceMapper;
import com.cupk.service.LearningRecordService;
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

    public LearningRecordServiceImpl(LearningRecordMapper recordMapper, TeachingResourceMapper resourceMapper) {
        this.recordMapper = recordMapper;
        this.resourceMapper = resourceMapper;
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
        record.setProgress(dto.getProgress());
        record.setExperimentId(resource.getExperimentId());
        int currentDuration = record.getDurationSeconds() == null ? 0 : record.getDurationSeconds();
        int durationDelta = dto.getDurationSeconds() == null ? 0 : dto.getDurationSeconds();
        record.setDurationSeconds(currentDuration + durationDelta);
        record.setFinishFlag(dto.getFinishFlag() != null ? dto.getFinishFlag() :
                (dto.getProgress().compareTo(BigDecimal.valueOf(100)) >= 0 ? 1 : 0));
        record.setLastTime(LocalDateTime.now());
        recordMapper.updateById(record);
    }

    @Override
    public LearningProgressVO experimentProgress(Long experimentId, Long studentId) {
        long requiredCount = resourceMapper.selectCount(new LambdaQueryWrapper<TeachingResource>()
                .eq(TeachingResource::getExperimentId, experimentId)
                .eq(TeachingResource::getRequiredFlag, 1)
                .eq(TeachingResource::getStatus, 1));
        if (requiredCount == 0) {
            return new LearningProgressVO(experimentId, 0, 0, BigDecimal.ZERO);
        }
        List<Long> resourceIds = resourceMapper.selectList(new LambdaQueryWrapper<TeachingResource>()
                .select(TeachingResource::getId)
                .eq(TeachingResource::getExperimentId, experimentId)
                .eq(TeachingResource::getRequiredFlag, 1)
                .eq(TeachingResource::getStatus, 1))
                .stream().map(TeachingResource::getId).toList();
        long finished = recordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getStudentId, studentId)
                .in(LearningRecord::getResourceId, resourceIds)
                .eq(LearningRecord::getFinishFlag, 1));
        BigDecimal progress = BigDecimal.valueOf(finished)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(requiredCount), 2, RoundingMode.HALF_UP);
        return new LearningProgressVO(experimentId, Math.toIntExact(requiredCount), Math.toIntExact(finished), progress);
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
        if (resource == null) throw new BusinessException(404, "教学资源不存在");
        if (resource.getStatus() != 1) throw new BusinessException(403, "教学资源未开放");
        return resource;
    }
}
