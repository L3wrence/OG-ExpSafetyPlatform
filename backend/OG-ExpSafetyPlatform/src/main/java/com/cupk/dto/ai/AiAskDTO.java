package com.cupk.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI问答请求DTO
 */
@Data
public class AiAskDTO {
    /** SAFETY_QA / ERROR_EXPLAIN / REPORT_SUGGEST */
    @NotBlank(message = "场景不能为空")
    private String scene;

    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题长度不能超过2000字符")
    private String question;

    private Long courseId;
    private Long experimentId;
    private Long resourceId;
}
