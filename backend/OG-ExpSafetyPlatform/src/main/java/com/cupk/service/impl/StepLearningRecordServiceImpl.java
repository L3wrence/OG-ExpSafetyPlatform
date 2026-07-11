package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.ExperimentStepMapper;
import com.cupk.mapper.StepLearningRecordMapper;
import com.cupk.pojo.ExperimentStep;
import com.cupk.pojo.StepLearningRecord;
import com.cupk.service.StepLearningRecordService;
import com.cupk.util.AccessUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StepLearningRecordServiceImpl implements StepLearningRecordService {
    private final StepLearningRecordMapper recordMapper;
    private final ExperimentStepMapper stepMapper;

    public StepLearningRecordServiceImpl(StepLearningRecordMapper recordMapper, ExperimentStepMapper stepMapper) {
        this.recordMapper = recordMapper;
        this.stepMapper = stepMapper;
    }

    @Override
    public List<StepLearningRecord> myRecords() {
        AccessUtil.requireStudent();
        return recordMapper.selectList(new LambdaQueryWrapper<StepLearningRecord>()
                .eq(StepLearningRecord::getStudentId, UserContext.userId())
                .orderByDesc(StepLearningRecord::getCompleteTime));
    }

    @Override
    @Transactional
    public void complete(Long stepId) {
        AccessUtil.requireStudent();
        ExperimentStep step = stepMapper.selectById(stepId);
        if (step == null) {
            throw new BusinessException(404, "实验步骤不存在");
        }
        Long studentId = UserContext.userId();
        StepLearningRecord existing = recordMapper.selectOne(new LambdaQueryWrapper<StepLearningRecord>()
                .eq(StepLearningRecord::getStudentId, studentId)
                .eq(StepLearningRecord::getStepId, stepId));
        if (existing != null) {
            return;
        }
        StepLearningRecord record = new StepLearningRecord();
        record.setStudentId(studentId);
        record.setStepId(stepId);
        record.setExperimentId(step.getExperimentId());
        record.setCompleteTime(LocalDateTime.now());
        recordMapper.insert(record);
    }
}
