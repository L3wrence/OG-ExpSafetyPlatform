package com.cupk.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class UserDeleteDTO {
    @NotNull(message = "用户ID不能为空")
    private Long id;
}
