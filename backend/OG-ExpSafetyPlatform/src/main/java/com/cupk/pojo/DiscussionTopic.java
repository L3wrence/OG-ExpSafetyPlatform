package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_discussion_topic")
public class DiscussionTopic extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long experimentId;
    private Long userId;
    private String title;
    private String content;
    private String status;
    private Integer isAnonymous;
    private Integer isFeatured;
    private Integer replyCount;
}
