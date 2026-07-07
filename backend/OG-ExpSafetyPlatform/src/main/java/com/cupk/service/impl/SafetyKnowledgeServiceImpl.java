package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.PageResult;
import com.cupk.dto.SafetyKnowledgeCreateDTO;
import com.cupk.dto.SafetyKnowledgeQueryDTO;
import com.cupk.dto.SafetyKnowledgeUpdateDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.SafetyKnowledgeMapper;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.SafetyKnowledge;
import com.cupk.service.SafetyKnowledgeService;
import com.cupk.util.AccessUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class SafetyKnowledgeServiceImpl implements SafetyKnowledgeService {
    private final SafetyKnowledgeMapper knowledgeMapper;
    private final ExperimentMapper experimentMapper;
    private final LabCourseMapper courseMapper;

    public SafetyKnowledgeServiceImpl(SafetyKnowledgeMapper knowledgeMapper, ExperimentMapper experimentMapper,
                                      LabCourseMapper courseMapper) {
        this.knowledgeMapper = knowledgeMapper;
        this.experimentMapper = experimentMapper;
        this.courseMapper = courseMapper;
    }

    @Override
    public PageResult<SafetyKnowledge> page(SafetyKnowledgeQueryDTO dto) {
        LambdaQueryWrapper<SafetyKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(dto.getKeyword()), w -> w
                .like(SafetyKnowledge::getKnowledgePoint, dto.getKeyword())
                .or().like(SafetyKnowledge::getContent, dto.getKeyword()));
        wrapper.eq(dto.getExperimentId() != null, SafetyKnowledge::getExperimentId, dto.getExperimentId());
        wrapper.eq(StringUtils.hasText(dto.getCategory()), SafetyKnowledge::getCategory, dto.getCategory());
        wrapper.eq(StringUtils.hasText(dto.getRiskType()), SafetyKnowledge::getRiskType, dto.getRiskType());
        wrapper.eq(dto.getStatus() != null, SafetyKnowledge::getStatus, dto.getStatus());
        if (UserContext.isStudent()) {
            wrapper.eq(SafetyKnowledge::getStatus, 1);
        } else if (UserContext.isTeacher()) {
            List<Long> experimentIds = currentTeacherExperimentIds();
            if (experimentIds.isEmpty()) {
                wrapper.isNull(SafetyKnowledge::getExperimentId);
            } else {
                wrapper.and(w -> w.in(SafetyKnowledge::getExperimentId, experimentIds)
                        .or().isNull(SafetyKnowledge::getExperimentId));
            }
        }
        wrapper.orderByDesc(SafetyKnowledge::getCreateTime);
        Page<SafetyKnowledge> page = knowledgeMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional
    public Long create(SafetyKnowledgeCreateDTO dto) {
        assertKnowledgeWritable(dto.getExperimentId());
        SafetyKnowledge entity = new SafetyKnowledge();
        BeanUtils.copyProperties(dto, entity);
        applyDefaults(entity);
        knowledgeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, SafetyKnowledgeUpdateDTO dto) {
        SafetyKnowledge current = requireKnowledge(id);
        assertKnowledgeWritable(current.getExperimentId());
        assertKnowledgeWritable(dto.getExperimentId());
        BeanUtils.copyProperties(dto, current);
        applyDefaults(current);
        knowledgeMapper.updateById(current);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SafetyKnowledge current = requireKnowledge(id);
        assertKnowledgeWritable(current.getExperimentId());
        knowledgeMapper.deleteById(id);
    }

    private void assertKnowledgeWritable(Long experimentId) {
        AccessUtil.requireTeacherOrAdmin();
        if (experimentId == null) {
            if (UserContext.isTeacher()) {
                throw new BusinessException(403, "教师只能维护自己课程下的安全知识");
            }
            return;
        }
        Experiment experiment = requireExperiment(experimentId);
        AccessUtil.assertCourseWritable(requireCourse(experiment.getCourseId()));
    }

    private void applyDefaults(SafetyKnowledge entity) {
        if (!StringUtils.hasText(entity.getCategory())) {
            entity.setCategory("HSE_BASIC");
        }
        entity.setEmergencyFlag(entity.getEmergencyFlag() == null ? 0 : entity.getEmergencyFlag());
        entity.setStatus(entity.getStatus() == null ? 1 : entity.getStatus());
        if (Integer.valueOf(1).equals(entity.getEmergencyFlag()) || "EMERGENCY".equals(entity.getCategory())) {
            String notice = "涉及应急处理时，以学校正式制度和现场教师要求为准。";
            if (entity.getContent() != null && !entity.getContent().contains("学校正式制度")) {
                entity.setContent(entity.getContent() + "\n" + notice);
            }
        }
    }

    private List<Long> currentTeacherExperimentIds() {
        List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<LabCourse>()
                .select(LabCourse::getId)
                .eq(LabCourse::getTeacherId, UserContext.userId()))
                .stream().map(LabCourse::getId).toList();
        if (courseIds.isEmpty()) {
            return Collections.emptyList();
        }
        return experimentMapper.selectList(new LambdaQueryWrapper<Experiment>()
                .select(Experiment::getId)
                .in(Experiment::getCourseId, courseIds))
                .stream().map(Experiment::getId).toList();
    }

    private SafetyKnowledge requireKnowledge(Long id) {
        SafetyKnowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            throw new BusinessException(404, "安全知识不存在");
        }
        return knowledge;
    }

    private Experiment requireExperiment(Long id) {
        Experiment experiment = experimentMapper.selectById(id);
        if (experiment == null) {
            throw new BusinessException(404, "实验项目不存在");
        }
        return experiment;
    }

    private LabCourse requireCourse(Long id) {
        LabCourse course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        return course;
    }
}
