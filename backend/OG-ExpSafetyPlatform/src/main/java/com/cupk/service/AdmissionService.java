package com.cupk.service;

import com.cupk.pojo.ExamPaper;
import com.cupk.pojo.ExamRecord;
import com.cupk.pojo.ExperimentAdmission;

import java.util.Map;

public interface AdmissionService {
    ExperimentAdmission issueOnPassedExam(ExamRecord record, ExamPaper paper);

    void revokeByExamRecord(Long recordId, String reason);

    Map<String, Object> getAdmissionStatus(Long studentId, Long experimentId);

    void assertReservable(Long studentId, Long experimentId);
}
