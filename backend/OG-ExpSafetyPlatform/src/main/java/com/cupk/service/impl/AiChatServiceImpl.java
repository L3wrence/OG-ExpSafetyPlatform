package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.AiChatRecordMapper;
import com.cupk.pojo.AiChatRecord;
import com.cupk.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * AI问答服务实现
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    @Autowired
    private AiChatRecordMapper aiChatRecordMapper;

    @Override
    public Map<String, Object> ask(String scene, String question, Long experimentId) {
        // TODO: 查询本地知识库 → 组装Prompt → 调用AI模型
        String answer = "[AI辅助生成，仅供参考] 关于\"" + question + "\"的解答...";

        // 保存记录
        AiChatRecord record = new AiChatRecord();
        record.setUserId(UserContext.getUserId());
        record.setScene(scene);
        record.setQuestion(question);
        record.setAnswer(answer);
        record.setToolName("Claude");
        record.setExperimentId(experimentId);
        record.setCreateTime(new Date());
        aiChatRecordMapper.insert(record);

        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("id", record.getId());
        return result;
    }

    @Override
    public Page<AiChatRecord> getRecords(int pageNum, int pageSize, String scene) {
        Page<AiChatRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatRecord::getUserId, UserContext.getUserId())
               .eq(scene != null && !scene.isEmpty(), AiChatRecord::getScene, scene)
               .orderByDesc(AiChatRecord::getCreateTime);
        return aiChatRecordMapper.selectPage(page, wrapper);
    }

    @Override
    public void updateFeedback(Long id, String manualRevision) {
        AiChatRecord record = new AiChatRecord();
        record.setId(id);
        record.setManualRevision(manualRevision);
        aiChatRecordMapper.updateById(record);
    }
}
