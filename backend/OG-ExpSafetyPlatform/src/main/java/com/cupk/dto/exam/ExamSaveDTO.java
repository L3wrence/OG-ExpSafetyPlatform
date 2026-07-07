package com.cupk.dto.exam;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExamSaveDTO {
    private List<AnswerItemDTO> answers = new ArrayList<>();
}
