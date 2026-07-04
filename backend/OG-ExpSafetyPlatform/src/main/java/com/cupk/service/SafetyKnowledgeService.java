package com.cupk.service;

import com.cupk.common.PageResult;
import com.cupk.dto.SafetyKnowledgeCreateDTO;
import com.cupk.dto.SafetyKnowledgeQueryDTO;
import com.cupk.dto.SafetyKnowledgeUpdateDTO;
import com.cupk.pojo.SafetyKnowledge;

public interface SafetyKnowledgeService {
    PageResult<SafetyKnowledge> page(SafetyKnowledgeQueryDTO dto);
    Long create(SafetyKnowledgeCreateDTO dto);
    void update(Long id, SafetyKnowledgeUpdateDTO dto);
    void delete(Long id);
}
