package com.cupk.vo;

import com.cupk.pojo.LearningTask;
import lombok.Data;

@Data
public class LearningTaskDistributionVO {
    private LearningTask task;
    private Integer studentCount;
    private Integer notStartedCount;
    private Integer inProgressCount;
    private Integer completedCount;
    private Integer dueSoonCount;
    private Integer overdueCount;
}
