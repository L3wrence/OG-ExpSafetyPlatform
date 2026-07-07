package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceUpdateDTO {
    private Long courseId;
    @NotNull(message = "所属实验不能为空")
    private Long experimentId;
    @NotBlank(message = "资源标题不能为空")
    private String title;
    @NotBlank(message = "资源类型不能为空")
    private String resourceType;
    private String knowledgePoint;
    private String riskType;
    private String tags;
    private String category;
    private String description;
    private String url;
    private String filePath;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private Integer requiredFlag;
    private String completionRule;
    private Integer minStudySeconds;
    private Integer minProgress;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private String openScope;
    private Integer invalidFlag;
    private Integer status;
    private Integer sort;
}
