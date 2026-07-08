package com.cupk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassInviteCreateDTO {
    private Long teachingClassId;
    private LocalDateTime expireTime;
    @Min(value = 1, message = "最大使用次数不能小于1")
    @Max(value = 10000, message = "最大使用次数不能超过10000")
    private Integer maxUses;
}
