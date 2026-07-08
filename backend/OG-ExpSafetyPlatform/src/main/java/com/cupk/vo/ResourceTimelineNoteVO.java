package com.cupk.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceTimelineNoteVO {
    private Long id;
    private Long resourceId;
    private Long experimentId;
    private Long userId;
    private String userName;
    private Integer positionSeconds;
    private String noteType;
    private String content;
    private String visibility;
    private LocalDateTime createTime;
}
