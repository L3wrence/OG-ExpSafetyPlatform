package com.cupk.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class HsePracticeSubmitDTO {
    private String practiceType = "RANDOM";
    @Valid
    @NotEmpty(message = "答案不能为空")
    private List<HsePracticeSubmitItemDTO> answers;
}
