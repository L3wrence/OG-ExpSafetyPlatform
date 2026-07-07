package com.cupk.service;

import java.util.Collection;

public interface PortalMessageService {
    void send(Long userId, String title, String content, String bizType, Long bizId, String path);
    void sendOnce(Long userId, String title, String content, String bizType, Long bizId, String path);
    void sendToUsers(Collection<Long> userIds, String title, String content, String bizType, Long bizId, String path);
    void sendToCourseStudents(Long courseId, String title, String content, String bizType, Long bizId, String path);
}
