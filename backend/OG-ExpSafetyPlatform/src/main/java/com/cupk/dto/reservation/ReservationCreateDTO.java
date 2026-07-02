package com.cupk.dto.reservation;

import lombok.Data;

/**
 * 预约创建请求DTO
 */
@Data
public class ReservationCreateDTO {
    private Long timeSlotId;
    private Long labId;
    private Long experimentId;
    private String purpose;
}
