package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.AiChatRequest;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.RegistrationMapper;
import com.scs.volunteer.service.AiChatService;
import com.scs.volunteer.service.AiIntent;
import com.scs.volunteer.service.AiModelClient;
import com.scs.volunteer.service.IntentRouterService;
import com.scs.volunteer.service.RagService;
import com.scs.volunteer.service.UserProfileService;
import com.scs.volunteer.vo.AiActivityCandidateVO;
import com.scs.volunteer.vo.AiChatResponseVO;
import com.scs.volunteer.vo.AiRecommendedActivityVO;
import com.scs.volunteer.vo.RagAnswerVO;
import com.scs.volunteer.vo.UserProfileVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserProfileService userProfileService;
    private final RegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;
    private final AiModelClient aiModelClient;
    private final RagService ragService;
    private final IntentRouterService intentRouterService;

    public AiChatServiceImpl(UserProfileService userProfileService, RegistrationMapper registrationMapper,
                             ActivityMapper activityMapper, AiModelClient aiModelClient, RagService ragService,
                             IntentRouterService intentRouterService) {
        this.userProfileService = userProfileService;
        this.registrationMapper = registrationMapper;
        this.activityMapper = activityMapper;
        this.aiModelClient = aiModelClient;
        this.ragService = ragService;
        this.intentRouterService = intentRouterService;
    }

    @Override
    public AiChatResponseVO chat(AiChatRequest request, CurrentUser currentUser) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BizException("请输入问题");
        }
        if (currentUser == null) {
            throw new BizException("请先登录");
        }
        String message = request.getMessage().trim();
        AiIntent intent = intentRouterService.route(message, request.getActivityId());
        AiChatResponseVO response = baseResponse(request, intent);

        return switch (intent) {
            case RULE_QA -> handleRuleQa(message, response);
            case ACTIVITY_RECOMMEND -> handleActivityRecommend(message, currentUser, response);
            case ACTIVITY_MATCH -> handleActivityMatch(message, request.getActivityId(), currentUser, response);
            case MONTHLY_REPORT -> handleMonthlyReport(message, currentUser, response);
            case ACTIVITY_SUMMARY -> handleActivitySummary(message, request.getActivityId(), response);
            case PROFILE_QUERY -> handleProfileQuery(message, currentUser, response);
            case GENERAL_CHAT -> handleGeneralChat(message, response);
        };
    }

    private AiChatResponseVO handleRuleQa(String message, AiChatResponseVO response) {
        try {
            RagAnswerVO rag = ragService.answer(message, null);
            response.setSources(rag.getSources() == null ? List.of() : rag.getSources());
            if (response.getSources().isEmpty()) {
                response.setAnswer("暂未找到明确规则依据");
            } else {
                String answer = withSources(rag);
                if (isCheckinAdjustmentQuestion(message)) {
                    answer += "\n\n如果规则允许补签，你可以在“我的活动”中找到对应活动，点击“申请补签”，填写原因并上传证明后等待管理员审核。";
                }
                response.setAnswer(answer);
            }
        } catch (Exception e) {
            log.warn("RULE_QA RAG failed: {}", e.getMessage(), e);
            response.setSources(List.of());
            response.setAnswer("暂未找到明确规则依据");
        }
        response.setRecommendations(List.of());
        return response;
    }

    private boolean isCheckinAdjustmentQuestion(String message) {
        return message.contains("补签") || message.contains("漏签") || message.contains("迟到") || message.contains("签到异常");
    }

    private AiChatResponseVO handleActivityRecommend(String message, CurrentUser currentUser, AiChatResponseVO response) {
        UserProfileVO profile = userProfileService.profile(currentUser.getId());
        List<Map<String, Object>> history = registrationMapper.aiHistory(currentUser.getId());
        List<AiActivityCandidateVO> candidates = filterCandidates(message, profile, history);
        List<AiRecommendedActivityVO> recommendations = toRecommended(candidates);
        response.setSources(List.of());
        response.setRecommendations(recommendations);
        if (candidates.isEmpty()) {
            response.setAnswer("暂时没有找到可报名且匹配度较高的活动，我不会编造平台中不存在的活动。");
            return response;
        }
        response.setAnswer(recommendationAnswer(recommendations));
        return response;
    }

    private AiChatResponseVO handleActivityMatch(String message, Long activityId, CurrentUser currentUser, AiChatResponseVO response) {
        if (activityId == null) {
            response.setAnswer("请先引用或打开一个具体活动，我才能分析它是否适合你。");
            response.setSources(List.of());
            response.setRecommendations(List.of());
            return response;
        }
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        UserProfileVO profile = userProfileService.profile(currentUser.getId());
        List<Map<String, Object>> history = registrationMapper.aiHistory(currentUser.getId());
        String prompt = """
                你是校园志愿服务活动匹配分析助手。必须基于真实用户画像、历史记录和活动详情分析，不要编造数据。
                用户问题：%s
                用户画像：%s
                历史参与活动：%s
                活动详情：%s

                请输出：匹配度、技能匹配、时间匹配、经验匹配、是否推荐报名、注意事项。语言简洁自然。
                """.formatted(message, profileText(profile), history, activityText(activity));
        response.setSources(List.of("activity:" + activity.getId()));
        response.setRecommendations(List.of());
        response.setAnswer(chatOrFallback(prompt, "已找到活动「" + activity.getName() + "」，但 AI 暂时不可用，建议你先核对活动时间、地点和技能要求是否匹配。"));
        return response;
    }

    private AiChatResponseVO handleMonthlyReport(String message, CurrentUser currentUser, AiChatResponseVO response) {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime start = firstDay.atStartOfDay();
        LocalDateTime end = firstDay.plusMonths(1).atStartOfDay();
        Map<String, Object> stats = registrationMapper.monthlyStats(currentUser.getId(), start, end);
        List<Map<String, Object>> categoryStats = registrationMapper.monthlyCategoryStats(currentUser.getId(), start, end);
        String prompt = """
                你是校园志愿服务成长报告助手。后端已完成统计，请只基于统计数据生成自然语言月总结，不要编造活动。
                用户问题：%s
                统计月份：%s
                本月统计：%s
                活动类型分布：%s

                请概括服务情况、完成表现、签到情况、优势和下月建议，简洁自然。
                """.formatted(message, firstDay.format(DateTimeFormatter.ofPattern("yyyy-MM")), stats, categoryStats);
        response.setSources(List.of("registration", "activity_checkin"));
        response.setRecommendations(List.of());
        response.setAnswer(chatOrFallback(prompt, monthlyFallback(stats, categoryStats)));
        return response;
    }

    private AiChatResponseVO handleActivitySummary(String message, Long activityId, AiChatResponseVO response) {
        if (activityId == null) {
            response.setAnswer("请先引用或打开一个具体活动，我才能总结活动内容和注意事项。");
            response.setSources(List.of());
            response.setRecommendations(List.of());
            return response;
        }
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        String prompt = """
                你是校园志愿服务活动解读助手。请只根据活动详情总结，不要补充数据库不存在的信息。
                用户问题：%s
                活动详情：%s

                请输出：简洁活动总结、适合人群、注意事项、是否适合新手。
                """.formatted(message, activityText(activity));
        response.setSources(List.of("activity:" + activity.getId()));
        response.setRecommendations(List.of());
        response.setAnswer(chatOrFallback(prompt, "「" + activity.getName() + "」属于" + safe(activity.getCategory()) + "活动，地点在" + safe(activity.getLocation()) + "。请重点关注活动时间、报名要求和注意事项。"));
        return response;
    }

    private AiChatResponseVO handleProfileQuery(String message, CurrentUser currentUser, AiChatResponseVO response) {
        UserProfileVO profile = userProfileService.profile(currentUser.getId());
        List<Map<String, Object>> registrations = registrationMapper.my(currentUser.getId());
        String prompt = """
                你是校园志愿服务个人数据助手。请只基于给定个人画像和报名记录回答，不要编造不存在的数据。
                用户问题：%s
                用户画像：%s
                报名记录：%s

                如果数据不足，请明确说明。回答简洁自然。
                """.formatted(message, profileText(profile), registrations);
        response.setSources(List.of("registration", "volunteer_profile"));
        response.setRecommendations(List.of());
        response.setAnswer(chatOrFallback(prompt, profileFallback(profile, registrations)));
        return response;
    }

    private AiChatResponseVO handleGeneralChat(String message, AiChatResponseVO response) {
        String prompt = """
                你是校园志愿服务小程序的 AI 助手。用户当前是普通聊天或非业务问题。
                请简洁自然地回应，并可提示你能帮助查询规则、推荐活动、分析活动、生成月总结。
                用户：%s
                """.formatted(message);
        response.setSources(List.of());
        response.setRecommendations(List.of());
        response.setAnswer(chatOrFallback(prompt, "你好，我是志愿服务助手。你可以问我报名规则、活动推荐、活动匹配分析、个人志愿数据和月度总结。"));
        return response;
    }

    private AiChatResponseVO baseResponse(AiChatRequest request, AiIntent intent) {
        AiChatResponseVO response = new AiChatResponseVO();
        response.setSessionId(request.getSessionId() == null || request.getSessionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.getSessionId());
        response.setIntent(intent.name());
        response.setSources(List.of());
        response.setRecommendations(List.of());
        return response;
    }

    private String chatOrFallback(String prompt, String fallback) {
        if (!aiModelClient.available()) {
            return fallback;
        }
        try {
            String answer = aiModelClient.chat(prompt);
            return answer == null || answer.isBlank() ? fallback : answer;
        } catch (Exception e) {
            log.warn("AI model request failed: {}", e.getMessage(), e);
            return fallback;
        }
    }

    private List<AiActivityCandidateVO> filterCandidates(String message, UserProfileVO profile, List<Map<String, Object>> history) {
        List<String> userSkills = split(profile.getSkillTags());
        List<String> historyCategories = history.stream()
                .map(item -> String.valueOf(item.getOrDefault("category", "")))
                .filter(item -> !item.isBlank())
                .distinct()
                .collect(Collectors.toList());
        List<String> historyDomains = history.stream()
                .map(item -> safeText(String.valueOf(item.getOrDefault("activity_name", ""))) + " "
                        + safeText(String.valueOf(item.getOrDefault("category", ""))))
                .map(this::topicOf)
                .filter(item -> !item.isBlank())
                .distinct()
                .collect(Collectors.toList());
        ActivityRequestCriteria criteria = parseCriteria(message);
        List<AiActivityCandidateVO> matched = activityMapper.availableForAi().stream()
                .filter(activity -> matchesCriteria(activity, criteria))
                .filter(activity -> activity.getRemainingSlots() != null && activity.getRemainingSlots() > 0)
                .peek(activity -> score(activity, message, profile, userSkills, historyCategories, historyDomains, criteria))
                .sorted(Comparator.comparing(AiActivityCandidateVO::getScore).reversed()
                        .thenComparing(AiActivityCandidateVO::getStartTime))
                .collect(Collectors.toList());
        if (criteria.preferNewCategory) {
            List<AiActivityCandidateVO> freshCategories = matched.stream()
                    .filter(activity -> isNewDomain(activity, historyCategories, historyDomains))
                    .limit(10)
                    .collect(Collectors.toList());
            if (!freshCategories.isEmpty()) {
                return freshCategories;
            }
        }
        return matched.stream().limit(10).collect(Collectors.toList());
    }

    private ActivityRequestCriteria parseCriteria(String message) {
        ActivityRequestCriteria criteria = keywordCriteria(message);
        if (!aiModelClient.available()) return criteria;
        try {
            String prompt = """
                    请把用户的志愿活动推荐需求解析为结构化条件，只输出JSON，不要markdown。
                    可选topic值：COMPETITION, ENVIRONMENT, WELCOME, ACADEMIC, COMMUNITY, PROGRAMMING, LOGISTICS, GUIDE, ANY。
                    字段：
                    {"topic":"主题或ANY","keywords":["用户关心的关键词"],"excludeTopics":["明确不想要的主题"],"timePreference":"周末/工作日/上午/下午/晚上/不限","skillPreference":["技能偏好"],"reviewPreference":"AUTO/MANUAL/ANY","preferNewCategory":true/false}
                    语义说明：
                    COMPETITION 包含比赛、竞赛、赛事、赛务、运动会、评比、挑战赛等说法。
                    ENVIRONMENT 包含环保、低碳、垃圾分类、清洁、绿色校园等说法。
                    WELCOME 包含迎新、新生接待、报到引导等说法。
                    ACADEMIC 包含讲座、论坛、会议、学术活动等说法。
                    COMMUNITY 包含社区、助老、公益服务等说法。
                    PROGRAMMING 包含编程、程序设计、机房技术支持、代码等说法。
                    用户要求不用人工审核、无需审核、免审、自动通过或报名后直接通过时，reviewPreference 为 AUTO。
                    用户明确要求人工审核或需要审核的活动时，reviewPreference 为 MANUAL；没有审核偏好时为 ANY。
                    当用户表达新领域、未尝试、没参加过、换个类型、不想和以前类似时，preferNewCategory 为 true。

                    用户需求：%s
                    """.formatted(message);
            String json = sanitizeJson(aiModelClient.chat(prompt));
            Map<String, Object> parsed = parseSimpleJson(json);
            String topic = text(parsed.get("topic")).toUpperCase();
            if (criteria.topic == null && !topic.isBlank() && !"ANY".equals(topic)) criteria.topic = topic;
            criteria.keywords.addAll(listValue(parsed.get("keywords")));
            criteria.excludeTopics.addAll(listValue(parsed.get("excludeTopics")));
            String timePreference = text(parsed.get("timePreference"));
            if (!timePreference.isBlank() && !"不限".equals(timePreference)) criteria.timePreference = timePreference;
            criteria.skillPreference.addAll(listValue(parsed.get("skillPreference")));
            String reviewPreference = text(parsed.get("reviewPreference")).toUpperCase();
            if ("AUTO".equals(reviewPreference) || "MANUAL".equals(reviewPreference)) {
                criteria.reviewPreference = reviewPreference;
            }
            String preferNewCategory = text(parsed.get("preferNewCategory"));
            if (parsed.get("preferNewCategory") instanceof Boolean booleanValue) {
                criteria.preferNewCategory = booleanValue;
            } else if ("true".equalsIgnoreCase(preferNewCategory)) {
                criteria.preferNewCategory = true;
            }
        } catch (Exception e) {
            log.warn("Activity recommend criteria parse failed: {}", e.getMessage());
        }
        return criteria;
    }

    private ActivityRequestCriteria keywordCriteria(String message) {
        ActivityRequestCriteria criteria = new ActivityRequestCriteria();
        if (containsAny(message, "比赛", "竞赛", "赛事", "赛务", "运动会", "挑战赛", "评比")) criteria.topic = "COMPETITION";
        else if (containsAny(message, "环保", "环境保护", "垃圾分类", "低碳", "清洁校园", "绿色校园")) criteria.topic = "ENVIRONMENT";
        else if (containsAny(message, "迎新", "新生报到", "新生接待", "报到引导")) criteria.topic = "WELCOME";
        else if (containsAny(message, "学术", "讲座", "会议", "论坛")) criteria.topic = "ACADEMIC";
        else if (containsAny(message, "社区", "敬老", "助老", "公益")) criteria.topic = "COMMUNITY";
        else if (containsAny(message, "编程", "程序设计", "代码", "机房", "技术支持")) criteria.topic = "PROGRAMMING";
        else if (containsAny(message, "讲解", "引导", "介绍")) criteria.topic = "GUIDE";
        else if (containsAny(message, "搬运", "物资", "后勤")) criteria.topic = "LOGISTICS";
        if (containsAny(message, "新领域", "新类型", "新的类型", "没尝试", "未尝试", "没参加过", "未参加过", "没做过", "未做过",
                "不一样", "不同类型", "换个类型", "换一种", "拓展", "别和以前类似", "不要类似")) {
            criteria.preferNewCategory = true;
        }
        if (containsAny(message, "周末")) criteria.timePreference = "周末";
        else if (containsAny(message, "工作日")) criteria.timePreference = "工作日";
        else if (containsAny(message, "上午")) criteria.timePreference = "上午";
        else if (containsAny(message, "下午")) criteria.timePreference = "下午";
        else if (containsAny(message, "晚上")) criteria.timePreference = "晚上";
        if (containsAny(message, "不用人工审核", "无需人工审核", "不需要人工审核", "免审核", "免审",
                "自动通过", "直接通过", "报名就通过", "报名后直接通过")) {
            criteria.reviewPreference = "AUTO";
        } else if (containsAny(message, "人工审核的活动", "需要人工审核", "需要审核的活动")) {
            criteria.reviewPreference = "MANUAL";
        }
        return criteria;
    }

    private boolean matchesCriteria(AiActivityCandidateVO activity, ActivityRequestCriteria criteria) {
        if (criteria.topic != null && !matchesTopic(activity, criteria.topic)) return false;
        for (String excludeTopic : criteria.excludeTopics) {
            if (matchesTopic(activity, excludeTopic)) return false;
        }
        if (criteria.timePreference != null && !matchesTimePreference(activity, criteria.timePreference)) return false;
        if ("AUTO".equals(criteria.reviewPreference) && !"自动通过".equals(activity.getReviewMethod())) return false;
        if ("MANUAL".equals(criteria.reviewPreference) && "自动通过".equals(activity.getReviewMethod())) return false;
        return true;
    }

    private boolean matchesTopic(AiActivityCandidateVO activity, String topic) {
        String text = safeText(activity.getName()) + " "
                + safeText(activity.getCategory()) + " "
                + safeText(activity.getDescription()) + " "
                + safeText(activity.getSkillRequirements());
        return switch (topic) {
            case "COMPETITION" -> containsAny(text, "比赛", "竞赛", "赛事", "赛务", "运动会", "赛事保障", "挑战赛", "评比");
            case "ENVIRONMENT" -> containsAny(text, "环保", "环境保护", "垃圾分类", "低碳", "清洁校园");
            case "WELCOME" -> containsAny(text, "迎新", "新生报到", "新生接待");
            case "ACADEMIC" -> containsAny(text, "学术", "讲座", "会议", "论坛");
            case "COMMUNITY" -> containsAny(text, "社区", "敬老", "助老", "公益");
            case "PROGRAMMING" -> containsAny(text, "编程", "程序设计", "代码", "机房");
            case "GUIDE" -> containsAny(text, "讲解", "引导", "介绍", "路线");
            case "LOGISTICS" -> containsAny(text, "搬运", "物资", "后勤", "保障");
            default -> true;
        };
    }

    private String topicOf(AiActivityCandidateVO activity) {
        String text = safeText(activity.getName()) + " "
                + safeText(activity.getCategory()) + " "
                + safeText(activity.getDescription()) + " "
                + safeText(activity.getSkillRequirements());
        return topicOf(text);
    }

    private String topicOf(String text) {
        if (containsAny(text, "比赛", "竞赛", "赛事", "赛务", "运动会", "赛事保障", "挑战赛", "评比", "蓝桥杯")) return "COMPETITION";
        if (containsAny(text, "环保", "环境保护", "垃圾分类", "低碳", "清洁校园", "绿色校园")) return "ENVIRONMENT";
        if (containsAny(text, "迎新", "新生报到", "新生接待", "报到引导")) return "WELCOME";
        if (containsAny(text, "学术", "讲座", "会议", "论坛")) return "ACADEMIC";
        if (containsAny(text, "社区", "敬老", "助老", "公益")) return "COMMUNITY";
        if (containsAny(text, "编程", "程序设计", "代码", "机房", "技术支持")) return "PROGRAMMING";
        if (containsAny(text, "讲解", "引导", "介绍", "路线")) return "GUIDE";
        if (containsAny(text, "搬运", "物资", "后勤", "保障")) return "LOGISTICS";
        return "";
    }

    private boolean matchesTimePreference(AiActivityCandidateVO activity, String preference) {
        if (activity.getStartTime() == null) return true;
        return switch (preference) {
            case "周末" -> isWeekend(activity);
            case "工作日" -> !isWeekend(activity);
            case "上午" -> activity.getStartTime().getHour() < 12;
            case "下午" -> activity.getStartTime().getHour() >= 12 && activity.getStartTime().getHour() < 18;
            case "晚上" -> activity.getStartTime().getHour() >= 18;
            default -> true;
        };
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void score(AiActivityCandidateVO activity, String message, UserProfileVO profile,
                       List<String> userSkills, List<String> historyCategories, List<String> historyDomains,
                       ActivityRequestCriteria criteria) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        String skills = activity.getSkillRequirements() == null ? "" : activity.getSkillRequirements();
        for (String skill : userSkills) {
            if (!skill.isBlank() && skills.contains(skill)) {
                score += 40;
                reasons.add("技能标签包含" + skill);
                break;
            }
        }
        if (matchesAvailableTime(profile.getAvailableTime(), activity)) {
            score += 25;
            reasons.add("时间与你的可服务时间较匹配");
        }
        if (criteria.preferNewCategory) {
            if (isNewDomain(activity, historyCategories, historyDomains)) {
                score += 45;
                reasons.add("这是你未尝试过的活动类型");
            } else {
                score -= 30;
            }
        } else if (historyCategories.contains(activity.getCategory())) {
            score += 15;
            reasons.add("你曾参加过类似类型活动");
        }
        if (criteria.topic != null) {
            score += 50;
            reasons.add("活动主题符合你的需求");
        }
        if ("AUTO".equals(criteria.reviewPreference)) {
            score += 35;
            reasons.add("报名后自动通过，无需人工审核");
        } else if ("MANUAL".equals(criteria.reviewPreference)) {
            score += 20;
            reasons.add("该活动采用人工审核");
        }
        for (String skill : criteria.skillPreference) {
            if (!skill.isBlank() && skills.contains(skill)) {
                score += 20;
                reasons.add("符合你提到的" + skill + "偏好");
            }
        }
        if (message.contains("周末") && isWeekend(activity)) {
            score += 25;
            reasons.add("这是周末活动");
        }
        if (message.contains("摄影") && skills.contains("摄影")) {
            score += 30;
            reasons.add("活动需要摄影能力");
        }
        if (activity.getRemainingSlots() != null && activity.getRemainingSlots() <= 3) {
            score += 5;
        }
        activity.setScore(score);
        activity.setReason(reasons.isEmpty() ? "活动仍有名额，适合进一步查看详情" : String.join("，", reasons));
    }

    private boolean isNewDomain(AiActivityCandidateVO activity, List<String> historyCategories, List<String> historyDomains) {
        String category = activity.getCategory();
        String topic = topicOf(activity);
        boolean categoryIsNew = category == null || category.isBlank() || !historyCategories.contains(category);
        boolean topicIsNew = topic.isBlank() || !historyDomains.contains(topic);
        return categoryIsNew && topicIsNew;
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

    private Map<String, Object> parseSimpleJson(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return map;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            return map;
        }
    }

    private List<String> listValue(Object value) {
        if (!(value instanceof List<?> list)) return new ArrayList<>();
        return list.stream().map(String::valueOf).filter(item -> !item.isBlank() && !"null".equals(item)).collect(Collectors.toCollection(ArrayList::new));
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static class ActivityRequestCriteria {
        private String topic;
        private String timePreference;
        private String reviewPreference;
        private boolean preferNewCategory;
        private final List<String> keywords = new ArrayList<>();
        private final List<String> excludeTopics = new ArrayList<>();
        private final List<String> skillPreference = new ArrayList<>();
    }

    private boolean matchesAvailableTime(String availableTime, AiActivityCandidateVO activity) {
        if (availableTime == null || availableTime.isBlank() || activity.getStartTime() == null) {
            return false;
        }
        if (availableTime.contains("周末") && isWeekend(activity)) return true;
        if (availableTime.contains("工作日") && !isWeekend(activity)) return true;
        if (availableTime.contains("晚上") && activity.getStartTime().getHour() >= 18) return true;
        if (availableTime.contains("下午") && activity.getStartTime().getHour() >= 12 && activity.getStartTime().getHour() < 18) return true;
        return availableTime.contains("上午") && activity.getStartTime().getHour() < 12;
    }

    private boolean isWeekend(AiActivityCandidateVO activity) {
        DayOfWeek day = activity.getStartTime().getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private String withSources(RagAnswerVO rag) {
        String answer = rag.getAnswer() == null ? "" : rag.getAnswer();
        if (rag.getSources() == null || rag.getSources().isEmpty()) {
            return "暂未找到明确规则依据";
        }
        String sourceLine = "引用：" + String.join("；", rag.getSources());
        return answer.contains("引用：") ? answer : answer + "\n\n" + sourceLine;
    }

    private List<AiRecommendedActivityVO> toRecommended(List<AiActivityCandidateVO> candidates) {
        return candidates.stream().limit(3).map(item -> {
            AiRecommendedActivityVO vo = new AiRecommendedActivityVO();
            vo.setId(item.getId());
            vo.setTitle(item.getName());
            vo.setTime(item.getStartTime() == null ? "" : TIME_FORMAT.format(item.getStartTime()));
            vo.setLocation(item.getLocation());
            vo.setRemainingSlots(item.getRemainingSlots());
            vo.setReviewMethod(item.getReviewMethod());
            vo.setReason(item.getReason());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<String> split(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return List.of(tags.split(",")).stream().map(String::trim).filter(item -> !item.isBlank()).collect(Collectors.toList());
    }

    private String profileText(UserProfileVO profile) {
        return "学院=" + safe(profile.getCollege())
                + "，专业=" + safe(profile.getMajorClass())
                + "，技能=" + safe(profile.getSkillTags())
                + "，可服务时间=" + safe(profile.getAvailableTime())
                + "，累计时长=" + profile.getTotalHours()
                + "，服务次数=" + profile.getServiceCount()
                + "，等级=" + safe(profile.getVolunteerLevel()) + safe(profile.getLevelName())
                + "，积分=" + profile.getVolunteerPoints();
    }

    private String activityText(Activity activity) {
        return "ID=" + activity.getId()
                + "，标题=" + safe(activity.getName())
                + "，类型=" + safe(activity.getCategory())
                + "，时间=" + activity.getStartTime() + "至" + activity.getEndTime()
                + "，地点=" + safe(activity.getLocation())
                + "，技能要求=" + safe(activity.getSkillRequirements())
                + "，服务时长=" + activity.getServiceHours()
                + "，招募人数=" + activity.getRecruitNumber()
                + "，剩余名额=" + Math.max(0, intValue(activity.getRecruitNumber()) - intValue(activity.getRegisteredNumber()))
                + "，简介=" + safe(activity.getDescription())
                + "，报名要求=" + safe(activity.getSignupRequirement())
                + "，注意事项=" + safe(activity.getTips());
    }

    private String candidatesText(List<AiActivityCandidateVO> candidates) {
        return candidates.stream().map(item ->
                item.getId() + "." + item.getName() + "/" + item.getCategory() + "/" + item.getSkillRequirements()
                        + "/" + item.getStartTime() + "/" + item.getLocation() + "/审核方式=" + item.getReviewMethod()
                        + "/剩余" + item.getRemainingSlots()
                        + "/推荐理由=" + item.getReason())
                .collect(Collectors.joining("; "));
    }

    private String recommendFallback(List<AiActivityCandidateVO> candidates) {
        AiActivityCandidateVO first = candidates.get(0);
        return "根据你的技能、可服务时间和历史参与情况，优先推荐「" + first.getName() + "」。" + first.getReason() + "。";
    }

    private String recommendationAnswer(List<AiRecommendedActivityVO> recommendations) {
        StringBuilder answer = new StringBuilder("根据你的需求和平台当前可报名活动，推荐：");
        for (int i = 0; i < recommendations.size(); i++) {
            AiRecommendedActivityVO item = recommendations.get(i);
            answer.append("\n")
                    .append(i + 1)
                    .append(". 「")
                    .append(item.getTitle())
                    .append("」：")
                    .append(safe(item.getReason()));
        }
        answer.append("\n\n下方活动卡片与以上推荐一一对应，可直接查看详情。");
        return answer.toString();
    }

    private String monthlyFallback(Map<String, Object> stats, List<Map<String, Object>> categoryStats) {
        return "本月你参与了 " + number(stats.get("activityCount")) + " 个活动，已完成服务时长 "
                + number(stats.get("completedHours")) + " 小时。活动类型分布：" + categoryStats
                + "。如有未签到或补签记录，建议下次提前确认活动时间和签到范围。";
    }

    private String profileFallback(UserProfileVO profile, List<Map<String, Object>> registrations) {
        return "你的累计志愿时长为 " + profile.getTotalHours() + " 小时，服务次数为 "
                + profile.getServiceCount() + " 次，当前报名记录共 " + registrations.size() + " 条。";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未提供" : value;
    }

    private String number(Object value) {
        return value instanceof Number ? String.valueOf(value) : "0";
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }
}
