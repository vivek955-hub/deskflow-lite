package com.vivek.deskflow.ai;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A tiny client for any OpenAI-compatible chat API (Groq, OpenAI, a local Ollama server...).
 * It is only switched on when AI_API_KEY is set.
 */
@Component
public class LlmClient {
    private final RestClient http;
    private final String model;
    private final boolean enabled;

    public LlmClient(@Value("${app.ai.base-url:}") String baseUrl,
                     @Value("${app.ai.api-key:}") String apiKey,
                     @Value("${app.ai.model:}") String model) {
        this.enabled = !apiKey.isBlank() && !baseUrl.isBlank() && !model.isBlank();
        this.model = model;
        if (enabled) {
            // Short timeouts: a slow AI call must never make the helpdesk feel stuck.
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5_000);
            factory.setReadTimeout(20_000);
            this.http = RestClient.builder()
                    .baseUrl(baseUrl)
                    .requestFactory(factory)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .build();
        } else {
            this.http = null;
        }
    }

    public boolean isEnabled() { return enabled; }

    /** Sends one system + one user message and returns the model's text answer. */
    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        JsonNode response = http.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String text = response == null ? "" : response.path("choices").path(0).path("message").path("content").asText("");
        if (text.isBlank()) {
            throw new IllegalStateException("The AI model returned an empty answer");
        }
        return text.trim();
    }
}
