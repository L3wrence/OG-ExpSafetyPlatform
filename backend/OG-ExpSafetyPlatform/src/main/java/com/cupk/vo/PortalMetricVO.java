package com.cupk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortalMetricVO {
    private String code;
    private String label;
    private Object value;
    private String unit;
    private String path;
    private String type;
}
