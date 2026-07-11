package com.cupk.vo.ai;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiReportPrecheckVO {
    private Long recordId;
    private String overallStatus;
    private String summary;
    private List<String> missingItems = new ArrayList<>();
    private List<String> evidenceNeeded = new ArrayList<>();
    private List<String> safetyQuestions = new ArrayList<>();
    private List<AiRewriteHintVO> rewriteHints = new ArrayList<>();
    private String fabricationWarning;
    private Boolean fallback;
    private String model;
    private String disclaimer;
}
