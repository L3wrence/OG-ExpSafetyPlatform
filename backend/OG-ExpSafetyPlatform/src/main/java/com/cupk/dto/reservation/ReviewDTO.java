package com.cupk.dto.reservation;

import lombok.Data;

/**
 * 审核请求DTO
 */
@Data
public class ReviewDTO {
    /** APPROVED / REJECTED */
    private String status;
    private String reviewComment;
}
