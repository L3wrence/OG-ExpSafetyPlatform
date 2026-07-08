package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResourcePreviewVO {
    private Long id;
    private String title;
    private String resourceType;
    private String knowledgePoint;
    private String riskType;
    private String tags;
    private String contentType;
    private String previewUrl;
    private String originalFilename;
    private Long experimentId;
    private String experimentName;
    private String completionRule;
    private Integer minStudySeconds;
    private Integer minProgress;
    private String aiSummary;
    private Integer invalidFlag;
    private BigDecimal progress;
    private Integer durationSeconds;
    private Integer lastPositionSeconds;
    private String note;
}
