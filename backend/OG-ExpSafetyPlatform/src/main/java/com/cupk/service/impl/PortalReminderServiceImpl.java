package com.cupk.service.impl;

import com.cupk.mapper.PortalReminderMapper;
import com.cupk.service.PortalMessageService;
import com.cupk.service.PortalReminderService;
import com.cupk.vo.ReminderTargetVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PortalReminderServiceImpl implements PortalReminderService {
    private final PortalReminderMapper reminderMapper;
    private final PortalMessageService messageService;

    public PortalReminderServiceImpl(PortalReminderMapper reminderMapper, PortalMessageService messageService) {
        this.reminderMapper = reminderMapper;
        this.messageService = messageService;
    }

    @Override
    @Transactional
    public void sendDueReminders() {
        send(reminderMapper.examDeadlineTargets(), "EXAM_DEADLINE_REMINDER");
        send(reminderMapper.reservationStartTargets(), "RESERVATION_START_REMINDER");
        send(reminderMapper.reportDeadlineTargets(), "REPORT_DEADLINE_REMINDER");
        send(reminderMapper.admissionExpiringTargets(), "ADMISSION_EXPIRING_REMINDER");
    }

    private void send(List<ReminderTargetVO> targets, String bizType) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        for (ReminderTargetVO target : targets) {
            messageService.sendOnce(
                    target.getUserId(),
                    target.getTitle(),
                    target.getContent(),
                    bizType,
                    target.getBizId(),
                    target.getPath()
            );
        }
    }
}
