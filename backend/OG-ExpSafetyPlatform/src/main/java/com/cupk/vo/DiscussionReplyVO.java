package com.cupk.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DiscussionReplyVO {
    private Long id;
    private Long topicId;
    private Long userId;
    private String userName;
    private String content;
    private Integer isTeacherReply;
    private LocalDateTime createTime;
}
