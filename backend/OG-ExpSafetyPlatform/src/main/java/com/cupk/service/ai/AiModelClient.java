package com.cupk.service.ai;

public interface AiModelClient {
    boolean isAvailable();
    String generateJson(String systemPrompt, String userPrompt);
    String modelName();
}
