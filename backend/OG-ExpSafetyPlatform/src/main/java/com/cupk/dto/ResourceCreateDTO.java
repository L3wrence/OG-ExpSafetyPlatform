package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResourceCreateDTO {
    @NotNull(message = "所属实验不能为空")
    private Long experimentId;
    @NotBlank(message = "资源标题不能为空")
    private String title;
    @NotBlank(message = "资源类型不能为空")
    private String resourceType;
    private String url;
    private String filePath;
    private Long fileSize = 0L;
    private Integer requiredFlag = 0;
    private Integer status = 1;
    private Integer sort = 0;
}
