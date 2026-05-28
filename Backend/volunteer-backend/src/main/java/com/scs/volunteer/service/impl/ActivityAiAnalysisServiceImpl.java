package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.RegistrationMapper;
import com.scs.volunteer.service.ActivityAiAnalysisService;
import com.scs.volunteer.service.AiModelClient;
import com.scs.volunteer.service.UserProfileService;
import com.scs.volunteer.vo.AiActivityAnalysisVO;
import com.scs.volunteer.vo.UserProfileVO;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ActivityAiAnalysisServiceImpl implements ActivityAiAnalysisService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ActivityMapper activityMapper;
    private final UserProfileService userProfileService;
    private final RegistrationMapper registrationMapper;
    private final AiModelClient aiModelClient;

    public ActivityAiAnalysisServiceImpl(ActivityMapper activityMapper,
                                         UserProfileService userProfileService,
                                         RegistrationMapper registrationMapper,
                                         AiModelClient aiModelClient) {
        this.activityMapper = activityMapper;
        this.userProfileService = userProfileService;
        this.registrationMapper = registrationMapper;
        this.aiModelClient = aiModelClient;
    }

    @Override
    public AiActivityAnalysisVO analyze(Long activityId, CurrentUser currentUser, String question) {
        if (currentUser == null || !"VOLUNTEER".equals(currentUser.getRole())) {
            throw new BizException("仅志愿者可使用AI活动分析");
        }
        if (!aiModelClient.available()) {
            throw new BizException("Gemini API未配置，无法生成AI活动分析");
        }
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        UserProfileVO profile = userProfileService.profile(currentUser.getId());
        List<Map<String, Object>> history = registrationMapper.aiHistory(currentUser.getId());
        String analysis = aiModelClient.chat(buildPrompt(profile, history, activity, question));
        if (analysis == null || analysis.isBlank()) {
            throw new BizException("Gemini未返回有效分析结果");
        }
        return new AiActivityAnalysisVO(activityId, analysis.trim());
    }

    private String buildPrompt(UserProfileVO profile, List<Map<String, Object>> history, Activity activity, String question) {
        String historyText = history.stream()
                .limit(20)
                .map(this::historyLine)
                .collect(Collectors.joining("\n"));
        if (historyText.isBlank()) {
            historyText = "暂无历史报名记录";
        }
        String categoryPreference = history.stream()
                .map(item -> text(item.get("category")))
                .filter(item -> !item.isBlank())
                .collect(Collectors.groupingBy(item -> item, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(item -> item.getKey() + "x" + item.getValue())
                .collect(Collectors.joining("，"));
        if (categoryPreference.isBlank()) {
            categoryPreference = "暂无明显偏好";
        }

        return """
                你是学院志愿服务小程序中的AI活动分析助手。请只基于下方真实业务数据，判断该活动是否适合当前志愿者报名。

                严格要求：
                1. 必须使用自然、直接、适合小程序展示的中文表达。
                2. 不要只套固定模板，但必须覆盖：综合匹配度、技能匹配分析、时间匹配分析、历史经验匹配分析、是否推荐报名、活动注意事项、自然语言总结。
                3. 综合匹配度用百分比表示，并解释主要依据。
                4. 如果技能或时间不匹配，也要给出可执行建议。
                5. 禁止编造不存在的数据；数据缺失时明确说明“平台暂未提供”或“暂无记录”。
                6. 不要输出JSON，不要输出Markdown表格。

                当前志愿者：
                姓名/昵称：%s
                学院：%s
                专业班级：%s
                技能标签：%s
                可服务时间：%s
                累计志愿时长：%s小时
                服务次数：%s
                信用分：%s
                历史活动类型偏好：%s
                历史参与/报名活动：
                %s

                当前活动：
                活动标题：%s
                活动类型：%s
                活动时间：%s 至 %s
                活动地点：%s
                所需技能：%s
                服务时长：%s小时
                招募人数：%s
                已报名人数：%s
                活动简介：%s

                用户追问：%s

                如果“用户追问”为空，请给出面向该志愿者的完整活动适配分析。
                如果“用户追问”不为空，请仍然只基于上述真实数据回答追问，不要编造新事实。
                """.formatted(
                first(profile.getNickname(), profile.getName(), "平台暂未提供"),
                empty(profile.getCollege()),
                empty(profile.getMajorClass()),
                empty(profile.getSkillTags()),
                empty(profile.getAvailableTime()),
                profile.getTotalHours() == null ? "0" : profile.getTotalHours(),
                profile.getServiceCount() == null ? "0" : profile.getServiceCount(),
                profile.getCreditScore() == null ? "平台暂未提供" : profile.getCreditScore(),
                categoryPreference,
                historyText,
                empty(activity.getName()),
                empty(activity.getCategory()),
                activity.getStartTime() == null ? "平台暂未提供" : TIME_FORMAT.format(activity.getStartTime()),
                activity.getEndTime() == null ? "平台暂未提供" : TIME_FORMAT.format(activity.getEndTime()),
                empty(activity.getLocation()),
                empty(activity.getSkillRequirements()),
                activity.getServiceHours() == null ? "平台暂未提供" : activity.getServiceHours(),
                activity.getRecruitNumber() == null ? "平台暂未提供" : activity.getRecruitNumber(),
                activity.getRegisteredNumber() == null ? "平台暂未提供" : activity.getRegisteredNumber(),
                empty(activity.getDescription()),
                question == null || question.isBlank() ? "" : question.trim());
    }

    private String historyLine(Map<String, Object> item) {
        return "- 活动：" + empty(text(item.get("activity_name")))
                + "，类型：" + empty(text(item.get("category")))
                + "，状态：" + empty(text(item.get("status")))
                + "，服务时长：" + empty(text(item.get("service_hours")));
    }

    private String first(String primary, String secondary, String fallback) {
        if (primary != null && !primary.isBlank()) return primary;
        if (secondary != null && !secondary.isBlank()) return secondary;
        return fallback;
    }

    private String empty(String value) {
        return value == null || value.isBlank() ? "平台暂未提供" : value;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
