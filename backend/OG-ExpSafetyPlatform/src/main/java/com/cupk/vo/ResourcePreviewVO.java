package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResourcePreviewVO {
    private Long id;
    private String title;
    private String resourceType;
    private String contentType;
    private String previewUrl;
    private String originalFilename;
    private Integer invalidFlag;
    private BigDecimal progress;
    private Integer durationSeconds;
    private Integer lastPositionSeconds;
    private String note;
}
