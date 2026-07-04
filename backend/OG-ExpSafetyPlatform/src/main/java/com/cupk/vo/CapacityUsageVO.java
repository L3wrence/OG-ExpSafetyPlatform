package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CapacityUsageVO {
    private Long timeSlotId;
    private Long experimentId;
    private String expName;
    private String slotDate;
    private String startTime;
    private String endTime;
    private Long capacity;
    private Long bookedCount;
    private BigDecimal usageRate;
}
