package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.PageResult;
import com.cupk.dto.ExperimentCreateDTO;
import com.cupk.dto.ExperimentQueryDTO;
import com.cupk.dto.ExperimentStepDTO;
import com.cupk.dto.ExperimentUpdateDTO;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.ExperimentStep;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.SafetyKnowledge;
import com.cupk.pojo.TeachingResource;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.*;
import com.cupk.service.ExperimentService;
import com.cupk.service.LearningRecordService;
import com.cupk.util.AccessUtil;
import com.cupk.vo.ExperimentDetailVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ExperimentServiceImpl implements ExperimentService {
    private final ExperimentMapper experimentMapper;
    private final LabCourseMapper courseMapper;
    private final ExperimentStepMapper stepMapper;
    private final TeachingResourceMapper resourceMapper;
    private final SafetyKnowledgeMapper knowledgeMapper;
    private final BusinessReferenceMapper businessReferenceMapper;
    private final LearningRecordService learningRecordService;
    private final ExamRecordMapper examRecordMapper;

    public ExperimentServiceImpl(ExperimentMapper experimentMapper, LabCourseMapper courseMapper,
                                 ExperimentStepMapper stepMapper, TeachingResourceMapper resourceMapper,
                                 SafetyKnowledgeMapper knowledgeMapper, BusinessReferenceMapper businessReferenceMapper,
                                 LearningRecordService learningRecordService, ExamRecordMapper examRecordMapper) {
        this.experimentMapper = experimentMapper;
        this.courseMapper = courseMapper;
        this.stepMapper = stepMapper;
        this.resourceMapper = resourceMapper;
        this.knowledgeMapper = knowledgeMapper;
        this.businessReferenceMapper = businessReferenceMapper;
        this.learningRecordService = learningRecordService;
        this.examRecordMapper = examRecordMapper;
    }

    @Override
    @Transactional
    public Long create(ExperimentCreateDTO dto) {
        LabCourse course = requireCourse(dto.getCourseId());
        AccessUtil.assertCourseWritable(course);
        checkCode(dto.getCourseId(), dto.getExpCode(), null);
        validateRisk(dto.getRiskLevel());
        Experiment entity = new Experiment();
        BeanUtils.copyProperties(dto, entity);
        experimentMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ExperimentUpdateDTO dto) {
        Experiment current = requireExperiment(id);
        LabCourse oldCourse = requireCourse(current.getCourseId());
        AccessUtil.assertCourseWritable(oldCourse);
        LabCourse targetCourse = requireCourse(dto.getCourseId());
        AccessUtil.assertCourseWritable(targetCourse);
        checkCode(dto.getCourseId(), dto.getExpCode(), id);
        validateRisk(dto.getRiskLevel());
        BeanUtils.copyProperties(dto, current);
        experimentMapper.updateById(current);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Experiment entity = requireExperiment(id);
        AccessUtil.assertCourseWritable(requireCourse(entity.getCourseId()));
        if (businessReferenceMapper.countExperimentHistory(id) > 0) {
            throw new BusinessException(409, "实验已有考试、预约或报告历史，只能关闭，不能删除");
        }
        stepMapper.delete(new LambdaQueryWrapper<ExperimentStep>().eq(ExperimentStep::getExperimentId, id));
        resourceMapper.delete(new LambdaQueryWrapper<TeachingResource>().eq(TeachingResource::getExperimentId, id));
        knowledgeMapper.delete(new LambdaQueryWrapper<SafetyKnowledge>().eq(SafetyKnowledge::getExperimentId, id));
        experimentMapper.deleteById(id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) throw new BusinessException(400, "状态只能为0或1");
        Experiment entity = requireExperiment(id);
        AccessUtil.assertCourseWritable(requireCourse(entity.getCourseId()));
        entity.setStatus(status);
        experimentMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void saveSteps(Long id, List<ExperimentStepDTO> steps) {
        Experiment experiment = requireExperiment(id);
        AccessUtil.assertCourseWritable(requireCourse(experiment.getCourseId()));
        if (steps == null) {
            throw new BusinessException(400, "步骤列表不能为空");
        }
        Set<Integer> stepNos = new HashSet<>();
        for (ExperimentStepDTO step : steps) {
            if (!stepNos.add(step.getStepNo())) {
                throw new BusinessException(400, "步骤序号不能重复");
            }
        }
        stepMapper.delete(new LambdaQueryWrapper<ExperimentStep>().eq(ExperimentStep::getExperimentId, id));
        for (ExperimentStepDTO dto : steps) {
            ExperimentStep entity = new ExperimentStep();
            BeanUtils.copyProperties(dto, entity);
            entity.setExperimentId(id);
            stepMapper.insert(entity);
        }
    }

    @Override
    public PageResult<Experiment> page(ExperimentQueryDTO dto) {
        LambdaQueryWrapper<Experiment> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(dto.getKeyword()), w -> w.like(Experiment::getExpName, dto.getKeyword())
                .or().like(Experiment::getExpCode, dto.getKeyword()));
        wrapper.eq(dto.getCourseId() != null, Experiment::getCourseId, dto.getCourseId());
        wrapper.eq(StringUtils.hasText(dto.getRiskLevel()), Experiment::getRiskLevel, dto.getRiskLevel());
        wrapper.eq(dto.getStatus() != null, Experiment::getStatus, dto.getStatus());
        if (UserContext.isStudent()) {
            wrapper.eq(Experiment::getStatus, 1);
        } else if (UserContext.isTeacher()) {
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<LabCourse>()
                    .select(LabCourse::getId).eq(LabCourse::getTeacherId, UserContext.userId()))
                    .stream().map(LabCourse::getId).toList();
            if (courseIds.isEmpty()) return new PageResult<>(Collections.emptyList(), 0L, dto.getPageNum(), dto.getPageSize());
            wrapper.in(Experiment::getCourseId, courseIds);
        }
        wrapper.orderByAsc(Experiment::getSort).orderByDesc(Experiment::getCreateTime);
        Page<Experiment> page = experimentMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public ExperimentDetailVO detail(Long id) {
        Experiment experiment = requireExperiment(id);
        LabCourse course = requireCourse(experiment.getCourseId());
        if (UserContext.isStudent() && (experiment.getStatus() != 1 || course.getStatus() != 1)) {
            throw new BusinessException(403, "实验或课程未开放");
        }
        if (UserContext.isTeacher()) AccessUtil.assertCourseWritable(course);

        ExperimentDetailVO vo = new ExperimentDetailVO();
        vo.setExperiment(experiment);
        vo.setCourseName(course.getCourseName());
        vo.setSteps(stepMapper.selectList(new LambdaQueryWrapper<ExperimentStep>()
                .eq(ExperimentStep::getExperimentId, id).orderByAsc(ExperimentStep::getStepNo)));
        vo.setResources(resourceMapper.selectList(new LambdaQueryWrapper<TeachingResource>()
                .eq(TeachingResource::getExperimentId, id)
                .eq(UserContext.isStudent(), TeachingResource::getStatus, 1)
                .orderByAsc(TeachingResource::getSort).orderByAsc(TeachingResource::getId)));
        vo.setSafetyKnowledge(knowledgeMapper.selectList(new LambdaQueryWrapper<SafetyKnowledge>()
                .eq(SafetyKnowledge::getExperimentId, id)
                .eq(UserContext.isStudent(), SafetyKnowledge::getStatus, 1)
                .orderByAsc(SafetyKnowledge::getId)));

        if (UserContext.isStudent()) {
            BigDecimal progress = learningRecordService.experimentProgress(id, UserContext.userId()).getProgress();
            boolean passed = examRecordMapper.countPassed(UserContext.userId(), id) > 0;
            vo.setLearningProgress(progress);
            vo.setExamPassed(passed);
            vo.setReservationAllowed(experiment.getStatus() == 1
                    && Integer.valueOf(1).equals(experiment.getReservationEnabled()) && passed);
        } else {
            vo.setLearningProgress(BigDecimal.ZERO);
            vo.setExamPassed(false);
            vo.setReservationAllowed(false);
        }
        return vo;
    }

    private void checkCode(Long courseId, String code, Long excludeId) {
        long count = experimentMapper.selectCount(new LambdaQueryWrapper<Experiment>()
                .eq(Experiment::getCourseId, courseId)
                .eq(Experiment::getExpCode, code)
                .ne(excludeId != null, Experiment::getId, excludeId));
        if (count > 0) throw new BusinessException(409, "同一课程中的实验编号不能重复");
    }

    private void validateRisk(String risk) {
        if (!List.of("LOW", "MEDIUM", "HIGH").contains(risk)) {
            throw new BusinessException(400, "风险等级必须是LOW、MEDIUM或HIGH");
        }
    }

    private Experiment requireExperiment(Long id) {
        Experiment entity = experimentMapper.selectById(id);
        if (entity == null) throw new BusinessException(404, "实验项目不存在");
        return entity;
    }

    private LabCourse requireCourse(Long id) {
        LabCourse course = courseMapper.selectById(id);
        if (course == null) throw new BusinessException(404, "课程不存在");
        return course;
    }
}
