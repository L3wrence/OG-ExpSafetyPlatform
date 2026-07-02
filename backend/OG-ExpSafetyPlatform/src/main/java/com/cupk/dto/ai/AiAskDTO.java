package com.cupk.dto.ai;

import lombok.Data;

/**
 * AI问答请求DTO
 */
@Data
public class AiAskDTO {
    /** SAFETY_QA / ERROR_EXPLAIN / REPORT_SUGGEST */
    private String scene;
    private String question;
    private Long experimentId;
}
