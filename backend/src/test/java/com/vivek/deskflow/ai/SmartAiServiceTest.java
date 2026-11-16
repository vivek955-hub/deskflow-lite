package com.vivek.deskflow.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivek.deskflow.ai.AiService.TriageResult;

class SmartAiServiceTest {

    @Test
    void extractJsonIgnoresTextAroundTheObject() {
        String answer = "Sure! ```json\n{\"category\":\"BILLING\"}\n``` hope that helps";
        assertEquals("{\"category\":\"BILLING\"}", SmartAiService.extractJson(answer));
    }

    @Test
    void extractJsonFailsWhenThereIsNoObject() {
        assertThrows(IllegalArgumentException.class, () -> SmartAiService.extractJson("no json here"));
    }

    @Test
    void withoutAnApiKeyTheRuleEngineAnswers() {
        LlmClient disabled = new LlmClient("https://example.invalid", "", "some-model");
        SmartAiService service = new SmartAiService(disabled, new RuleBasedAi(), new ObjectMapper());
        TriageResult result = service.triage("Refund please", "I was charged twice.");
        assertEquals("RULES", result.source());
    }
}
