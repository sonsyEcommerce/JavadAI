package com.javadai.agent.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Talks to a local Ollama server. No cloud, works offline. */
@Component
public class OllamaClient {

    private final RestClient client;
    private final String model;

    public OllamaClient(@Value("${javadai.ollama.url:http://localhost:11434}") String url,
                        @Value("${javadai.ollama.model:qwen2.5-coder:7b}") String model) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newHttpClient());
        factory.setReadTimeout(Duration.ofMinutes(5));
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
        this.model = model;
    }

    /** Returns the assistant "message" map (content + optional tool_calls). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> body = Map.of(
                "model", model, "messages", messages, "tools", tools, "stream", false,
                "options", Map.of("temperature", 0.2));
        Map<String, Object> res = client.post().uri("/api/chat").body(body).retrieve().body(Map.class);
        if (res == null || res.get("message") == null) throw new IllegalStateException("Empty response from Ollama");
        return (Map<String, Object>) res.get("message");
    }
}
