package com.cupk.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HsePracticeSubmitItemDTO {
    @NotNull(message = "题目ID不能为空")
    private Long questionId;
    private String answer;
}
