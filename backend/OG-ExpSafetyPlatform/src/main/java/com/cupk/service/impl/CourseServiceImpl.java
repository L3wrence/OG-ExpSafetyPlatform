package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.PageResult;
import com.cupk.dto.CourseCreateDTO;
import com.cupk.dto.CourseQueryDTO;
import com.cupk.dto.CourseStudentImportDTO;
import com.cupk.dto.CourseStudentImportItemDTO;
import com.cupk.dto.CourseStudentRemoveDTO;
import com.cupk.dto.CourseUpdateDTO;
import com.cupk.dto.TeachingClassCreateDTO;
import com.cupk.dto.TeachingClassUpdateDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.AuthMapper;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.ExamPaperMapper;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.LearningRecordMapper;
import com.cupk.mapper.OperationLogMapper;
import com.cupk.mapper.ReportMapper;
import com.cupk.mapper.ReservationMapper;
import com.cupk.mapper.RoleMapper;
import com.cupk.mapper.TeachingClassMapper;
import com.cupk.mapper.TeachingResourceMapper;
import com.cupk.mapper.UserMapper;
import com.cupk.mapper.UserRoleMapper;
import com.cupk.pojo.CourseStudent;
import com.cupk.pojo.ExamPaper;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.OperationLog;
import com.cupk.pojo.Report;
import com.cupk.pojo.Reservation;
import com.cupk.pojo.Role;
import com.cupk.pojo.TeachingClass;
import com.cupk.pojo.TeachingResource;
import com.cupk.pojo.User;
import com.cupk.pojo.UserRole;
import com.cupk.service.CourseService;
import com.cupk.util.AccessUtil;
import com.cupk.vo.CourseDetailVO;
import com.cupk.vo.CourseListVO;
import com.cupk.vo.CourseStudentImportFailureVO;
import com.cupk.vo.CourseStudentImportResultVO;
import com.cupk.vo.CourseStudentVO;
import com.cupk.vo.ExperimentSimpleVO;
import com.cupk.vo.TeachingClassVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CourseServiceImpl implements CourseService {
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_ARCHIVED = 2;

    private final LabCourseMapper courseMapper;
    private final ExperimentMapper experimentMapper;
    private final TeachingResourceMapper resourceMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final UserMapper userMapper;
    private final AuthMapper authMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final ExamPaperMapper examPaperMapper;
    private final ReservationMapper reservationMapper;
    private final ReportMapper reportMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final OperationLogMapper operationLogMapper;

    public CourseServiceImpl(LabCourseMapper courseMapper, ExperimentMapper experimentMapper,
                             TeachingResourceMapper resourceMapper, LearningRecordMapper learningRecordMapper,
                             UserMapper userMapper, AuthMapper authMapper, TeachingClassMapper teachingClassMapper,
                             CourseStudentMapper courseStudentMapper, ExamPaperMapper examPaperMapper,
                             ReservationMapper reservationMapper, ReportMapper reportMapper, RoleMapper roleMapper,
                             UserRoleMapper userRoleMapper, OperationLogMapper operationLogMapper) {
        this.courseMapper = courseMapper;
        this.experimentMapper = experimentMapper;
        this.resourceMapper = resourceMapper;
        this.learningRecordMapper = learningRecordMapper;
        this.userMapper = userMapper;
        this.authMapper = authMapper;
        this.teachingClassMapper = teachingClassMapper;
        this.courseStudentMapper = courseStudentMapper;
        this.examPaperMapper = examPaperMapper;
        this.reservationMapper = reservationMapper;
        this.reportMapper = reportMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.operationLogMapper = operationLogMapper;
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
        course.setAllowEmptyPublish(flag(dto.getAllowEmptyPublish()));
        if (dto.getStatus() != null && dto.getStatus() == STATUS_PUBLISHED) {
            if (course.getAllowEmptyPublish() != 1) {
                throw new BusinessException(400, "新课程没有实验项目，需保存为草稿或允许空课程发布");
            }
            course.setStatus(STATUS_PUBLISHED);
        } else {
            course.setStatus(STATUS_DRAFT);
        }
        courseMapper.insert(course);
        log("CREATE", "创建课程：" + course.getCourseName(), "SUCCESS");
        return course.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CourseUpdateDTO dto) {
        LabCourse current = requireCourse(id);
        assertCourseManageable(current);
        assertCourseMutable(current);
        validateTeacher(dto.getTeacherId());
        if (UserContext.isTeacher() && !UserContext.userId().equals(dto.getTeacherId())) {
            throw new BusinessException(403, "教师不能把课程转交给其他教师");
        }
        if (dto.getStatus() != null && dto.getStatus() == STATUS_ARCHIVED) {
            throw new BusinessException(400, "课程归档请使用归档接口");
        }
        checkCode(dto.getCourseCode(), id);
        boolean publishRequested = dto.getStatus() != null && dto.getStatus() == STATUS_PUBLISHED && current.getStatus() != STATUS_PUBLISHED;
        BeanUtils.copyProperties(dto, current);
        current.setAllowEmptyPublish(flag(dto.getAllowEmptyPublish()));
        if (publishRequested) {
            long experimentCount = experimentMapper.selectCount(new LambdaQueryWrapper<Experiment>()
                    .eq(Experiment::getCourseId, id));
            if (experimentCount == 0 && current.getAllowEmptyPublish() != 1) {
                throw new BusinessException(400, "课程发布前必须至少包含一个实验项目，或明确允许空课程发布");
            }
        }
        courseMapper.updateById(current);
        log("UPDATE", "修改课程：" + current.getCourseName(), "SUCCESS");
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LabCourse course = requireCourse(id);
        assertCourseManageable(course);
        assertCourseMutable(course);
        List<Long> experimentIds = selectExperimentIds(id);
        if (hasCoreBusiness(id, experimentIds)) {
            throw new BusinessException(409, "课程存在考试、预约或报告数据，请归档课程，不要删除");
        }
        if (!experimentIds.isEmpty()) {
            throw new BusinessException(409, "课程下存在实验项目，请先处理实验项目或归档课程");
        }
        courseMapper.deleteById(id);
        log("DELETE", "删除课程：" + course.getCourseName(), "SUCCESS");
    }

    @Override
    @Transactional
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_DRAFT && status != STATUS_PUBLISHED && status != STATUS_ARCHIVED)) {
            throw new BusinessException(400, "状态只能为0、1或2");
        }
        if (status == STATUS_PUBLISHED) {
            publish(id, false);
            return;
        }
        if (status == STATUS_ARCHIVED) {
            archive(id);
            return;
        }
        LabCourse course = requireCourse(id);
        assertCourseManageable(course);
        assertCourseMutable(course);
        course.setStatus(STATUS_DRAFT);
        courseMapper.updateById(course);
        log("STATUS", "课程设为草稿：" + course.getCourseName(), "SUCCESS");
    }

    @Override
    @Transactional
    public void publish(Long id, Boolean allowEmpty) {
        LabCourse course = requireCourse(id);
        assertCourseManageable(course);
        assertCourseMutable(course);
        long experimentCount = experimentMapper.selectCount(new LambdaQueryWrapper<Experiment>()
                .eq(Experiment::getCourseId, id));
        boolean explicitAllowEmpty = Boolean.TRUE.equals(allowEmpty) || flag(course.getAllowEmptyPublish()) == 1;
        if (experimentCount == 0 && !explicitAllowEmpty) {
            throw new BusinessException(400, "课程发布前必须至少包含一个实验项目，或明确允许空课程发布");
        }
        if (Boolean.TRUE.equals(allowEmpty)) {
            course.setAllowEmptyPublish(1);
        }
        course.setStatus(STATUS_PUBLISHED);
        course.setArchiveTime(null);
        courseMapper.updateById(course);
        log("PUBLISH", "发布课程：" + course.getCourseName(), "SUCCESS");
    }

    @Override
    @Transactional
    public void archive(Long id) {
        LabCourse course = requireCourse(id);
        assertCourseManageable(course);
        course.setStatus(STATUS_ARCHIVED);
        course.setArchiveTime(LocalDateTime.now());
        courseMapper.updateById(course);
        log("ARCHIVE", "归档课程：" + course.getCourseName(), "SUCCESS");
    }

    @Override
    @Transactional
    public Long copy(Long id) {
        LabCourse source = requireCourse(id);
        assertCourseManageable(source);
        LabCourse copy = new LabCourse();
        BeanUtils.copyProperties(source, copy);
        copy.setId(null);
        copy.setCourseName(source.getCourseName() + " 副本");
        copy.setCourseCode(nextCopyCode(source.getCourseCode()));
        copy.setStatus(STATUS_DRAFT);
        copy.setArchiveTime(null);
        courseMapper.insert(copy);

        List<Experiment> experiments = experimentMapper.selectList(new LambdaQueryWrapper<Experiment>()
                .eq(Experiment::getCourseId, id)
                .orderByAsc(Experiment::getSort));
        for (Experiment experiment : experiments) {
            Experiment item = new Experiment();
            BeanUtils.copyProperties(experiment, item);
            item.setId(null);
            item.setCourseId(copy.getId());
            item.setStatus(STATUS_DRAFT);
            experimentMapper.insert(item);
        }
        log("COPY", "复制课程：" + source.getCourseName() + " -> " + copy.getCourseName(), "SUCCESS");
        return copy.getId();
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
            List<Long> authorizedCourseIds = teacherAuthorizedCourseIds(UserContext.userId());
            wrapper.and(w -> {
                w.eq(LabCourse::getTeacherId, UserContext.userId());
                if (!authorizedCourseIds.isEmpty()) {
                    w.or().in(LabCourse::getId, authorizedCourseIds);
                }
            });
        } else if (UserContext.isAdmin() && dto.getTeacherId() != null) {
            wrapper.eq(LabCourse::getTeacherId, dto.getTeacherId());
        } else if (UserContext.isStudent()) {
            List<Long> courseIds = studentCourseIds(UserContext.userId());
            if (courseIds.isEmpty()) {
                wrapper.eq(LabCourse::getId, -1L);
            } else {
                wrapper.in(LabCourse::getId, courseIds).eq(LabCourse::getStatus, STATUS_PUBLISHED);
            }
        }
        wrapper.orderByAsc(LabCourse::getSort).orderByDesc(LabCourse::getCreateTime);

        Page<LabCourse> page = courseMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        List<CourseListVO> records = page.getRecords().stream().map(this::toListVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public CourseDetailVO detail(Long id) {
        LabCourse course = requireCourse(id);
        if (UserContext.isStudent()) {
            assertStudentEnrolled(id);
            if (!STATUS_PUBLISHED_EQUALS(course)) {
                throw new BusinessException(403, "课程未开放");
            }
        }
        if (UserContext.isTeacher()) {
            assertCourseManageable(course);
        }
        List<Experiment> experiments = experimentMapper.selectList(new LambdaQueryWrapper<Experiment>()
                .eq(Experiment::getCourseId, id)
                .eq(UserContext.isStudent(), Experiment::getStatus, STATUS_PUBLISHED)
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
        vo.setLearningRequirement(course.getLearningRequirement());
        vo.setTeachingClasses(listClasses(id));
        vo.setStudents(UserContext.isStudent() ? Collections.emptyList() : listStudents(id, null, null, null));
        vo.setTeachingClassCount(vo.getTeachingClasses().size());
        vo.setStudentCount(Math.toIntExact(courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, id)
                .eq(CourseStudent::getStatus, STATUS_PUBLISHED))));
        vo.setAnnouncements(defaultAnnouncements(course));
        return vo;
    }

    @Override
    public List<TeachingClassVO> listClasses(Long courseId) {
        LabCourse course = requireCourse(courseId);
        assertCourseReadable(course);
        return teachingClassMapper.selectList(new LambdaQueryWrapper<TeachingClass>()
                        .eq(TeachingClass::getCourseId, courseId)
                        .orderByAsc(TeachingClass::getId))
                .stream().map(this::toClassVO).toList();
    }

    @Override
    @Transactional
    public Long createClass(Long courseId, TeachingClassCreateDTO dto) {
        LabCourse course = requireCourse(courseId);
        assertCourseManageable(course);
        assertCourseMutable(course);
        validateTeacher(dto.getTeacherId());
        validateAssistant(dto.getAssistantId());
        TeachingClass entity = new TeachingClass();
        BeanUtils.copyProperties(dto, entity);
        entity.setCourseId(courseId);
        entity.setSemester(StringUtils.hasText(dto.getSemester()) ? dto.getSemester() : course.getSemester());
        entity.setStatus(dto.getStatus() == null ? STATUS_PUBLISHED : dto.getStatus());
        teachingClassMapper.insert(entity);
        log("CLASS_CREATE", "创建教学班：" + entity.getClassName(), "SUCCESS");
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateClass(Long courseId, Long classId, TeachingClassUpdateDTO dto) {
        LabCourse course = requireCourse(courseId);
        assertCourseManageable(course);
        assertCourseMutable(course);
        TeachingClass entity = requireClass(courseId, classId);
        validateTeacher(dto.getTeacherId());
        validateAssistant(dto.getAssistantId());
        BeanUtils.copyProperties(dto, entity);
        teachingClassMapper.updateById(entity);
        log("CLASS_UPDATE", "修改教学班：" + entity.getClassName(), "SUCCESS");
    }

    @Override
    @Transactional
    public void deleteClass(Long courseId, Long classId) {
        LabCourse course = requireCourse(courseId);
        assertCourseManageable(course);
        assertCourseMutable(course);
        TeachingClass entity = requireClass(courseId, classId);
        Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getTeachingClassId, classId));
        if (count > 0) {
            throw new BusinessException(409, "教学班下存在学生，请先移出学生");
        }
        teachingClassMapper.deleteById(classId);
        log("CLASS_DELETE", "删除教学班：" + entity.getClassName(), "SUCCESS");
    }

    @Override
    public List<CourseStudentVO> listStudents(Long courseId, Long teachingClassId, String keyword, String groupName) {
        LabCourse course = requireCourse(courseId);
        assertCourseReadable(course);
        if (teachingClassId != null) {
            requireClass(courseId, teachingClassId);
        }
        LambdaQueryWrapper<CourseStudent> wrapper = new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(teachingClassId != null, CourseStudent::getTeachingClassId, teachingClassId)
                .eq(StringUtils.hasText(groupName), CourseStudent::getGroupName, groupName)
                .orderByDesc(CourseStudent::getJoinTime);
        return courseStudentMapper.selectList(wrapper).stream()
                .map(this::toStudentVO)
                .filter(item -> !StringUtils.hasText(keyword)
                        || contains(item.getUsername(), keyword)
                        || contains(item.getRealName(), keyword)
                        || contains(item.getClassName(), keyword))
                .toList();
    }

    @Override
    @Transactional
    public CourseStudentImportResultVO importStudents(Long courseId, CourseStudentImportDTO dto) {
        LabCourse course = requireCourse(courseId);
        assertCourseManageable(course);
        assertCourseMutable(course);
        TeachingClass teachingClass = dto.getTeachingClassId() == null ? null : requireClass(courseId, dto.getTeachingClassId());
        CourseStudentImportResultVO result = new CourseStudentImportResultVO();
        Set<String> seen = new HashSet<>();
        Role studentRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, "STUDENT").last("LIMIT 1"));
        if (studentRole == null) {
            throw new BusinessException(500, "系统缺少学生角色，无法导入名单");
        }

        for (int i = 0; i < dto.getStudents().size(); i++) {
            CourseStudentImportItemDTO item = dto.getStudents().get(i);
            int rowIndex = i + 1;
            String username = trim(item.getUsername());
            if (!StringUtils.hasText(username)) {
                addFailure(result, rowIndex, username, "学号为空");
                continue;
            }
            if (!seen.add(username)) {
                addFailure(result, rowIndex, username, "导入名单中学号重复");
                continue;
            }

            User student = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("LIMIT 1"));
            if (student == null) {
                student = createStudentUser(item, username, studentRole.getId());
            } else if (student.getStatus() == null || student.getStatus() != 1) {
                addFailure(result, rowIndex, username, "账号已停用");
                continue;
            } else if (authMapper.countRole(student.getId(), "STUDENT") == 0) {
                addFailure(result, rowIndex, username, "账号不是学生角色");
                continue;
            }

            Long exists = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, courseId)
                    .eq(CourseStudent::getStudentId, student.getId()));
            if (exists > 0) {
                addFailure(result, rowIndex, username, "学生已在课程名单中");
                continue;
            }
            CourseStudent relation = new CourseStudent();
            relation.setCourseId(courseId);
            relation.setTeachingClassId(teachingClass == null ? null : teachingClass.getId());
            relation.setStudentId(student.getId());
            relation.setSemester(StringUtils.hasText(course.getSemester())
                    ? course.getSemester()
                    : (teachingClass == null ? null : teachingClass.getSemester()));
            relation.setGroupName(StringUtils.hasText(item.getGroupName()) ? item.getGroupName() : dto.getDefaultGroupName());
            relation.setStatus(STATUS_PUBLISHED);
            relation.setJoinTime(LocalDateTime.now());
            courseStudentMapper.insert(relation);
            result.setSuccessCount(result.getSuccessCount() + 1);
        }
        log("STUDENT_IMPORT", "导入课程学生：" + course.getCourseName() + "，成功" + result.getSuccessCount()
                + "，失败" + result.getFailCount(), "SUCCESS");
        return result;
    }

    @Override
    @Transactional
    public void removeStudents(Long courseId, CourseStudentRemoveDTO dto) {
        LabCourse course = requireCourse(courseId);
        assertCourseManageable(course);
        assertCourseMutable(course);
        for (Long studentId : dto.getStudentIds()) {
            courseStudentMapper.delete(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, courseId)
                    .eq(CourseStudent::getStudentId, studentId));
        }
        log("STUDENT_REMOVE", "移出课程学生：" + course.getCourseName() + "，人数" + dto.getStudentIds().size(), "SUCCESS");
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
        vo.setTeachingClassCount(Math.toIntExact(teachingClassMapper.selectCount(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getCourseId, course.getId()))));
        vo.setStudentCount(Math.toIntExact(courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, course.getId())
                .eq(CourseStudent::getStatus, STATUS_PUBLISHED))));
        return vo;
    }

    private TeachingClassVO toClassVO(TeachingClass entity) {
        TeachingClassVO vo = new TeachingClassVO();
        BeanUtils.copyProperties(entity, vo);
        User teacher = userMapper.selectById(entity.getTeacherId());
        User assistant = entity.getAssistantId() == null ? null : userMapper.selectById(entity.getAssistantId());
        vo.setTeacherName(teacher == null ? "未知教师" : teacher.getRealName());
        vo.setAssistantName(assistant == null ? null : assistant.getRealName());
        vo.setStudentCount(Math.toIntExact(courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, entity.getCourseId())
                .eq(CourseStudent::getTeachingClassId, entity.getId())
                .eq(CourseStudent::getStatus, STATUS_PUBLISHED))));
        return vo;
    }

    private CourseStudentVO toStudentVO(CourseStudent relation) {
        CourseStudentVO vo = new CourseStudentVO();
        BeanUtils.copyProperties(relation, vo);
        User student = userMapper.selectById(relation.getStudentId());
        if (student != null) {
            vo.setUsername(student.getUsername());
            vo.setRealName(student.getRealName());
            vo.setMajor(student.getMajor());
            vo.setClassName(student.getClassName());
            vo.setPhone(student.getPhone());
        }
        if (relation.getTeachingClassId() != null) {
            TeachingClass teachingClass = teachingClassMapper.selectById(relation.getTeachingClassId());
            if (teachingClass != null) {
                vo.setTeachingClassName(teachingClass.getClassName());
            }
        }
        return vo;
    }

    private ExperimentSimpleVO toSimpleVO(Experiment entity) {
        ExperimentSimpleVO vo = new ExperimentSimpleVO();
        BeanUtils.copyProperties(entity, vo);
        if (UserContext.isStudent()) {
            vo.setLearningProgress(learningRecordMapper.selectExperimentProgress(entity.getId(), UserContext.userId()));
        }
        return vo;
    }

    private User createStudentUser(CourseStudentImportItemDTO item, String username, Long studentRoleId) {
        User student = new User();
        student.setUsername(username);
        student.setPassword(md5("123456"));
        student.setRealName(StringUtils.hasText(item.getRealName()) ? item.getRealName() : username);
        student.setMajor(item.getMajor());
        student.setClassName(item.getClassName());
        student.setPhone(item.getPhone());
        student.setStatus(1);
        student.setCreateTime(LocalDateTime.now());
        student.setUpdateTime(LocalDateTime.now());
        userMapper.insert(student);
        UserRole userRole = new UserRole();
        userRole.setUserId(student.getId());
        userRole.setRoleId(studentRoleId);
        userRoleMapper.insert(userRole);
        return student;
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

    private void validateAssistant(Long assistantId) {
        if (assistantId == null) {
            return;
        }
        User assistant = userMapper.selectById(assistantId);
        if (assistant == null || assistant.getStatus() != 1 || authMapper.countRole(assistantId, "TEACHER") == 0) {
            throw new BusinessException(400, "助教不存在、已停用或不是教师角色");
        }
    }

    private LabCourse requireCourse(Long id) {
        LabCourse course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        return course;
    }

    private TeachingClass requireClass(Long courseId, Long classId) {
        TeachingClass entity = teachingClassMapper.selectById(classId);
        if (entity == null || !courseId.equals(entity.getCourseId())) {
            throw new BusinessException(404, "教学班不存在");
        }
        return entity;
    }

    private void assertCourseReadable(LabCourse course) {
        if (UserContext.isStudent()) {
            assertStudentEnrolled(course.getId());
            return;
        }
        if (UserContext.isTeacher()) {
            assertCourseManageable(course);
        }
    }

    private void assertCourseManageable(LabCourse course) {
        AccessUtil.requireTeacherOrAdmin();
        if (UserContext.isAdmin() || UserContext.isLabAdmin()) {
            return;
        }
        if (UserContext.userId().equals(course.getTeacherId())) {
            return;
        }
        boolean authorizedByClass = teachingClassMapper.selectCount(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getCourseId, course.getId())
                .and(w -> w.eq(TeachingClass::getTeacherId, UserContext.userId())
                        .or().eq(TeachingClass::getAssistantId, UserContext.userId()))) > 0;
        if (!authorizedByClass) {
            throw new BusinessException(403, "教师只能维护本人负责或被授权的课程");
        }
    }

    private void assertCourseMutable(LabCourse course) {
        if (course.getStatus() != null && course.getStatus() == STATUS_ARCHIVED) {
            throw new BusinessException(409, "课程已归档，只允许查看，不允许修改名单、教学班、考试或预约");
        }
    }

    private void assertStudentEnrolled(Long courseId) {
        Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, UserContext.userId())
                .eq(CourseStudent::getStatus, STATUS_PUBLISHED));
        if (count == 0) {
            throw new BusinessException(403, "只能查看本人课程");
        }
    }

    private List<Long> teacherAuthorizedCourseIds(Long teacherId) {
        return teachingClassMapper.selectList(new LambdaQueryWrapper<TeachingClass>()
                        .select(TeachingClass::getCourseId)
                        .and(w -> w.eq(TeachingClass::getTeacherId, teacherId)
                                .or().eq(TeachingClass::getAssistantId, teacherId)))
                .stream().map(TeachingClass::getCourseId).distinct().toList();
    }

    private List<Long> studentCourseIds(Long studentId) {
        return courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                        .select(CourseStudent::getCourseId)
                        .eq(CourseStudent::getStudentId, studentId)
                        .eq(CourseStudent::getStatus, STATUS_PUBLISHED))
                .stream().map(CourseStudent::getCourseId).distinct().toList();
    }

    private List<Long> selectExperimentIds(Long courseId) {
        return experimentMapper.selectList(new LambdaQueryWrapper<Experiment>()
                        .select(Experiment::getId)
                        .eq(Experiment::getCourseId, courseId))
                .stream().map(Experiment::getId).toList();
    }

    private boolean hasCoreBusiness(Long courseId, List<Long> experimentIds) {
        if (examPaperMapper.selectCount(new LambdaQueryWrapper<ExamPaper>().eq(ExamPaper::getCourseId, courseId)) > 0) {
            return true;
        }
        if (experimentIds.isEmpty()) {
            return false;
        }
        return reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>().in(Reservation::getExperimentId, experimentIds)) > 0
                || reportMapper.selectCount(new LambdaQueryWrapper<Report>().in(Report::getExperimentId, experimentIds)) > 0;
    }

    private String nextCopyCode(String sourceCode) {
        String prefix = sourceCode + "_COPY";
        String code = prefix;
        int index = 1;
        while (courseMapper.selectCount(new LambdaQueryWrapper<LabCourse>().eq(LabCourse::getCourseCode, code)) > 0) {
            index++;
            code = prefix + index;
        }
        return code;
    }

    private void addFailure(CourseStudentImportResultVO result, Integer rowIndex, String username, String reason) {
        result.setFailCount(result.getFailCount() + 1);
        result.getFailures().add(new CourseStudentImportFailureVO(rowIndex, username, reason));
    }

    private List<String> defaultAnnouncements(LabCourse course) {
        List<String> announcements = new ArrayList<>();
        if (!StringUtils.hasText(course.getLearningRequirement())) {
            announcements.add("请在课程详情中维护学习要求。");
        }
        if (course.getStatus() != null && course.getStatus() == STATUS_ARCHIVED) {
            announcements.add("课程已归档，仅供历史查看。");
        }
        return announcements;
    }

    private boolean contains(String value, String keyword) {
        return StringUtils.hasText(value) && value.contains(keyword);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private int flag(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean STATUS_PUBLISHED_EQUALS(LabCourse course) {
        return course.getStatus() != null && course.getStatus() == STATUS_PUBLISHED;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }

    private void log(String action, String content, String result) {
        OperationLog log = new OperationLog();
        log.setUserId(UserContext.userId());
        User user = userMapper.selectById(UserContext.userId());
        log.setUsername(user == null ? null : user.getUsername());
        log.setModule("COURSE_ORGANIZATION");
        log.setAction(action);
        log.setContent(content);
        log.setResult(result);
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
