package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DiscussionReplyCreateDTO {
    @NotBlank(message = "回复内容不能为空")
    private String content;
}
