package com.cupk.service;

import com.cupk.common.PageResult;
import com.cupk.dto.OperationLogQueryDTO;
import com.cupk.dto.PortalNoticeQueryDTO;
import com.cupk.dto.PortalNoticeSaveDTO;
import com.cupk.pojo.OperationLog;
import com.cupk.pojo.PortalNotice;

public interface AdminPortalService {
    PageResult<PortalNotice> pageNotices(PortalNoticeQueryDTO dto);
    Long createNotice(PortalNoticeSaveDTO dto);
    void updateNotice(Long id, PortalNoticeSaveDTO dto);
    void publishNotice(Long id);
    void offlineNotice(Long id);
    void deleteNotice(Long id);
    PageResult<OperationLog> pageOperationLogs(OperationLogQueryDTO dto);
}
