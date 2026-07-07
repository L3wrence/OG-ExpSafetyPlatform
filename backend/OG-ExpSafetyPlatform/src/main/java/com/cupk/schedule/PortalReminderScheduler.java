package com.cupk.schedule;

import com.cupk.service.PortalReminderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PortalReminderScheduler {
    private final PortalReminderService reminderService;

    public PortalReminderScheduler(PortalReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void sendDueReminders() {
        reminderService.sendDueReminders();
    }
}
