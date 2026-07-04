package com.cupk.dto.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 预约创建请求DTO
 */
@Data
public class ReservationCreateDTO {
    @NotNull(message = "时间段ID不能为空")
    private Long timeSlotId;

    @NotNull(message = "实验室ID不能为空")
    private Long labId;

    private Long experimentId;
    private String purpose;
}
