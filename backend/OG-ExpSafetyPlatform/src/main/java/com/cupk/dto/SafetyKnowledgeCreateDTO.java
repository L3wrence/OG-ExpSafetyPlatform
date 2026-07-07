package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SafetyKnowledgeCreateDTO {
    private Long experimentId;
    private String category;
    @NotBlank(message = "知识点不能为空")
    private String knowledgePoint;
    private String riskType;
    @NotBlank(message = "知识内容不能为空")
    private String content;
    private Long relatedStepId;
    private Long referenceResourceId;
    private Integer emergencyFlag = 0;
    private Integer status = 1;
}
