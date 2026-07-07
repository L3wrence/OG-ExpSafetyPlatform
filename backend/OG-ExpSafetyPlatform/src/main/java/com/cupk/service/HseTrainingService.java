package com.cupk.service;

import com.cupk.dto.HsePracticeQueryDTO;
import com.cupk.dto.HsePracticeSubmitDTO;
import com.cupk.pojo.HseWrongQuestion;
import com.cupk.vo.HsePracticeResultVO;
import com.cupk.vo.HseQuestionVO;
import com.cupk.vo.HseWeakPointVO;

import java.util.List;

public interface HseTrainingService {
    List<HseQuestionVO> practice(HsePracticeQueryDTO dto);
    HsePracticeResultVO submit(HsePracticeSubmitDTO dto);
    List<HseWrongQuestion> wrongBook();
    List<HseWeakPointVO> myWeakPoints();
    List<HseWeakPointVO> classWeakPoints(Long courseId);
    void favorite(Long questionId);
    void unfavorite(Long questionId);
}
