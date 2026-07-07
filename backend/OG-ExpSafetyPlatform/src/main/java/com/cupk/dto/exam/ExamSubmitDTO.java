package com.cupk.dto.exam;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

/**
 * 提交答案请求DTO
 */
@Data
public class ExamSubmitDTO {
    @NotEmpty(message = "答案列表不能为空")
    private List<AnswerItemDTO> answers;
    private Boolean autoSubmit;
}
