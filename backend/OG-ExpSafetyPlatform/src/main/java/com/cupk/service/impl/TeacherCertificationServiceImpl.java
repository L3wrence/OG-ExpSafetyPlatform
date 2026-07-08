package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.PageResult;
import com.cupk.dto.ReviewDTO;
import com.cupk.dto.TeacherCertificationApplyDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.TeacherCertificationMapper;
import com.cupk.mapper.UserMapper;
import com.cupk.pojo.TeacherCertification;
import com.cupk.pojo.User;
import com.cupk.service.TeacherCertificationService;
import com.cupk.vo.TeacherCertificationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class TeacherCertificationServiceImpl implements TeacherCertificationService {
    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final TeacherCertificationMapper certificationMapper;
    private final UserMapper userMapper;

    public TeacherCertificationServiceImpl(TeacherCertificationMapper certificationMapper, UserMapper userMapper) {
        this.certificationMapper = certificationMapper;
        this.userMapper = userMapper;
    }

    @Override
    public TeacherCertificationVO my() {
        TeacherCertification entity = certificationMapper.selectOne(new LambdaQueryWrapper<TeacherCertification>()
                .eq(TeacherCertification::getUserId, UserContext.userId())
                .orderByDesc(TeacherCertification::getCreateTime)
                .last("LIMIT 1"));
        return entity == null ? null : toVO(entity);
    }

    @Override
    @Transactional
    public Long apply(TeacherCertificationApplyDTO dto) {
        if (UserContext.isTeacher()) {
            throw new BusinessException(400, "当前账号已具备教师权限");
        }
        TeacherCertification existing = certificationMapper.selectOne(new LambdaQueryWrapper<TeacherCertification>()
                .eq(TeacherCertification::getUserId, UserContext.userId())
                .in(TeacherCertification::getStatus, PENDING, APPROVED)
                .last("LIMIT 1"));
        if (existing != null && PENDING.equals(existing.getStatus())) {
            throw new BusinessException(409, "已有待审核的教师认证申请");
        }
        if (existing != null && APPROVED.equals(existing.getStatus())) {
            throw new BusinessException(400, "教师认证已通过");
        }
        TeacherCertification entity = new TeacherCertification();
        entity.setUserId(UserContext.userId());
        entity.setSchool(dto.getSchool().trim());
        entity.setEmployeeNo(dto.getEmployeeNo().trim());
        entity.setEducationEmail(dto.getEducationEmail().trim());
        entity.setStatus(PENDING);
        certificationMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public PageResult<TeacherCertificationVO> page(String status, Long pageNum, Long pageSize) {
        LambdaQueryWrapper<TeacherCertification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), TeacherCertification::getStatus, status);
        wrapper.orderByDesc(TeacherCertification::getCreateTime);
        Page<TeacherCertification> page = certificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional
    public void approve(Long id, ReviewDTO dto) {
        TeacherCertification entity = requirePending(id);
        entity.setStatus(APPROVED);
        entity.setReviewerId(UserContext.userId());
        entity.setReviewComment(StringUtils.hasText(dto.getReviewComment()) ? dto.getReviewComment() : "认证通过");
        entity.setReviewTime(LocalDateTime.now());
        certificationMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void reject(Long id, ReviewDTO dto) {
        TeacherCertification entity = requirePending(id);
        entity.setStatus(REJECTED);
        entity.setReviewerId(UserContext.userId());
        entity.setReviewComment(StringUtils.hasText(dto.getReviewComment()) ? dto.getReviewComment() : "认证未通过");
        entity.setReviewTime(LocalDateTime.now());
        certificationMapper.updateById(entity);
    }

    private TeacherCertification requirePending(Long id) {
        TeacherCertification entity = certificationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "教师认证申请不存在");
        }
        if (!PENDING.equals(entity.getStatus())) {
            throw new BusinessException(409, "只能审核待处理申请");
        }
        return entity;
    }

    private TeacherCertificationVO toVO(TeacherCertification entity) {
        TeacherCertificationVO vo = new TeacherCertificationVO();
        BeanUtils.copyProperties(entity, vo);
        User user = userMapper.selectById(entity.getUserId());
        if (user != null) {
            vo.setUserName(user.getRealName());
            vo.setUsername(user.getUsername());
        }
        return vo;
    }
}
