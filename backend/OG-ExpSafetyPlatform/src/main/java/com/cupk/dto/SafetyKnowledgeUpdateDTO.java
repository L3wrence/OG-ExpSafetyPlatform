package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SafetyKnowledgeUpdateDTO {
    private Long experimentId;
    @NotBlank(message = "知识点不能为空")
    private String knowledgePoint;
    private String riskType;
    @NotBlank(message = "知识内容不能为空")
    private String content;
    private Long relatedStepId;
    private Integer status;
}
