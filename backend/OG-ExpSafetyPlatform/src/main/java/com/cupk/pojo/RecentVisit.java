package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_recent_visit")
public class RecentVisit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String path;
    private String module;
    private Integer visitCount;
    private LocalDateTime lastVisitTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
