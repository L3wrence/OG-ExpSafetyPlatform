package com.cupk.vo;

import com.cupk.pojo.LearningTask;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearningTaskVO {
    private LearningTask task;
    private Boolean opened;
    private Boolean locked;
    private Boolean completed;
    private String state;
    private String actionPath;
    private String lockedReason;
    private String prerequisiteTaskName;
    private LocalDateTime completeTime;
}
