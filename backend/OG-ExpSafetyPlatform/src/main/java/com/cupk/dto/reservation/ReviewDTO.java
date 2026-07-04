package com.cupk.dto.reservation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审核请求DTO
 */
@Data
public class ReviewDTO {
    @NotBlank(message = "审核状态不能为空")
    private String status;          // APPROVED / REJECTED

    private String reviewComment;   // 审核意见
}
