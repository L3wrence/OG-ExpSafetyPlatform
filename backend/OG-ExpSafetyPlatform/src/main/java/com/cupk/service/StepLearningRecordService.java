package com.cupk.service;

import com.cupk.pojo.StepLearningRecord;

import java.util.List;

public interface StepLearningRecordService {
    List<StepLearningRecord> myRecords();

    void complete(Long stepId);
}
