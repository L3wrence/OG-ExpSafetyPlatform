package com.cupk.controller;

import com.cupk.common.PageResult;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.OperationLogQueryDTO;
import com.cupk.dto.PortalNoticeQueryDTO;
import com.cupk.dto.PortalNoticeSaveDTO;
import com.cupk.pojo.OperationLog;
import com.cupk.pojo.PortalNotice;
import com.cupk.service.AdminPortalService;
import com.cupk.service.PortalReminderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminPortalController {
    private final AdminPortalService adminPortalService;
    private final PortalReminderService reminderService;

    public AdminPortalController(AdminPortalService adminPortalService, PortalReminderService reminderService) {
        this.adminPortalService = adminPortalService;
        this.reminderService = reminderService;
    }

    @GetMapping("/notices")
    @RequirePermission("portal:notice:manage")
    public Result<PageResult<PortalNotice>> notices(@Valid PortalNoticeQueryDTO dto) {
        return Result.success(adminPortalService.pageNotices(dto));
    }

    @PostMapping("/notices")
    @RequirePermission("portal:notice:manage")
    public Result<Long> createNotice(@Valid @RequestBody PortalNoticeSaveDTO dto) {
        return Result.success(adminPortalService.createNotice(dto));
    }

    @PutMapping("/notices/{id}")
    @RequirePermission("portal:notice:manage")
    public Result<Void> updateNotice(@PathVariable Long id, @Valid @RequestBody PortalNoticeSaveDTO dto) {
        adminPortalService.updateNotice(id, dto);
        return Result.success();
    }

    @PutMapping("/notices/{id}/publish")
    @RequirePermission("portal:notice:manage")
    public Result<Void> publishNotice(@PathVariable Long id) {
        adminPortalService.publishNotice(id);
        return Result.success();
    }

    @PutMapping("/notices/{id}/offline")
    @RequirePermission("portal:notice:manage")
    public Result<Void> offlineNotice(@PathVariable Long id) {
        adminPortalService.offlineNotice(id);
        return Result.success();
    }

    @DeleteMapping("/notices/{id}")
    @RequirePermission("portal:notice:manage")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        adminPortalService.deleteNotice(id);
        return Result.success();
    }

    @GetMapping("/operation-logs")
    @RequirePermission("operation-log:view")
    public Result<PageResult<OperationLog>> operationLogs(@Valid OperationLogQueryDTO dto) {
        return Result.success(adminPortalService.pageOperationLogs(dto));
    }

    @PostMapping("/reminders/run")
    @RequirePermission("operation-log:view")
    public Result<Void> runReminders() {
        reminderService.sendDueReminders();
        return Result.success();
    }
}
