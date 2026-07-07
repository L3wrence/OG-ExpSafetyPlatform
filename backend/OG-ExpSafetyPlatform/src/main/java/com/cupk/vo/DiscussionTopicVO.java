package com.cupk.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DiscussionTopicVO {
    private Long id;
    private Long courseId;
    private Long experimentId;
    private Long userId;
    private String userName;
    private String title;
    private String content;
    private String status;
    private Integer isAnonymous;
    private Integer isFeatured;
    private Integer replyCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<DiscussionReplyVO> replies = new ArrayList<>();
}
