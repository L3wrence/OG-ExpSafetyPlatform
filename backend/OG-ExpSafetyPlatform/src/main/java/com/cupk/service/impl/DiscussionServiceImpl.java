package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.PageResult;
import com.cupk.dto.DiscussionReplyCreateDTO;
import com.cupk.dto.DiscussionTopicCreateDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.DiscussionReplyMapper;
import com.cupk.mapper.DiscussionTopicMapper;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.UserMapper;
import com.cupk.pojo.CourseStudent;
import com.cupk.pojo.DiscussionReply;
import com.cupk.pojo.DiscussionTopic;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.User;
import com.cupk.service.DiscussionService;
import com.cupk.service.PortalMessageService;
import com.cupk.vo.DiscussionReplyVO;
import com.cupk.vo.DiscussionTopicVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DiscussionServiceImpl implements DiscussionService {
    private final DiscussionTopicMapper topicMapper;
    private final DiscussionReplyMapper replyMapper;
    private final LabCourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final ExperimentMapper experimentMapper;
    private final UserMapper userMapper;
    private final PortalMessageService messageService;

    public DiscussionServiceImpl(DiscussionTopicMapper topicMapper,
                                 DiscussionReplyMapper replyMapper,
                                 LabCourseMapper courseMapper,
                                 CourseStudentMapper courseStudentMapper,
                                 ExperimentMapper experimentMapper,
                                 UserMapper userMapper,
                                 PortalMessageService messageService) {
        this.topicMapper = topicMapper;
        this.replyMapper = replyMapper;
        this.courseMapper = courseMapper;
        this.courseStudentMapper = courseStudentMapper;
        this.experimentMapper = experimentMapper;
        this.userMapper = userMapper;
        this.messageService = messageService;
    }

    @Override
    public PageResult<DiscussionTopicVO> page(Long courseId, Long experimentId, String status, int pageNum, int pageSize) {
        if (courseId != null) {
            assertCourseReadable(courseId);
        }
        if (experimentId != null) {
            Experiment experiment = requireExperiment(experimentId);
            assertCourseReadable(experiment.getCourseId());
        }
        LambdaQueryWrapper<DiscussionTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(courseId != null, DiscussionTopic::getCourseId, courseId)
                .eq(experimentId != null, DiscussionTopic::getExperimentId, experimentId)
                .eq(StringUtils.hasText(status), DiscussionTopic::getStatus, status)
                .orderByDesc(DiscussionTopic::getIsFeatured)
                .orderByDesc(DiscussionTopic::getUpdateTime)
                .orderByDesc(DiscussionTopic::getCreateTime);
        if (courseId == null && experimentId == null) {
            wrapper.isNull(DiscussionTopic::getCourseId);
        } else {
            applyScope(wrapper);
        }
        Page<DiscussionTopic> page = topicMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<DiscussionTopicVO> records = page.getRecords().stream().map(topic -> toTopicVO(topic, false)).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public DiscussionTopicVO detail(Long id) {
        DiscussionTopic topic = requireTopic(id);
        if (topic.getCourseId() != null) {
            assertCourseReadable(topic.getCourseId());
        }
        return toTopicVO(topic, true);
    }

    @Override
    @Transactional
    public Long create(DiscussionTopicCreateDTO dto) {
        if (!UserContext.isStudent() && !UserContext.isTeacher() && !UserContext.isAdmin()) {
            throw new BusinessException(403, "无权发布课程讨论");
        }
        if (dto.getCourseId() != null) {
            assertCourseReadable(dto.getCourseId());
        }
        if (dto.getExperimentId() != null) {
            Experiment experiment = requireExperiment(dto.getExperimentId());
            if (dto.getCourseId() == null || !dto.getCourseId().equals(experiment.getCourseId())) {
                throw new BusinessException(400, "实验不属于当前课程");
            }
        }
        DiscussionTopic topic = new DiscussionTopic();
        BeanUtils.copyProperties(dto, topic);
        topic.setUserId(UserContext.userId());
        topic.setStatus("OPEN");
        topic.setIsAnonymous(flag(dto.getIsAnonymous()));
        topic.setIsFeatured(0);
        topic.setReplyCount(0);
        topicMapper.insert(topic);

        LabCourse course = dto.getCourseId() == null ? null : courseMapper.selectById(dto.getCourseId());
        if (course != null && course.getTeacherId() != null && !course.getTeacherId().equals(UserContext.userId())) {
            messageService.send(course.getTeacherId(), "有新的课程提问", topic.getTitle(),
                    "DISCUSSION", topic.getId(), "/discussions?topicId=" + topic.getId());
        }
        return topic.getId();
    }

    @Override
    @Transactional
    public Long reply(Long topicId, DiscussionReplyCreateDTO dto) {
        DiscussionTopic topic = requireTopic(topicId);
        if (topic.getCourseId() != null) {
            assertCourseReadable(topic.getCourseId());
        }
        DiscussionReply reply = new DiscussionReply();
        reply.setTopicId(topicId);
        reply.setUserId(UserContext.userId());
        reply.setContent(dto.getContent());
        reply.setIsTeacherReply(UserContext.isTeacher() || UserContext.isAdmin() ? 1 : 0);
        replyMapper.insert(reply);
        topicMapper.update(null, new LambdaUpdateWrapper<DiscussionTopic>()
                .eq(DiscussionTopic::getId, topicId)
                .setSql("reply_count = COALESCE(reply_count, 0) + 1")
                .set(DiscussionTopic::getUpdateTime, java.time.LocalDateTime.now()));
        if (reply.getIsTeacherReply() == 1 && !topic.getUserId().equals(UserContext.userId())) {
            messageService.send(topic.getUserId(), "教师回复了你的提问", topic.getTitle(),
                    "DISCUSSION", topic.getId(), "/discussions?topicId=" + topic.getId());
        }
        return reply.getId();
    }

    @Override
    @Transactional
    public void updateStatus(Long topicId, String status) {
        if (!List.of("OPEN", "RESOLVED", "CLOSED").contains(status)) {
            throw new BusinessException(400, "讨论状态不合法");
        }
        DiscussionTopic topic = requireTopic(topicId);
        assertCourseWritable(topic.getCourseId());
        topic.setStatus(status);
        topicMapper.updateById(topic);
    }

    @Override
    @Transactional
    public void updateFeatured(Long topicId, Integer featured) {
        DiscussionTopic topic = requireTopic(topicId);
        assertCourseWritable(topic.getCourseId());
        topic.setIsFeatured(flag(featured));
        topicMapper.updateById(topic);
    }

    private void applyScope(LambdaQueryWrapper<DiscussionTopic> wrapper) {
        if (UserContext.isAdmin()) {
            return;
        }
        if (UserContext.isTeacher()) {
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<LabCourse>()
                            .select(LabCourse::getId)
                            .eq(LabCourse::getTeacherId, UserContext.userId()))
                    .stream().map(LabCourse::getId).toList();
            if (courseIds.isEmpty()) {
                wrapper.eq(DiscussionTopic::getCourseId, -1L);
            } else {
                wrapper.in(DiscussionTopic::getCourseId, courseIds);
            }
            return;
        }
        if (UserContext.isStudent()) {
            List<Long> courseIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                            .select(CourseStudent::getCourseId)
                            .eq(CourseStudent::getStudentId, UserContext.userId())
                            .eq(CourseStudent::getStatus, 1)
                            .eq(CourseStudent::getDeleted, 0))
                    .stream().map(CourseStudent::getCourseId).toList();
            if (courseIds.isEmpty()) {
                wrapper.eq(DiscussionTopic::getCourseId, -1L);
            } else {
                wrapper.in(DiscussionTopic::getCourseId, courseIds);
            }
            return;
        }
        throw new BusinessException(403, "无权访问课程讨论");
    }

    private void assertCourseReadable(Long courseId) {
        LabCourse course = requireCourse(courseId);
        if (UserContext.isAdmin()) {
            return;
        }
        if (UserContext.isTeacher()) {
            if (!UserContext.userId().equals(course.getTeacherId())) {
                throw new BusinessException(403, "不能访问非本人负责课程的讨论");
            }
            return;
        }
        if (UserContext.isStudent()) {
            Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, courseId)
                    .eq(CourseStudent::getStudentId, UserContext.userId())
                    .eq(CourseStudent::getStatus, 1)
                    .eq(CourseStudent::getDeleted, 0));
            if (count == null || count == 0) {
                throw new BusinessException(403, "不能访问未选课程的讨论");
            }
            return;
        }
        throw new BusinessException(403, "无权访问课程讨论");
    }

    private void assertCourseWritable(Long courseId) {
        if (courseId == null) {
            if (UserContext.isAdmin()) {
                return;
            }
            throw new BusinessException(403, "只有管理员可以管理公共讨论状态");
        }
        LabCourse course = requireCourse(courseId);
        if (UserContext.isAdmin()) {
            return;
        }
        if (UserContext.isTeacher() && UserContext.userId().equals(course.getTeacherId())) {
            return;
        }
        throw new BusinessException(403, "不能管理非本人负责课程的讨论");
    }

    private DiscussionTopic requireTopic(Long id) {
        DiscussionTopic topic = topicMapper.selectById(id);
        if (topic == null) {
            throw new BusinessException(404, "讨论不存在");
        }
        return topic;
    }

    private LabCourse requireCourse(Long id) {
        LabCourse course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        return course;
    }

    private Experiment requireExperiment(Long id) {
        Experiment experiment = experimentMapper.selectById(id);
        if (experiment == null) {
            throw new BusinessException(404, "实验不存在");
        }
        return experiment;
    }

    private DiscussionTopicVO toTopicVO(DiscussionTopic topic, boolean includeReplies) {
        DiscussionTopicVO vo = new DiscussionTopicVO();
        BeanUtils.copyProperties(topic, vo);
        User user = userMapper.selectById(topic.getUserId());
        boolean anonymous = Integer.valueOf(1).equals(topic.getIsAnonymous()) && !UserContext.isTeacher() && !UserContext.isAdmin();
        vo.setUserName(anonymous ? "匿名同学" : displayName(user));
        if (includeReplies) {
            List<DiscussionReplyVO> replies = replyMapper.selectList(new LambdaQueryWrapper<DiscussionReply>()
                            .eq(DiscussionReply::getTopicId, topic.getId())
                            .orderByAsc(DiscussionReply::getCreateTime))
                    .stream().map(this::toReplyVO).toList();
            vo.setReplies(replies);
        }
        return vo;
    }

    private DiscussionReplyVO toReplyVO(DiscussionReply reply) {
        DiscussionReplyVO vo = new DiscussionReplyVO();
        BeanUtils.copyProperties(reply, vo);
        vo.setUserName(displayName(userMapper.selectById(reply.getUserId())));
        return vo;
    }

    private String displayName(User user) {
        if (user == null) return "用户";
        if (StringUtils.hasText(user.getRealName())) return user.getRealName();
        return user.getUsername();
    }

    private int flag(Integer value) {
        return value == null ? 0 : value;
    }
}
