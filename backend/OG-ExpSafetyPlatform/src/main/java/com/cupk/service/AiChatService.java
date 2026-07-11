package com.cupk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.pojo.AiChatRecord;
import com.cupk.dto.ai.AiAskDTO;
import com.cupk.dto.ai.AiReportPrecheckDTO;
import com.cupk.vo.ai.AiAnswerVO;
import com.cupk.vo.ai.AiReportPrecheckVO;
import com.cupk.vo.ai.AiWrongAnswerExplainVO;

/**
 * AI问答服务接口
 */
public interface AiChatService {

    /** AI问答 */
    AiAnswerVO ask(AiAskDTO dto);

    AiWrongAnswerExplainVO explainWrongAnswer(Long answerId);

    AiReportPrecheckVO precheckReport(AiReportPrecheckDTO dto);

    /** 我的AI问答历史 */
    Page<AiChatRecord> getRecords(int pageNum, int pageSize, String scene);

    /** 记录人工修改 */
    void updateFeedback(Long id, String manualRevision);
}
