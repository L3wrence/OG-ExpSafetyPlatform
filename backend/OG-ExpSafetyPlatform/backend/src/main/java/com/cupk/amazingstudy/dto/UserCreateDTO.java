package com.cupk.amazingstudy.dto;


import lombok.Data;
import jakarta.validation.constraints.NotBlank;
// ... existing code ...


@Data
public class UserCreateDTO {
    @NotBlank(message = "学号/工号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String realName;
    private String phone;
    private Long roleId;
}