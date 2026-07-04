package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.PageResult;
import com.cupk.dto.CourseCreateDTO;
import com.cupk.dto.CourseQueryDTO;
import com.cupk.dto.CourseUpdateDTO;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.TeachingResource;
import com.cupk.pojo.User;
import com.cupk.exception.BusinessException;
import com.cupk.common.UserContext;
import com.cupk.mapper.AuthMapper;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.LearningRecordMapper;
import com.cupk.mapper.TeachingResourceMapper;
import com.cupk.mapper.UserMapper;
import com.cupk.service.CourseService;
import com.cupk.util.AccessUtil;
import com.cupk.vo.CourseDetailVO;
import com.cupk.vo.CourseListVO;
import com.cupk.vo.ExperimentSimpleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {
    private final LabCourseMapper courseMapper;
    private final ExperimentMapper experimentMapper;
    private final TeachingResourceMapper resourceMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final UserMapper userMapper;
    private final AuthMapper authMapper;

    public CourseServiceImpl(LabCourseMapper courseMapper, ExperimentMapper experimentMapper,
                             TeachingResourceMapper resourceMapper, LearningRecordMapper learningRecordMapper,
                             UserMapper userMapper, AuthMapper authMapper) {
        this.courseMapper = courseMapper;
        this.experimentMapper = experimentMapper;
        this.resourceMapper = resourceMapper;
        this.learningRecordMapper = learningRecordMapper;
        this.userMapper = userMapper;
        this.authMapper = authMapper;
    }

    @Override
    @Transactional
    public Long create(CourseCreateDTO dto) {
        AccessUtil.requireTeacherOrAdmin();
        validateTeacher(dto.getTeacherId());
        checkCode(dto.getCourseCode(), null);
        if (UserContext.isTeacher() && !UserContext.userId().equals(dto.getTeacherId())) {
            throw new BusinessException(403, "教师只能创建自己负责的课程");
        }
        LabCourse course = new LabCourse();
        BeanUtils.copyProperties(dto, course);
        courseMapper.insert(course);
        return course.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CourseUpdateDTO dto) {
        LabCourse current = requireCourse(id);
        AccessUtil.assertCourseWritable(current);
        validateTeacher(dto.getTeacherId());
        if (UserContext.isTeacher() && !UserContext.userId().equals(dto.getTeacherId())) {
            throw new BusinessException(403, "教师不能把课程转交给其他教师");
        }
        checkCode(dto.getCourseCode(), id);
        BeanUtils.copyProperties(dto, current);
        courseMapper.updateById(current);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LabCourse course = requireCourse(id);
        AccessUtil.assertCourseWritable(course);
        Long experimentCount = experimentMapper.selectCount(new LambdaQueryWrapper<Experiment>()
                .eq(Experiment::getCourseId, id));
        if (experimentCount > 0) {
            throw new BusinessException(409, "课程下存在实验项目，请先关闭课程或处理实验项目");
        }
        courseMapper.deleteById(id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "状态只能为0或1");
        }
        LabCourse course = requireCourse(id);
        AccessUtil.assertCourseWritable(course);
        course.setStatus(status);
        courseMapper.updateById(course);
    }

    @Override
    public PageResult<CourseListVO> page(CourseQueryDTO dto) {
        LambdaQueryWrapper<LabCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(dto.getKeyword()), w -> w
                .like(LabCourse::getCourseName, dto.getKeyword())
                .or().like(LabCourse::getCourseCode, dto.getKeyword()));
        wrapper.eq(StringUtils.hasText(dto.getDirection()), LabCourse::getDirection, dto.getDirection());
        wrapper.eq(StringUtils.hasText(dto.getSemester()), LabCourse::getSemester, dto.getSemester());
        wrapper.eq(dto.getStatus() != null, LabCourse::getStatus, dto.getStatus());

        if (UserContext.isTeacher()) {
            wrapper.eq(LabCourse::getTeacherId, UserContext.userId());
        } else if (UserContext.isAdmin() && dto.getTeacherId() != null) {
            wrapper.eq(LabCourse::getTeacherId, dto.getTeacherId());
        } else if (UserContext.isStudent()) {
            wrapper.eq(LabCourse::getStatus, 1);
        }
        wrapper.orderByAsc(LabCourse::getSort).orderByDesc(LabCourse::getCreateTime);

        Page<LabCourse> page = courseMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        List<CourseListVO> records = page.getRecords().stream().map(this::toListVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public CourseDetailVO detail(Long id) {
        LabCourse course = requireCourse(id);
        if (UserContext.isStudent() && course.getStatus() != 1) {
            throw new BusinessException(403, "课程未开放");
        }
        if (UserContext.isTeacher()) {
            AccessUtil.assertCourseWritable(course);
        }
        List<Experiment> experiments = experimentMapper.selectList(new LambdaQueryWrapper<Experiment>()
                .eq(Experiment::getCourseId, id)
                .eq(UserContext.isStudent(), Experiment::getStatus, 1)
                .orderByAsc(Experiment::getSort));
        List<Long> experimentIds = experiments.stream().map(Experiment::getId).toList();
        long resourceCount = experimentIds.isEmpty() ? 0 : resourceMapper.selectCount(
                new LambdaQueryWrapper<TeachingResource>().in(TeachingResource::getExperimentId, experimentIds));

        CourseDetailVO vo = new CourseDetailVO();
        vo.setCourse(course);
        User teacher = userMapper.selectById(course.getTeacherId());
        vo.setTeacherName(teacher == null ? "未知教师" : teacher.getRealName());
        vo.setExperimentCount((long) experiments.size());
        vo.setResourceCount(resourceCount);
        vo.setAverageProgress(defaultDecimal(learningRecordMapper.selectCourseAverageProgress(id)));
        vo.setExperiments(experiments.stream().map(this::toSimpleVO).toList());
        return vo;
    }

    private CourseListVO toListVO(LabCourse course) {
        CourseListVO vo = new CourseListVO();
        BeanUtils.copyProperties(course, vo);
        User teacher = userMapper.selectById(course.getTeacherId());
        vo.setTeacherName(teacher == null ? "未知教师" : teacher.getRealName());
        List<Experiment> experiments = experimentMapper.selectList(new LambdaQueryWrapper<Experiment>()
                .select(Experiment::getId).eq(Experiment::getCourseId, course.getId()));
        vo.setExperimentCount(experiments.size());
        List<Long> ids = experiments.stream().map(Experiment::getId).toList();
        vo.setResourceCount(ids.isEmpty() ? 0 : Math.toIntExact(resourceMapper.selectCount(
                new LambdaQueryWrapper<TeachingResource>().in(TeachingResource::getExperimentId, ids))));
        vo.setAverageProgress(defaultDecimal(learningRecordMapper.selectCourseAverageProgress(course.getId())));
        return vo;
    }

    private ExperimentSimpleVO toSimpleVO(Experiment entity) {
        ExperimentSimpleVO vo = new ExperimentSimpleVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private void checkCode(String code, Long excludeId) {
        LambdaQueryWrapper<LabCourse> wrapper = new LambdaQueryWrapper<LabCourse>()
                .eq(LabCourse::getCourseCode, code)
                .ne(excludeId != null, LabCourse::getId, excludeId);
        if (courseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(409, "课程编号已存在");
        }
    }

    private void validateTeacher(Long teacherId) {
        User teacher = userMapper.selectById(teacherId);
        if (teacher == null || teacher.getStatus() != 1 || authMapper.countRole(teacherId, "TEACHER") == 0) {
            throw new BusinessException(400, "负责教师不存在或已停用");
        }
    }

    private LabCourse requireCourse(Long id) {
        LabCourse course = courseMapper.selectById(id);
        if (course == null) throw new BusinessException(404, "课程不存在");
        return course;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
