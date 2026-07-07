package com.cupk.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogQueryDTO {
    private Long pageNum = 1L;
    private Long pageSize = 10L;
    private String keyword;
    private String module;
    private String action;
    private String result;
    private Long userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
