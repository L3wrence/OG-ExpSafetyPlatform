package com.cupk.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateDTO {
    @NotNull(message = "用户ID不能为空")
    private Long id;
    private String realName;
    private String phone;
    private Integer status;
    private Long roleId;
}
