package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限表 t_permission
 * 成员A交付
 */
@Data
@TableName("t_permission")
public class Permission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private Integer type;       // 1菜单 2按钮
    private Long parentId;
    private String path;
    private String icon;
    private Integer sort;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private List<Permission> children;
}
