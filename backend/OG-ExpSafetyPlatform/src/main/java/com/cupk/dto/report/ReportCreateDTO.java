package com.cupk.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 报告创建/编辑请求DTO
 */
@Data
public class ReportCreateDTO {
    @NotNull(message = "实验项目ID不能为空")
    private Long experimentId;

    @NotBlank(message = "报告标题不能为空")
    private String title;

    @NotBlank(message = "报告内容不能为空")
    private String content;

    private String fileUrl;
}
