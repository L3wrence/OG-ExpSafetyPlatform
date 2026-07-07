package com.cupk.vo;

import lombok.Data;

@Data
public class SearchResultVO {
    private Long id;
    private String title;
    private String type;
    private String description;
    private String path;
}
