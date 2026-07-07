package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportTemplateDTO {
    @NotNull(message = "实验ID不能为空")
    private Long experimentId;
    @NotBlank(message = "模板标题不能为空")
    @Size(max = 200, message = "模板标题不能超过200个字符")
    private String title;
    @NotBlank(message = "模板结构不能为空")
    private String schemaJson;
    private Integer status;
}
