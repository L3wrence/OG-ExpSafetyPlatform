package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShortcutUpdateDTO {
    @NotBlank(message = "快捷入口标题不能为空")
    @Size(max = 80, message = "快捷入口标题长度不能超过80个字符")
    private String title;

    @NotBlank(message = "快捷入口路径不能为空")
    @Size(max = 255, message = "快捷入口路径长度不能超过255个字符")
    private String path;

    @Size(max = 50, message = "图标长度不能超过50个字符")
    private String icon;

    private Integer sort;
}
