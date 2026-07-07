package com.cupk.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReportRubricGradeDTO {
    @Valid
    @NotEmpty(message = "评分项不能为空")
    private List<ReportRubricScoreItemDTO> items;
    @Size(max = 500, message = "总评不能超过500个字符")
    private String comment;
}
