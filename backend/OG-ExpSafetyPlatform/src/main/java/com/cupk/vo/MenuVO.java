package com.cupk.vo;

import lombok.Data;
import java.util.List;

@Data
public class MenuVO {
    private Long id;
    private String name;
    private String code;
    private String path;
    private String icon;
    private List<MenuVO> children;
}