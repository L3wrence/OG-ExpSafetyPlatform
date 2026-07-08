package com.cupk.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResourceTimelineNoteDTO {
    private Long resourceId;
    private Long experimentId;
    @Min(value = 0, message = "时间点不能小于0")
    private Integer positionSeconds = 0;
    private String noteType = "NOTE";
    @NotBlank(message = "内容不能为空")
    @Size(max = 1000, message = "内容不能超过1000字")
    private String content;
    private String visibility = "PRIVATE";
}
