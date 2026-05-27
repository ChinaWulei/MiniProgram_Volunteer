package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.AiChatRequest;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.RegistrationMapper;
import com.scs.volunteer.service.AiChatService;
import com.scs.volunteer.service.AiModelClient;
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

    public AiChatServiceImpl(UserProfileService userProfileService, RegistrationMapper registrationMapper,
                             ActivityMapper activityMapper, AiModelClient aiModelClient, RagService ragService) {
        this.userProfileService = userProfileService;
        this.registrationMapper = registrationMapper;
        this.activityMapper = activityMapper;
        this.aiModelClient = aiModelClient;
        this.ragService = ragService;
    }

    @Override
    public AiChatResponseVO chat(AiChatRequest request, CurrentUser currentUser) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BizException("请输入问题");
        }
        String message = request.getMessage().trim();
        Long userId = currentUser.getId();
        UserProfileVO profile = userProfileService.profile(userId);
        List<Map<String, Object>> history = registrationMapper.aiHistory(userId);
        List<AiActivityCandidateVO> candidates = filterCandidates(message, profile, history);
        String prompt = buildPrompt(message, profile, history, candidates);
        boolean recommendIntent = hasRecommendIntent(message);

        AiChatResponseVO response = new AiChatResponseVO();
        response.setSessionId(request.getSessionId() == null || request.getSessionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.getSessionId());
        if (hasRuleIntent(message)) {
            try {
                RagAnswerVO rag = ragService.answer(message, null);
                response.setReply(withSources(rag));
                response.setRecommendedActivities(List.of());
                return response;
            } catch (Exception e) {
                log.warn("RAG answer failed, fallback to normal AI chat: {}", e.getMessage());
            }
        }
        response.setRecommendedActivities(recommendIntent ? toRecommended(candidates) : List.of());
        String reply = "";
        if (aiModelClient.available()) {
            try {
                reply = aiModelClient.chat(prompt);
            } catch (Exception e) {
                log.warn("AI model request failed, fallback to local reply: {}", e.getMessage());
            }
        }
        response.setReply(reply == null || reply.isBlank() ? mockReply(message, profile, candidates) : reply);
        return response;
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
                reasons.add("你的技能标签包含" + skill);
                break;
            }
        }
        if (matchesAvailableTime(profile.getAvailableTime(), activity)) {
            score += 25;
            reasons.add("活动时间与你的可服务时间较匹配");
        }
        if (historyCategories.contains(activity.getCategory())) {
            score += 15;
            reasons.add("你曾报名过类似类型活动");
        }
        if (message.contains("周末") && isWeekend(activity)) {
            score += 25;
            reasons.add("这是周末活动");
        }
        if (message.contains("摄影") && skills.contains("摄影")) {
            score += 30;
            reasons.add("该活动需要摄影能力");
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
        if (availableTime.contains("周末") && isWeekend(activity)) {
            return true;
        }
        if (availableTime.contains("工作日") && !isWeekend(activity)) {
            return true;
        }
        if (availableTime.contains("晚上") && activity.getStartTime().getHour() >= 18) {
            return true;
        }
        if (availableTime.contains("下午") && activity.getStartTime().getHour() >= 12 && activity.getStartTime().getHour() < 18) {
            return true;
        }
        if (availableTime.contains("上午") && activity.getStartTime().getHour() < 12) {
            return true;
        }
        return false;
    }

    private boolean isWeekend(AiActivityCandidateVO activity) {
        DayOfWeek day = activity.getStartTime().getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private String buildPrompt(String message, UserProfileVO profile, List<Map<String, Object>> history, List<AiActivityCandidateVO> candidates) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("系统角色：你是校园志愿服务小程序的智能助手。")
                .append("你必须先判断用户意图：")
                .append("如果用户只是问候、闲聊或表达感谢，应自然简短回应，不要主动推荐活动；")
                .append("只有当用户明确表达想找活动、推荐活动、报名、参加志愿服务、查询适合自己的活动时，才基于候选活动列表推荐；")
                .append("如果用户询问积分、等级、报名记录等平台信息，应基于用户画像和历史报名记录回答；")
                .append("不要编造平台没有提供的信息。")
                .append("\n");
        prompt.append("用户问题：").append(message).append("\n");
        prompt.append("用户画像：昵称=").append(profile.getNickname())
                .append("，学院=").append(profile.getCollege())
                .append("，专业班级=").append(profile.getMajorClass())
                .append("，技能标签=").append(profile.getSkillTags())
                .append("，可服务时间=").append(profile.getAvailableTime())
                .append("，累计服务时长=").append(profile.getTotalHours())
                .append("，志愿等级=").append(profile.getVolunteerLevel()).append(profile.getLevelName())
                .append("，志愿积分=").append(profile.getVolunteerPoints()).append("\n");
        prompt.append("历史报名记录：").append(history).append("\n");
        prompt.append("候选活动列表：").append(candidates.stream().map(item ->
                item.getId() + "." + item.getName() + "/" + item.getCategory() + "/" + item.getSkillRequirements()
                        + "/" + item.getStartTime() + "/" + item.getLocation() + "/剩余" + item.getRemainingSlots())
                .collect(Collectors.joining("; "))).append("\n");
        prompt.append("回答规则：不要在用户没有表达活动推荐意图时主动推荐活动；")
                .append("需要推荐活动时，只能基于候选活动列表推荐，不能编造数据库中不存在的活动；")
                .append("没有合适活动时说明暂时没有找到合适活动；")
                .append("推荐活动时必须说明理由；")
                .append("回答简洁自然，适合小程序展示；")
                .append("平台数据未提供时说明暂时无法确认。");
        return prompt.toString();
    }

    private String mockReply(String message, UserProfileVO profile, List<AiActivityCandidateVO> candidates) {
        if (!hasRecommendIntent(message) && isGreetingOrSmallTalk(message)) {
            return "你好，我是志愿服务助手。你可以问我活动推荐、报名记录、积分等级等问题。";
        }
        if (message.contains("积分") || message.contains("等级")) {
            return "你的当前等级是" + profile.getVolunteerLevel() + " " + profile.getLevelName()
                    + "，志愿积分为" + profile.getVolunteerPoints()
                    + "。提升积分的核心方式是完成已报名活动并通过管理员确认服务时长。";
        }
        if (candidates.isEmpty()) {
            return "暂时没有找到合适活动。我只会基于平台当前可报名活动回答，不会编造不存在的活动。";
        }
        AiActivityCandidateVO first = candidates.get(0);
        return "根据你的技能标签、可服务时间和历史报名记录，我优先推荐「" + first.getName()
                + "」。" + first.getReason() + "。你可以点击下方活动卡片查看详情。";
    }

    private boolean hasRecommendIntent(String message) {
        return message.contains("推荐")
                || message.contains("活动")
                || message.contains("报名")
                || message.contains("志愿")
                || message.contains("周末")
                || message.contains("适合")
                || message.contains("参加")
                || message.contains("服务");
    }

    private boolean hasRuleIntent(String message) {
        return message.contains("规则")
                || message.contains("制度")
                || message.contains("公告")
                || message.contains("附件")
                || message.contains("要求")
                || message.contains("流程")
                || message.contains("规定")
                || message.contains("办法")
                || message.contains("通知")
                || message.contains("文件");
    }

    private String withSources(RagAnswerVO rag) {
        String answer = rag.getAnswer() == null ? "" : rag.getAnswer();
        if (rag.getSources() == null || rag.getSources().isEmpty()) {
            return answer;
        }
        String sourceLine = "引用：" + String.join("；", rag.getSources());
        return answer.contains("引用：") ? answer : answer + "\n\n" + sourceLine;
    }

    private boolean isGreetingOrSmallTalk(String message) {
        String normalized = message.toLowerCase();
        return normalized.equals("你好")
                || normalized.equals("您好")
                || normalized.equals("嗨")
                || normalized.equals("hi")
                || normalized.equals("hello")
                || normalized.equals("在吗")
                || normalized.equals("谢谢")
                || normalized.equals("感谢");
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
}
