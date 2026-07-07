package com.cupk.common.enums;

public enum ExamRecordStatus {
    IN_PROGRESS,
    PENDING_REVIEW,
    GRADED,
    EXPIRED;

    public static boolean isFinal(String status) {
        return GRADED.name().equals(status) || EXPIRED.name().equals(status);
    }
}
