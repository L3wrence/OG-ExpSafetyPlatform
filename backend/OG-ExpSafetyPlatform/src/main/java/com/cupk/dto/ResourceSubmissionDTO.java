package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResourceSubmissionDTO {
    @NotBlank(message = "资源标题不能为空")
    @Size(max = 180, message = "资源标题不能超过180字")
    private String title;
    @NotBlank(message = "资源类型不能为空")
    @Size(max = 40, message = "资源类型不能超过40字")
    private String resourceType;
    private String knowledgePoint;
    private String riskType;
    private String tags;
    @Size(max = 1000, message = "资源简介不能超过1000字")
    private String description;
    private String url;
    private String filePath;
    private String originalFilename;
    private String contentType;
}
