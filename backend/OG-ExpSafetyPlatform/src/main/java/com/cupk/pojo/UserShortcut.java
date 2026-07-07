package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_shortcut")
public class UserShortcut {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String path;
    private String icon;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
