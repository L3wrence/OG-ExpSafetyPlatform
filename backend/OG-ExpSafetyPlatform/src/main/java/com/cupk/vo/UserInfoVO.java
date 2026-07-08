package com.cupk.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String avatarUrl;
    private String major;
    private String className;
    private String email;
    private Integer status;
    private String role;
    private Boolean teacherCertified;
    private List<String> roles;
}
