package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.PageResult;
import com.cupk.dto.OperationLogQueryDTO;
import com.cupk.dto.PortalNoticeQueryDTO;
import com.cupk.dto.PortalNoticeSaveDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.OperationLogMapper;
import com.cupk.mapper.PortalNoticeMapper;
import com.cupk.mapper.UserMapper;
import com.cupk.pojo.OperationLog;
import com.cupk.pojo.PortalNotice;
import com.cupk.pojo.User;
import com.cupk.service.AdminPortalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AdminPortalServiceImpl implements AdminPortalService {
    private final PortalNoticeMapper noticeMapper;
    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;

    public AdminPortalServiceImpl(PortalNoticeMapper noticeMapper,
                                  OperationLogMapper operationLogMapper,
                                  UserMapper userMapper) {
        this.noticeMapper = noticeMapper;
        this.operationLogMapper = operationLogMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<PortalNotice> pageNotices(PortalNoticeQueryDTO dto) {
        requireAdmin();
        Page<PortalNotice> page = new Page<>(pageNum(dto.getPageNum()), pageSize(dto.getPageSize()));
        LambdaQueryWrapper<PortalNotice> wrapper = new LambdaQueryWrapper<PortalNotice>()
                .eq(PortalNotice::getDeleted, 0)
                .and(StringUtils.hasText(dto.getKeyword()), w -> w
                        .like(PortalNotice::getTitle, dto.getKeyword())
                        .or()
                        .like(PortalNotice::getContent, dto.getKeyword()))
                .eq(StringUtils.hasText(dto.getTargetRole()), PortalNotice::getTargetRole, normalizeRole(dto.getTargetRole()))
                .eq(StringUtils.hasText(dto.getPriority()), PortalNotice::getPriority, normalizePriority(dto.getPriority()))
                .eq(dto.getStatus() != null, PortalNotice::getStatus, dto.getStatus())
                .orderByDesc(PortalNotice::getPublishTime)
                .orderByDesc(PortalNotice::getId);
        Page<PortalNotice> result = noticeMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public Long createNotice(PortalNoticeSaveDTO dto) {
        requireAdmin();
        PortalNotice notice = new PortalNotice();
        fillNotice(notice, dto);
        notice.setCreateBy(UserContext.userId());
        notice.setCreateTime(LocalDateTime.now());
        notice.setDeleted(0);
        noticeMapper.insert(notice);
        writeLog("公告管理", "创建公告", "创建公告：" + notice.getTitle(), "SUCCESS");
        return notice.getId();
    }

    @Override
    @Transactional
    public void updateNotice(Long id, PortalNoticeSaveDTO dto) {
        requireAdmin();
        PortalNotice notice = requireNotice(id);
        fillNotice(notice, dto);
        notice.setUpdateTime(LocalDateTime.now());
        noticeMapper.updateById(notice);
        writeLog("公告管理", "更新公告", "更新公告：" + notice.getTitle(), "SUCCESS");
    }

    @Override
    @Transactional
    public void publishNotice(Long id) {
        changeStatus(id, 1, "发布公告");
    }

    @Override
    @Transactional
    public void offlineNotice(Long id) {
        changeStatus(id, 0, "下线公告");
    }

    @Override
    @Transactional
    public void deleteNotice(Long id) {
        requireAdmin();
        PortalNotice notice = requireNotice(id);
        notice.setDeleted(1);
        notice.setUpdateTime(LocalDateTime.now());
        noticeMapper.updateById(notice);
        writeLog("公告管理", "删除公告", "删除公告：" + notice.getTitle(), "SUCCESS");
    }

    @Override
    public PageResult<OperationLog> pageOperationLogs(OperationLogQueryDTO dto) {
        requireAdmin();
        Page<OperationLog> page = new Page<>(pageNum(dto.getPageNum()), pageSize(dto.getPageSize()));
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .eq(dto.getUserId() != null, OperationLog::getUserId, dto.getUserId())
                .eq(StringUtils.hasText(dto.getModule()), OperationLog::getModule, dto.getModule())
                .eq(StringUtils.hasText(dto.getAction()), OperationLog::getAction, dto.getAction())
                .eq(StringUtils.hasText(dto.getResult()), OperationLog::getResult, dto.getResult())
                .ge(dto.getStartTime() != null, OperationLog::getCreateTime, dto.getStartTime())
                .le(dto.getEndTime() != null, OperationLog::getCreateTime, dto.getEndTime())
                .and(StringUtils.hasText(dto.getKeyword()), w -> w
                        .like(OperationLog::getUsername, dto.getKeyword())
                        .or()
                        .like(OperationLog::getContent, dto.getKeyword()))
                .orderByDesc(OperationLog::getCreateTime)
                .orderByDesc(OperationLog::getId);
        Page<OperationLog> result = operationLogMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    private void changeStatus(Long id, Integer status, String action) {
        requireAdmin();
        PortalNotice notice = requireNotice(id);
        notice.setStatus(status);
        if (status == 1 && notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        notice.setUpdateTime(LocalDateTime.now());
        noticeMapper.updateById(notice);
        writeLog("公告管理", action, action + "：" + notice.getTitle(), "SUCCESS");
    }

    private void fillNotice(PortalNotice notice, PortalNoticeSaveDTO dto) {
        notice.setTitle(dto.getTitle().trim());
        notice.setContent(dto.getContent().trim());
        notice.setTargetRole(normalizeRole(dto.getTargetRole()));
        notice.setPriority(normalizePriority(dto.getPriority()));
        notice.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        notice.setPublishTime(dto.getPublishTime() == null ? LocalDateTime.now() : dto.getPublishTime());
        notice.setExpireTime(dto.getExpireTime());
    }

    private PortalNotice requireNotice(Long id) {
        PortalNotice notice = noticeMapper.selectById(id);
        if (notice == null || Integer.valueOf(1).equals(notice.getDeleted())) {
            throw new BusinessException(404, "公告不存在");
        }
        return notice;
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "仅管理员可以操作公告和日志");
        }
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "ALL";
        }
        String value = role.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "ADMIN", "TEACHER", "STUDENT", "LAB_ADMIN", "ALL" -> value;
            default -> "ALL";
        };
    }

    private String normalizePriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return "MEDIUM";
        }
        String value = priority.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "HIGH", "MEDIUM", "LOW" -> value;
            default -> "MEDIUM";
        };
    }

    private long pageNum(Long value) {
        return value == null || value < 1 ? 1 : value;
    }

    private long pageSize(Long value) {
        if (value == null || value < 1) return 10;
        return Math.min(value, 100);
    }

    private void writeLog(String module, String action, String content, String result) {
        OperationLog log = new OperationLog();
        log.setUserId(UserContext.userId());
        User user = userMapper.selectById(UserContext.userId());
        log.setUsername(user == null ? null : user.getUsername());
        log.setModule(module);
        log.setAction(action);
        log.setContent(content);
        log.setResult(result);
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
