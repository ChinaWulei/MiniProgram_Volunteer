package com.scs.volunteer.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityEvaluationDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.CreditMapper;
import com.scs.volunteer.mapper.EvaluationMapper;
import com.scs.volunteer.mapper.RegistrationMapper;
import com.scs.volunteer.service.AiModelClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/activities/{activityId}/evaluations")
public class EvaluationController extends BaseController {
    private final ActivityMapper activityMapper;
    private final EvaluationMapper evaluationMapper;
    private final CreditMapper creditMapper;
    private final AiModelClient aiModelClient;
    private final RegistrationMapper registrationMapper;
    private final ObjectMapper objectMapper;

    public EvaluationController(ActivityMapper activityMapper, EvaluationMapper evaluationMapper, CreditMapper creditMapper,
                                AiModelClient aiModelClient, RegistrationMapper registrationMapper,
                                ObjectMapper objectMapper) {
        this.activityMapper = activityMapper;
        this.evaluationMapper = evaluationMapper;
        this.creditMapper = creditMapper;
        this.aiModelClient = aiModelClient;
        this.registrationMapper = registrationMapper;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> submit(@PathVariable Long activityId, @RequestBody ActivityEvaluationDTO dto, HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        boolean endedByStatus = "已结束".equals(activity.getStatus());
        if (!endedByStatus && activity.getEndTime() != null && activity.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BizException("活动结束后才可评价");
        }
        validate(user, dto);
        if ("ACTIVITY".equals(dto.getTargetType()) || "LEADER".equals(dto.getTargetType())) {
            String status = registrationMapper.findStatus(activityId, user.getId());
            if (!"已完成".equals(status)) {
                throw new BizException("仅已完成该活动的志愿者可评价");
            }
        }
        if (evaluationMapper.exists(activityId, user.getId(), dto.getTargetType(), dto.getTargetUserId())) {
            throw new BizException("请勿重复评价");
        }
        Long id = evaluationMapper.insert(activityId, user.getId(), dto);
        if ("ACTIVITY".equals(dto.getTargetType()) || "LEADER".equals(dto.getTargetType())) {
            Map<String, String> parsed = parseEvaluation(dto.getContent());
            evaluationMapper.saveAnalysis(id, parsed.get("overall"), parsed.get("advantages"),
                    parsed.get("problems"), parsed.get("suggestions"), "DONE");
        }
        applyCredit(user, activityId, dto);
        return ApiResponse.ok(Map.of("id", id));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable Long activityId, HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user == null || !"ADMIN".equals(user.getRole())) throw new BizException("仅管理员可查看评价明细");
        return ApiResponse.ok(evaluationMapper.byActivity(activityId));
    }

    @GetMapping("/feedback")
    public ApiResponse<List<Map<String, Object>>> feedback(@PathVariable Long activityId, HttpServletRequest request) {
        requireAdmin(request);
        activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        return ApiResponse.ok(evaluationMapper.feedbackByActivity(activityId));
    }

    @PostMapping("/feedback/ai-summary")
    public ApiResponse<Map<String, Object>> feedbackSummary(@PathVariable Long activityId, HttpServletRequest request) {
        requireAdmin(request);
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        List<Map<String, Object>> feedback = evaluationMapper.feedbackByActivity(activityId);
        if (feedback.isEmpty()) throw new BizException("暂无志愿者评价，无法生成总结");

        double average = feedback.stream()
                .mapToDouble(item -> ((Number) item.get("score")).doubleValue())
                .average()
                .orElse(0);
        Map<Integer, Long> distribution = feedback.stream()
                .collect(Collectors.groupingBy(
                        item -> ((Number) item.get("score")).intValue(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        String fallback = fallbackSummary(activity.getName(), feedback, average);
        String summary = fallback;
        if (aiModelClient.available()) {
            String prompt = """
                    你是校园志愿服务活动复盘助手。请仅依据下面的真实评价生成中文总结，不得编造数据。
                    活动名称：%s
                    评价数量：%d
                    平均评分：%.1f/5
                    评分分布：%s
                    志愿者评价：%s

                    请按“总体评价、主要亮点、集中问题、改进建议”四部分输出，语言简洁、客观，
                    对涉及个人的信息匿名处理，不逐字重复评价，不超过500字。
                    """.formatted(activity.getName(), feedback.size(), average, distribution, feedbackText(feedback));
            try {
                String aiSummary = aiModelClient.chat(prompt);
                if (aiSummary != null && !aiSummary.isBlank()) summary = aiSummary.trim();
            } catch (Exception ignored) {
                summary = fallback;
            }
        }
        return ApiResponse.ok(Map.of(
                "count", feedback.size(),
                "averageScore", Math.round(average * 10.0) / 10.0,
                "summary", summary
        ));
    }

    private Map<String, String> parseEvaluation(String content) {
        String fallback = text(content, "");
        Map<String, String> result = new java.util.LinkedHashMap<>();
        result.put("overall", fallback.isBlank() ? "无" : fallback.length() > 120 ? fallback.substring(0, 120) : fallback);
        result.put("advantages", "无");
        result.put("problems", "无");
        result.put("suggestions", "无");
        if (!aiModelClient.available()) {
            return heuristicParse(content, result);
        }
        String prompt = """
                你是活动评价解析助手。请从用户的一段自然语言评价中提取结构化信息。
                未提及的字段必须填“无”。只输出 JSON，不要 markdown。
                字段固定为：
                {"overall":"活动评价内容","advantages":"活动优点","problems":"活动不足","suggestions":"改进建议"}

                用户评价：%s
                """.formatted(text(content, ""));
        try {
            String answer = aiModelClient.chat(prompt);
            String json = sanitizeJson(answer);
            Map<String, String> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            result.replaceAll((key, value) -> normalizeParsed(parsed.get(key)));
        } catch (Exception ignored) {
            return heuristicParse(content, result);
        }
        return result;
    }

    private Map<String, String> heuristicParse(String content, Map<String, String> result) {
        String text = text(content, "");
        if (text.contains("好") || text.contains("认真") || text.contains("顺利") || text.contains("负责")) {
            result.put("advantages", trim(text));
        }
        if (text.contains("不足") || text.contains("不好") || text.contains("不太") || text.contains("问题") || text.contains("混乱")) {
            result.put("problems", trim(text));
        }
        if (text.contains("建议") || text.contains("希望") || text.contains("下次") || text.contains("提前")) {
            result.put("suggestions", trim(text));
        }
        return result;
    }

    private String sanitizeJson(String text) {
        String value = text == null ? "" : text.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        return start >= 0 && end > start ? value.substring(start, end + 1) : value;
    }

    private String normalizeParsed(String value) {
        String text = value == null ? "" : value.trim();
        return text.isBlank() ? "无" : trim(text);
    }

    private String trim(String value) {
        return value == null ? "无" : (value.length() > 500 ? value.substring(0, 500) : value);
    }

    private void requireAdmin(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user == null || !"ADMIN".equals(user.getRole())) throw new BizException("仅管理员可查看评价");
    }

    private String feedbackText(List<Map<String, Object>> feedback) {
        return feedback.stream()
                .map(item -> item.get("score") + "分：" + text(item.get("content"), "未填写文字评价"))
                .collect(Collectors.joining("\n"));
    }

    private String fallbackSummary(String activityName, List<Map<String, Object>> feedback, double average) {
        long positive = feedback.stream().filter(item -> ((Number) item.get("score")).intValue() >= 4).count();
        long negative = feedback.stream().filter(item -> ((Number) item.get("score")).intValue() <= 2).count();
        List<String> comments = feedback.stream()
                .map(item -> text(item.get("content"), ""))
                .filter(content -> !content.isBlank())
                .limit(5)
                .toList();
        String commentText = comments.isEmpty() ? "暂无文字评价" : String.join("；", comments);
        return "总体评价：活动“" + activityName + "”共收到" + feedback.size() + "条评价，平均评分"
                + Math.round(average * 10.0) / 10.0 + "分，" + positive + "条为4至5分评价。"
                + "\n主要反馈：" + commentText
                + "\n改进建议：" + (negative > 0
                ? "存在低分反馈，建议管理员结合评价内容复盘活动组织、沟通和现场安排。"
                : "整体反馈较好，建议保留有效组织方式，并继续收集更具体的改进意见。");
    }

    private String text(Object value, String fallback) {
        if (value == null || value.toString().isBlank()) return fallback;
        return value.toString().trim();
    }

    private void validate(CurrentUser user, ActivityEvaluationDTO dto) {
        if (user == null) throw new BizException("请先登录");
        if (dto == null) throw new BizException("评价内容不能为空");
        if (dto.getScore() == null || dto.getScore() < 1 || dto.getScore() > 5) throw new BizException("评分范围为1到5分");
        String targetType = dto.getTargetType() == null ? "" : dto.getTargetType().trim().toUpperCase();
        if (!targetType.equals("ACTIVITY") && !targetType.equals("LEADER") && !targetType.equals("VOLUNTEER")) {
            throw new BizException("评价对象不正确");
        }
        dto.setTargetType(targetType);
        if ("VOLUNTEER".equals(targetType)) {
            if (!"ADMIN".equals(user.getRole())) throw new BizException("仅负责人可评价志愿者");
            if (dto.getTargetUserId() == null) throw new BizException("请选择志愿者");
        }
        if (("ACTIVITY".equals(targetType) || "LEADER".equals(targetType)) && !"VOLUNTEER".equals(user.getRole())) {
            throw new BizException("仅志愿者可评价活动或负责人");
        }
    }

    private void applyCredit(CurrentUser user, Long activityId, ActivityEvaluationDTO dto) {
        if (!"VOLUNTEER".equals(dto.getTargetType()) || dto.getTargetUserId() == null) return;
        if (dto.getScore() <= 2) {
            creditMapper.apply(dto.getTargetUserId(), -5, "负责人低分评价", "EVALUATION", activityId);
        } else if (dto.getScore() >= 5) {
            creditMapper.apply(dto.getTargetUserId(), 2, "负责人五星评价", "EVALUATION", activityId);
        }
    }
}
