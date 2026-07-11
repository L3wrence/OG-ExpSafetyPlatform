package com.cupk.vo.ai;

import lombok.Data;

@Data
public class AiSourceVO {
    private Long resourceId;
    private Long experimentId;
    private String title;
    private String knowledgePoint;
    private String riskType;
}
