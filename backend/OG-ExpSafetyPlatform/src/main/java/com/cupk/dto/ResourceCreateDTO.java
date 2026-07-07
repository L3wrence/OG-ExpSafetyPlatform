package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceCreateDTO {
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
    private String category = "EXTENSION";
    private String description;
    private String url;
    private String filePath;
    private String originalFilename;
    private String contentType;
    private Long fileSize = 0L;
    private Integer requiredFlag = 0;
    private String completionRule = "CONFIRM";
    private Integer minStudySeconds = 0;
    private Integer minProgress = 100;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private String openScope = "COURSE";
    private Integer status = 1;
    private Integer sort = 0;
}
