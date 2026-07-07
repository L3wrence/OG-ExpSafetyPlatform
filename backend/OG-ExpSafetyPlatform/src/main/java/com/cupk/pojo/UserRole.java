package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_user_role")
public class UserRole {
    @TableId(value = "user_id")
    private Long userId;
    private Long roleId;
}
