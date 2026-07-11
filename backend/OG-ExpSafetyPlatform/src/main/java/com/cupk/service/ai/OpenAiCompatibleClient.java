package com.cupk.service.ai;

import com.cupk.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleClient implements AiModelClient {
    private final AiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;

    public OpenAiCompatibleClient(AiProperties properties) {
        this.properties = properties;
        Duration timeout = Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds()));
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled() && StringUtils.hasText(properties.getApiKey());
    }

    @Override
    public String generateJson(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new IllegalStateException("AI 模型未启用或未配置 API Key");
        }
        String baseUrl = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().replaceAll("/+$", "");
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "thinking", Map.of("type", "disabled"),
                "temperature", properties.getTemperature(),
                "max_tokens", properties.getMaxTokens());
        String response = restClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(body)
                .retrieve()
                .body(String.class);
        if (!StringUtils.hasText(response)) {
            throw new IllegalStateException("模型服务返回空响应");
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || !StringUtils.hasText(content.asText())) {
                throw new IllegalStateException("模型响应结构无效");
            }
            return content.asText();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("模型响应解析失败", e);
        }
    }

    @Override
    public String modelName() {
        return properties.getModel();
    }
}
