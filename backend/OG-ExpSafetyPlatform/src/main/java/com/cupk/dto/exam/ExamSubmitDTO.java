package com.cupk.dto.exam;

import lombok.Data;

import java.util.List;

/**
 * 提交答案请求DTO
 */
@Data
public class ExamSubmitDTO {
    /** 答案列表 */
    private List<AnswerItemDTO> answers;
}
