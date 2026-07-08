package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.PageResult;
import com.cupk.dto.ResourceSubmissionDTO;
import com.cupk.dto.ReviewDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.ResourceSubmissionMapper;
import com.cupk.mapper.UserMapper;
import com.cupk.pojo.ResourceSubmission;
import com.cupk.pojo.User;
import com.cupk.service.ResourceSubmissionService;
import com.cupk.vo.ResourcePreviewVO;
import com.cupk.vo.ResourceSubmissionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class ResourceSubmissionServiceImpl implements ResourceSubmissionService {
    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final ResourceSubmissionMapper submissionMapper;
    private final UserMapper userMapper;

    public ResourceSubmissionServiceImpl(ResourceSubmissionMapper submissionMapper, UserMapper userMapper) {
        this.submissionMapper = submissionMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public Long submit(ResourceSubmissionDTO dto) {
        if (!StringUtils.hasText(dto.getUrl()) && !StringUtils.hasText(dto.getFilePath())) {
            throw new BusinessException(400, "请提供外链或文件地址");
        }
        ResourceSubmission entity = new ResourceSubmission();
        BeanUtils.copyProperties(dto, entity);
        entity.setSubmitterId(UserContext.userId());
        entity.setStatus(PENDING);
        submissionMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public PageResult<ResourceSubmissionVO> my(Long pageNum, Long pageSize) {
        LambdaQueryWrapper<ResourceSubmission> wrapper = new LambdaQueryWrapper<ResourceSubmission>()
                .eq(ResourceSubmission::getSubmitterId, UserContext.userId())
                .orderByDesc(ResourceSubmission::getCreateTime);
        Page<ResourceSubmission> page = submissionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return toPage(page);
    }

    @Override
    public PageResult<ResourceSubmissionVO> page(String status, Long pageNum, Long pageSize, String keyword) {
        LambdaQueryWrapper<ResourceSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), ResourceSubmission::getStatus, status);
        wrapper.and(StringUtils.hasText(keyword), w -> w.like(ResourceSubmission::getTitle, keyword)
                .or().like(ResourceSubmission::getKnowledgePoint, keyword)
                .or().like(ResourceSubmission::getTags, keyword)
                .or().like(ResourceSubmission::getDescription, keyword));
        wrapper.orderByDesc(ResourceSubmission::getCreateTime);
        Page<ResourceSubmission> page = submissionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return toPage(page);
    }

    @Override
    @Transactional
    public void approve(Long id, ReviewDTO dto) {
        ResourceSubmission entity = requirePending(id);
        entity.setStatus(APPROVED);
        entity.setReviewerId(UserContext.userId());
        entity.setReviewComment(StringUtils.hasText(dto.getReviewComment()) ? dto.getReviewComment() : "审核通过");
        entity.setReviewTime(LocalDateTime.now());
        submissionMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void reject(Long id, ReviewDTO dto) {
        ResourceSubmission entity = requirePending(id);
        entity.setStatus(REJECTED);
        entity.setReviewerId(UserContext.userId());
        entity.setReviewComment(StringUtils.hasText(dto.getReviewComment()) ? dto.getReviewComment() : "资源暂不适合公开");
        entity.setReviewTime(LocalDateTime.now());
        submissionMapper.updateById(entity);
    }

    @Override
    public ResourcePreviewVO preview(Long id) {
        ResourceSubmission entity = submissionMapper.selectById(id);
        if (entity == null || !APPROVED.equals(entity.getStatus())) {
            throw new BusinessException(404, "公共资源不存在");
        }
        ResourcePreviewVO vo = new ResourcePreviewVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setResourceType(entity.getResourceType());
        vo.setKnowledgePoint(entity.getKnowledgePoint());
        vo.setRiskType(entity.getRiskType());
        vo.setTags(entity.getTags());
        vo.setContentType(entity.getContentType());
        vo.setPreviewUrl(StringUtils.hasText(entity.getUrl()) ? entity.getUrl() : entity.getFilePath());
        vo.setOriginalFilename(entity.getOriginalFilename());
        vo.setCompletionRule("CONFIRM");
        vo.setMinProgress(100);
        vo.setMinStudySeconds(0);
        vo.setAiSummary("把该资源中的知识点放回油气工程现场情境理解，再结合风险提示形成操作判断。");
        return vo;
    }

    private ResourceSubmission requirePending(Long id) {
        ResourceSubmission entity = submissionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "资源投稿不存在");
        }
        if (!PENDING.equals(entity.getStatus())) {
            throw new BusinessException(409, "只能审核待处理投稿");
        }
        return entity;
    }

    private PageResult<ResourceSubmissionVO> toPage(Page<ResourceSubmission> page) {
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    private ResourceSubmissionVO toVO(ResourceSubmission entity) {
        ResourceSubmissionVO vo = new ResourceSubmissionVO();
        BeanUtils.copyProperties(entity, vo);
        User user = userMapper.selectById(entity.getSubmitterId());
        vo.setSubmitterName(user == null ? "用户" : user.getRealName());
        return vo;
    }
}
