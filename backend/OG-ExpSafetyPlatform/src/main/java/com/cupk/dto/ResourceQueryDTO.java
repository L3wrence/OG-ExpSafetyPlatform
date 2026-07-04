package com.cupk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ResourceQueryDTO {
    @Min(1)
    private Long pageNum = 1L;
    @Min(1)
    @Max(100)
    private Long pageSize = 10L;
    private String keyword;
    private Long experimentId;
    private String resourceType;
    private Integer requiredFlag;
    private Integer status;
}
