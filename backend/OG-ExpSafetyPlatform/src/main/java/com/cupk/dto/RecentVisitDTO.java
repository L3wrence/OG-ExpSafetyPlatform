package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecentVisitDTO {
    @NotBlank(message = "访问标题不能为空")
    @Size(max = 120, message = "访问标题长度不能超过120个字符")
    private String title;

    @NotBlank(message = "访问路径不能为空")
    @Size(max = 255, message = "访问路径长度不能超过255个字符")
    private String path;

    @Size(max = 50, message = "模块长度不能超过50个字符")
    private String module;
}
