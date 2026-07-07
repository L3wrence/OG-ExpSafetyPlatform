package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_discussion_reply")
public class DiscussionReply extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long topicId;
    private Long userId;
    private String content;
    private Integer isTeacherReply;
}
