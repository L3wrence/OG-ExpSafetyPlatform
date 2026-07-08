package com.cupk.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceSubmissionVO {
    private Long id;
    private Long submitterId;
    private String submitterName;
    private String title;
    private String resourceType;
    private String knowledgePoint;
    private String riskType;
    private String tags;
    private String description;
    private String url;
    private String filePath;
    private String originalFilename;
    private String contentType;
    private String status;
    private String reviewComment;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
