package com.cupk.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI Model Client (DeepSeek / Tongyi Qianwen / OpenAI compatible)
 *
 * Usage:
 *   1. Register at https://platform.deepseek.com to get API Key
 *   2. Set api-key in application.yml under ai.deepseek.api-key
 *   3. Restart backend
 */
@Component
public class AiClient {

    private final RestTemplate restTemplate;

    @Value("${ai.deepseek.enabled:false}")
    private boolean enabled;

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${ai.deepseek.max-tokens:1024}")
    private int maxTokens;

    @Value("${ai.deepseek.temperature:0.7}")
    private double temperature;

    public AiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isEmpty()
                && !apiKey.equals("sk-your-deepseek-api-key");
    }

    public String getModelName() {
        return model;
    }

    /**
     * Send chat request to AI API
     * @return AI response text, or null on failure (fallback to local template)
     */
    public String chat(String systemPrompt, String userMessage) {
        if (!isEnabled()) return null;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("temperature", temperature);

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> sysMsg = new LinkedHashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);

            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/v1/chat/completions", request, Map.class);

            if (response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices =
                        (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message =
                            (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            System.err.println("[AiClient] AI call failed, fallback to template: " + e.getMessage());
        }
        return null;
    }
}
