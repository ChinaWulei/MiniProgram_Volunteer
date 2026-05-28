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
public class ConfigurableEmbeddingService implements EmbeddingService {
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String GEMINI_MODEL = "text-embedding-004";
    private static final String OPENAI_BASE_URL = "https://api.openai.com";
    private static final String OPENAI_MODEL = "text-embedding-3-small";

    private final AiProperties aiProperties;
    private final RagProperties ragProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public ConfigurableEmbeddingService(AiProperties aiProperties, RagProperties ragProperties) {
        this.aiProperties = aiProperties;
        this.ragProperties = ragProperties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new BizException("AI API Key 未配置");
        }
        if ("openai".equalsIgnoreCase(aiProperties.getProvider())) {
            return openAiEmbed(text);
        }
        return geminiEmbed(text);
    }

    @SuppressWarnings("unchecked")
    private float[] geminiEmbed(String text) {
        String baseUrl = blank(aiProperties.getBaseUrl()) ? GEMINI_BASE_URL : aiProperties.getBaseUrl();
        String model = blank(aiProperties.getEmbeddingModel()) ? GEMINI_MODEL : aiProperties.getEmbeddingModel();
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
        if (!(embeddingValue instanceof Map<?, ?> embedding)) throw new BizException("AI embedding 响应为空");
        Object valuesValue = embedding.get("values");
        if (!(valuesValue instanceof List<?> values)) throw new BizException("AI embedding values 为空");
        return toFloatArray(values);
    }

    @SuppressWarnings("unchecked")
    private float[] openAiEmbed(String text) {
        String baseUrl = blank(aiProperties.getBaseUrl()) ? OPENAI_BASE_URL : aiProperties.getBaseUrl();
        String model = blank(aiProperties.getEmbeddingModel()) ? OPENAI_MODEL : aiProperties.getEmbeddingModel();
        String url = trimTrailingSlash(baseUrl) + "/v1/embeddings";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        Map<String, Object> body = Map.of(
                "model", model,
                "input", text,
                "dimensions", ragProperties.getEmbeddingDimensions()
        );

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        Object dataValue = response.getBody() == null ? null : response.getBody().get("data");
        if (!(dataValue instanceof List<?> data) || data.isEmpty()) throw new BizException("AI embedding 响应为空");
        Object firstValue = data.get(0);
        if (!(firstValue instanceof Map<?, ?> first)) throw new BizException("AI embedding 响应为空");
        Object embeddingValue = first.get("embedding");
        if (!(embeddingValue instanceof List<?> values)) throw new BizException("AI embedding values 为空");
        return toFloatArray(values);
    }

    private float[] toFloatArray(List<?> values) {
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
