package com.cupk.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassInviteVO {
    private Long id;
    private Long courseId;
    private Long teachingClassId;
    private String className;
    private String inviteCode;
    private LocalDateTime expireTime;
    private Integer maxUses;
    private Integer usedCount;
    private Integer status;
    private LocalDateTime createTime;
}
