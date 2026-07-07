package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_portal_notice")
public class PortalNotice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String targetRole;
    private String priority;
    private Integer status;
    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
