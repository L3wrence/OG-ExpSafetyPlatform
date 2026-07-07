package com.cupk.dto;

import lombok.Data;

@Data
public class PortalNoticeQueryDTO {
    private Long pageNum = 1L;
    private Long pageSize = 10L;
    private String keyword;
    private String targetRole;
    private String priority;
    private Integer status;
}
