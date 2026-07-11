package com.cupk.service.ai;

import com.cupk.vo.ai.AiAnswerVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiResponseParserTest {
    private final AiResponseParser parser = new AiResponseParser();

    @Test
    void parsesPlainJson() {
        AiAnswerVO result = parser.parse("{\"answer\":\"安全回答\",\"keyPoints\":[\"要点\"]}", AiAnswerVO.class);
        assertEquals("安全回答", result.getAnswer());
        assertEquals(1, result.getKeyPoints().size());
    }

    @Test
    void parsesMarkdownFenceAndSurroundingText() {
        AiAnswerVO result = parser.parse("说明文字\n```json\n{\"answer\":\"围栏回答\"}\n```\n结束", AiAnswerVO.class);
        assertEquals("围栏回答", result.getAnswer());
    }

    @Test
    void rejectsInvalidJson() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("not-json", AiAnswerVO.class));
    }
}
