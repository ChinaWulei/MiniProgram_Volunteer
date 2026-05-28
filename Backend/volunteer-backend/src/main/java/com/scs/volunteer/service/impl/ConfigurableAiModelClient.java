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
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    private final AiProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public ConfigurableAiModelClient(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean available() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank()
                && "gemini".equalsIgnoreCase(properties.getProvider());
    }

    @Override
    @SuppressWarnings("unchecked")
    public String chat(String prompt) {
        String baseUrl = blank(properties.getBaseUrl()) ? DEFAULT_BASE_URL : properties.getBaseUrl();
        String model = blank(properties.getModel()) ? DEFAULT_MODEL : properties.getModel();
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
                        "maxOutputTokens", 1000
                )
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
        return extractText(response.getBody());
    }

    private String extractText(Map<String, Object> responseBody) {
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

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
