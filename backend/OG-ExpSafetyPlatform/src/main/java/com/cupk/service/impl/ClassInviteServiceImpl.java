package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.dto.ClassInviteCreateDTO;
import com.cupk.dto.ClassJoinDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.ClassInviteMapper;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.TeachingClassMapper;
import com.cupk.pojo.ClassInvite;
import com.cupk.pojo.CourseStudent;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.TeachingClass;
import com.cupk.service.ClassInviteService;
import com.cupk.util.AccessUtil;
import com.cupk.vo.ClassInviteVO;
import com.cupk.vo.CourseJoinVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClassInviteServiceImpl implements ClassInviteService {
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClassInviteMapper inviteMapper;
    private final LabCourseMapper courseMapper;
    private final TeachingClassMapper classMapper;
    private final CourseStudentMapper courseStudentMapper;

    public ClassInviteServiceImpl(ClassInviteMapper inviteMapper, LabCourseMapper courseMapper,
                                  TeachingClassMapper classMapper, CourseStudentMapper courseStudentMapper) {
        this.inviteMapper = inviteMapper;
        this.courseMapper = courseMapper;
        this.classMapper = classMapper;
        this.courseStudentMapper = courseStudentMapper;
    }

    @Override
    @Transactional
    public Long create(Long courseId, ClassInviteCreateDTO dto) {
        LabCourse course = requireManageableCourse(courseId);
        TeachingClass teachingClass = dto.getTeachingClassId() == null ? null : requireClass(courseId, dto.getTeachingClassId());
        ClassInvite invite = new ClassInvite();
        invite.setCourseId(course.getId());
        invite.setTeachingClassId(teachingClass == null ? null : teachingClass.getId());
        invite.setInviteCode(uniqueCode());
        invite.setExpireTime(dto.getExpireTime());
        invite.setMaxUses(dto.getMaxUses());
        invite.setUsedCount(0);
        invite.setStatus(1);
        invite.setCreatedBy(UserContext.userId());
        inviteMapper.insert(invite);
        return invite.getId();
    }

    @Override
    public List<ClassInviteVO> list(Long courseId) {
        requireManageableCourse(courseId);
        return inviteMapper.selectList(new LambdaQueryWrapper<ClassInvite>()
                .eq(ClassInvite::getCourseId, courseId)
                .orderByDesc(ClassInvite::getCreateTime))
                .stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public void disable(Long inviteId) {
        ClassInvite invite = requireInvite(inviteId);
        requireManageableCourse(invite.getCourseId());
        invite.setStatus(0);
        inviteMapper.updateById(invite);
    }

    @Override
    @Transactional
    public CourseJoinVO join(ClassJoinDTO dto) {
        ClassInvite invite = inviteMapper.selectOne(new LambdaQueryWrapper<ClassInvite>()
                .eq(ClassInvite::getInviteCode, dto.getInviteCode().trim().toUpperCase())
                .eq(ClassInvite::getStatus, 1)
                .last("LIMIT 1"));
        if (invite == null) {
            throw new BusinessException(404, "邀请码不存在或已停用");
        }
        if (invite.getExpireTime() != null && invite.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(409, "邀请码已过期");
        }
        if (invite.getMaxUses() != null && invite.getUsedCount() != null && invite.getUsedCount() >= invite.getMaxUses()) {
            throw new BusinessException(409, "邀请码使用次数已满");
        }
        LabCourse course = courseMapper.selectById(invite.getCourseId());
        if (course == null || !Integer.valueOf(1).equals(course.getStatus())) {
            throw new BusinessException(403, "课堂未开放，暂不能加入");
        }
        Long exists = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, invite.getCourseId())
                .eq(CourseStudent::getStudentId, UserContext.userId())
                .eq(CourseStudent::getStatus, 1));
        if (exists == 0) {
            CourseStudent relation = new CourseStudent();
            relation.setCourseId(invite.getCourseId());
            relation.setTeachingClassId(invite.getTeachingClassId());
            relation.setStudentId(UserContext.userId());
            relation.setSemester(course.getSemester());
            relation.setStatus(1);
            relation.setJoinTime(LocalDateTime.now());
            courseStudentMapper.insert(relation);
            invite.setUsedCount((invite.getUsedCount() == null ? 0 : invite.getUsedCount()) + 1);
            inviteMapper.updateById(invite);
        }
        CourseJoinVO vo = new CourseJoinVO();
        vo.setCourseId(course.getId());
        vo.setCourseName(course.getCourseName());
        vo.setTeachingClassId(invite.getTeachingClassId());
        TeachingClass teachingClass = invite.getTeachingClassId() == null ? null : classMapper.selectById(invite.getTeachingClassId());
        vo.setClassName(teachingClass == null ? null : teachingClass.getClassName());
        return vo;
    }

    private LabCourse requireManageableCourse(Long courseId) {
        LabCourse course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(404, "课堂不存在");
        }
        AccessUtil.requireTeacherOrAdmin();
        if (UserContext.isAdmin() || UserContext.userId().equals(course.getTeacherId())) {
            return course;
        }
        Long authorized = classMapper.selectCount(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getCourseId, courseId)
                .and(w -> w.eq(TeachingClass::getTeacherId, UserContext.userId())
                        .or().eq(TeachingClass::getAssistantId, UserContext.userId())));
        if (authorized == 0) {
            throw new BusinessException(403, "只能管理本人负责或被授权的课堂");
        }
        return course;
    }

    private TeachingClass requireClass(Long courseId, Long classId) {
        TeachingClass teachingClass = classMapper.selectById(classId);
        if (teachingClass == null || !courseId.equals(teachingClass.getCourseId())) {
            throw new BusinessException(404, "教学班不存在");
        }
        return teachingClass;
    }

    private ClassInvite requireInvite(Long id) {
        ClassInvite invite = inviteMapper.selectById(id);
        if (invite == null) {
            throw new BusinessException(404, "邀请码不存在");
        }
        return invite;
    }

    private String uniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder builder = new StringBuilder("OG");
            for (int i = 0; i < 6; i++) {
                builder.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = builder.toString();
            Long exists = inviteMapper.selectCount(new LambdaQueryWrapper<ClassInvite>().eq(ClassInvite::getInviteCode, code));
            if (exists == 0) {
                return code;
            }
        }
        throw new BusinessException(500, "邀请码生成失败，请重试");
    }

    private ClassInviteVO toVO(ClassInvite entity) {
        ClassInviteVO vo = new ClassInviteVO();
        BeanUtils.copyProperties(entity, vo);
        TeachingClass teachingClass = entity.getTeachingClassId() == null ? null : classMapper.selectById(entity.getTeachingClassId());
        vo.setClassName(teachingClass == null ? null : teachingClass.getClassName());
        return vo;
    }
}
