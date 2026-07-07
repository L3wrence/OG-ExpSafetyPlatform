package com.cupk.service;

import com.cupk.dto.LearningProgressDTO;
import com.cupk.pojo.LearningRecord;
import com.cupk.vo.LearningProgressVO;

import java.util.List;

public interface LearningRecordService {
    void start(Long resourceId);
    void updateProgress(LearningProgressDTO dto);
    LearningRecord resourceRecord(Long resourceId);
    LearningProgressVO experimentProgress(Long experimentId, Long studentId);
    List<LearningRecord> myRecords();
}
