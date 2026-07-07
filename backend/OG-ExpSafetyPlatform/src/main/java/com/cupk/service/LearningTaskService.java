package com.cupk.service;

import com.cupk.common.PageResult;
import com.cupk.dto.LearningTaskCreateDTO;
import com.cupk.dto.LearningTaskQueryDTO;
import com.cupk.dto.LearningTaskUpdateDTO;
import com.cupk.pojo.LearningTask;
import com.cupk.vo.LearningPathVO;
import com.cupk.vo.LearningTaskDistributionVO;

import java.util.List;

public interface LearningTaskService {
    PageResult<LearningTask> page(LearningTaskQueryDTO dto);
    Long create(LearningTaskCreateDTO dto);
    void update(Long id, LearningTaskUpdateDTO dto);
    void disable(Long id);
    LearningPathVO path(Long experimentId);
    void confirmChecklist(Long id);
    void completeSafetyKnowledge(Long knowledgeId);
    void syncResourceCompleted(Long studentId, Long resourceId);
    void syncExamCompleted(Long studentId, Long paperId, Long experimentId);
    List<LearningTaskDistributionVO> distribution(Long experimentId);
}
