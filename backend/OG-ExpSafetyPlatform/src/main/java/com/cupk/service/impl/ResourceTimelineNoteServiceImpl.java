package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.dto.ResourceTimelineNoteDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.ResourceTimelineNoteMapper;
import com.cupk.mapper.TeachingResourceMapper;
import com.cupk.mapper.UserMapper;
import com.cupk.pojo.CourseStudent;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.ResourceTimelineNote;
import com.cupk.pojo.TeachingResource;
import com.cupk.pojo.User;
import com.cupk.service.ResourceTimelineNoteService;
import com.cupk.util.AccessUtil;
import com.cupk.vo.ResourceTimelineNoteVO;
import com.cupk.vo.ResourceTimelineStatsVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResourceTimelineNoteServiceImpl implements ResourceTimelineNoteService {
    private static final Set<String> NOTE_TYPES = Set.of("NOTE", "QUESTION", "RISK");
    private static final Set<String> VISIBILITIES = Set.of("PRIVATE", "COURSE");

    private final ResourceTimelineNoteMapper noteMapper;
    private final TeachingResourceMapper resourceMapper;
    private final ExperimentMapper experimentMapper;
    private final LabCourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final UserMapper userMapper;

    public ResourceTimelineNoteServiceImpl(ResourceTimelineNoteMapper noteMapper,
                                           TeachingResourceMapper resourceMapper,
                                           ExperimentMapper experimentMapper,
                                           LabCourseMapper courseMapper,
                                           CourseStudentMapper courseStudentMapper,
                                           UserMapper userMapper) {
        this.noteMapper = noteMapper;
        this.resourceMapper = resourceMapper;
        this.experimentMapper = experimentMapper;
        this.courseMapper = courseMapper;
        this.courseStudentMapper = courseStudentMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<ResourceTimelineNoteVO> listByResource(Long resourceId, Boolean mineOnly) {
        TeachingResource resource = requireReadableResource(resourceId);
        Long userId = UserContext.userId();
        LambdaQueryWrapper<ResourceTimelineNote> wrapper = new LambdaQueryWrapper<ResourceTimelineNote>()
                .eq(ResourceTimelineNote::getResourceId, resource.getId())
                .eq(ResourceTimelineNote::getStatus, 1)
                .orderByAsc(ResourceTimelineNote::getPositionSeconds)
                .orderByDesc(ResourceTimelineNote::getCreateTime);
        if (Boolean.TRUE.equals(mineOnly)) {
            wrapper.eq(ResourceTimelineNote::getUserId, userId);
        } else {
            wrapper.and(w -> w.eq(ResourceTimelineNote::getUserId, userId)
                    .or().eq(ResourceTimelineNote::getVisibility, "COURSE"));
        }
        return noteMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public Long create(ResourceTimelineNoteDTO dto) {
        TeachingResource resource = requireReadableResource(dto.getResourceId());
        String noteType = normalize(dto.getNoteType(), "NOTE");
        String visibility = normalize(dto.getVisibility(), "PRIVATE");
        if (!NOTE_TYPES.contains(noteType)) {
            throw new BusinessException(400, "不支持的时间点类型");
        }
        if (!VISIBILITIES.contains(visibility)) {
            throw new BusinessException(400, "不支持的可见范围");
        }
        ResourceTimelineNote note = new ResourceTimelineNote();
        note.setResourceId(resource.getId());
        note.setExperimentId(dto.getExperimentId() != null ? dto.getExperimentId() : resource.getExperimentId());
        note.setUserId(UserContext.userId());
        note.setPositionSeconds(dto.getPositionSeconds() == null ? 0 : Math.max(0, dto.getPositionSeconds()));
        note.setNoteType(noteType);
        note.setVisibility(visibility);
        note.setContent(dto.getContent().trim());
        note.setStatus(1);
        noteMapper.insert(note);
        return note.getId();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ResourceTimelineNote note = noteMapper.selectById(id);
        if (note == null) {
            throw new BusinessException(404, "时间点记录不存在");
        }
        if (!UserContext.userId().equals(note.getUserId())) {
            TeachingResource resource = requireResource(note.getResourceId());
            assertWritable(resource);
        }
        noteMapper.deleteById(id);
    }

    @Override
    public List<ResourceTimelineStatsVO> hotspots(Long experimentId) {
        assertExperimentWritable(experimentId);
        List<ResourceTimelineNote> notes = noteMapper.selectList(new LambdaQueryWrapper<ResourceTimelineNote>()
                .eq(ResourceTimelineNote::getExperimentId, experimentId)
                .eq(ResourceTimelineNote::getStatus, 1)
                .eq(ResourceTimelineNote::getVisibility, "COURSE")
                .orderByDesc(ResourceTimelineNote::getCreateTime));
        Map<Long, List<ResourceTimelineNote>> grouped = notes.stream()
                .collect(Collectors.groupingBy(ResourceTimelineNote::getResourceId, LinkedHashMap::new, Collectors.toList()));
        List<ResourceTimelineStatsVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<ResourceTimelineNote>> entry : grouped.entrySet()) {
            TeachingResource resource = resourceMapper.selectById(entry.getKey());
            ResourceTimelineStatsVO vo = new ResourceTimelineStatsVO();
            vo.setResourceId(entry.getKey());
            vo.setResourceTitle(resource == null ? "资源 " + entry.getKey() : resource.getTitle());
            vo.setExperimentId(experimentId);
            vo.setNoteCount(entry.getValue().size());
            vo.setQuestionCount((int) entry.getValue().stream().filter(item -> "QUESTION".equals(item.getNoteType())).count());
            vo.setRiskCount((int) entry.getValue().stream().filter(item -> "RISK".equals(item.getNoteType())).count());
            vo.setLatestQuestion(entry.getValue().stream()
                    .filter(item -> "QUESTION".equals(item.getNoteType()))
                    .map(ResourceTimelineNote::getContent)
                    .findFirst().orElse(""));
            result.add(vo);
        }
        result.sort(Comparator.comparing(ResourceTimelineStatsVO::getQuestionCount).reversed()
                .thenComparing(ResourceTimelineStatsVO::getRiskCount, Comparator.reverseOrder()));
        return result;
    }

    private ResourceTimelineNoteVO toVO(ResourceTimelineNote note) {
        ResourceTimelineNoteVO vo = new ResourceTimelineNoteVO();
        BeanUtils.copyProperties(note, vo);
        User user = userMapper.selectById(note.getUserId());
        vo.setUserName(user == null ? "学习者" : user.getRealName());
        return vo;
    }

    private TeachingResource requireReadableResource(Long resourceId) {
        TeachingResource resource = requireResource(resourceId);
        if (UserContext.isTeacher() && canWrite(resource)) {
            return resource;
        }
        if (UserContext.isLearner()) {
            if (!Integer.valueOf(1).equals(resource.getStatus()) || Integer.valueOf(1).equals(resource.getInvalidFlag())) {
                throw new BusinessException(403, "资源未开放或已失效");
            }
            Long courseId = resource.getCourseId();
            if (courseId != null
                    && !"PUBLIC".equalsIgnoreCase(resource.getOpenScope())
                    && courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, courseId)
                    .eq(CourseStudent::getStudentId, UserContext.userId())
                    .eq(CourseStudent::getStatus, 1)) == 0) {
                throw new BusinessException(403, "无权访问该课程资源");
            }
        } else if (UserContext.isTeacher()) {
            assertWritable(resource);
        }
        return resource;
    }

    private boolean canWrite(TeachingResource resource) {
        try {
            assertWritable(resource);
            return true;
        } catch (BusinessException e) {
            if (e.getCode() != null && e.getCode() == 403) {
                return false;
            }
            throw e;
        }
    }

    private TeachingResource requireResource(Long resourceId) {
        TeachingResource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException(404, "教学资源不存在");
        }
        return resource;
    }

    private void assertWritable(TeachingResource resource) {
        assertExperimentWritable(resource.getExperimentId());
    }

    private void assertExperimentWritable(Long experimentId) {
        Experiment experiment = experimentMapper.selectById(experimentId);
        if (experiment == null) {
            throw new BusinessException(404, "实验项目不存在");
        }
        LabCourse course = courseMapper.selectById(experiment.getCourseId());
        AccessUtil.assertCourseWritable(course);
    }

    private String normalize(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : fallback;
    }
}
