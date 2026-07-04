package com.cupk.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardOverviewVO {
    private Long courseCount;
    private Long experimentCount;
    private Long resourceCount;
    private Long studentCount;
    private Long monthReservationCount;
    private Long pendingReservationCount;
    private Long pendingReportCount;
    private BigDecimal examPassRate;
}
