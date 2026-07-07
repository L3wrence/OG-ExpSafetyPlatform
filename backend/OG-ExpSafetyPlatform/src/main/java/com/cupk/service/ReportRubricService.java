package com.cupk.service;

import com.cupk.dto.ReportRubricGradeDTO;
import com.cupk.dto.ReportRubricItemDTO;
import com.cupk.dto.ReportTemplateDTO;
import com.cupk.pojo.ReportRubricItem;
import com.cupk.pojo.ReportTemplate;

import java.util.List;
import java.util.Map;

public interface ReportRubricService {
    ReportTemplate saveTemplate(ReportTemplateDTO dto);
    ReportTemplate template(Long experimentId);
    List<ReportRubricItem> saveRubric(Long experimentId, List<ReportRubricItemDTO> items);
    List<ReportRubricItem> rubric(Long experimentId);
    void grade(Long reportId, ReportRubricGradeDTO dto);
    Map<String, Object> scoreItems(Long reportScoreId);
}
