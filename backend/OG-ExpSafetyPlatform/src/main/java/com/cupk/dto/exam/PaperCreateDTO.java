package com.cupk.dto.exam;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Date;

/**
 * 试卷创建/编辑请求DTO
 */
@Data
public class PaperCreateDTO {
    @NotBlank(message = "试卷标题不能为空")
    private String title;

    private String description;
    private Long courseId;
    private Integer totalScore;
    private Integer passScore;
    private Integer duration;
    private Date startTime;
    private Date endTime;
}
