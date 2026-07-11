package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_resource_submission")
public class ResourceSubmission extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submitterId;
    private String title;
    private String resourceType;
    private String businessCategory;
    private String knowledgePoint;
    private String riskType;
    private String tags;
    private String description;
    private String filePath;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private String status;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime reviewTime;
    private Long publicResourceId;
}
