package com.cupk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.pojo.AiChatRecord;

import java.util.Map;

/**
 * AI问答服务接口
 */
public interface AiChatService {

    /** AI问答 */
    Map<String, Object> ask(String scene, String question, Long experimentId);

    /** 我的AI问答历史 */
    Page<AiChatRecord> getRecords(int pageNum, int pageSize, String scene);

    /** 记录人工修改 */
    void updateFeedback(Long id, String manualRevision);
}
