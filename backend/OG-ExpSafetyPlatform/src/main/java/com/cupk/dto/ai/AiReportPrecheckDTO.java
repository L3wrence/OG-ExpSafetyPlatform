package com.cupk.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiReportPrecheckDTO {
    @NotNull(message = "实验不能为空")
    private Long experimentId;

    @NotBlank(message = "报告标题不能为空")
    @Size(max = 200, message = "报告标题不能超过200字符")
    private String title;

    @NotBlank(message = "报告正文不能为空")
    @Size(max = 20000, message = "报告正文不能超过20000字符")
    private String content;
}
