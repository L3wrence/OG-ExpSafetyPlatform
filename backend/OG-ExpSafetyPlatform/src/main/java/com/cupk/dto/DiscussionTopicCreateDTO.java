package com.cupk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DiscussionTopicCreateDTO {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    private Long experimentId;
    @NotBlank(message = "问题标题不能为空")
    @Size(max = 200, message = "问题标题不能超过200个字符")
    private String title;
    @NotBlank(message = "问题内容不能为空")
    private String content;
    private Integer isAnonymous;
}
