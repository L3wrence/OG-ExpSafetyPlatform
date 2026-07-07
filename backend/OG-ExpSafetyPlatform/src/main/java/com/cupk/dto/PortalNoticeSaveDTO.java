package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PortalNoticeSaveDTO {
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题不能超过200字")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    private String targetRole;
    private String priority;
    private Integer status;
    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
}
