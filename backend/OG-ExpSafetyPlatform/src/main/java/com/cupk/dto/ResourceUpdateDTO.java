package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResourceUpdateDTO {
    @NotNull(message = "所属实验不能为空")
    private Long experimentId;
    @NotBlank(message = "资源标题不能为空")
    private String title;
    @NotBlank(message = "资源类型不能为空")
    private String resourceType;
    private String url;
    private String filePath;
    private Long fileSize;
    private Integer requiredFlag;
    private Integer status;
    private Integer sort;
}
