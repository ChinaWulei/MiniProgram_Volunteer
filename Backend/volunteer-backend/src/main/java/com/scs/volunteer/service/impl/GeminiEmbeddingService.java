package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.config.AiProperties;
import com.scs.volunteer.config.RagProperties;
import com.scs.volunteer.service.EmbeddingService;
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
public class GeminiEmbeddingService implements EmbeddingService {
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String DEFAULT_MODEL = "text-embedding-004";

    private final AiProperties aiProperties;
    private final RagProperties ragProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public GeminiEmbeddingService(AiProperties aiProperties, RagProperties ragProperties) {
        this.aiProperties = aiProperties;
        this.ragProperties = ragProperties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new BizException("Gemini API Key 未配置");
        }
        String baseUrl = blank(aiProperties.getBaseUrl()) ? DEFAULT_BASE_URL : aiProperties.getBaseUrl();
        String model = blank(aiProperties.getEmbeddingModel()) ? DEFAULT_MODEL : aiProperties.getEmbeddingModel();
        String url = trimTrailingSlash(baseUrl) + "/v1beta/models/" + model + ":embedContent";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", aiProperties.getApiKey());

        Map<String, Object> body = Map.of(
                "model", "models/" + model,
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "outputDimensionality", ragProperties.getEmbeddingDimensions()
        );

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        Object embeddingValue = response.getBody() == null ? null : response.getBody().get("embedding");
        if (!(embeddingValue instanceof Map<?, ?> embedding)) throw new BizException("Gemini embedding 响应为空");
        Object valuesValue = embedding.get("values");
        if (!(valuesValue instanceof List<?> values)) throw new BizException("Gemini embedding values 为空");
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = ((Number) values.get(i)).floatValue();
        }
        return result;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
