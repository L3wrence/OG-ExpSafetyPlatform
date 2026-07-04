package com.cupk.vo;

import lombok.Data;

@Data
public class ResourceRankingVO {
    private Long resourceId;
    private String title;
    private String resourceType;
    private Long viewCount;
}
