package com.scs.volunteer.service.impl;

import com.scs.volunteer.config.AiProperties;
import com.scs.volunteer.service.AiModelClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class ConfigurableAiModelClient implements AiModelClient {
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String GEMINI_MODEL = "gemini-2.5-flash";
    private static final String OPENAI_BASE_URL = "https://api.openai.com";
    private static final String OPENAI_MODEL = "gpt-4.1-mini";

    private final AiProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public ConfigurableAiModelClient(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean available() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank()
                && ("gemini".equalsIgnoreCase(properties.getProvider())
                || "openai".equalsIgnoreCase(properties.getProvider()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String chat(String prompt) {
        if ("openai".equalsIgnoreCase(properties.getProvider())) {
            return openAiChat(prompt);
        }
        return geminiChat(prompt);
    }

    @SuppressWarnings("unchecked")
    private String geminiChat(String prompt) {
        String baseUrl = blank(properties.getBaseUrl()) ? GEMINI_BASE_URL : properties.getBaseUrl();
        String model = blank(properties.getModel()) ? GEMINI_MODEL : properties.getModel();
        String url = trimTrailingSlash(baseUrl) + "/v1beta/models/" + model + ":generateContent";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", properties.getApiKey());

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 1600
                )
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
        return extractGeminiText(response.getBody());
    }

    private String openAiChat(String prompt) {
        String baseUrl = blank(properties.getBaseUrl()) ? OPENAI_BASE_URL : properties.getBaseUrl();
        String model = blank(properties.getModel()) ? OPENAI_MODEL : properties.getModel();
        String url = trimTrailingSlash(baseUrl) + "/v1/responses";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        Map<String, Object> body = Map.of(
                "model", model,
                "input", prompt,
                "temperature", 0.3,
                "max_output_tokens", 1600
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
        return extractOpenAiText(response.getBody());
    }

    private String extractGeminiText(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return "";
        }
        Object candidatesValue = responseBody.get("candidates");
        if (!(candidatesValue instanceof List<?> candidates) || candidates.isEmpty()) {
            return "";
        }
        Object firstCandidate = candidates.get(0);
        if (!(firstCandidate instanceof Map<?, ?> candidate)) {
            return "";
        }
        Object contentValue = candidate.get("content");
        if (!(contentValue instanceof Map<?, ?> content)) {
            return "";
        }
        Object partsValue = content.get("parts");
        if (!(partsValue instanceof List<?> parts) || parts.isEmpty()) {
            return "";
        }
        Object firstPart = parts.get(0);
        if (!(firstPart instanceof Map<?, ?> part)) {
            return "";
        }
        Object text = part.get("text");
        return text == null ? "" : text.toString();
    }

    private String extractOpenAiText(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return "";
        }
        Object outputText = responseBody.get("output_text");
        if (outputText != null && !outputText.toString().isBlank()) {
            return outputText.toString();
        }
        Object outputValue = responseBody.get("output");
        if (!(outputValue instanceof List<?> output)) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (Object itemValue : output) {
            if (!(itemValue instanceof Map<?, ?> item)) continue;
            Object contentValue = item.get("content");
            if (!(contentValue instanceof List<?> content)) continue;
            for (Object partValue : content) {
                if (!(partValue instanceof Map<?, ?> part)) continue;
                Object partText = part.get("text");
                if (partText == null) partText = part.get("output_text");
                if (partText != null && !partText.toString().isBlank()) {
                    if (!text.isEmpty()) text.append("\n");
                    text.append(partText);
                }
            }
        }
        return text.toString();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
