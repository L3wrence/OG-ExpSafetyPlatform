package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.PageResult;
import com.cupk.dto.ResourceCreateDTO;
import com.cupk.dto.ResourceQueryDTO;
import com.cupk.dto.ResourceUpdateDTO;
import com.cupk.exception.BusinessException;
import com.cupk.common.UserContext;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.TeachingResourceMapper;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.TeachingResource;
import com.cupk.service.ResourceService;
import com.cupk.util.AccessUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class ResourceServiceImpl implements ResourceService {
    private final TeachingResourceMapper resourceMapper;
    private final ExperimentMapper experimentMapper;
    private final LabCourseMapper courseMapper;

    public ResourceServiceImpl(TeachingResourceMapper resourceMapper, ExperimentMapper experimentMapper,
                               LabCourseMapper courseMapper) {
        this.resourceMapper = resourceMapper;
        this.experimentMapper = experimentMapper;
        this.courseMapper = courseMapper;
    }

    @Override
    public PageResult<TeachingResource> page(ResourceQueryDTO dto) {
        LambdaQueryWrapper<TeachingResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(dto.getKeyword()), w -> w
                .like(TeachingResource::getTitle, dto.getKeyword())
                .or().like(TeachingResource::getResourceType, dto.getKeyword()));
        wrapper.eq(dto.getExperimentId() != null, TeachingResource::getExperimentId, dto.getExperimentId());
        wrapper.eq(StringUtils.hasText(dto.getResourceType()), TeachingResource::getResourceType, dto.getResourceType());
        wrapper.eq(dto.getRequiredFlag() != null, TeachingResource::getRequiredFlag, dto.getRequiredFlag());
        wrapper.eq(dto.getStatus() != null, TeachingResource::getStatus, dto.getStatus());
        if (UserContext.isStudent()) {
            wrapper.eq(TeachingResource::getStatus, 1);
        } else if (UserContext.isTeacher()) {
            List<Long> experimentIds = currentTeacherExperimentIds();
            if (experimentIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, dto.getPageNum(), dto.getPageSize());
            }
            wrapper.in(TeachingResource::getExperimentId, experimentIds);
        }
        wrapper.orderByAsc(TeachingResource::getSort).orderByDesc(TeachingResource::getCreateTime);
        Page<TeachingResource> page = resourceMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional
    public Long create(ResourceCreateDTO dto) {
        Experiment experiment = requireExperiment(dto.getExperimentId());
        AccessUtil.assertCourseWritable(requireCourse(experiment.getCourseId()));
        TeachingResource entity = new TeachingResource();
        BeanUtils.copyProperties(dto, entity);
        entity.setUploadUserId(UserContext.userId());
        entity.setViewCount(0);
        resourceMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ResourceUpdateDTO dto) {
        TeachingResource current = requireResource(id);
        Experiment oldExperiment = requireExperiment(current.getExperimentId());
        AccessUtil.assertCourseWritable(requireCourse(oldExperiment.getCourseId()));
        Experiment targetExperiment = requireExperiment(dto.getExperimentId());
        AccessUtil.assertCourseWritable(requireCourse(targetExperiment.getCourseId()));
        BeanUtils.copyProperties(dto, current);
        resourceMapper.updateById(current);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TeachingResource current = requireResource(id);
        Experiment experiment = requireExperiment(current.getExperimentId());
        AccessUtil.assertCourseWritable(requireCourse(experiment.getCourseId()));
        resourceMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "状态只能为0或1");
        }
        TeachingResource current = requireResource(id);
        Experiment experiment = requireExperiment(current.getExperimentId());
        AccessUtil.assertCourseWritable(requireCourse(experiment.getCourseId()));
        current.setStatus(status);
        resourceMapper.updateById(current);
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

    private TeachingResource requireResource(Long id) {
        TeachingResource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException(404, "教学资源不存在");
        }
        return resource;
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
