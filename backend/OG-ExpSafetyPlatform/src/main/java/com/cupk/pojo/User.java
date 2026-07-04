package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户表 t_user
 * 成员A交付（整合 RBAC 支持）
 */
@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;    // MD5加密
    private String realName;
    @TableField(exist = false)
    private String role;        // STUDENT / TEACHER / ADMIN（非DB字段，来自RBAC查询）
    private String phone;
    @TableField(exist = false)
    private String email;
    private Integer status;     // 1启用 0禁用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
