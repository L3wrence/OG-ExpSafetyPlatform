package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户表
 */
@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;   // MD5加密
    private String realName;
    private String role;       // STUDENT / TEACHER / ADMIN
    private String phone;
    private String email;
    private Integer status;    // 1启用 0禁用
    private Date createTime;
    private Date updateTime;
}
