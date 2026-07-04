package com.cupk.vo;

import com.cupk.pojo.Experiment;
import com.cupk.pojo.ExperimentStep;
import com.cupk.pojo.SafetyKnowledge;
import com.cupk.pojo.TeachingResource;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ExperimentDetailVO {
    private Experiment experiment;
    private String courseName;
    private List<ExperimentStep> steps;
    private List<TeachingResource> resources;
    private List<SafetyKnowledge> safetyKnowledge;
    private BigDecimal learningProgress;
    private Boolean examPassed;
    private Boolean reservationAllowed;
}
