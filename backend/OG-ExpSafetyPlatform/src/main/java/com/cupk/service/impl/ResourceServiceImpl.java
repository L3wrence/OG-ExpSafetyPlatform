package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.PageResult;
import com.cupk.dto.ResourceCreateDTO;
import com.cupk.dto.ResourceInteractionDTO;
import com.cupk.dto.ResourceQueryDTO;
import com.cupk.dto.ResourceUpdateDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.LearningRecordMapper;
import com.cupk.mapper.ResourceInteractionMapper;
import com.cupk.mapper.TeachingClassMapper;
import com.cupk.mapper.TeachingResourceMapper;
import com.cupk.pojo.CourseStudent;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.LearningRecord;
import com.cupk.pojo.ResourceInteraction;
import com.cupk.pojo.TeachingClass;
import com.cupk.pojo.TeachingResource;
import com.cupk.service.PortalMessageService;
import com.cupk.service.ResourceService;
import com.cupk.util.AccessUtil;
import com.cupk.vo.ResourcePreviewVO;
import com.cupk.vo.ResourceStatsVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ResourceServiceImpl implements ResourceService {
    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt",
            "jpg", "jpeg", "png", "gif", "mp4", "webm", "mp3", "wav", "zip"
    ));
    private static final List<String> RESOURCE_TYPES = List.of(
            "GUIDE", "LECTURE", "PPT", "TEACHING_VIDEO", "MICRO_COURSE", "INSTRUMENT_VIDEO",
            "DEVICE_MANUAL", "EXPERIMENT_CASE", "ACCIDENT_CASE", "EMERGENCY_PROCESS",
            "REPORT_TEMPLATE", "REFERENCE", "EXTERNAL_COURSE", "VIRTUAL_SIMULATION",
            "DOCUMENT", "IMAGE", "VIDEO", "AUDIO", "LINK", "FILE"
    );

    private final TeachingResourceMapper resourceMapper;
    private final ExperimentMapper experimentMapper;
    private final LabCourseMapper courseMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final ResourceInteractionMapper interactionMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final PortalMessageService messageService;

    public ResourceServiceImpl(TeachingResourceMapper resourceMapper, ExperimentMapper experimentMapper,
                               LabCourseMapper courseMapper, TeachingClassMapper teachingClassMapper,
                               CourseStudentMapper courseStudentMapper, ResourceInteractionMapper interactionMapper,
                               LearningRecordMapper learningRecordMapper, PortalMessageService messageService) {
        this.resourceMapper = resourceMapper;
        this.experimentMapper = experimentMapper;
        this.courseMapper = courseMapper;
        this.teachingClassMapper = teachingClassMapper;
        this.courseStudentMapper = courseStudentMapper;
        this.interactionMapper = interactionMapper;
        this.learningRecordMapper = learningRecordMapper;
        this.messageService = messageService;
    }

    @Override
    public PageResult<TeachingResource> page(ResourceQueryDTO dto) {
        LambdaQueryWrapper<TeachingResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(dto.getKeyword()), w -> w
                .like(TeachingResource::getTitle, dto.getKeyword())
                .or().like(TeachingResource::getResourceType, dto.getKeyword())
                .or().like(TeachingResource::getKnowledgePoint, dto.getKeyword())
                .or().like(TeachingResource::getTags, dto.getKeyword())
                .or().like(TeachingResource::getDescription, dto.getKeyword()));
        wrapper.eq(dto.getCourseId() != null, TeachingResource::getCourseId, dto.getCourseId());
        wrapper.eq(dto.getExperimentId() != null, TeachingResource::getExperimentId, dto.getExperimentId());
        wrapper.eq(StringUtils.hasText(dto.getKnowledgePoint()), TeachingResource::getKnowledgePoint, dto.getKnowledgePoint());
        wrapper.eq(StringUtils.hasText(dto.getRiskType()), TeachingResource::getRiskType, dto.getRiskType());
        wrapper.like(StringUtils.hasText(dto.getTags()), TeachingResource::getTags, dto.getTags());
        wrapper.eq(StringUtils.hasText(dto.getResourceType()), TeachingResource::getResourceType, dto.getResourceType());
        wrapper.eq(StringUtils.hasText(dto.getOpenScope()), TeachingResource::getOpenScope, dto.getOpenScope());
        wrapper.eq(dto.getRequiredFlag() != null, TeachingResource::getRequiredFlag, dto.getRequiredFlag());
        wrapper.eq(dto.getInvalidFlag() != null, TeachingResource::getInvalidFlag, dto.getInvalidFlag());
        wrapper.eq(dto.getStatus() != null, TeachingResource::getStatus, dto.getStatus());
        if (UserContext.isTeacher()) {
            List<Long> experimentIds = currentTeacherExperimentIds();
            List<Long> courseIds = currentStudentCourseIds();
            wrapper.and(w -> {
                w.eq(TeachingResource::getOpenScope, "PUBLIC").or().isNull(TeachingResource::getCourseId);
                if (!courseIds.isEmpty()) {
                    w.or().in(TeachingResource::getCourseId, courseIds);
                }
                if (!experimentIds.isEmpty()) {
                    w.or().in(TeachingResource::getExperimentId, experimentIds);
                }
            });
            if (dto.getFavoriteOnly() != null && dto.getFavoriteOnly() == 1) {
                List<Long> favoriteIds = interactionMapper.selectList(new LambdaQueryWrapper<ResourceInteraction>()
                                .select(ResourceInteraction::getResourceId)
                                .eq(ResourceInteraction::getUserId, UserContext.userId())
                                .eq(ResourceInteraction::getFavoriteFlag, 1))
                        .stream().map(ResourceInteraction::getResourceId).toList();
                if (favoriteIds.isEmpty()) {
                    return new PageResult<>(Collections.emptyList(), 0L, dto.getPageNum(), dto.getPageSize());
                }
                wrapper.in(TeachingResource::getId, favoriteIds);
            }
        } else if (UserContext.isLearner()) {
            wrapper.eq(TeachingResource::getStatus, 1);
            wrapper.eq(TeachingResource::getInvalidFlag, 0);
            wrapper.and(w -> w.isNull(TeachingResource::getOpenTime).or().le(TeachingResource::getOpenTime, LocalDateTime.now()));
            wrapper.and(w -> w.isNull(TeachingResource::getCloseTime).or().ge(TeachingResource::getCloseTime, LocalDateTime.now()));
            List<Long> courseIds = currentStudentCourseIds();
            wrapper.and(w -> {
                w.eq(TeachingResource::getOpenScope, "PUBLIC").or().isNull(TeachingResource::getCourseId);
                if (!courseIds.isEmpty()) {
                    w.or().in(TeachingResource::getCourseId, courseIds);
                }
            });
            if (dto.getFavoriteOnly() != null && dto.getFavoriteOnly() == 1) {
                List<Long> favoriteIds = interactionMapper.selectList(new LambdaQueryWrapper<ResourceInteraction>()
                                .select(ResourceInteraction::getResourceId)
                                .eq(ResourceInteraction::getUserId, UserContext.userId())
                                .eq(ResourceInteraction::getFavoriteFlag, 1))
                        .stream().map(ResourceInteraction::getResourceId).toList();
                if (favoriteIds.isEmpty()) {
                    return new PageResult<>(Collections.emptyList(), 0L, dto.getPageNum(), dto.getPageSize());
                }
                wrapper.in(TeachingResource::getId, favoriteIds);
            }
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
        validateResourceType(dto.getResourceType());
        validateUrl(dto.getUrl());
        validateFilePath(dto.getFilePath());
        TeachingResource entity = new TeachingResource();
        BeanUtils.copyProperties(dto, entity);
        entity.setCourseId(experiment.getCourseId());
        applyDefaults(entity);
        entity.setUploadUserId(UserContext.userId());
        entity.setViewCount(0);
        resourceMapper.insert(entity);
        notifyPublished(entity);
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
        validateResourceType(dto.getResourceType());
        validateUrl(dto.getUrl());
        validateFilePath(dto.getFilePath());
        BeanUtils.copyProperties(dto, current);
        current.setCourseId(targetExperiment.getCourseId());
        applyDefaults(current);
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
        Integer oldStatus = current.getStatus();
        current.setStatus(status);
        resourceMapper.updateById(current);
        if (!Integer.valueOf(1).equals(oldStatus) && status == 1) {
            notifyPublished(current);
        }
    }

    @Override
    public TeachingResource detail(Long id) {
        TeachingResource resource = requireResource(id);
        assertReadable(resource);
        return resource;
    }

    @Override
    public TeachingResource upload(MultipartFile file) {
        AccessUtil.requireTeacherOrAdmin();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "文件大小不能超过200MB");
        }
        String originalFilename = cleanFilename(file.getOriginalFilename());
        String extension = extension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "不支持的文件类型");
        }
        String safeName = UUID.randomUUID() + "." + extension;
        Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "resources").toAbsolutePath().normalize();
        Path target = uploadDir.resolve(safeName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new BusinessException(400, "非法文件路径");
        }
        try {
            Files.createDirectories(uploadDir);
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败");
        }
        TeachingResource result = new TeachingResource();
        result.setFilePath("/uploads/resources/" + safeName);
        result.setOriginalFilename(originalFilename);
        result.setFileSize(file.getSize());
        result.setContentType(file.getContentType());
        return result;
    }

    @Override
    @Transactional
    public void markDownload(Long id) {
        TeachingResource resource = requireResource(id);
        assertReadable(resource);
        resourceMapper.update(null, new LambdaUpdateWrapper<TeachingResource>()
                .eq(TeachingResource::getId, id)
                .setSql("download_count = COALESCE(download_count, 0) + 1"));
    }

    @Override
    @Transactional
    public void interact(Long id, ResourceInteractionDTO dto) {
        TeachingResource resource = requireResource(id);
        assertReadable(resource);
        ResourceInteraction interaction = interactionMapper.selectOne(new LambdaQueryWrapper<ResourceInteraction>()
                .eq(ResourceInteraction::getResourceId, id)
                .eq(ResourceInteraction::getUserId, UserContext.userId()));
        if (interaction == null) {
            interaction = new ResourceInteraction();
            interaction.setResourceId(id);
            interaction.setUserId(UserContext.userId());
        }
        if (dto.getFavoriteFlag() != null) interaction.setFavoriteFlag(flag(dto.getFavoriteFlag()));
        if (dto.getLikeFlag() != null) interaction.setLikeFlag(flag(dto.getLikeFlag()));
        if (dto.getRating() != null) interaction.setRating(dto.getRating());
        if (dto.getComment() != null) interaction.setComment(dto.getComment());
        if (interaction.getId() == null) {
            interactionMapper.insert(interaction);
        } else {
            interactionMapper.updateById(interaction);
        }
        refreshInteractionStats(resource.getId());
    }

    @Override
    @Transactional
    public void markInvalid(Long id, Integer invalidFlag) {
        TeachingResource resource = requireResource(id);
        assertWritable(resource);
        resource.setInvalidFlag(flag(invalidFlag));
        resource.setInvalidCheckTime(LocalDateTime.now());
        resourceMapper.updateById(resource);
    }

    @Override
    public ResourceStatsVO stats(Long id) {
        TeachingResource resource = requireResource(id);
        assertReadable(resource);
        ResourceStatsVO vo = new ResourceStatsVO();
        vo.setResourceId(id);
        vo.setViewCount(value(resource.getViewCount()));
        vo.setDownloadCount(value(resource.getDownloadCount()));
        vo.setFavoriteCount(value(resource.getFavoriteCount()));
        vo.setLikeCount(value(resource.getLikeCount()));
        vo.setCommentCount(value(resource.getCommentCount()));
        vo.setRatingCount(value(resource.getRatingCount()));
        vo.setRatingAvg(resource.getRatingAvg() == null ? BigDecimal.ZERO : resource.getRatingAvg());
        int learners = Math.toIntExact(learningRecordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getResourceId, id)));
        int completed = Math.toIntExact(learningRecordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getResourceId, id)
                .eq(LearningRecord::getFinishFlag, 1)));
        vo.setLearnerCount(learners);
        vo.setCompletedCount(completed);
        vo.setCompletionRate(learners == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(learners), 2, RoundingMode.HALF_UP));
        return vo;
    }

    @Override
    public ResourcePreviewVO preview(Long id) {
        TeachingResource resource = requireResource(id);
        assertReadable(resource);
        LearningRecord record = UserContext.isLearner()
                ? learningRecordMapper.selectOne(new LambdaQueryWrapper<LearningRecord>()
                    .eq(LearningRecord::getResourceId, id)
                    .eq(LearningRecord::getStudentId, UserContext.userId()))
                : null;
        ResourcePreviewVO vo = new ResourcePreviewVO();
        vo.setId(resource.getId());
        vo.setTitle(resource.getTitle());
        vo.setResourceType(resource.getResourceType());
        vo.setKnowledgePoint(resource.getKnowledgePoint());
        vo.setRiskType(resource.getRiskType());
        vo.setTags(resource.getTags());
        vo.setContentType(resource.getContentType());
        vo.setOriginalFilename(resource.getOriginalFilename());
        vo.setExperimentId(resource.getExperimentId());
        vo.setCompletionRule(resource.getCompletionRule());
        vo.setMinStudySeconds(resource.getMinStudySeconds());
        vo.setMinProgress(resource.getMinProgress());
        vo.setInvalidFlag(resource.getInvalidFlag());
        vo.setPreviewUrl(StringUtils.hasText(resource.getUrl()) ? resource.getUrl() : "/api/files/resources/" + resource.getId());
        Experiment experiment = experimentMapper.selectById(resource.getExperimentId());
        if (experiment != null) {
            vo.setExperimentName(experiment.getExpName());
            vo.setAiSummary(aiSummary(resource, experiment));
        }
        if (record != null) {
            vo.setProgress(record.getProgress());
            vo.setDurationSeconds(record.getDurationSeconds());
            vo.setLastPositionSeconds(record.getLastPositionSeconds());
            vo.setNote(record.getNote());
        }
        return vo;
    }

    @Override
    public Path resourceFilePath(Long id) {
        TeachingResource resource = requireResource(id);
        assertReadable(resource);
        if (!StringUtils.hasText(resource.getFilePath())) {
            throw new BusinessException(404, "资源文件不存在");
        }
        String filename = Path.of(resource.getFilePath()).getFileName().toString();
        Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "resources").toAbsolutePath().normalize();
        Path file = uploadDir.resolve(filename).normalize();
        if (!file.startsWith(uploadDir) || !Files.exists(file) || !Files.isRegularFile(file)) {
            throw new BusinessException(404, "资源文件不存在");
        }
        return file;
    }

    private List<Long> currentTeacherExperimentIds() {
        List<Long> ownCourseIds = courseMapper.selectList(new LambdaQueryWrapper<LabCourse>()
                .select(LabCourse::getId)
                .eq(LabCourse::getTeacherId, UserContext.userId()))
                .stream().map(LabCourse::getId).toList();
        List<Long> authorizedCourseIds = teachingClassMapper.selectList(new LambdaQueryWrapper<TeachingClass>()
                        .select(TeachingClass::getCourseId)
                        .and(w -> w.eq(TeachingClass::getTeacherId, UserContext.userId())
                                .or().eq(TeachingClass::getAssistantId, UserContext.userId())))
                .stream().map(TeachingClass::getCourseId).toList();
        List<Long> courseIds = new java.util.ArrayList<>();
        courseIds.addAll(ownCourseIds);
        courseIds.addAll(authorizedCourseIds);
        courseIds = courseIds.stream().distinct().toList();
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

    private void assertReadable(TeachingResource resource) {
        if (UserContext.isTeacher() && canWrite(resource)) {
            return;
        }
        if (UserContext.isLearner()) {
            if (resource.getStatus() == null || resource.getStatus() != 1 || flag(resource.getInvalidFlag()) == 1) {
                throw new BusinessException(403, "资源未开放或已失效");
            }
            LocalDateTime now = LocalDateTime.now();
            if (resource.getOpenTime() != null && resource.getOpenTime().isAfter(now)) {
                throw new BusinessException(403, "资源尚未开放");
            }
            if (resource.getCloseTime() != null && resource.getCloseTime().isBefore(now)) {
                throw new BusinessException(403, "资源已过期");
            }
            if (!isPublicResource(resource)
                    && resource.getCourseId() != null
                    && !currentStudentCourseIds().contains(resource.getCourseId())) {
                throw new BusinessException(403, "无权访问该课程资源");
            }
        } else if (UserContext.isTeacher()) {
            assertWritable(resource);
        }
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

    private List<Long> currentStudentCourseIds() {
        return courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .select(CourseStudent::getCourseId)
                .eq(CourseStudent::getStudentId, UserContext.userId())
                .eq(CourseStudent::getStatus, 1))
                .stream().map(CourseStudent::getCourseId).toList();
    }

    private boolean isPublicResource(TeachingResource resource) {
        return "PUBLIC".equalsIgnoreCase(resource.getOpenScope()) || resource.getCourseId() == null;
    }

    private void assertWritable(TeachingResource resource) {
        Experiment experiment = requireExperiment(resource.getExperimentId());
        LabCourse course = requireCourse(experiment.getCourseId());
        AccessUtil.requireTeacherOrAdmin();
        if (UserContext.isAdmin() || UserContext.userId().equals(course.getTeacherId()) || isTeachingClassManager(course.getId())) {
            return;
        }
        throw new BusinessException(403, "不能修改其他教师负责的课程");
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

    private boolean isTeachingClassManager(Long courseId) {
        if (!UserContext.isTeacher()) {
            return false;
        }
        return teachingClassMapper.selectCount(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getCourseId, courseId)
                .and(w -> w.eq(TeachingClass::getTeacherId, UserContext.userId())
                        .or().eq(TeachingClass::getAssistantId, UserContext.userId()))) > 0;
    }

    private void validateResourceType(String resourceType) {
        if (!RESOURCE_TYPES.contains(resourceType)) {
            throw new BusinessException(400, "不支持的资源类型");
        }
    }

    private void validateUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        try {
            URI uri = new URI(url);
            if (!List.of("http", "https").contains(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                throw new BusinessException(400, "外部链接必须是合法的HTTP/HTTPS URL");
            }
        } catch (URISyntaxException e) {
            throw new BusinessException(400, "外部链接格式不正确");
        }
    }

    private void validateFilePath(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return;
        }
        if (filePath.contains("..") || filePath.contains("\\") || filePath.startsWith("/../")) {
            throw new BusinessException(400, "文件路径不合法");
        }
    }

    private void applyDefaults(TeachingResource entity) {
        entity.setCategory(StringUtils.hasText(entity.getCategory()) ? entity.getCategory() : (flag(entity.getRequiredFlag()) == 1 ? "REQUIRED" : "EXTENSION"));
        entity.setCompletionRule(StringUtils.hasText(entity.getCompletionRule()) ? entity.getCompletionRule() : defaultCompletionRule(entity.getResourceType()));
        entity.setMinProgress(entity.getMinProgress() == null ? defaultMinProgress(entity.getResourceType()) : entity.getMinProgress());
        entity.setMinStudySeconds(entity.getMinStudySeconds() == null ? 0 : entity.getMinStudySeconds());
        entity.setInvalidFlag(flag(entity.getInvalidFlag()));
        entity.setDownloadCount(value(entity.getDownloadCount()));
        entity.setFavoriteCount(value(entity.getFavoriteCount()));
        entity.setLikeCount(value(entity.getLikeCount()));
        entity.setCommentCount(value(entity.getCommentCount()));
        entity.setRatingCount(value(entity.getRatingCount()));
        entity.setRatingAvg(entity.getRatingAvg() == null ? BigDecimal.ZERO : entity.getRatingAvg());
    }

    private String defaultCompletionRule(String type) {
        if (type != null && (type.contains("VIDEO") || "VIDEO".equals(type) || "AUDIO".equals(type))) {
            return "PROGRESS";
        }
        return "CONFIRM";
    }

    private Integer defaultMinProgress(String type) {
        if (type != null && (type.contains("VIDEO") || "VIDEO".equals(type) || "AUDIO".equals(type))) {
            return 80;
        }
        return 100;
    }

    private void refreshInteractionStats(Long resourceId) {
        List<ResourceInteraction> interactions = interactionMapper.selectList(new LambdaQueryWrapper<ResourceInteraction>()
                .eq(ResourceInteraction::getResourceId, resourceId));
        int favoriteCount = (int) interactions.stream().filter(item -> flag(item.getFavoriteFlag()) == 1).count();
        int likeCount = (int) interactions.stream().filter(item -> flag(item.getLikeFlag()) == 1).count();
        int commentCount = (int) interactions.stream().filter(item -> StringUtils.hasText(item.getComment())).count();
        List<BigDecimal> ratings = interactions.stream()
                .map(ResourceInteraction::getRating)
                .filter(rating -> rating != null && rating.compareTo(BigDecimal.ZERO) > 0)
                .toList();
        BigDecimal ratingAvg = ratings.isEmpty() ? BigDecimal.ZERO : ratings.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
        TeachingResource update = new TeachingResource();
        update.setId(resourceId);
        update.setFavoriteCount(favoriteCount);
        update.setLikeCount(likeCount);
        update.setCommentCount(commentCount);
        update.setRatingCount(ratings.size());
        update.setRatingAvg(ratingAvg);
        resourceMapper.updateById(update);
    }

    private void notifyPublished(TeachingResource resource) {
        if (!Integer.valueOf(1).equals(resource.getStatus())) {
            return;
        }
        messageService.sendToCourseStudents(resource.getCourseId(),
                "课程发布了新资源",
                resource.getTitle(),
                "RESOURCE_PUBLISHED",
                resource.getId(),
                "/student/learning/" + resource.getCourseId() + "?experimentId=" + resource.getExperimentId());
    }

    private String cleanFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "resource";
        }
        return Path.of(filename).getFileName().toString().replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            throw new BusinessException(400, "文件缺少扩展名");
        }
        return filename.substring(index + 1).toLowerCase();
    }

    private int flag(Integer value) {
        return value == null ? 0 : value;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String aiSummary(TeachingResource resource, Experiment experiment) {
        String point = StringUtils.hasText(resource.getKnowledgePoint()) ? resource.getKnowledgePoint() : experiment.getExpName();
        String risk = StringUtils.hasText(resource.getRiskType()) ? "，重点留意" + resource.getRiskType() + "风险" : "";
        return "先把" + point + "放回" + experiment.getExpName() + "的操作场景理解" + risk + "。";
    }
}
