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
                response.setAnswer(withSources(rag));
            }
        } catch (Exception e) {
            log.warn("RULE_QA RAG failed: {}", e.getMessage(), e);
            response.setSources(List.of());
            response.setAnswer("暂未找到明确规则依据");
        }
        response.setRecommendations(List.of());
        return response;
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
        String prompt = """
                你是校园志愿服务小程序的活动推荐助手。请只基于给定用户画像、历史报名和候选活动推荐，不要编造不存在的活动。
                用户问题：%s
                用户画像：%s
                历史参与活动：%s
                可报名活动：%s

                请输出 2-3 个推荐活动和理由，简洁自然。
                """.formatted(message, profileText(profile), history, candidatesText(candidates));
        response.setAnswer(chatOrFallback(prompt, recommendFallback(candidates)));
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
        return activityMapper.availableForAi().stream()
                .peek(activity -> score(activity, message, profile, userSkills, historyCategories))
                .filter(activity -> activity.getRemainingSlots() != null && activity.getRemainingSlots() > 0)
                .sorted(Comparator.comparing(AiActivityCandidateVO::getScore).reversed()
                        .thenComparing(AiActivityCandidateVO::getStartTime))
                .limit(10)
                .collect(Collectors.toList());
    }

    private void score(AiActivityCandidateVO activity, String message, UserProfileVO profile,
                       List<String> userSkills, List<String> historyCategories) {
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
        if (historyCategories.contains(activity.getCategory())) {
            score += 15;
            reasons.add("你曾参加过类似类型活动");
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
                        + "/" + item.getStartTime() + "/" + item.getLocation() + "/剩余" + item.getRemainingSlots()
                        + "/推荐理由=" + item.getReason())
                .collect(Collectors.joining("; "));
    }

    private String recommendFallback(List<AiActivityCandidateVO> candidates) {
        AiActivityCandidateVO first = candidates.get(0);
        return "根据你的技能、可服务时间和历史参与情况，优先推荐「" + first.getName() + "」。" + first.getReason() + "。";
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
