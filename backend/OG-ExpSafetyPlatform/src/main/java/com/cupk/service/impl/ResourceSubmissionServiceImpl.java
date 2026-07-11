package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.PageResult;
import com.cupk.common.FileUsage;
import com.cupk.dto.ResourceSubmissionDTO;
import com.cupk.dto.ReviewDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.ResourceSubmissionMapper;
import com.cupk.mapper.UserMapper;
import com.cupk.mapper.TeachingResourceMapper;
import com.cupk.pojo.ResourceSubmission;
import com.cupk.pojo.User;
import com.cupk.pojo.TeachingResource;
import com.cupk.service.FileStorageService;
import com.cupk.service.ResourceSubmissionService;
import com.cupk.vo.ResourcePreviewVO;
import com.cupk.vo.ResourceSubmissionVO;
import com.cupk.vo.StoredFileVO;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResourceSubmissionServiceImpl implements ResourceSubmissionService {
    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final ResourceSubmissionMapper submissionMapper;
    private final UserMapper userMapper;
    private final TeachingResourceMapper resourceMapper;
    private final FileStorageService fileStorageService;

    public ResourceSubmissionServiceImpl(ResourceSubmissionMapper submissionMapper, UserMapper userMapper,
                                         TeachingResourceMapper resourceMapper, FileStorageService fileStorageService) {
        this.submissionMapper = submissionMapper;
        this.userMapper = userMapper;
        this.resourceMapper = resourceMapper;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public Long submit(ResourceSubmissionDTO dto, MultipartFile file) {
        StoredFileVO stored = fileStorageService.store(file, FileUsage.SUBMISSION, dto.getResourceType());
        ResourceSubmission entity = new ResourceSubmission();
        BeanUtils.copyProperties(dto, entity);
        entity.setFilePath(stored.getFilePath());
        entity.setOriginalFilename(stored.getOriginalFilename());
        entity.setContentType(stored.getContentType());
        entity.setFileSize(stored.getFileSize());
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
        fileStorageService.resolve(entity.getFilePath());
        if (entity.getPublicResourceId() != null) throw new BusinessException(409, "投稿已生成正式资源");
        TeachingResource resource = new TeachingResource();
        BeanUtils.copyProperties(entity, resource, "id", "status", "createTime", "updateTime", "deleted");
        resource.setId(null);
        resource.setCourseId(null);
        resource.setExperimentId(null);
        resource.setOpenScope("PUBLIC");
        resource.setStatus(1);
        resource.setInvalidFlag(0);
        resource.setUploadUserId(entity.getSubmitterId());
        resource.setCategory("EXTENSION");
        resource.setRequiredFlag(0);
        resource.setCompletionRule(List.of("VIDEO", "AUDIO").contains(entity.getResourceType()) ? "PROGRESS" : "CONFIRM");
        resource.setMinProgress(List.of("VIDEO", "AUDIO").contains(entity.getResourceType()) ? 80 : 100);
        resource.setMinStudySeconds(0);
        resource.setViewCount(0); resource.setDownloadCount(0); resource.setFavoriteCount(0); resource.setLikeCount(0);
        resource.setCommentCount(0); resource.setRatingCount(0); resource.setSort(0);
        resourceMapper.insert(resource);
        entity.setPublicResourceId(resource.getId());
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
        vo.setPreviewUrl("/api/files/resource-submissions/" + entity.getId());
        vo.setOriginalFilename(entity.getOriginalFilename());
        vo.setCompletionRule("CONFIRM");
        vo.setMinProgress(100);
        vo.setMinStudySeconds(0);
        vo.setAiSummary("把该资源中的知识点放回油气工程现场情境理解，再结合风险提示形成操作判断。");
        return vo;
    }

    @Override
    public Path filePath(Long id) {
        ResourceSubmission entity = submissionMapper.selectById(id);
        if (entity == null) throw new BusinessException(404, "资源投稿不存在");
        if (!UserContext.isAdmin() && !UserContext.userId().equals(entity.getSubmitterId())) {
            throw new BusinessException(403, "无权查看该投稿文件");
        }
        return fileStorageService.resolve(entity.getFilePath());
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
