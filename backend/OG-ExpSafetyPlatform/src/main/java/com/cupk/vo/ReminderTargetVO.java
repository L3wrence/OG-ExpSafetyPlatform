package com.cupk.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReminderTargetVO {
    private Long userId;
    private Long bizId;
    private String title;
    private String content;
    private String path;
    private LocalDateTime eventTime;
}
