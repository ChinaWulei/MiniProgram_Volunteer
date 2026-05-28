package com.scs.volunteer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.config.AiProperties;
import com.scs.volunteer.dto.ActivityAiGenerateRequest;
import com.scs.volunteer.service.ActivityAiGenerateService;
import com.scs.volunteer.service.AiModelClient;
import com.scs.volunteer.service.S3StorageService;
import com.scs.volunteer.vo.ActivityAiGenerateVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ActivityAiGenerateServiceImpl implements ActivityAiGenerateService {
    private static final Logger log = LoggerFactory.getLogger(ActivityAiGenerateServiceImpl.class);
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String IMAGEN_MODEL = "imagen-3.0-generate-002";

    private final AiModelClient aiModelClient;
    private final AiProperties aiProperties;
    private final S3StorageService s3StorageService;
    private final ObjectMapper objectMapper;
    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    public ActivityAiGenerateServiceImpl(AiModelClient aiModelClient, AiProperties aiProperties,
                                         S3StorageService s3StorageService, ObjectMapper objectMapper) {
        this.aiModelClient = aiModelClient;
        this.aiProperties = aiProperties;
        this.s3StorageService = s3StorageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ActivityAiGenerateVO generate(ActivityAiGenerateRequest request, CurrentUser currentUser) {
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new BizException("仅管理员可操作");
        }
        if (request == null || blank(request.getPrompt())) {
            throw new BizException("请输入活动需求");
        }
        if (!aiModelClient.available()) {
            throw new BizException("AI服务未配置");
        }

        String mode = blank(request.getMode()) ? "FULL" : request.getMode().trim().toUpperCase();
        try {
            ActivityAiGenerateVO result = switch (mode) {
                case "DESCRIPTION" -> generatePartial(request, "DESCRIPTION");
                case "SKILLS" -> generatePartial(request, "SKILLS");
                case "COVER" -> generateCoverOnly(request);
                default -> generateFull(request);
            };
            log.info("AI activity generation result adminId={}, mode={}, title={}, coverUrl={}",
                    currentUser.getId(), mode, result.getTitle(), result.getCoverUrl());
            return result;
        } catch (HttpServerErrorException.ServiceUnavailable | ResourceAccessException e) {
            log.warn("AI activity generation upstream busy: {}", e.getMessage(), e);
            throw new BizException("AI服务当前较繁忙，请稍后再试");
        } catch (HttpServerErrorException e) {
            log.warn("AI activity generation upstream failed status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BizException("AI服务暂时不可用，请稍后再试");
        } catch (RestClientException e) {
            log.warn("AI activity generation request failed: {}", e.getMessage(), e);
            throw new BizException("AI服务暂时不可用，请稍后再试");
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("AI activity generation parse failed: {}", e.getMessage(), e);
            throw new BizException("AI生成结果解析失败，请重新生成");
        }
    }

    private ActivityAiGenerateVO generateFull(ActivityAiGenerateRequest request) throws JsonProcessingException {
        String text = aiModelClient.chat(buildTextPrompt(request, "FULL"));
        log.info("AI activity generation raw text length={}, content={}", text == null ? 0 : text.length(), text);
        ActivityAiGenerateVO vo = parseJson(text, true);
        if (blank(vo.getCoverUrl())) {
            vo.setCoverUrl(generateCover(buildImagePrompt(vo, request.getPrompt())));
        }
        return vo;
    }

    private ActivityAiGenerateVO generatePartial(ActivityAiGenerateRequest request, String mode) throws JsonProcessingException {
        String text = aiModelClient.chat(buildTextPrompt(request, mode));
        log.info("AI activity generation raw text length={}, content={}", text == null ? 0 : text.length(), text);
        return parseJson(text, false);
    }

    private ActivityAiGenerateVO generateCoverOnly(ActivityAiGenerateRequest request) {
        ActivityAiGenerateVO vo = new ActivityAiGenerateVO();
        vo.setCoverUrl(generateCover(buildImagePrompt(fromRequest(request), request.getPrompt())));
        return vo;
    }

    @SuppressWarnings("unchecked")
    private String generateCover(String imagePrompt) {
        String apiKey = blank(aiProperties.getImageApiKey()) && "gemini".equalsIgnoreCase(aiProperties.getProvider())
                ? aiProperties.getApiKey()
                : aiProperties.getImageApiKey();
        if (blank(apiKey)) {
            throw new BizException("AI图片服务未配置");
        }
        String baseUrl = blank(aiProperties.getImageBaseUrl()) ? GEMINI_BASE_URL : aiProperties.getImageBaseUrl();
        String model = blank(aiProperties.getImageModel()) ? IMAGEN_MODEL : aiProperties.getImageModel();
        String url = trimTrailingSlash(baseUrl) + "/v1beta/models/" + model + ":predict";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);
        Map<String, Object> body = Map.of(
                "instances", List.of(Map.of("prompt", imagePrompt)),
                "parameters", Map.of(
                        "sampleCount", 1,
                        "aspectRatio", "16:9",
                        "outputOptions", Map.of("mimeType", "image/png")
                )
        );
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        Map<String, Object> responseBody = response.getBody();
        log.info("AI activity cover generation response keys={}", responseBody == null ? "null" : responseBody.keySet());
        String base64 = extractImageBase64(responseBody);
        byte[] imageBytes = Base64.getDecoder().decode(base64);
        return s3StorageService.uploadActivityCoverBytes(imageBytes, "png", MediaType.IMAGE_PNG_VALUE);
    }

    private String extractImageBase64(Map<String, Object> body) {
        if (body == null) {
            throw new BizException("AI图片生成结果为空");
        }
        Object predictionsValue = body.get("predictions");
        if (!(predictionsValue instanceof List<?> predictions) || predictions.isEmpty()) {
            throw new BizException("AI图片生成结果为空");
        }
        Object firstValue = predictions.get(0);
        if (!(firstValue instanceof Map<?, ?> first)) {
            throw new BizException("AI图片生成结果格式异常");
        }
        Object bytes = first.get("bytesBase64Encoded");
        if (bytes == null) {
            bytes = first.get("imageBytes");
        }
        if (bytes == null || bytes.toString().isBlank()) {
            throw new BizException("AI图片生成结果缺少图片数据");
        }
        return bytes.toString();
    }

    private ActivityAiGenerateVO parseJson(String text, boolean normalizeDefaults) throws JsonProcessingException {
        if (blank(text)) {
            throw new BizException("AI生成结果为空");
        }
        String json = sanitizeJson(text);
        ActivityAiGenerateVO vo = objectMapper.readValue(json, ActivityAiGenerateVO.class);
        if (normalizeDefaults) {
            normalize(vo);
        } else if (vo.getSkills() == null) {
            vo.setSkills(List.of());
        }
        return vo;
    }

    private void normalize(ActivityAiGenerateVO vo) {
        if (vo.getRecruitCount() == null || vo.getRecruitCount() <= 0) {
            vo.setRecruitCount(20);
        }
        if (vo.getServiceHours() == null || vo.getServiceHours() <= 0) {
            vo.setServiceHours(3.0);
        }
        if (vo.getSkills() == null) {
            vo.setSkills(List.of());
        }
    }

    private String sanitizeJson(String text) {
        String value = text.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return value.substring(start, end + 1);
        }
        return value;
    }

    private String buildTextPrompt(ActivityAiGenerateRequest request, String mode) throws JsonProcessingException {
        return """
                你是校园志愿服务平台的活动策划助手。请基于管理员输入和当前表单内容生成活动发布表单 JSON。

                管理员输入：
                %s

                当前表单内容：
                %s

                生成模式：%s

                要求：
                1. 内容必须符合校园志愿活动场景，安全合规，不生成违规内容。
                2. 内容自然真实，活动简介不要过短，报名要求和注意事项要具体。
                3. 技能标签尽量合理，招募人数和服务时长要符合活动规模。
                4. 不允许编造真实不存在的学校、机构、具体政策或联系人。
                5. 输出必须是 JSON 对象，不要 markdown，不要解释文字。
                6. FULL 模式输出全部字段；DESCRIPTION 只优化 description；SKILLS 只推荐 skills。
                7. JSON 字段固定为：
                {
                  "title": "活动标题",
                  "category": "活动类型",
                  "description": "活动简介",
                  "requirements": "报名要求",
                  "skills": ["宣传", "摄影", "活动组织"],
                  "recruitCount": 20,
                  "serviceHours": 3,
                  "tips": "活动注意事项",
                  "promotionText": "活动宣传文案",
                  "coverUrl": ""
                }
                """.formatted(request.getPrompt(), objectMapper.writeValueAsString(request), mode);
    }

    private String buildImagePrompt(ActivityAiGenerateVO vo, String userPrompt) {
        return """
                A clean and modern campus volunteer activity cover image for WeChat mini program, 16:9 poster composition.
                Theme: %s. Activity title: %s. Category: %s. Description: %s.
                Youthful Chinese college students, public welfare atmosphere, fresh modern illustration style,
                bright natural lighting, clear focal point, suitable as a mobile app activity cover, no text, no logo.
                """.formatted(userPrompt, nullToEmpty(vo.getTitle()), nullToEmpty(vo.getCategory()), truncate(nullToEmpty(vo.getDescription()), 220));
    }

    private ActivityAiGenerateVO fromRequest(ActivityAiGenerateRequest request) {
        ActivityAiGenerateVO vo = new ActivityAiGenerateVO();
        vo.setTitle(request.getTitle());
        vo.setCategory(request.getCategory());
        vo.setDescription(request.getDescription());
        return vo;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
