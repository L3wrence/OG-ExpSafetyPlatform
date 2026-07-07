package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_role_permission")
public class RolePermission {
    @TableId(value = "role_id")
    private Long roleId;
    private Long permissionId;
}
