package com.cupk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateDTO {
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String realName;

    @Size(max = 30, message = "联系方式长度不能超过30个字符")
    private String phone;

    @Size(max = 500, message = "头像地址长度不能超过500个字符")
    private String avatarUrl;

    @Size(max = 100, message = "专业长度不能超过100个字符")
    private String major;

    @Size(max = 100, message = "班级长度不能超过100个字符")
    private String className;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;
}
