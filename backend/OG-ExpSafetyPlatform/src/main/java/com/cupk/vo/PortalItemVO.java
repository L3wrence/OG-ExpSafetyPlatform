package com.cupk.vo;

import lombok.Data;

@Data
public class PortalItemVO {
    private Long id;
    private String title;
    private String description;
    private String type;
    private String status;
    private String time;
    private String path;
    private Object value;
}
