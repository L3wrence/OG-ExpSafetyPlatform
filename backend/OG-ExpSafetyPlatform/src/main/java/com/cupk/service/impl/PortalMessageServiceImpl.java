package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.PortalMessageMapper;
import com.cupk.pojo.CourseStudent;
import com.cupk.pojo.PortalMessage;
import com.cupk.service.PortalMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class PortalMessageServiceImpl implements PortalMessageService {
    private final PortalMessageMapper messageMapper;
    private final CourseStudentMapper courseStudentMapper;

    public PortalMessageServiceImpl(PortalMessageMapper messageMapper, CourseStudentMapper courseStudentMapper) {
        this.messageMapper = messageMapper;
        this.courseStudentMapper = courseStudentMapper;
    }

    @Override
    @Transactional
    public void send(Long userId, String title, String content, String bizType, Long bizId, String path) {
        if (userId == null || !StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            return;
        }
        PortalMessage message = new PortalMessage();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setPath(path);
        message.setReadFlag(0);
        message.setCreateTime(LocalDateTime.now());
        message.setDeleted(0);
        messageMapper.insert(message);
    }

    @Override
    @Transactional
    public void sendOnce(Long userId, String title, String content, String bizType, Long bizId, String path) {
        if (userId == null || bizId == null || !StringUtils.hasText(bizType)) {
            send(userId, title, content, bizType, bizId, path);
            return;
        }
        Long existing = messageMapper.selectCount(new LambdaQueryWrapper<PortalMessage>()
                .eq(PortalMessage::getUserId, userId)
                .eq(PortalMessage::getBizType, bizType)
                .eq(PortalMessage::getBizId, bizId)
                .eq(PortalMessage::getDeleted, 0));
        if (existing == null || existing == 0) {
            send(userId, title, content, bizType, bizId, path);
        }
    }

    @Override
    @Transactional
    public void sendToUsers(Collection<Long> userIds, String title, String content, String bizType, Long bizId, String path) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(userIds);
        uniqueIds.forEach(userId -> send(userId, title, content, bizType, bizId, path));
    }

    @Override
    @Transactional
    public void sendToCourseStudents(Long courseId, String title, String content, String bizType, Long bizId, String path) {
        if (courseId == null) {
            return;
        }
        List<Long> studentIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                        .select(CourseStudent::getStudentId)
                        .eq(CourseStudent::getCourseId, courseId)
                        .eq(CourseStudent::getStatus, 1)
                        .eq(CourseStudent::getDeleted, 0))
                .stream()
                .map(CourseStudent::getStudentId)
                .toList();
        sendToUsers(studentIds, title, content, bizType, bizId, path);
    }
}
