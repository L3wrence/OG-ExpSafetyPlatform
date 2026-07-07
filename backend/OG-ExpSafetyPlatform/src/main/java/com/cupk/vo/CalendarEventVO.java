package com.cupk.vo;

import lombok.Data;

@Data
public class CalendarEventVO {
    private Long id;
    private String title;
    private String type;
    private String startTime;
    private String endTime;
    private String status;
    private String path;
}
