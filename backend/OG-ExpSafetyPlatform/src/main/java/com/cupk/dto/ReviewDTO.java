package com.cupk.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewDTO {
    @Size(max = 500, message = "审核意见不能超过500字")
    private String reviewComment;
}
