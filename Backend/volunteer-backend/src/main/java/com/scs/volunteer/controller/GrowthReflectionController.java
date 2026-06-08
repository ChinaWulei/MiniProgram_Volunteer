package com.scs.volunteer.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.GrowthReflectionDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.GrowthReflectionMapper;
import com.scs.volunteer.mapper.RegistrationMapper;
import com.scs.volunteer.service.AiModelClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class GrowthReflectionController extends BaseController {
    private final GrowthReflectionMapper growthMapper;
    private final RegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;
    private final AiModelClient aiModelClient;
    private final ObjectMapper objectMapper;

    public GrowthReflectionController(GrowthReflectionMapper growthMapper, RegistrationMapper registrationMapper,
                                      ActivityMapper activityMapper, AiModelClient aiModelClient, ObjectMapper objectMapper) {
        this.growthMapper = growthMapper;
        this.registrationMapper = registrationMapper;
        this.activityMapper = activityMapper;
        this.aiModelClient = aiModelClient;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/growth-reflections")
    public ApiResponse<Map<String, Long>> submit(HttpServletRequest request, @RequestBody GrowthReflectionDTO dto) {
        CurrentUser user = currentUser(request);
        if (user == null || !"VOLUNTEER".equals(user.getRole())) throw new BizException("仅志愿者可填写参与经验");
        if (dto == null || dto.getActivityId() == null) throw new BizException("活动不能为空");
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (content.isBlank()) throw new BizException("请填写参与经验");
        Activity activity = activityMapper.findById(dto.getActivityId()).orElseThrow(() -> new BizException("活动不存在"));
        if (!participantCanReflect(registrationMapper.findStatus(dto.getActivityId(), user.getId()), activity)) {
            throw new BizException("仅已完成或已通过且活动已结束的志愿者可填写参与经验");
        }
        if (growthMapper.exists(dto.getActivityId(), user.getId())) throw new BizException("该活动已填写参与经验");
        Long id = growthMapper.insert(dto.getActivityId(), user.getId(), content, Boolean.TRUE.equals(dto.getAnonymous()));
        growthMapper.saveAnalysis(id, parse(content));
        return ApiResponse.ok(Map.of("id", id));
    }

    private boolean participantCanReflect(String status, Activity activity) {
        if ("已完成".equals(status)) return true;
        if (!"已通过".equals(status)) return false;
        return "已结束".equals(activity.getStatus())
                || (activity.getEndTime() != null && !activity.getEndTime().isAfter(java.time.LocalDateTime.now()));
    }

    @GetMapping("/api/growth-reflections/my")
    public ApiResponse<List<Map<String, Object>>> my(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user == null) throw new BizException("请先登录");
        return ApiResponse.ok(growthMapper.my(user.getId()));
    }

    @GetMapping("/api/growth-profile")
    public ApiResponse<Map<String, Object>> profile(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user == null || !"VOLUNTEER".equals(user.getRole())) throw new BizException("仅志愿者可查看成长档案");
        List<Map<String, Object>> reflections = growthMapper.my(user.getId());
        Map<String, Object> result = new LinkedHashMap<>(growthMapper.profileStats(user.getId()));
        result.put("categoryStats", growthMapper.categoryStats(user.getId()));
        result.put("reflections", reflections);
        result.put("keywords", keywords(reflections));
        result.put("abilityProfile", abilityProfile(reflections));
        result.put("aiSummary", growthSummary(result, reflections));
        return ApiResponse.ok(result);
    }

    @PostMapping("/api/growth-profile/report")
    public ApiResponse<Map<String, String>> report(HttpServletRequest request) {
        Map<String, Object> profile = profile(request).getData();
        String fallback = "志愿服务参与情况：累计参与" + profile.get("activityCount") + "次活动，累计服务"
                + profile.get("totalHours") + "小时。\n能力成长分析：" + profile.get("abilityProfile")
                + "\n活动类型分析：" + profile.get("categoryStats")
                + "\n成长关键词统计：" + profile.get("keywords")
                + "\n未来成长建议：建议继续结合自身能力选择不同类型活动，持续记录真实成长感悟。";
        String report = fallback;
        if (aiModelClient.available()) {
            try {
                String prompt = """
                        请基于真实志愿服务数据和成长感悟生成个人成长报告，不得编造数据。
                        数据：%s
                        固定结构：志愿服务参与情况、能力成长分析、活动类型分析、成长关键词统计、未来成长建议。
                        风格积极、真实、可解释。
                        """.formatted(profile);
                String answer = aiModelClient.chat(prompt);
                if (answer != null && !answer.isBlank()) report = answer.trim();
            } catch (Exception ignored) {
                report = fallback;
            }
        }
        return ApiResponse.ok(Map.of("report", report));
    }

    @GetMapping("/api/activities/{activityId}/growth-experience")
    public ApiResponse<Map<String, Object>> activityExperience(@PathVariable Long activityId) {
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        List<Map<String, Object>> items = growthMapper.recommended(activityId, activity.getCategory(), 8);
        String advice = "暂无往届志愿者经验";
        if (!items.isEmpty()) {
            if (aiModelClient.available()) {
                try {
                    String answer = aiModelClient.chat("""
                            请基于以下真实成长感悟，生成“往届志愿者经验”，输出3到5条简洁建议，不得编造。
                            活动：%s，类型：%s
                            感悟：%s
                            """.formatted(activity.getName(), activity.getCategory(), items));
                    if (answer != null && !answer.isBlank()) advice = answer.trim();
                } catch (Exception ignored) {
                    advice = fallbackAdvice(items);
                }
            } else {
                advice = fallbackAdvice(items);
            }
        }
        return ApiResponse.ok(Map.of("items", items, "advice", advice));
    }

    private Map<String, String> parse(String content) {
        Map<String, String> fallback = new LinkedHashMap<>();
        fallback.put("gain", content.length() > 120 ? content.substring(0, 120) : content);
        fallback.put("ability", "无");
        fallback.put("experience", "无");
        fallback.put("advice", "无");
        if (!aiModelClient.available()) return heuristic(content, fallback);
        try {
            String answer = aiModelClient.chat("""
                    你是志愿成长感悟提炼助手。请从自然语言中提炼成长内容，未提及填“无”。只输出JSON。
                    字段固定：{"gain":"本次主要收获","ability":"获得的能力成长","experience":"学习到的经验","advice":"对后续参与者的建议"}
                    用户感悟：%s
                    """.formatted(content));
            Map<String, String> parsed = objectMapper.readValue(sanitizeJson(answer), new TypeReference<>() {});
            fallback.replaceAll((k, v) -> clean(parsed.get(k)));
        } catch (Exception ignored) {
            return heuristic(content, fallback);
        }
        return fallback;
    }

    private Map<String, String> heuristic(String content, Map<String, String> result) {
        if (content.contains("沟通")) result.put("ability", "沟通能力提升");
        if (content.contains("团队")) result.put("ability", "团队协作能力提升");
        if (content.contains("路线") || content.contains("流程") || content.contains("提前")) result.put("experience", content);
        if (content.contains("建议") || content.contains("后来") || content.contains("下次")) result.put("advice", content);
        return result;
    }

    private List<String> keywords(List<Map<String, Object>> reflections) {
        List<String> seeds = List.of("沟通能力", "团队协作能力", "组织协调能力", "责任意识", "公益服务意识");
        String all = reflections.stream().map(item -> String.valueOf(item.getOrDefault("content", ""))).collect(Collectors.joining(" "));
        return seeds.stream().filter(all::contains).collect(Collectors.toList());
    }

    private String abilityProfile(List<Map<String, Object>> reflections) {
        List<String> words = keywords(reflections);
        return words.isEmpty() ? "暂无足够成长感悟形成能力画像" : String.join("、", words);
    }

    private String growthSummary(Map<String, Object> profile, List<Map<String, Object>> reflections) {
        if (reflections.isEmpty()) return "暂无成长感悟，请完成活动后持续记录收获。";
        return "已记录" + reflections.size() + "条成长感悟，能力画像：" + profile.get("abilityProfile") + "。";
    }

    private String fallbackAdvice(List<Map<String, Object>> items) {
        String advice = items.stream().map(this::pickAdvice).filter(value -> !"无".equals(value))
                .map(value -> "- " + value)
                .limit(5).collect(Collectors.joining("\n"));
        return advice.isBlank() ? "暂无往届志愿者经验" : advice;
    }

    private String pickAdvice(Map<String, Object> item) {
        String advice = clean(value(item.get("parsedAdvice")));
        if (!"无".equals(advice)) return advice;
        String experience = clean(value(item.get("parsedExperience")));
        if (!"无".equals(experience)) return experience;
        return clean(value(item.get("content")));
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String sanitizeJson(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("```")) text = text.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return start >= 0 && end > start ? text.substring(start, end + 1) : text;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? "无" : (value.length() > 500 ? value.substring(0, 500) : value.trim());
    }
}
