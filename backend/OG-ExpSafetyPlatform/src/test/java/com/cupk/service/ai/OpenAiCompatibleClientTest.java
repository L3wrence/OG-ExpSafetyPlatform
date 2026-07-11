package com.cupk.service.ai;

import com.cupk.config.AiProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class OpenAiCompatibleClientTest {
    @Test
    void unavailableWhenDisabledOrKeyMissing() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(false);
        properties.setApiKey("");
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(properties);
        assertFalse(client.isAvailable());
        properties.setEnabled(true);
        assertFalse(client.isAvailable());
    }
}
