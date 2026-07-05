package com.cupk.dto.reservation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewDTO {
    @NotBlank(message = "审核状态不能为空")
    private String status;

    private String reviewComment;
}
