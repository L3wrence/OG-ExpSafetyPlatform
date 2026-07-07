package com.cupk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResourceInteractionDTO {
    private Integer favoriteFlag;
    private Integer likeFlag;
    @Min(0)
    @Max(5)
    private BigDecimal rating;
    private String comment;
}
