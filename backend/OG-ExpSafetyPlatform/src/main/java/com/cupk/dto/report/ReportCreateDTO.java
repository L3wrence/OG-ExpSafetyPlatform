package com.cupk.dto.report;

import lombok.Data;

/**
 * 报告创建/编辑请求DTO
 */
@Data
public class ReportCreateDTO {
    private Long experimentId;
    private String title;
    private String content;
    private String fileUrl;
}
