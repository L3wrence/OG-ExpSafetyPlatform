package com.cupk.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class AiResponseParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiResponseParser() {}

    public <T> T parse(String response, Class<T> type) {
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("模型响应为空");
        }
        String cleaned = response.replace("```json", "").replace("```JSON", "").replace("```", "").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("模型响应不包含有效 JSON 对象");
        }
        try {
            return objectMapper.readValue(cleaned.substring(start, end + 1), type);
        } catch (Exception e) {
            throw new IllegalArgumentException("模型响应 JSON 解析失败", e);
        }
    }
}
