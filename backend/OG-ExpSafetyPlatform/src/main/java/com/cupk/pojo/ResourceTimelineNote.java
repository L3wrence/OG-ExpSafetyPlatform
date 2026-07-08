package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_resource_timeline_note")
public class ResourceTimelineNote extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resourceId;
    private Long experimentId;
    private Long userId;
    private Integer positionSeconds;
    private String noteType;
    private String content;
    private String visibility;
    private Integer status;
}
