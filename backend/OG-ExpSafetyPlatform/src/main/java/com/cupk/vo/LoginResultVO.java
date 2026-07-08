package com.cupk.vo;

import lombok.Data;
import java.util.List;

@Data
public class LoginResultVO {
    private String token;
    private String role;
    private List<String> roles;
    private UserInfoVO userInfo;
    private List<MenuVO> menus;
    private List<String> permissions;
}
